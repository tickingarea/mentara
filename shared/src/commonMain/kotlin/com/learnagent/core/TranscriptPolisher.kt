package com.learnagent.core

import com.learnagent.data.TranscriptSegment
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable

/** 纠错后的一段：带出处处（时间戳）、最终文本、置信度、备选。 */
@Serializable
data class PolishedSegment(
    val index: Int,
    val start: Double,
    val end: Double,
    val original: String,
    val final: String,
    val confidence: Float,
    val alternatives: List<String> = emptyList(),
    val source: String = "三模型共识"
)

@Serializable
data class PolishedTranscript(
    val videoId: String,
    val models: List<String>,
    val segments: List<PolishedSegment>
)

/**
 * 转写纠错（去错别字 / 去 ASR 幻觉），三模型共识：
 *  - 每段让每个模型只做"修正"（改错别字、补明显漏字、删无意义插入），**禁止添加原文没有的信息**
 *  - 三模型一致 → 高置信；两票一致 → 中置信；全不一致 → 低置信并保留原始文本 + 备选
 *  - 每段保留 start/end（出处时间戳）
 */
class TranscriptPolisher(
    seats: List<Pair<String, LlmClient?>>,
    private val timeoutMs: Long = 120_000,
    private val retries: Int = 1
) {
    private val seats = seats.filter { it.second != null }

    suspend fun polish(
        videoId: String,
        segments: List<TranscriptSegment>,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): PolishedTranscript {
        val out = ArrayList<PolishedSegment>(segments.size)
        segments.forEachIndexed { i, seg ->
            onProgress(i, segments.size)
            val original = seg.text.trim()
            val variants = LinkedHashMap<String, String>() // model -> corrected
            seats.forEach { (label, llm) ->
                val r = call(llm!!, prompt(original))
                if (!r.isNullOrBlank()) variants[label] = r.lines().first().trim()
            }
            out += decide(i, seg, original, variants)
        }
        onProgress(segments.size, segments.size)
        return PolishedTranscript(videoId, seats.map { it.first }, out)
    }

    private fun decide(index: Int, seg: TranscriptSegment, original: String, variants: Map<String, String>): PolishedSegment {
        if (variants.isEmpty()) {
            return PolishedSegment(index, seg.start, seg.end, original, original, 0.2f, emptyList(), "无模型可用（原样保留）")
        }
        val groups = variants.values.groupingBy { norm(it) }.eachCount().entries.sortedByDescending { it.value }
        val top = groups.first()
        val topText = variants.values.first { norm(it) == top.key }
        return when {
            groups.size == 1 && variants.size >= 3 ->
                PolishedSegment(index, seg.start, seg.end, original, topText, 1.0f, emptyList())
            top.value >= 2 ->
                PolishedSegment(index, seg.start, seg.end, original, topText, 0.7f, variants.values.filter { norm(it) != top.key })
            else ->
                PolishedSegment(index, seg.start, seg.end, original, original, 0.3f, variants.values.toList(), "有分歧（保留原文）")
        }
    }

    private suspend fun call(llm: LlmClient, p: String): String? {
        repeat(retries + 1) {
            val r = withTimeoutOrNull(timeoutMs) {
                runCatching { llm.generateResponse(p, LlmConfig("nvidia", "", "", "")) }.getOrNull()
            }
            if (!r.isNullOrBlank()) return r
            delay(300)
        }
        return null
    }

    private fun prompt(text: String) = """
这是视频语音转写的一句话，可能有同音错别字、漏字、或多出的无意义字（ASR 幻觉）。
请**只做修正**：改错别字、补明显漏字、删除明显无意义的插入。
**严禁添加原句没有的信息，严禁改写表达方式或措辞风格**。只输出修正后的一句话本身。

原句：$text
""".trimIndent()

    private fun norm(s: String) = s.replace(Regex("[\\s，。、；：,.!?！？;:\"'（）()]+"), "").lowercase()

    /** 生成带出处的 Markdown。 */
    fun markdown(r: PolishedTranscript): String {
        fun ts(sec: Double): String {
            val t = sec.toInt().coerceAtLeast(0); fun two(v: Int) = if (v < 10) "0$v" else "$v"
            return two(t / 60) + ":" + two(t % 60)
        }
        val sb = StringBuilder()
        sb.append("# 转写纠错（三模型共识）：").append(r.videoId).append("\n\n")
        sb.append("模型：").append(r.models.joinToString(" / ")).append("\n\n")
        r.segments.forEach { s ->
            sb.append("[").append(ts(s.start)).append(" - ").append(ts(s.end)).append("] ")
            sb.append("（置信 ").append((s.confidence * 100).toInt()).append("%")
            if (s.source.isNotBlank() && s.source != "三模型共识") sb.append(" · ").append(s.source)
            sb.append("）\n")
            sb.append(s.final).append("\n")
            if (s.final != s.original) sb.append("原文：").append(s.original).append("\n")
            if (s.alternatives.isNotEmpty()) sb.append("备选：").append(s.alternatives.joinToString(" ｜ ")).append("\n")
            sb.append("\n")
        }
        return sb.toString()
    }
}
