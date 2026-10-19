package com.learnagent.core

import com.learnagent.data.Emotion
import com.learnagent.data.LearningUnit
import com.learnagent.data.Pace
import com.learnagent.data.SemanticType
import com.learnagent.data.TimelineItem
import com.learnagent.data.Tone
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * 上下文分析：每 [batchSize] 条 TimelineItem 一批，带前 [before] / 后 [after] 条上下文，
 * 交给 LLM 输出结构化 JSON。解析失败或无 LLM → 降级（oneLineSummary = speech.take(50)）。
 */
class ContextAnalyzer(private val llm: LlmClient? = null) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun analyze(
        items: List<TimelineItem>,
        videoId: String,
        batchSize: Int = 5,
        before: Int = 3,
        after: Int = 5
    ): List<LearningUnit> {
        val result = mutableListOf<LearningUnit>()
        var i = 0
        while (i < items.size) {
            val end = minOf(i + batchSize, items.size)
            val ctxBefore = items.subList(maxOf(0, i - before), i).joinToString(" ") { it.speech }
            val ctxAfter = items.subList(end, minOf(end + after, items.size)).joinToString(" ") { it.speech }
            for (item in items.subList(i, end)) {
                result.add(analyzeOne(item, videoId, ctxBefore, ctxAfter))
            }
            i = end
        }
        return result
    }

    private suspend fun analyzeOne(
        item: TimelineItem,
        videoId: String,
        before: String,
        after: String
    ): LearningUnit {
        val base = baseOf(item, videoId)
        val client = llm ?: return base
        return try {
            val prompt = buildString {
                append("分析以下视频片段，只输出 JSON（不要解释）：\n")
                append("{\"semanticType\":\"DEFINITION|EXAMPLE|DERIVATION|CONCLUSION|COMPARISON|WARNING|BACKGROUND|UNKNOWN\",")
                append("\"emotion\":\"NEUTRAL|EXCITED|CALM|CONFUSED|EMPHATIC|HUMOROUS\",")
                append("\"tone\":\"NORMAL|DEFINITION|EXAMPLE|EMPHASIS|TRANSITION|SUMMARY|WARNING\",")
                append("\"pace\":\"NORMAL|FAST|SLOW|PAUSE\",\"importance\":1-5,")
                append("\"keywords\":[...],\"oneLineSummary\":\"...\",\"intuition\":\"...\",\"whenToUse\":\"...\",\"commonPitfalls\":\"...\"}\n\n")
                append("上文：").append(before).append("\n")
                append("本段：").append(item.speech).append("\n画面：").append(item.screenText).append("\n")
                append("下文：").append(after)
            }
            parse(base, client.generateResponse(prompt, LlmConfig("nvidia", "", "", "")))
        } catch (e: Exception) {
            base
        }
    }

    private fun baseOf(item: TimelineItem, videoId: String) = LearningUnit(
        id = videoId + "_" + item.start.toInt(),
        videoId = videoId,
        start = item.start,
        end = item.end,
        speech = item.speech,
        screenText = item.screenText,
        framePath = item.framePath,
        oneLineSummary = item.speech.take(50)
    )

    private fun parse(base: LearningUnit, text: String): LearningUnit = try {
        val start = text.indexOf('{')
        val endIdx = text.lastIndexOf('}')
        if (start < 0 || endIdx <= start) base
        else {
            val o = json.parseToJsonElement(text.substring(start, endIdx + 1)).jsonObject
            fun str(k: String, d: String) = o[k]?.jsonPrimitive?.content ?: d
            base.copy(
                semanticType = runCatching { SemanticType.valueOf(str("semanticType", "UNKNOWN")) }.getOrDefault(SemanticType.UNKNOWN),
                emotion = runCatching { Emotion.valueOf(str("emotion", "NEUTRAL")) }.getOrDefault(Emotion.NEUTRAL),
                tone = runCatching { Tone.valueOf(str("tone", "NORMAL")) }.getOrDefault(Tone.NORMAL),
                pace = runCatching { Pace.valueOf(str("pace", "NORMAL")) }.getOrDefault(Pace.NORMAL),
                importance = (o["importance"]?.jsonPrimitive?.content?.toIntOrNull() ?: 3).coerceIn(1, 5),
                oneLineSummary = str("oneLineSummary", base.oneLineSummary),
                intuition = str("intuition", ""),
                whenToUse = str("whenToUse", ""),
                commonPitfalls = str("commonPitfalls", "")
            )
        }
    } catch (e: Exception) {
        base
    }
}
