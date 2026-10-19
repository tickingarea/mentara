package com.learnagent.core

import com.learnagent.data.LearningUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable

/** 一个分歧点：主题 + 各方观点。 */
@Serializable
data class DigestDivergence(val topic: String, val views: List<String>)

/** 单段（时间区间）的三模型深度整理结果。 */
@Serializable
data class SegmentDigest(
    val start: Double,
    val end: Double,
    val consensus: List<String> = emptyList(),
    val divergences: List<DigestDivergence> = emptyList(),
    /** 模型名 → 该段最终稿（原生表达，无字数限制）。 */
    val perModel: Map<String, String> = emptyMap(),
    /** 三模型都不可用时的降级标记。 */
    val degraded: Boolean = false
)

/** 全片深度整理报告。 */
@Serializable
data class DeepDigestReport(
    val videoId: String,
    val models: List<String>,
    val rounds: Int,
    val segments: List<SegmentDigest>,
    val consensus: List<String> = emptyList(),
    val divergences: List<DigestDivergence> = emptyList()
)

/** 进度（用于进度条）。 */
data class DigestProgress(val current: Int, val total: Int, val stage: String)

private data class Seat(val label: String, val primary: LlmClient?, val fallback: LlmClient?)

/**
 * 三模型深度整理引擎（批次 A）：
 *  1) 每段：三模型各自"一字不落"整理（保留全部技术名词/数字/公式/结论，去废话，不设字数限制、允许本体表达）
 *  2) 交叉校对 N 轮：每模型看到另两方稿件 + 自己的稿，融合补全并保留分歧
 *  3) 合成：共同观点 / 分歧点（各自立场）
 *  4) 鲁棒：单次调用超时 + 重试 + 备选模型接力；全失败则该段降级标记
 */
class DeepDigestEngine(
    seats: List<Triple<String, LlmClient?, LlmClient?>>,
    private val callTimeoutMs: Long = 180_000,
    private val retries: Int = 2,
    val maxRounds: Int = 3
) {
    private val seats = seats.map { Seat(it.first, it.second, it.third) }

    suspend fun digest(
        videoId: String,
        units: List<LearningUnit>,
        onProgress: (DigestProgress) -> Unit = {}
    ): DeepDigestReport {
        val segments = ArrayList<SegmentDigest>()
        units.forEachIndexed { i, u ->
            onProgress(DigestProgress(i, units.size, "第 ${i + 1}/${units.size} 段 · 三模型独立整理"))
            segments.add(digestSegment(u, onProgress))
        }
        onProgress(DigestProgress(units.size, units.size, "全局共识/分歧总结"))
        val pool = segments.flatMap { it.perModel.values }.joinToString("\n\n")
        val (cons, div) = synthesize(pool, "全片")
        return DeepDigestReport(videoId, seats.map { it.label }, maxRounds, segments, cons, div)
    }

    private suspend fun digestSegment(u: LearningUnit, onProgress: (DigestProgress) -> Unit): SegmentDigest {
        val base = buildString {
            append("课程片段（时间 ").append(fmt(u.start)).append(" - ").append(fmt(u.end)).append("）\n")
            append("语音：").append(u.speech).append('\n')
            append("画面文字：").append(u.screenText)
            if (u.visualDescription.isNotBlank()) append("\n画面描述：").append(u.visualDescription)
        }
        var drafts = LinkedHashMap<String, String>()
        seats.forEach { seat ->
            val r = call(seat, knowledgePrompt(seat.label, base))
            if (r != null) drafts[seat.label] = r
            else onProgress(DigestProgress(0, 0, "${seat.label} 超时/失败，已降级"))
        }
        if (drafts.isEmpty()) return SegmentDigest(u.start, u.end, degraded = true)

        var round = 0
        while (round < maxRounds && drafts.size >= 2) {
            round++
            val snapshot = drafts.toMap()
            val revised = LinkedHashMap<String, String>()
            seats.forEach { seat ->
                val mine = snapshot[seat.label] ?: return@forEach
                val others = snapshot.filterKeys { it != seat.label }
                if (others.isEmpty()) { revised[seat.label] = mine; return@forEach }
                val r = call(seat, reviewPrompt(seat.label, base, mine, others))
                revised[seat.label] = r ?: mine
            }
            drafts = revised
            onProgress(DigestProgress(0, 0, "交叉校对 $round/$maxRounds 完成"))
        }
        val (cons, div) = synthesize(drafts.values.joinToString("\n\n"), "本段 ${fmt(u.start)}-${fmt(u.end)}")
        return SegmentDigest(u.start, u.end, cons, div, drafts)
    }

    private suspend fun synthesize(text: String, scope: String): Pair<List<String>, List<DigestDivergence>> {
        if (text.isBlank()) return emptyList<String>() to emptyList()
        val seat = seats.firstOrNull { it.primary != null || it.fallback != null } ?: return emptyList<String>() to emptyList()
        val r = call(seat, synthPrompt(scope, text)) ?: return emptyList<String>() to emptyList()
        return parseSynth(r)
    }

    /** 超时 + 重试 + 备选接力；全失败返回 null。 */
    private suspend fun call(seat: Seat, prompt: String): String? {
        for (client in listOfNotNull(seat.primary, seat.fallback)) {
            repeat(retries + 1) {
                val r = withTimeoutOrNull(callTimeoutMs) {
                    runCatching { client.generateResponse(prompt, LlmConfig("nvidia", "", "", "")) }.getOrNull()
                }
                if (!r.isNullOrBlank()) return r.trim()
                delay(400)
            }
        }
        return null
    }

    private fun knowledgePrompt(label: String, base: String) = """
你是 $label。请对下面这段课程做"一字不落"的知识整理：
- 保留全部技术名词、定义、数字、公式、结论与前提条件；
- 去掉寒暄、口头禅、重复和废话；
- 用你本来的风格与判断表达，不要为了凑字数扩写，也不要为了简短删掉要点；
- 若语音与画面文字冲突，取并集，并注明冲突处；
- 只输出整理结果本身，不要客套。

$base
""".trimIndent()

    private fun reviewPrompt(label: String, base: String, mine: String, others: Map<String, String>) = buildString {
        append("你是 ").append(label).append("。下面是同一段课程的三份整理稿。\n")
        append("你的初稿：\n").append(mine).append("\n\n")
        others.forEach { (who, txt) -> append("$who 的初稿：\n").append(txt).append("\n\n") }
        append("请融合三份稿，补全所有遗漏的知识点，纠正错误；不要删减事实、不要硬凑字数。\n")
        append("最后用一行【分歧】列出你与另外两方仍不一致的地方（没有就写：无）。\n")
        append("只输出融合后的完整稿。\n\n原始内容：\n").append(base)
    }

    private fun synthPrompt(scope: String, text: String) = """
下面是三份（或多段）对同一内容的整理稿。请提炼出结构化结论，严格用两段输出：
【共同观点】
- （三方一致的核心知识点，尽量具体）
【分歧点】
- 主题：___；A：___；B：___；C：___

要求：只写有信息量的条目；共同观点优先写"必学知识点"；分歧点写清各自立场。
范围：$scope

$text
""".trimIndent()

    /** 解析【共同观点】/【分歧点】。 */
    fun parseSynth(raw: String): Pair<List<String>, List<DigestDivergence>> {
        val consensus = ArrayList<String>()
        val divergences = ArrayList<DigestDivergence>()
        var section = ""
        raw.lines().forEach { line ->
            val l = line.trim()
            when {
                l.contains("共同观点") -> section = "c"
                l.contains("分歧") -> section = "d"
                l.startsWith("-") || l.startsWith("•") -> {
                    val body = l.removePrefix("-").removePrefix("•").trim()
                    if (body.isBlank()) return@forEach
                    when (section) {
                        "c" -> consensus.add(body)
                        "d" -> {
                            val topic = Regex("主题[:：]([^；;]+)").find(body)?.groupValues?.get(1)?.trim()
                                ?: body.take(20)
                            val views = body.split('；', ';').map { it.trim() }
                                .filter { it.isNotBlank() && !it.startsWith("主题") }
                            divergences.add(DigestDivergence(topic, views))
                        }
                    }
                }
            }
        }
        return consensus to divergences
    }

    private fun fmt(sec: Double): String {
        val t = sec.toInt()
        fun two(v: Int) = if (v < 10) "0$v" else "$v"
        return two(t / 60) + ":" + two(t % 60)
    }
}
