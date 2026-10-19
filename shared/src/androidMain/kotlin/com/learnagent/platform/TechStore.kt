package com.learnagent.platform

import com.learnagent.data.CodeSnippet
import com.learnagent.data.Decision
import com.learnagent.data.KnowledgeMastery
import com.learnagent.data.TechCard
import com.learnagent.data.TechUsage
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Android 实现：<filesDir>/{tech_cards,tech_usage,decisions,snippets,mastery}/。
 * 与 jvm 同构（每个实体一个 JSON 文件）。
 * App 启动时调用 [init] 传入 context.filesDir.path；未初始化时退回当前目录。
 */
actual class TechStore actual constructor() {

    companion object {
        private var baseDirPath: String = ""

        /** App 启动时调用一次：TechStore.init(applicationContext.filesDir.path) */
        fun init(filesDirPath: String) {
            baseDirPath = filesDirPath
        }
    }

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; isLenient = true }

    private val rootDir: File
        get() = (if (baseDirPath.isBlank()) File(".") else File(baseDirPath)).apply { mkdirs() }

    private fun dir(name: String): File = File(rootDir, name).apply { mkdirs() }

    private fun safeName(id: String): String =
        id.replace(Regex("[^A-Za-z0-9_\\-\\u4e00-\\u9fa5.]"), "_").take(80).ifBlank { "unnamed" }

    private fun <T> readAll(dirName: String, serializer: KSerializer<T>): List<T> {
        val files = dir(dirName).listFiles { f -> f.isFile && f.name.endsWith(".json") } ?: return emptyList()
        return files.mapNotNull { f ->
            try {
                json.decodeFromString(serializer, f.readText(Charsets.UTF_8))
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun <T> write(dirName: String, id: String, serializer: KSerializer<T>, value: T) {
        File(dir(dirName), safeName(id) + ".json").writeText(json.encodeToString(serializer, value), Charsets.UTF_8)
    }

    actual fun saveTechCard(card: TechCard) = write("tech_cards", card.id, TechCard.serializer(), card)

    actual fun getAllTechCards(): List<TechCard> = readAll("tech_cards", TechCard.serializer())

    actual fun saveTechUsage(usage: TechUsage) = write("tech_usage", usage.techName, TechUsage.serializer(), usage)

    actual fun getAllTechUsage(): List<TechUsage> = readAll("tech_usage", TechUsage.serializer())

    actual fun saveDecision(decision: Decision) = write("decisions", decision.id, Decision.serializer(), decision)

    actual fun getAllDecisions(): List<Decision> = readAll("decisions", Decision.serializer())

    actual fun saveCodeSnippet(snippet: CodeSnippet) = write("snippets", snippet.id, CodeSnippet.serializer(), snippet)

    actual fun getAllCodeSnippets(): List<CodeSnippet> = readAll("snippets", CodeSnippet.serializer())

    actual fun saveMastery(mastery: KnowledgeMastery) = write("mastery", mastery.pointId, KnowledgeMastery.serializer(), mastery)

    actual fun getAllMastery(): List<KnowledgeMastery> = readAll("mastery", KnowledgeMastery.serializer())

    actual fun saveMeta(key: String, value: String) {
        File(dir("meta"), safeName(key) + ".txt").writeText(value, Charsets.UTF_8)
    }

    actual fun getMeta(key: String): String? {
        val f = File(dir("meta"), safeName(key) + ".txt")
        return if (f.isFile) f.readText(Charsets.UTF_8) else null
    }
}
