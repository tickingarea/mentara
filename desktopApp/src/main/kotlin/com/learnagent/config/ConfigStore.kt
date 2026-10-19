package com.learnagent.config

import com.learnagent.core.DebateResult
import com.learnagent.core.merge
import com.learnagent.data.FrameOcr
import com.learnagent.data.TimelineItem
import com.learnagent.data.TranscriptSegment
import com.learnagent.data.VideoMeta
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class AppConfig(
    val nvidiaApiKey: String = "",
    val model: String = "deepseek-ai/deepseek-v4.1-flash",
    val resultsDir: String = "",
    val themeMode: String = "system", // system | light | dark
    val soundEnabled: Boolean = true,
    /** 辩论三模型（空 = 用 DebateModels 内置默认，NIM 实测存在） */
    val debateModelA: String = "",
    val debateModelB: String = "",
    val debateModelC: String = "",
    /** UI 语言：en（默认）/ zh */
    val language: String = "en"
)

/**
 * 本地配置与结果目录。
 *
 * 全部位于 `~/.learnagent/`：
 *  - config.json       配置（API Key / 主题 / results 路径）
 *  - last_selected.txt 上次选中的视频
 *  - results/          每个视频一个子目录（videoId）
 */
object ConfigStore {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; isLenient = true }

    val baseDir: File = File(System.getProperty("user.home"), ".mentara").also { migrateFromLegacy(it) }

    /** 旧目录 ~/.learnagent 存在且新目录不存在时，迁移过去。 */
    private fun migrateFromLegacy(target: File) {
        val legacy = File(System.getProperty("user.home"), ".learnagent")
        if (legacy.isDirectory && !target.exists()) {
            runCatching { legacy.copyRecursively(target, overwrite = false) }
        }
    }

    fun resultsDir(config: AppConfig): File {
        val custom = config.resultsDir.trim()
        val dir = if (custom.isNotEmpty()) File(custom) else File(baseDir, "results")
        dir.mkdirs()
        return dir
    }

    private val configFile: File get() = File(baseDir, "config.json")
    private val lastSelectedFile: File get() = File(baseDir, "last_selected.txt")

    fun load(): AppConfig = try {
        if (configFile.isFile) json.decodeFromString(configFile.readText()) else AppConfig()
    } catch (e: Exception) {
        AppConfig()
    }

    fun save(config: AppConfig) {
        baseDir.mkdirs()
        configFile.writeText(json.encodeToString(config))
    }

    fun loadLastSelected(): String? = try {
        if (lastSelectedFile.isFile) lastSelectedFile.readText().trim().ifBlank { null } else null
    } catch (e: Exception) {
        null
    }

    fun saveLastSelected(videoId: String) {
        baseDir.mkdirs()
        lastSelectedFile.writeText(videoId)
    }

    /** 扫描 results 目录下的所有视频子目录。 */
    fun listVideos(config: AppConfig): List<String> {
        val dir = resultsDir(config)
        return dir.listFiles { f -> f.isDirectory }?.map { it.name }?.sorted().orEmpty()
    }

    /** 导入一个真实视频：在 results 下按文件名建立目录并写入真实 meta.json。 */
    fun importVideo(config: AppConfig, file: File): String {
        val dir = resultsDir(config)
        val base = file.nameWithoutExtension
            .replace(Regex("[^A-Za-z0-9_\\-\\u4e00-\\u9fa5]"), "_")
            .take(40)
            .ifBlank { "video" }
        val suffix = Integer.toHexString(file.absolutePath.hashCode())
        val id = "$base-$suffix"
        val videoDir = File(dir, id)
        videoDir.mkdirs()

        val meta = VideoMeta(
            videoId = id,
            deviceId = "desktop",
            lastModified = file.lastModified(),
            schemaVersion = 1,
            title = file.name,
            duration = 0.0
        )
        File(videoDir, "meta.json").writeText(json.encodeToString(meta))
        File(videoDir, "source.txt").writeText(file.absolutePath)
        return id
    }

    /** 读取某个视频的源文件路径（导入时记录）。 */
    fun sourceFile(config: AppConfig, videoId: String): File? {
        val f = File(resultsDir(config), "$videoId/source.txt")
        if (!f.isFile) return null
        return try {
            File(f.readText().trim()).takeIf { it.isFile }
        } catch (e: Exception) {
            null
        }
    }

    /** 读取某个视频的结果并融合成时间线（无结果则返回空）。 */
    fun loadTimeline(config: AppConfig, videoId: String): List<TimelineItem> {
        val dir = File(resultsDir(config), videoId)
        val transcript = readList<TranscriptSegment>(File(dir, "transcript.json"))
        val frames = readList<FrameOcr>(File(dir, "frames.json"))
        if (transcript.isEmpty() && frames.isEmpty()) return emptyList()
        return merge(transcript, frames)
    }

    fun hasResults(config: AppConfig, videoId: String): Boolean {
        val dir = File(resultsDir(config), videoId)
        return File(dir, "transcript.json").isFile || File(dir, "frames.json").isFile
    }

    /** 辩论结果持久化：results/<videoId>/debate.json */
    fun saveDebate(config: AppConfig, videoId: String, result: DebateResult) {
        val dir = File(resultsDir(config), videoId)
        dir.mkdirs()
        File(dir, "debate.json").writeText(json.encodeToString(result))
    }

    fun loadDebate(config: AppConfig, videoId: String): DebateResult? = try {
        val f = File(resultsDir(config), "$videoId/debate.json")
        if (f.isFile) json.decodeFromString<DebateResult>(f.readText()) else null
    } catch (e: Exception) {
        null
    }

    private inline fun <reified T> readList(file: File): List<T> = try {
        if (file.isFile) json.decodeFromString(file.readText()) else emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}
