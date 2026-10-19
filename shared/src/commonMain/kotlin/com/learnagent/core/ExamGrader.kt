package com.learnagent.core

import com.learnagent.data.QuestionType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlin.math.roundToInt

data class ExamGrade(
    val questionId: String,
    val score: Int,
    val maxScore: Int,
    val ratio: Float,
    val feedback: String,
    val source: String
)

data class ExamResult(
    val totalScore: Int,
    val maxScore: Int,
    val grades: List<ExamGrade>
)

/**
 * 批改引擎：
 * - 客观题：精确匹配（忽略大小写/空格；多选顺序无关）
 * - 主观题：LLM 严格评分（完全对 100% / 小错 70% / 半对 50% / 方向错 0% / 不规范额外扣 20%）
 * - LLM 不可用 → 关键词命中比例降级，feedback 标注"本地启发式"
 */
class ExamGrader(private val llm: LlmClient?) {

    fun gradeObjective(question: ExamQuestion, userAnswer: String): ExamGrade {
        val ratio = when (question.type) {
            QuestionType.MULTIPLE_CHOICE ->
                if (normalizeChoice(question.correctAnswer) == normalizeChoice(userAnswer)) 1f else 0f
            else ->
                if (normalizeText(question.correctAnswer) == normalizeText(userAnswer)) 1f else 0f
        }
        val score = (question.score * ratio).roundToInt()
        return ExamGrade(
            question.id, score, question.score, ratio,
            if (ratio >= 1f) "正确。" else "不正确，正确答案：${question.correctAnswer}",
            "精确匹配"
        )
    }

    suspend fun gradeSubjective(question: ExamQuestion, userAnswer: String): ExamGrade {
        if (llm != null) {
            try {
                return llmGrade(question, userAnswer)
            } catch (e: Exception) {
                // 降级
            }
        }
        return localSubjective(question, userAnswer)
    }

    suspend fun grade(exam: Exam, answers: Map<String, String>): ExamResult {
        val grades = exam.questions.map { q ->
            when (q.type) {
                QuestionType.SINGLE_CHOICE, QuestionType.MULTIPLE_CHOICE, QuestionType.FILL_BLANK ->
                    gradeObjective(q, answers[q.id].orEmpty())
                else -> gradeSubjective(q, answers[q.id].orEmpty())
            }
        }
        return ExamResult(grades.sumOf { it.score }, grades.sumOf { it.maxScore }, grades)
    }

    private fun localSubjective(question: ExamQuestion, userAnswer: String): ExamGrade {
        val tokens = question.correctAnswer
            .split(Regex("[^\\u4e00-\\u9fa5A-Za-z0-9]+"))
            .filter { it.length >= 2 }
        val hit = tokens.count { userAnswer.contains(it, ignoreCase = true) }
        var ratio = when {
            tokens.isEmpty() -> 0f
            hit.toFloat() / tokens.size >= 0.9f -> 1f
            hit.toFloat() / tokens.size >= 0.6f -> 0.7f
            hit.toFloat() / tokens.size >= 0.3f -> 0.5f
            else -> 0f
        }
        val hasStandardForm = userAnswer.contains("解：") || userAnswer.contains("答：") ||
            Regex("(?m)^\\s*\\d+[.、]").containsMatchIn(userAnswer)
        if (ratio >= 0.7f && !hasStandardForm) ratio *= 0.8f   // 不规范额外扣 20%
        val score = (question.score * ratio).roundToInt()
        return ExamGrade(
            question.id, score, question.score, ratio,
            "本地启发式：命中 $hit/${tokens.size} 个关键点" +
                if (ratio >= 0.7f && !hasStandardForm) "（无解题格式，扣 20%）" else "",
            "本地启发式"
        )
    }

    private suspend fun llmGrade(question: ExamQuestion, userAnswer: String): ExamGrade {
        val client = llm ?: throw IllegalStateException("no llm")
        val prompt = """
严格批改主观题，只输出 JSON：
{"ratio":0.7,"feedback":"..."}

评分规则：完全对 1.0 / 小错 0.7 / 半对 0.5 / 方向错 0 / 不规范额外扣 20%（ratio × 0.8）

题目：${question.stem}
标准答案：${question.correctAnswer}
学生答案：$userAnswer
""".trimIndent()
        val raw = client.generateResponse(prompt, LlmConfig("nvidia", "", "", ""))
        val s = raw.indexOf('{')
        val e = raw.lastIndexOf('}')
        if (s < 0 || e <= s) throw IllegalArgumentException("无 JSON")
        val obj = Json { ignoreUnknownKeys = true; isLenient = true }
            .parseToJsonElement(raw.substring(s, e + 1)).jsonObject
        val ratio = ((obj["ratio"] as? JsonPrimitive)?.contentOrNull?.toFloatOrNull()
            ?: 0f).coerceIn(0f, 1f)
        val feedback = (obj["feedback"] as? JsonPrimitive)?.contentOrNull.orEmpty()
        return ExamGrade(question.id, (question.score * ratio).roundToInt(), question.score, ratio, feedback, "LLM")
    }

    private fun normalizeText(s: String): String = s.trim().lowercase().replace(Regex("\\s+"), "")

    private fun normalizeChoice(s: String): String =
        s.uppercase().filter { it.isLetterOrDigit() }.toCharArray().sorted().joinToString("")
}
