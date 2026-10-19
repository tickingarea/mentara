package com.learnagent.app

import com.learnagent.config.AppConfig
import com.learnagent.config.ConfigStore
import com.learnagent.core.Indexer
import com.learnagent.data.TimelineItem
import com.learnagent.llm.NvidiaLlmClient
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 学习工具：闪卡、学习报告、概念对比、知识导出。
 */
object StudyTools {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; isLenient = true }

    private fun stamp(seconds: Double): String {
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

    fun exportFlashcards(config: AppConfig, videoId: String, timeline: List<TimelineItem>): File {
        val dir = File(ConfigStore.resultsDir(config), videoId).apply { mkdirs() }
        val out = File(dir, "flashcards.csv")
        out.writeText(flashcardsCsv(generateFlashcards(timeline)))
        return out
    }

    // ---------- 功能 5：学习报告 ----------

    fun buildWeeklyReportMarkdown(config: AppConfig, allTimelines: Map<String, List<TimelineItem>>): String {
        val since = System.currentTimeMillis() - 7L * 86_400_000L
        val views = ReviewTracker.recentViews(since)
        val viewedIds = if (views.isEmpty()) allTimelines.keys.toList() else views.map { it.first }

        var segments = 0
        var seconds = 0.0
        val keywordCount = mutableMapOf<String, Int>()
        for (id in viewedIds) {
            val items = allTimelines[id] ?: continue
            segments += items.size
            seconds += items.sumOf { (it.end - it.start).coerceAtLeast(0.0) }
            items.forEach { item ->
                com.learnagent.ui.extractKeywords(item.speech + " " + item.screenText)
                    .forEach { keywordCount[it] = (keywordCount[it] ?: 0) + 1 }
            }
        }
        val minutes = (seconds / 60.0)
        val top = keywordCount.entries.sortedByDescending { it.value }.take(8).map { it.key }

        val statsBlock = buildString {
            append("## 本周学习统计\n\n")
            append("- 学习视频：").append(viewedIds.size).append(" 个\n")
            append("- 时间线片段：").append(segments).append(" 段\n")
            append("- 覆盖时长：约 ").append(String.format(Locale.US, "%.1f", minutes)).append(" 分钟\n")
            append("- 高频概念：").append(if (top.isEmpty()) "（暂无）" else top.joinToString("、")).append("\n")
        }

        val aiSummary = if (config.nvidiaApiKey.isNotBlank()) {
            try {
                runBlocking {
                    NvidiaLlmClient(config.nvidiaApiKey, model = config.model).generateResponse(
                        "根据以下学习数据，用中文写一段 3-4 句的周报总结，指出主要领域并给出复习建议：\n$statsBlock",
                        com.learnagent.core.LlmConfig("nvidia", "", "", "")
                    )
                }
            } catch (e: Exception) {
                "（AI 总结失败：${e.message}）"
            }
        } else {
            "（未配置 NVIDIA API Key，以下为本地统计）本周你处理了 ${viewedIds.size} 个视频、约 ${
                String.format(Locale.US, "%.1f", minutes)
            } 分钟内容，主要涉及：${if (top.isEmpty()) "暂无" else top.take(5).joinToString("、")}。建议优先复习以上高频概念。"
        }

        return "# LearnAgent 学习周报\n\n> 生成时间：${
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        }\n\n## 总结\n\n$aiSummary\n\n$statsBlock"
    }

    fun exportReport(config: AppConfig, markdown: String): File {
        val dir = File(ConfigStore.resultsDir(config), "_reports").apply { mkdirs() }
        val name = "weekly-" + SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date()) + ".md"
        val out = File(dir, name)
        out.writeText(markdown)
        return out
    }

    // ---------- 功能 7：概念对比 ----------

    fun compareMarkdown(config: AppConfig, x: String, y: String, timeline: List<TimelineItem>): String {
        val indexer = Indexer()
        indexer.index(timeline)
        val xs = indexer.search(x, 4)
        val ys = indexer.search(y, 4)

        fun block(items: List<TimelineItem>): String =
            if (items.isEmpty()) "- （未找到相关内容）"
            else items.joinToString("\n") { "- [${stamp(it.start)}] ${it.speech.ifBlank { it.screenText }}" }

        if (config.nvidiaApiKey.isNotBlank()) {
            val prompt = buildString {
                append("请用 Markdown 表格对比「$x」与「$y」，只依据给定内容。\n\n")
                append("### $x 相关内容\n").append(block(xs)).append("\n\n")
                append("### $y 相关内容\n").append(block(ys)).append("\n\n")
                append("输出两列表格：维度 | $x | $y")
            }
            return try {
                runBlocking {
                    NvidiaLlmClient(config.nvidiaApiKey, model = config.model)
                        .generateResponse(prompt, com.learnagent.core.LlmConfig("nvidia", "", "", ""))
                }
            } catch (e: Exception) {
                "（AI 对比失败：${e.message}）\n\n" + localCompare(x, y, block(xs), block(ys))
            }
        }
        return localCompare(x, y, block(xs), block(ys))
    }

    private fun localCompare(x: String, y: String, xs: String, ys: String): String =
        buildString {
            append("### $x\n").append(xs).append("\n\n")
            append("### $y\n").append(ys).append("\n\n")
            append("| 维度 | $x | $y |\n|---|---|---|\n")
            append("| 命中片段 | ${xs.lines().size} | ${ys.lines().size} |\n")
            append("| 说明 | 见上方时间戳 | 见上方时间戳 |\n")
        }

    // ---------- 功能 12：知识导出 ----------

    fun exportMarkdown(file: File, videoId: String, timeline: List<TimelineItem>) {
        val md = buildString {
            append("# $videoId\n\n")
            timeline.forEach { item ->
                append("## [${stamp(item.start)} - ${stamp(item.end)}]\n\n")
                if (item.speech.isNotBlank()) append("- 语音：").append(item.speech).append('\n')
                if (item.screenText.isNotBlank()) append("- 画面：").append(item.screenText).append('\n')
                if (item.framePath.isNotBlank()) append("- 关键帧：").append(item.framePath).append('\n')
                append('\n')
            }
        }
        file.writeText(md)
    }

    fun exportJson(file: File, timeline: List<TimelineItem>) {
        file.writeText(json.encodeToString(timeline))
    }

    fun exportCsv(file: File, timeline: List<TimelineItem>) {
        file.writeText(flashcardsCsv(generateFlashcards(timeline)))
    }
}
