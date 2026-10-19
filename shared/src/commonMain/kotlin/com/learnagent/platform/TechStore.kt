package com.learnagent.platform

import com.learnagent.data.CodeSnippet
import com.learnagent.data.Decision
import com.learnagent.data.KnowledgeMastery
import com.learnagent.data.TechCard
import com.learnagent.data.TechUsage

/**
 * 学习引擎存储（expect）：技术卡 / 技术使用 / 决策 / 代码片段 / 掌握度。
 *
 * 存储布局（每个实体一个 JSON 文件，文件名 = 实体 id，非法字符替换为 '_'）：
 * - jvm    : ~/.mentara/{tech_cards,tech_usage,decisions,snippets,mastery}/
 * - android: <context.filesDir>/{tech_cards,tech_usage,decisions,snippets,mastery}/
 *            （App 启动时调用 android actual 的 TechStore.init(filesDir.path)）
 */
expect class TechStore() {
    fun saveTechCard(card: TechCard)
    fun getAllTechCards(): List<TechCard>
    fun saveTechUsage(usage: TechUsage)
    fun getAllTechUsage(): List<TechUsage>
    fun saveDecision(decision: Decision)
    fun getAllDecisions(): List<Decision>
    fun saveCodeSnippet(snippet: CodeSnippet)
    fun getAllCodeSnippets(): List<CodeSnippet>
    fun saveMastery(mastery: KnowledgeMastery)
    fun getAllMastery(): List<KnowledgeMastery>
    /** 轻量键值（周报已读标记等）。 */
    fun saveMeta(key: String, value: String)
    fun getMeta(key: String): String?
}
