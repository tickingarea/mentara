package com.learnagent.core

import com.learnagent.data.AnswerStandard
import com.learnagent.data.AnswerStep
import com.learnagent.data.QuestionType
import com.learnagent.data.ScoringPoint
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

data class StandardGradingResult(
    val score: Int,                      // 实际得分
    val maxScore: Int,
    val missingRequired: List<AnswerStep>,
    val extraOptional: List<AnswerStep>,
    val forbidden: List<String>,
    val orderCorrect: Boolean,
    val feedback: String
)

/**
 * 规范答题老师：
 * - analyzeStandardAnswer：LLM 解析标准答案 → AnswerStandard；失败按学科内置库兜底
 * - gradeWithStandard：关键词匹配为基线；LLM 可用时做语义评分
 * - LLM 不可用 → 只做关键词匹配，不判断顺序，feedback 标注"本地启发式"
 */
class AnswerStandardTeacher(private val llm: LlmClient?) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun analyzeStandardAnswer(question: String, standardAnswer: String, subject: String): AnswerStandard {
        if (llm != null) {
            try {
                return parseStandard(subject, callLlm(question, standardAnswer, subject))
            } catch (e: Exception) {
                // 降级
            }
        }
        return fallbackStandard(standardAnswer, subject)
    }

    suspend fun gradeWithStandard(standard: AnswerStandard, userAnswer: String): StandardGradingResult {
        val keyword = keywordGrade(standard, userAnswer)
        if (llm == null) {
            return keyword.copy(feedback = "本地启发式：${keyword.feedback}")
        }
        return try {
            llmGrade(standard, userAnswer, keyword)
        } catch (e: Exception) {
            keyword.copy(feedback = "本地启发式：${keyword.feedback}")
        }
    }

    // ---------- 关键词基线 ----------

    fun keywordGrade(standard: AnswerStandard, userAnswer: String): StandardGradingResult {
        val missing = standard.requiredSteps.filter { matchPosition(it.content, userAnswer) < 0 }
        val optionalHit = standard.optionalSteps.filter { matchPosition(it.content, userAnswer) >= 0 }
        val forbidden = standard.forbiddenSteps.filter { userAnswer.contains(it) }

        val points = standard.scoringPoints
        val maxScore = if (points.isNotEmpty()) points.sumOf { it.score }
        else standard.requiredSteps.sumOf { it.scoreIfMissing }.coerceAtLeast(1)
        val earned = if (points.isNotEmpty()) {
            points.filter { it.mustAppear && matchPosition(it.point, userAnswer) >= 0 }.sumOf { it.score }
        } else {
            maxScore - missing.sumOf { it.scoreIfMissing }
        }
        val score = earned.coerceIn(0, maxScore)

        // 顺序检查：命中步骤的首个位置应单调不减
        val positions = standard.requiredSteps
            .map { matchPosition(it.content, userAnswer) }
            .filter { it >= 0 }
        val orderCorrect = positions.zipWithNext().all { (a, b) -> a <= b }

        val feedback = buildString {
            append("得分 $score/$maxScore。")
            if (missing.isNotEmpty()) append("缺失步骤：").append(missing.joinToString("；") { it.content }).append("。")
            if (forbidden.isNotEmpty()) append("出现禁止项：").append(forbidden.joinToString("；")).append("。")
            if (!orderCorrect) append("步骤顺序有误。")
            if (missing.isEmpty() && forbidden.isEmpty() && orderCorrect) append("步骤完整，顺序正确。")
        }
        return StandardGradingResult(score, maxScore, missing, optionalHit, forbidden, orderCorrect, feedback)
    }

    /** 步骤文本在答案中的命中位置（整串优先，2-gram 兜底）。 */
    fun matchPosition(stepContent: String, userAnswer: String): Int {
        val cleaned = clean(stepContent)
        if (cleaned.isBlank()) return -1
        if (userAnswer.contains(cleaned)) return userAnswer.indexOf(cleaned)
        return grams(cleaned).firstNotNullOfOrNull { g -> userAnswer.indexOf(g).takeIf { it >= 0 } } ?: -1
    }

    private fun clean(s: String): String = s.replace(Regex("[^\\u4e00-\\u9fa5A-Za-z0-9]"), "")

    private fun grams(cleaned: String): List<String> {
        if (cleaned.length <= 2) return listOf(cleaned).filter { it.isNotBlank() }
        return (0..cleaned.length - 2).map { cleaned.substring(it, it + 2) }
    }

    // ---------- LLM ----------

    private suspend fun callLlm(question: String, standardAnswer: String, subject: String): String {
        val client = llm ?: return ""
        val prompt = """
你是阅卷组长。把标准答案解析成结构化答题规范，只输出 JSON：
{"questionType":"SHORT_ANSWER","requiredSteps":[{"order":1,"content":"...","reason":"...","scoreIfMissing":2}],
 "optionalSteps":[],"forbiddenSteps":[],"scoringPoints":[{"point":"...","score":2,"mustAppear":true}],
 "standardTemplate":"..."}

学科：$subject
题目：$question
标准答案：$standardAnswer
""".trimIndent()
        return client.generateResponse(prompt, LlmConfig("nvidia", "", "", ""))
    }

    private fun parseStandard(subject: String, raw: String): AnswerStandard {
        val s = raw.indexOf('{')
        val e = raw.lastIndexOf('}')
        if (s < 0 || e <= s) throw IllegalArgumentException("无 JSON")
        val o = json.parseToJsonElement(raw.substring(s, e + 1)).jsonObject
        fun str(key: String, def: String = ""): String = (o[key] as? JsonPrimitive)?.contentOrNull ?: def
        val steps = (o["requiredSteps"] as? JsonArray)?.mapNotNull { el ->
            val obj = el as? kotlinx.serialization.json.JsonObject ?: return@mapNotNull null
            val content = (obj["content"] as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
            AnswerStep(
                order = (obj["order"] as? JsonPrimitive)?.contentOrNull?.toIntOrNull() ?: 0,
                content = content,
                reason = (obj["reason"] as? JsonPrimitive)?.contentOrNull.orEmpty(),
                scoreIfMissing = (obj["scoreIfMissing"] as? JsonPrimitive)?.contentOrNull?.toIntOrNull() ?: 1
            )
        } ?: emptyList()
        val points = (o["scoringPoints"] as? JsonArray)?.mapNotNull { el ->
            val obj = el as? kotlinx.serialization.json.JsonObject ?: return@mapNotNull null
            val point = (obj["point"] as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
            ScoringPoint(
                point = point,
                score = (obj["score"] as? JsonPrimitive)?.contentOrNull?.toIntOrNull() ?: 1,
                mustAppear = (obj["mustAppear"] as? JsonPrimitive)?.contentOrNull?.toBooleanStrictOrNull() ?: true
            )
        } ?: emptyList()
        val type = runCatching { QuestionType.valueOf(str("questionType", "SHORT_ANSWER").uppercase()) }
            .getOrDefault(QuestionType.SHORT_ANSWER)
        return AnswerStandard(
            id = "std_" + subject,
            questionType = type,
            subject = subject,
            requiredSteps = steps,
            scoringPoints = points,
            standardTemplate = str("standardTemplate")
        )
    }

    private suspend fun llmGrade(
        standard: AnswerStandard,
        userAnswer: String,
        baseline: StandardGradingResult
    ): StandardGradingResult {
        val client = llm ?: return baseline
        val standardText = buildString {
            append("解题步骤：").append(standard.requiredSteps.joinToString("；") { "${it.order}.${it.content}" }).append("\n")
            if (standard.forbiddenSteps.isNotEmpty()) append("禁止：").append(standard.forbiddenSteps.joinToString("；")).append("\n")
            if (standard.scoringPoints.isNotEmpty()) append("采分点：").append(standard.scoringPoints.joinToString("；") { "${it.point}(${it.score}分)" })
        }
        val prompt = """
严格按规范评分，只输出 JSON：
{"score":0,"maxScore":${baseline.maxScore},"missingRequired":["步骤"],"forbidden":["..."],"orderCorrect":true,"feedback":"..."}

规范：
$standardText

学生答案：$userAnswer
""".trimIndent()
        val raw = client.generateResponse(prompt, LlmConfig("nvidia", "", "", ""))
        val s = raw.indexOf('{')
        val e = raw.lastIndexOf('}')
        if (s < 0 || e <= s) throw IllegalArgumentException("无 JSON")
        val o = json.parseToJsonElement(raw.substring(s, e + 1)).jsonObject
        fun str(key: String, def: String = ""): String = (o[key] as? JsonPrimitive)?.contentOrNull ?: def
        val missingTexts = (o["missingRequired"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull } ?: emptyList()
        val missing = if (missingTexts.isEmpty()) baseline.missingRequired
        else standard.requiredSteps.filter { st -> missingTexts.any { t -> st.content.contains(t) || t.contains(st.content) } }
        val forbidden = (o["forbidden"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull } ?: baseline.forbidden
        val orderCorrect = (o["orderCorrect"] as? JsonPrimitive)?.contentOrNull?.toBooleanStrictOrNull() ?: baseline.orderCorrect
        return baseline.copy(
            score = str("score").toIntOrNull()?.coerceIn(0, baseline.maxScore) ?: baseline.score,
            missingRequired = missing,
            forbidden = forbidden,
            orderCorrect = orderCorrect,
            feedback = str("feedback", baseline.feedback)
        )
    }

    private fun fallbackStandard(standardAnswer: String, subject: String): AnswerStandard {
        val lib = AnswerStandardLibrary.get(subject, QuestionType.SHORT_ANSWER)
            ?: AnswerStandardLibrary.all.firstOrNull { it.subject == subject }
        if (lib != null) return lib.copy(standardTemplate = standardAnswer)
        val points = standardAnswer.split(Regex("[。；;\\n]+"))
            .map { it.trim() }
            .filter { it.length >= 3 }
            .take(5)
            .mapIndexed { i, text -> ScoringPoint("步骤${i + 1}：${text.take(20)}", 1, true) }
        return AnswerStandard(
            id = "std_generic",
            questionType = QuestionType.SHORT_ANSWER,
            subject = subject,
            scoringPoints = points,
            standardTemplate = standardAnswer
        )
    }
}
