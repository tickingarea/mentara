package com.learnagent.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 技术使用记录：记录"反复遇见"，不做复习调度。
 * seenCount = 在视频/文档中遇见次数；usedCount = 实际使用次数；decisionCount = 相关决策数。
 */
@Serializable
data class TechUsage(
    @SerialName("techName") val techName: String,
    @SerialName("firstSeenAt") val firstSeenAt: Long,
    @SerialName("lastSeenAt") val lastSeenAt: Long,
    @SerialName("seenCount") val seenCount: Int = 0,
    @SerialName("usedCount") val usedCount: Int = 0,
    @SerialName("decisionCount") val decisionCount: Int = 0,
    @SerialName("status") val status: TechStatus = TechStatus.DISCOVERED
)

@Serializable
enum class TechStatus {
    DISCOVERED, FAMILIAR, USED, FLUENT, EXPERT
}
