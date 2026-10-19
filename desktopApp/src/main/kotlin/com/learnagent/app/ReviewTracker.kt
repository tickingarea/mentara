package com.learnagent.app

import com.learnagent.config.ConfigStore
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 复习历史：记录每个视频最后一次查看时间，用于"主动提醒"。
 * 存储于 ~/.learnagent/review_history.json。
 */
object ReviewTracker {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; isLenient = true }
    private val file: File get() = File(ConfigStore.baseDir, "review_history.json")

    private fun load(): MutableMap<String, Long> = try {
        if (file.isFile) {
            val parsed = json.decodeFromString<Map<String, Long>>(file.readText())
            LinkedHashMap(parsed)
        } else {
            LinkedHashMap()
        }
    } catch (e: Exception) {
        LinkedHashMap()
    }

    private fun save(map: Map<String, Long>) {
        ConfigStore.baseDir.mkdirs()
        file.writeText(json.encodeToString(MapSerializer(String.serializer(), Long.serializer()), map))
    }

    fun markViewed(videoId: String) {
        val m = load()
        m[videoId] = System.currentTimeMillis()
        save(m)
    }

    /** 距离上次查看的天数；无记录返回 null。 */
    fun daysSince(videoId: String): Long? {
        val last = load()[videoId] ?: return null
        return (System.currentTimeMillis() - last) / 86_400_000L
    }

    /** 超过 [days] 天未查看的视频（从未查看的不算）。 */
    fun staleVideos(days: Long = 7): List<Pair<String, Long>> =
        load().mapNotNull { (id, last) ->
            val d = (System.currentTimeMillis() - last) / 86_400_000L
            if (d >= days) id to d else null
        }.sortedByDescending { it.second }

    /** 供"学习报告"使用的最近查看记录。 */
    fun recentViews(sinceMillis: Long): List<Pair<String, Long>> =
        load().filterValues { it >= sinceMillis }.map { it.key to it.value }.sortedByDescending { it.second }
}
