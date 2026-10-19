package com.learnagent.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 规范答题标准：不同学科/题型的步骤与采分点。
 * requiredSteps 必须出现；forbiddenSteps 出现即扣分；scoringPoints 为可机检的采分点。
 */
@Serializable
data class AnswerStandard(
    @SerialName("id") val id: String = "",
    @SerialName("questionType") val questionType: QuestionType,
    @SerialName("subject") val subject: String,
    @SerialName("requiredSteps") val requiredSteps: List<AnswerStep> = emptyList(),
    @SerialName("optionalSteps") val optionalSteps: List<AnswerStep> = emptyList(),
    @SerialName("forbiddenSteps") val forbiddenSteps: List<String> = emptyList(),
    @SerialName("scoringPoints") val scoringPoints: List<ScoringPoint> = emptyList(),
    @SerialName("standardTemplate") val standardTemplate: String = ""
)

@Serializable
data class AnswerStep(
    @SerialName("order") val order: Int,
    @SerialName("content") val content: String,
    @SerialName("reason") val reason: String,
    @SerialName("scoreIfMissing") val scoreIfMissing: Int,
    @SerialName("canSkipCondition") val canSkipCondition: String = ""
)

@Serializable
data class ScoringPoint(
    @SerialName("point") val point: String,
    @SerialName("score") val score: Int,
    @SerialName("mustAppear") val mustAppear: Boolean = true
)

@Serializable
enum class QuestionType {
    SINGLE_CHOICE, MULTIPLE_CHOICE, FILL_BLANK, SHORT_ANSWER, COMPARISON, CALCULATION, PROOF
}
