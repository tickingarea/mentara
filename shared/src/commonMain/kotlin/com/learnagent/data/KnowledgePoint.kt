package com.learnagent.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 考纲等级：决定知识点的学习优先级。 */
@Serializable
enum class ExamLevel {
    MUST_KNOW, OFTEN, UNDERSTAND, OPTIONAL
}

/**
 * 知识点：由 LearningUnit 提取，供学业模式与 SM-2 复习使用。
 * masteryStatus 初始为 NOT_LEARNED；关联 id 来自 LearningUnit.relatedIds。
 */
@Serializable
data class KnowledgePoint(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("definition") val definition: String,
    /** 章节 id（交错学习分组用；空 = 未分章） */
    @SerialName("chapterId") val chapterId: String = "",
    @SerialName("sourceVideoId") val sourceVideoId: String = "",
    @SerialName("sourceTimestamp") val sourceTimestamp: Double = 0.0,
    @SerialName("examLevel") val examLevel: ExamLevel = ExamLevel.UNDERSTAND,
    @SerialName("masteryStatus") val masteryStatus: MasteryStatus = MasteryStatus.NOT_LEARNED,
    @SerialName("relatedIds") val relatedIds: List<String> = emptyList()
)
