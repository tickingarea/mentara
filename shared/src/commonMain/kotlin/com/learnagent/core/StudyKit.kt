package com.learnagent.core

import com.learnagent.data.TimelineItem

/** 学习报告统计（跨平台，纯数据）。 */
data class WeeklyStats(
    val videoCount: Int,
    val segmentCount: Int,
    val minutes: Double,
    val topKeywords: List<String>
)

/**
 * 学习工具内容生成（纯逻辑，跨平台）：
 * 闪卡 CSV、Markdown / JSON / CSV 导出、周报 Markdown、概念对比。
 */
object StudyKit {

    fun stamp(seconds: Double): String {
        val t = seconds.toInt()
        fun two(v: Int) = if (v < 10) "0$v" else "$v"
        return two(t / 60) + ":" + two(t % 60)
    }

    private fun conceptOf(text: String): String {
        val token = text.split(Regex("[\\s,，。、！？；：\"'`()（）]+"))
            .filter { it.length >= 2 }
            .maxByOrNull { it.length }
        return token ?: text.take(8)
    }

    // ---------- 功能 4：闪卡 ----------

    fun generateFlashcards(timeline: List<TimelineItem>): List<Pair<String, String>> =
        timeline.filter { it.speech.isNotBlank() }.map { item ->
            "视频里讲的「${conceptOf(item.speech)}」是什么？" to item.speech
        }

    fun flashcardsCsv(cards: List<Pair<String, String>>): String {
        fun esc(s: String) = "\"" + s.replace("\"", "\"\"") + "\""
        return buildString {
            append("question,answer\n")
            cards.forEach { (q, a) -> append(esc(q)).append(',').append(esc(a)).append('\n') }
        }
    }

    // ---------- 功能 5：学习报告 ----------

    fun weeklyReportMarkdown(stats: WeeklyStats, aiSummary: String? = null): String {
        val summary = aiSummary ?: (
            "本周你处理了 ${stats.videoCount} 个视频、约 " +
                formatMinutes(stats.minutes) + " 分钟内容，主要涉及：" +
                (if (stats.topKeywords.isEmpty()) "暂无" else stats.topKeywords.take(5).joinToString("、")) +
                "。建议优先复习以上高频概念。"
            )
        return buildString {
            append("# LearnAgent 学习周报\n\n")
            append("## 总结\n\n").append(summary).append("\n\n")
            append("## 本周学习统计\n\n")
            append("- 学习视频：").append(stats.videoCount).append(" 个\n")
            append("- 时间线片段：").append(stats.segmentCount).append(" 段\n")
            append("- 覆盖时长：约 ").append(formatMinutes(stats.minutes)).append(" 分钟\n")
            append("- 高频概念：").append(if (stats.topKeywords.isEmpty()) "（暂无）" else stats.topKeywords.joinToString("、")).append("\n")
        }
    }

    fun computeStats(timelines: Map<String, List<TimelineItem>>): WeeklyStats {
        var segments = 0
        var seconds = 0.0
        val keywordCount = mutableMapOf<String, Int>()
        timelines.values.forEach { items ->
            segments += items.size
            seconds += items.sumOf { (it.end - it.start).coerceAtLeast(0.0) }
            items.forEach { item ->
                GraphBuilder.keywordsOf(item.speech + " " + item.screenText)
                    .forEach { keywordCount[it] = (keywordCount[it] ?: 0) + 1 }
            }
        }
        val top = keywordCount.entries.sortedByDescending { it.value }.take(8).map { it.key }
        return WeeklyStats(timelines.size, segments, seconds / 60.0, top)
    }

    private fun formatMinutes(minutes: Double): String {
        val whole = minutes.toInt()
        val frac = ((minutes - whole) * 10).toInt()
        return "$whole.$frac"
    }

    // ---------- 功能 7：概念对比 ----------

    fun compareMarkdown(x: String, y: String, xs: List<TimelineItem>, ys: List<TimelineItem>): String {
        fun block(items: List<TimelineItem>): String =
            if (items.isEmpty()) "- （未找到相关内容）"
            else items.joinToString("\n") { "- [${stamp(it.start)}] ${it.speech.ifBlank { it.screenText }}" }

        return buildString {
            append("### ").append(x).append("\n").append(block(xs)).append("\n\n")
            append("### ").append(y).append("\n").append(block(ys)).append("\n\n")
            append("| 维度 | ").append(x).append(" | ").append(y).append(" |\n")
            append("|---|---|---|\n")
            append("| 命中片段 | ").append(xs.size).append(" | ").append(ys.size).append(" |\n")
            append("| 说明 | 见上方时间戳 | 见上方时间戳 |\n")
        }
    }

    // ---------- 功能 12：知识导出 ----------

    fun toMarkdown(videoId: String, timeline: List<TimelineItem>): String = buildString {
        append("# ").append(videoId).append("\n\n")
        timeline.forEach { item ->
            append("## [").append(stamp(item.start)).append(" - ").append(stamp(item.end)).append("]\n\n")
            if (item.speech.isNotBlank()) append("- 语音：").append(item.speech).append('\n')
            if (item.screenText.isNotBlank()) append("- 画面：").append(item.screenText).append('\n')
            if (item.framePath.isNotBlank()) append("- 关键帧：").append(item.framePath).append('\n')
            append('\n')
        }
    }

    fun toCsv(timeline: List<TimelineItem>): String =
        flashcardsCsv(generateFlashcards(timeline))
}
