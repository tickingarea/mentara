package com.learnagent.core

import com.learnagent.data.KnowledgePoint
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

data class FeynmanResult(
    val score: Int,                  // 0-10
    val accuracy: Int,               // 0-4
    val simplicity: Int,             // 0-3
    val completeness: Int,           // 0-3
    val passed: Boolean,             // score >= 6
    val feedback: String,
    val missingPoints: List<String>
)

/**
 * 费曼检查：准确性 4 + 简洁性 3 + 完整性 3，<6 不通过。
 * LLM 不可用 → 关键词匹配：命中定义 3 个以上关键词 = 通过。
 */
class FeynmanCheck(private val llm: LlmClient?) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun check(point: KnowledgePoint, userExplanation: String): FeynmanResult {
        if (userExplanation.isBlank()) {
            return FeynmanResult(0, 0, 0, 0, false, "没有内容可评分。", keywordsOf(point).take(3))
        }
        if (llm != null) {
            try {
                return parse(callLlm(point, userExplanation))
            } catch (e: Exception) {
                // 降级
            }
        }
        return fallback(point, userExplanation)
    }

    private suspend fun callLlm(point: KnowledgePoint, userExplanation: String): String {
        val client = llm ?: return ""
        val prompt = """
用费曼学习法检查学生的解释。
知识点：${point.name}
标准定义：${point.definition}
学生解释：$userExplanation

评分标准（总分 10）：
- 准确性 4 分：概念有没有讲错
- 简洁性 3 分：有没有用简单语言（像给 10 岁小孩讲）
- 完整性 3 分：有没有漏关键点
6 分以下 = 不通过

只输出 JSON：
{"score":7,"accuracy":3,"simplicity":2,"completeness":2,"feedback":"...","missingPoints":["..."]}
""".trimIndent()
        return client.generateResponse(prompt, LlmConfig("nvidia", "", "", ""))
    }

    private fun parse(raw: String): FeynmanResult {
        val s = raw.indexOf('{')
        val e = raw.lastIndexOf('}')
        if (s < 0 || e <= s) throw IllegalArgumentException("无 JSON")
        val o = json.parseToJsonElement(raw.substring(s, e + 1)).jsonObject
        fun num(key: String): Int = (o[key] as? JsonPrimitive)?.contentOrNull?.toIntOrNull() ?: 0
        val accuracy = num("accuracy").coerceIn(0, 4)
        val simplicity = num("simplicity").coerceIn(0, 3)
        val completeness = num("completeness").coerceIn(0, 3)
        val score = num("score").coerceIn(0, 10).let { if (it == 0) accuracy + simplicity + completeness else it }
        val feedback = (o["feedback"] as? JsonPrimitive)?.contentOrNull.orEmpty()
        val missing = (o["missingPoints"] as? JsonArray)
            ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull } ?: emptyList()
        return FeynmanResult(score, accuracy, simplicity, completeness, score >= 6, feedback, missing)
    }

    private fun fallback(point: KnowledgePoint, explanation: String): FeynmanResult {
        val keywords = keywordsOf(point)
        val hit = keywords.count { explanation.contains(it) } + if (explanation.contains(point.name)) 1 else 0
        val accuracy = hit.coerceAtMost(4)
        val simplicity = if (explanation.length in 10..200) 2 else 1
        val completeness = when {
            hit >= 3 -> 3
            hit == 2 -> 1
            else -> 0
        }
        val score = (accuracy + simplicity + completeness).coerceAtMost(10)
        val passed = score >= 6
        val missing = keywords.filterNot { explanation.contains(it) }.take(3)
        val feedback = if (passed) {
            "本地启发式：命中 $hit 个核心关键词，解释基本到位。"
        } else {
            "本地启发式：仅命中 $hit 个核心关键词（需 ≥3），建议补充定义中的关键概念。"
        }
        return FeynmanResult(score, accuracy, simplicity, completeness, passed, feedback, missing)
    }

    private fun keywordsOf(point: KnowledgePoint): List<String> =
        (point.definition + " " + point.name)
            .split(Regex("[\\s，。、；;,.!?：:（）()\\[\\]【】]+"))
            .map { it.trim() }
            .filter { it.length >= 2 }
            .distinct()
}
