package com.learnagent.core

import com.learnagent.data.TimelineItem

/**
 * 统一证据层：把「语音(转写) + 画面(OCR)」合并成给大模型的证据文本，
 * 并提供"两项是否齐全"的判定——所有大模型判定都要求音频与画面齐全后才进行。
 */
object Evidence {

    fun speechCount(timeline: List<TimelineItem>): Int = timeline.count { it.speech.isNotBlank() }
    fun screenCount(timeline: List<TimelineItem>): Int = timeline.count { it.screenText.isNotBlank() }

    /** 语音与画面是否都齐全（两项齐全才允许大模型判定）。 */
    fun ready(timeline: List<TimelineItem>): Boolean =
        speechCount(timeline) > 0 && screenCount(timeline) > 0

    fun fmt(seconds: Double): String {
        val t = seconds.toInt()
        fun two(v: Int) = if (v < 10) "0$v" else "$v"
        return two(t / 60) + ":" + two(t % 60)
    }

    /** 合并渲染「语音 + 画面」证据文本（带时间戳）。 */
    fun render(timeline: List<TimelineItem>, maxChars: Int = 12000): String =
        timeline.joinToString("\n") { item ->
            val sp = item.speech.trim().replace("\n", " ")
            val sc = item.screenText.trim().replace("\n", " ")
            buildString {
                append("[").append(fmt(item.start)).append("]")
                if (sp.isNotEmpty()) append(" 语音：").append(sp.take(140))
                if (sc.isNotEmpty()) append(" ｜画面：").append(sc.take(100))
                if (item.caption.isNotBlank()) append(" ｜画面描述：").append(item.caption.take(80))
            }
        }.take(maxChars)

    /** 带编号版本（用于需要按序号回填的判定，如重点/题外话）。 */
    fun renderNumbered(timeline: List<TimelineItem>): String =
        renderNumbered(timeline, List(timeline.size) { it })

    /** 带编号版本（指定原始下标，采样后仍能准确回填到原位置）。 */
    fun renderNumbered(timeline: List<TimelineItem>, indices: List<Int>): String =
        indices.joinToString("\n") { i ->
            val item = timeline[i]
            val sp = item.speech.trim().replace("\n", " ")
            val sc = item.screenText.trim().replace("\n", " ")
            buildString {
                append("[").append(i).append("]")
                if (sp.isNotEmpty()) append(" 语音：").append(sp.take(120))
                if (sc.isNotEmpty()) append(" ｜画面：").append(sc.take(80))
                if (item.caption.isNotBlank()) append(" ｜画面描述：").append(item.caption.take(60))
            }
        }

    /** 全片均匀采样 [n] 条（保持顺序，时间从头到尾全覆盖，不再"只看开头"）。 */
    fun <T> spread(list: List<T>, n: Int): List<T> {
        if (n <= 0 || list.isEmpty()) return emptyList()
        if (list.size <= n) return list
        val out = ArrayList<T>(n)
        val step = (list.size - 1).toDouble() / (n - 1).coerceAtLeast(1)
        var last = -1
        for (i in 0 until n) {
            val idx = ((i * step).toInt()).coerceAtMost(list.size - 1)
            if (idx != last) { out.add(list[idx]); last = idx }
        }
        return out
    }

    /** 全片均匀采样的原始下标（采样后仍可精确回填）。 */
    fun spreadIndices(size: Int, n: Int): List<Int> {
        if (n <= 0 || size <= 0) return emptyList()
        if (size <= n) return List(size) { it }
        val out = ArrayList<Int>(n)
        val step = (size - 1).toDouble() / (n - 1).coerceAtLeast(1)
        var last = -1
        for (i in 0 until n) {
            val idx = ((i * step).toInt()).coerceAtMost(size - 1)
            if (idx != last) { out.add(idx); last = idx }
        }
        return out
    }

    /**
     * AI 用的「全片摘要」：语音段优先（信息密度最高），数量不足再用画面帧均匀补齐，
     * 按时间排序后渲染 —— 保证模型"看得到整门课"，而不是只看开头几秒。
     */
    fun aiDigest(timeline: List<TimelineItem>, maxItems: Int = 120, maxChars: Int = 12000): String {
        if (timeline.isEmpty()) return ""
        val speech = timeline.filter { it.speech.isNotBlank() }
        val picked = if (speech.size >= maxItems) {
            spread(speech, maxItems)
        } else {
            val rest = timeline.filter { it.speech.isBlank() }
            val fillCount = (maxItems - speech.size).coerceAtMost(rest.size)
            (speech + spread(rest, fillCount)).sortedBy { it.start }
        }
        return render(picked, maxChars)
    }
}
