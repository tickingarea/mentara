package com.learnagent.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 学业模式掌握度：SM-2 算法状态。
 * easeFactor 默认 2.5（SM-2 标准初始值）；interval 单位天。
 */
@Serializable
data class KnowledgeMastery(
    @SerialName("pointId") val pointId: String,
    @SerialName("status") val status: MasteryStatus = MasteryStatus.NOT_LEARNED,
    @SerialName("recallAccuracy") val recallAccuracy: Float = 0f,
    @SerialName("reviewCount") val reviewCount: Int = 0,
    @SerialName("correctCount") val correctCount: Int = 0,
    @SerialName("lastReviewAt") val lastReviewAt: Long = 0L,
    @SerialName("nextReviewAt") val nextReviewAt: Long = 0L,
    @SerialName("easeFactor") val easeFactor: Float = 2.5f,   // SM-2 默认值
    @SerialName("interval") val interval: Int = 0,
    @SerialName("totalStudyMinutes") val totalStudyMinutes: Int = 0
)

@Serializable
enum class MasteryStatus {
    NOT_LEARNED, LEARNED, FAMILIAR, MASTERED, EXPERT
}
