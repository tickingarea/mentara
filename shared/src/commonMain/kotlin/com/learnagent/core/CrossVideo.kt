package com.learnagent.core

import com.learnagent.data.TimelineItem

/** 跨视频连接：某个关键词在某个视频中出现的次数。 */
data class VideoConnection(
    val videoId: String,
    val keyword: String,
    val count: Int
)

/**
 * 跨视频索引：把多个视频的时间线放进同一个索引，支持查找
 * "某个概念在哪些视频里出现过"。
 */
class CrossVideoIndex {

    private val videos = linkedMapOf<String, List<TimelineItem>>()

    fun indexVideo(videoId: String, items: List<TimelineItem>) {
        videos[videoId] = items
    }

    fun clear() = videos.clear()

    /** 查询关键词在每个视频中的命中次数，按次数降序。 */
    fun findCrossVideoConnections(query: String): List<VideoConnection> {
        val keywords = query
            .lowercase()
            .split(Regex("[\\s,，。、/]+"))
            .filter { it.isNotBlank() }
        if (keywords.isEmpty()) return emptyList()

        val result = mutableListOf<VideoConnection>()
        for ((videoId, items) in videos) {
            for (kw in keywords) {
                val count = items.count { item ->
                    item.speech.lowercase().contains(kw) || item.screenText.lowercase().contains(kw)
                }
                if (count > 0) result.add(VideoConnection(videoId, kw, count))
            }
        }
        return result.sortedByDescending { it.count }
    }

    /** 在所有视频中查找共享的关键词（出现在 >= n 个视频里）。 */
    fun sharedKeywords(minVideos: Int = 2, limit: Int = 20): Map<String, List<String>> {
        val keywordToVideos = mutableMapOf<String, MutableSet<String>>()
        for ((videoId, items) in videos) {
            val tokens = items.flatMap { tokenize(it.speech) + tokenize(it.screenText) }.toSet()
            for (t in tokens) keywordToVideos.getOrPut(t) { mutableSetOf() }.add(videoId)
        }
        return keywordToVideos
            .filter { it.value.size >= minVideos }
            .entries
            .sortedByDescending { it.value.size }
            .take(limit)
            .associate { it.key to it.value.toList() }
    }

    private fun tokenize(text: String): List<String> {
        val cleaned = text.lowercase()
            .replace(Regex("[\\s,，。、/！？；：\"'`()（）]"), " ")
        return cleaned.split(" ").filter { it.length >= 2 }
    }
}
