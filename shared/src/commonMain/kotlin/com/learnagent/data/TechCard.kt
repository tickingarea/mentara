package com.learnagent.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 技术卡：开发者模式的核心。
 * 目标：用户查技术时 3 秒读完，因此 whatItIs 必须 ≤15 字。
 */
@Serializable
data class TechCard(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("category") val category: TechCategory,
    @SerialName("whatItIs") val whatItIs: String,           // 一句话≤15字，太多说明没提炼
    @SerialName("whatItSolves") val whatItSolves: String,   // 2-3 个场景，逗号分隔
    @SerialName("whenToUse") val whenToUse: String,
    @SerialName("whenNotToUse") val whenNotToUse: String,
    @SerialName("howItWorks") val howItWorks: String,       // 只讲大概
    @SerialName("alternatives") val alternatives: List<String> = emptyList(),
    @SerialName("relatedConcepts") val relatedConcepts: List<String> = emptyList(),
    @SerialName("codeSnippet") val codeSnippet: String = "",
    @SerialName("sourceVideoId") val sourceVideoId: String = "",
    @SerialName("sourceTimestamp") val sourceTimestamp: Double = 0.0,
    /** 提取来源：LLM / 本地启发式（提取器降级时标注） */
    @SerialName("source") val source: String = "LLM"
)

@Serializable
enum class TechCategory {
    DISTRIBUTED, CONCURRENCY, DATABASE, NETWORK,
    SECURITY, PERFORMANCE, ARCHITECTURE, ALGORITHM,
    FRAMEWORK, TOOL, OTHER
}
