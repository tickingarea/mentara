package com.learnagent.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 错题条目（错题本）。 */
@Serializable
data class ErrorEntry(
    @SerialName("id") val id: String,
    @SerialName("questionId") val questionId: String,
    @SerialName("knowledgePointId") val knowledgePointId: String,
    @SerialName("userAnswer") val userAnswer: String,
    @SerialName("correctAnswer") val correctAnswer: String,
    @SerialName("explanation") val explanation: String,
    @SerialName("errorType") val errorType: ErrorType,
    @SerialName("errorCount") val errorCount: Int = 1,
    @SerialName("lastErrorAt") val lastErrorAt: Long,
    @SerialName("resolved") val resolved: Boolean = false
)

@Serializable
enum class ErrorType {
    CONCEPT_ERROR, CALCULATION_ERROR, STANDARD_ERROR, CARELESS_ERROR, MISUNDERSTANDING
}
