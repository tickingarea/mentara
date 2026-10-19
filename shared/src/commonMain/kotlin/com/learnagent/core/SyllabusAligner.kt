package com.learnagent.core

import com.learnagent.data.KnowledgePoint
import com.learnagent.data.LearningUnit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

/** 考纲等级（Study Mode 本地枚举，与 data.ExamLevel 同名值但独立）。 */
enum class SyllabusLevel { MUST_KNOW, OFTEN, UNDERSTAND, OPTIONAL }

data class SyllabusInfo(
    val pointId: String,
    val level: SyllabusLevel,
    val estimatedWeight: Float,            // 0.0-1.0，预估考试占比
    val commonQuestionTypes: List<String>  // ["单选", "简答", "计算"]
)

/**
 * 考纲对齐：LLM 判断等级/权重/常见题型；
 * LLM 不可用 → 全部 OFTEN / 0.05；无法确定 → UNDERSTAND。
 */
class SyllabusAligner(private val llm: LlmClient?) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun align(point: KnowledgePoint, unit: LearningUnit): SyllabusInfo {
        if (llm == null) return fallback(point)
        return try {
            parse(point, callLlm(point, unit))
        } catch (e: Exception) {
            fallback(point)
        }
    }

    private suspend fun callLlm(point: KnowledgePoint, unit: LearningUnit): String {
        val client = llm ?: return ""
        val prompt = """
知识点：${point.name}
定义：${point.definition}
来源：${point.sourceVideoId} ${point.sourceTimestamp}

判断考纲等级：
- MUST_KNOW：近 5 年出现 3+ 次
- OFTEN：出现 1-2 次
- UNDERSTAND：了解即可
- OPTIONAL：选学

输出 JSON：
{
  "level": "MUST_KNOW",
  "estimatedWeight": 0.15,
  "commonQuestionTypes": ["单选", "简答"]
}
""".trimIndent()
        return client.generateResponse(prompt, LlmConfig("nvidia", "", "", ""))
    }

    private fun parse(point: KnowledgePoint, raw: String): SyllabusInfo {
        val s = raw.indexOf('{')
        val e = raw.lastIndexOf('}')
        if (s < 0 || e <= s) throw IllegalArgumentException("无 JSON")
        val o = json.parseToJsonElement(raw.substring(s, e + 1)).jsonObject
        fun str(key: String, def: String = ""): String = (o[key] as? JsonPrimitive)?.contentOrNull ?: def
        val levelRaw = str("level")
        val level = if (levelRaw.isBlank()) SyllabusLevel.UNDERSTAND
        else runCatching { SyllabusLevel.valueOf(levelRaw.uppercase()) }.getOrDefault(SyllabusLevel.UNDERSTAND)
        val weight = str("estimatedWeight").toFloatOrNull()?.coerceIn(0f, 1f) ?: 0.05f
        val types = (o["commonQuestionTypes"] as? JsonArray)
            ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
            ?.filter { it.isNotBlank() }
            ?: emptyList()
        return SyllabusInfo(point.id, level, weight, types.ifEmpty { listOf("单选", "简答") })
    }

    private fun fallback(point: KnowledgePoint) =
        SyllabusInfo(point.id, SyllabusLevel.OFTEN, 0.05f, listOf("单选", "简答"))
}
