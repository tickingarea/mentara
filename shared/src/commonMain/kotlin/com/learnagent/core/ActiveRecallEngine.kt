package com.learnagent.core

import com.learnagent.data.KnowledgePoint
import com.learnagent.data.LearningUnit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

data class RecallCard(
    val id: String,
    val pointId: String,
    val question: String,
    val hint: String = "",
    val answer: String,
    val explanation: String,
    val difficulty: Difficulty,
    val sourceUnitId: String,
    val sourceTimestamp: Double
)

enum class Difficulty { EASY, MEDIUM, HARD }

/**
 * 主动回忆卡生成：问题必须让用户主动提取、不暗示答案。
 * LLM 不可用 → 模板："请解释 X 是什么"，answer = definition。
 */
class ActiveRecallEngine(private val llm: LlmClient?) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun generateRecall(point: KnowledgePoint, unit: LearningUnit): RecallCard {
        if (llm != null) {
            try {
                return parse(point, unit, callLlm(point))
            } catch (e: Exception) {
                // 降级
            }
        }
        return fallback(point, unit)
    }

    private suspend fun callLlm(point: KnowledgePoint): String {
        val client = llm ?: return ""
        val prompt = """
为知识点生成一张主动回忆卡（不要暗示答案，不要"是不是 X"的提问）。
知识点：${point.name}
定义：${point.definition}

要求：
- 问题让用户主动提取（如"请解释/请对比/请举例"）
- 难度：EASY=直接复述，MEDIUM=用自己的话，HARD=举例/对比

只输出 JSON：
{"question":"...","hint":"...","answer":"...","explanation":"...","difficulty":"MEDIUM"}
""".trimIndent()
        return client.generateResponse(prompt, LlmConfig("nvidia", "", "", ""))
    }

    private fun parse(point: KnowledgePoint, unit: LearningUnit, raw: String): RecallCard {
        val s = raw.indexOf('{')
        val e = raw.lastIndexOf('}')
        if (s < 0 || e <= s) throw IllegalArgumentException("无 JSON")
        val o = json.parseToJsonElement(raw.substring(s, e + 1)).jsonObject
        fun str(key: String, def: String = ""): String = (o[key] as? JsonPrimitive)?.contentOrNull ?: def
        val question = str("question").ifBlank { "请解释 ${point.name} 是什么。" }
        val difficulty = runCatching { Difficulty.valueOf(str("difficulty", "MEDIUM").uppercase()) }
            .getOrDefault(Difficulty.MEDIUM)
        return RecallCard(
            id = "recall_" + point.id,
            pointId = point.id,
            question = question,
            hint = str("hint"),
            answer = str("answer", point.definition),
            explanation = str("explanation"),
            difficulty = difficulty,
            sourceUnitId = unit.id,
            sourceTimestamp = unit.start
        )
    }

    private fun fallback(point: KnowledgePoint, unit: LearningUnit): RecallCard = RecallCard(
        id = "recall_" + point.id,
        pointId = point.id,
        question = "请解释 ${point.name} 是什么。",
        hint = "先回忆定义，再想一个使用场景。",
        answer = point.definition,
        explanation = "本地启发式：来自内容定义（${unit.videoId} @ ${unit.start}）",
        difficulty = Difficulty.EASY,
        sourceUnitId = unit.id,
        sourceTimestamp = unit.start
    )
}
