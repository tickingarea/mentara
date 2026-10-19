package com.learnagent.core

import com.learnagent.data.KnowledgePoint
import com.learnagent.data.QuestionType
import com.learnagent.platform.currentTimeMillis
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlin.math.roundToInt

enum class ExamScope { SINGLE_VIDEO, MULTI_VIDEO, KNOWLEDGE_POINT, WEAK_POINTS }

data class ExamQuestion(
    val id: String,
    val type: QuestionType,
    val stem: String,
    val options: List<String> = emptyList(),
    val correctAnswer: String,
    val explanation: String = "",
    val knowledgePointId: String = "",
    val score: Int = 5,
    val subject: String = ""
)

data class Exam(
    val id: String,
    val scope: ExamScope,
    val difficulty: Difficulty,
    val questions: List<ExamQuestion>,
    val generatedAt: Long,
    val source: String
)

/**
 * 出卷引擎：
 * - 题型配比 单选40% / 多选15% / 填空15% / 简答20% / 计算证明10%
 * - 知识点不足 → 有几题出几题（不崩）
 * - LLM 不可用 → 模板生成（单选选项由同池其它定义充当干扰项）
 */
class ExamGenerator(
    private val llm: LlmClient?,
    private val points: List<KnowledgePoint> = emptyList()
) {

    suspend fun generate(
        scope: ExamScope,
        videoIds: List<String>,
        questionCount: Int,
        difficulty: Difficulty,
        interleaver: InterleavingEngine
    ): Exam {
        val pool = if (videoIds.isEmpty()) points else points.filter { it.sourceVideoId in videoIds }.ifEmpty { points }
        val selected = interleaver.interleave(pool, questionCount)
        val types = allocateTypes(selected.size)
        val examId = "exam_" + currentTimeMillis().toString(16)
        var usedLlm = false
        val questions = selected.mapIndexed { i, point ->
            val type = types.getOrElse(i) { QuestionType.SHORT_ANSWER }
            val llmQuestion = if (llm != null) {
                runCatching { llmQuestion(point, type, difficulty, examId, i, pool) }.getOrNull()
            } else null
            if (llmQuestion != null) {
                usedLlm = true
                llmQuestion
            } else {
                templateQuestion(point, type, difficulty, pool, i, examId)
            }
        }
        return Exam(examId, scope, difficulty, questions, currentTimeMillis(), if (usedLlm) "LLM" else "本地启发式")
    }

    /** 按配比分配题型；总数恒等于 n。 */
    fun allocateTypes(n: Int): List<QuestionType> {
        if (n <= 0) return emptyList()
        val single = (n * 0.40f).roundToInt()
        val multiple = (n * 0.15f).roundToInt()
        val fill = (n * 0.15f).roundToInt()
        val short = (n * 0.20f).roundToInt()
        val calc = (n - single - multiple - fill - short).coerceAtLeast(0)
        return buildList {
            repeat(single) { add(QuestionType.SINGLE_CHOICE) }
            repeat(multiple) { add(QuestionType.MULTIPLE_CHOICE) }
            repeat(fill) { add(QuestionType.FILL_BLANK) }
            repeat(short) { add(QuestionType.SHORT_ANSWER) }
            repeat(calc) { add(QuestionType.CALCULATION) }
        }.take(n)
    }

    private fun templateQuestion(
        point: KnowledgePoint,
        type: QuestionType,
        difficulty: Difficulty,
        pool: List<KnowledgePoint>,
        index: Int,
        examId: String
    ): ExamQuestion {
        val qid = "${examId}_q$index"
        val score = scoreFor(type, difficulty)
        return when (type) {
            QuestionType.SINGLE_CHOICE -> {
                val distractors = pool.filter { it.id != point.id }
                    .map { it.definition.take(40) }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .take(3)
                val raw = listOf(point.definition) + distractors
                val rotate = if (raw.isEmpty()) 0 else index % raw.size
                val options = raw.drop(rotate) + raw.take(rotate)
                val correctIdx = options.indexOf(point.definition).coerceAtLeast(0)
                ExamQuestion(
                    id = qid, type = type,
                    stem = "关于「${point.name}」，下列说法正确的是？",
                    options = options,
                    correctAnswer = ('A' + correctIdx).toString(),
                    explanation = "正确项即标准定义：${point.definition}",
                    knowledgePointId = point.id, score = score
                )
            }
            QuestionType.MULTIPLE_CHOICE -> {
                val correct = listOf(
                    point.definition.take(40),
                    "${point.name} 属于本课程知识点"
                ).distinct()
                val others = pool.filter { it.id != point.id }.map { it.definition.take(30) }.take(2)
                val options = correct + others
                val letters = correct.map { c -> ('A' + options.indexOf(c)).toString() }.sorted()
                ExamQuestion(
                    id = qid, type = type,
                    stem = "关于「${point.name}」，下列说法正确的有？（多选）",
                    options = options,
                    correctAnswer = letters.joinToString(","),
                    explanation = "正确项来自标准定义与课程范围。",
                    knowledgePointId = point.id, score = score
                )
            }
            QuestionType.FILL_BLANK -> ExamQuestion(
                id = qid, type = type,
                stem = "填空：${point.name} 的定义是 ____。",
                correctAnswer = point.definition,
                explanation = "标准定义：${point.definition}",
                knowledgePointId = point.id, score = score
            )
            QuestionType.SHORT_ANSWER, QuestionType.COMPARISON -> ExamQuestion(
                id = qid, type = type,
                stem = "请解释「${point.name}」是什么，并说明其作用。",
                correctAnswer = point.definition,
                explanation = "本地启发式模板题（LLM 不可用时生成）。",
                knowledgePointId = point.id, score = score
            )
            QuestionType.CALCULATION, QuestionType.PROOF -> ExamQuestion(
                id = qid, type = type,
                stem = "请针对「${point.name}」写出完整解题/推导步骤（含每一步依据）。",
                correctAnswer = "步骤1：明确已知（${point.definition.take(30)}）；步骤2：写出依据；步骤3：推导；步骤4：得出结论。",
                explanation = "按规范答题库的步骤评分。",
                knowledgePointId = point.id, score = score
            )
        }
    }

    private suspend fun llmQuestion(
        point: KnowledgePoint,
        type: QuestionType,
        difficulty: Difficulty,
        examId: String,
        index: Int,
        pool: List<KnowledgePoint>
    ): ExamQuestion? {
        val client = llm ?: return null
        val distractors = pool.filter { it.id != point.id }.map { it.definition.take(40) }.take(3)
        val prompt = """
出一道 $type 题目（难度 $difficulty），只输出 JSON：
{"stem":"...","options":["..."],"correctAnswer":"...","explanation":"..."}

知识点：${point.name}
定义：${point.definition}
干扰项素材：${distractors.joinToString("｜")}
""".trimIndent()
        val raw = client.generateResponse(prompt, LlmConfig("nvidia", "", "", ""))
        val s = raw.indexOf('{')
        val e = raw.lastIndexOf('}')
        if (s < 0 || e <= s) return null
        val obj = (Json { ignoreUnknownKeys = true; isLenient = true }
            .parseToJsonElement(raw.substring(s, e + 1)) as? JsonObject) ?: return null
        fun str(key: String, def: String = ""): String =
            (obj[key] as? JsonPrimitive)?.contentOrNull ?: def
        val stem = str("stem").ifBlank { return null }
        val options = (obj["options"] as? JsonArray)
            ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull } ?: emptyList()
        return ExamQuestion(
            id = "${examId}_q$index", type = type, stem = stem,
            options = options,
            correctAnswer = str("correctAnswer"),
            explanation = str("explanation"),
            knowledgePointId = point.id,
            score = scoreFor(type, difficulty)
        )
    }

    private fun scoreFor(type: QuestionType, difficulty: Difficulty): Int {
        val base = when (type) {
            QuestionType.SINGLE_CHOICE, QuestionType.FILL_BLANK -> 5
            QuestionType.MULTIPLE_CHOICE -> 6
            QuestionType.SHORT_ANSWER, QuestionType.COMPARISON -> 8
            QuestionType.CALCULATION, QuestionType.PROOF -> 10
        }
        return when (difficulty) {
            Difficulty.EASY -> base
            Difficulty.MEDIUM -> base
            Difficulty.HARD -> (base * 1.2f).roundToInt()
        }
    }
}
