package com.learnagent.core

import com.learnagent.data.FrameOcr
import com.learnagent.data.TimelineItem
import com.learnagent.data.TranscriptSegment

/**
 * 数据融合：transcript（语音）+ frames（画面 OCR / 氛围 / 画面描述）→ 统一时间线。
 *
 * 性能版：帧先按时间排序 + 二分定位（20fps 全量抽帧可达十万级，旧"逐段 filter"是 O(n·m) 会卡死）。
 * screenText 聚合时折叠「连续重复」的相同 OCR 文本（同一页 PPT 连续多帧只保留一份文字——不丢帧，只去重字）。
 * 输出按 start 排序，任何帧都不丢弃（无语音归属的帧独立成条）。
 */
fun merge(
    transcript: List<TranscriptSegment>,
    frames: List<FrameOcr>
): List<TimelineItem> {
    if (transcript.isEmpty() && frames.isEmpty()) return emptyList()

    val sorted = frames.sortedBy { it.timestamp }
    val ts = DoubleArray(sorted.size) { sorted[it].timestamp }
    val covered = BooleanArray(sorted.size)

    // 二分：第一个 >= x 的下标
    fun lowerBound(x: Double): Int {
        var lo = 0
        var hi = sorted.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (ts[mid] < x) lo = mid + 1 else hi = mid
        }
        return lo
    }

    val out = ArrayList<TimelineItem>(transcript.size + 64)

    for (segment in transcript) {
        val lo = lowerBound(segment.start)
        val rel = ArrayList<FrameOcr>()
        var j = lo
        while (j < sorted.size && ts[j] < segment.end + 2.5) { // 吸收邻近画面帧 → 语音+画面同屏
            rel.add(sorted[j])
            j++
        }
        // 覆盖标记（与旧语义一致：[start, end] 闭区间内的帧视为已有归属）
        var k = lo
        while (k < sorted.size && ts[k] <= segment.end + 2.5) {
            covered[k] = true
            k++
        }
        val screenText = rel.map { it.text }.filter { it.isNotBlank() }
            .fold(ArrayList<String>()) { acc, s -> if (acc.lastOrNull() != s) acc.add(s); acc }
            .joinToString(" ")
            .trim()
        out.add(
            TimelineItem(
                start = segment.start,
                end = segment.end,
                speech = segment.text,
                screenText = screenText,
                framePath = rel.firstOrNull()?.imagePath ?: "",
                caption = rel.map { it.caption }.filter { it.isNotBlank() }.distinct().joinToString(" ").take(200)
            )
        )
    }

    // 只有画面、没有语音归属的帧 → 独立条目；连续「完全重复」（OCR 文字一模一样）的帧折叠成一个时段，
    // 原始帧一帧不删，全部保留在 frames.json（后台留底给 Mentara）。
    var idx = 0
    while (idx < sorted.size) {
        if (covered[idx]) { idx++; continue }
        val first = sorted[idx]
        val text = first.text.trim()
        var last = idx
        while (last + 1 < sorted.size && !covered[last + 1] && sorted[last + 1].text.trim() == text) last++
        val dupCount = last - idx + 1
        out.add(
            TimelineItem(
                start = first.timestamp,
                end = sorted[last].timestamp + 5.0,
                speech = "",
                screenText = if (dupCount > 1) first.text + "（同画面连续 $dupCount 帧，已折叠）" else first.text,
                framePath = first.imagePath,
                caption = first.caption
            )
        )
        idx = last + 1
    }

    out.sortBy { it.start }
    return out
}
