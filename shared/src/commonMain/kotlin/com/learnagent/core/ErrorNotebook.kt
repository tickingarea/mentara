package com.learnagent.core

import com.learnagent.data.ErrorEntry
import com.learnagent.data.ErrorType
import com.learnagent.data.KnowledgeMastery
import com.learnagent.data.MasteryStatus
import com.learnagent.platform.currentTimeMillis

/**
 * 错题本（内存态）：
 * - 同 pointId 错 3 次 → 高危
 * - 错题自动加入复习队列（nextReviewAt = 最后一次错误 + 1 天）
 * - 考前 3 天 → 可生成"错题冲刺卷"（sprintReviewQueue）
 */
class ErrorNotebook(private val now: () -> Long = { currentTimeMillis() }) {

    private val entries = mutableListOf<ErrorEntry>()

    fun all(): List<ErrorEntry> = entries.toList()

    fun record(
        questionId: String,
        knowledgePointId: String,
        userAnswer: String,
        correctAnswer: String,
        explanation: String,
        errorType: ErrorType
    ): ErrorEntry {
        val t = now()
        val idx = entries.indexOfFirst {
            it.knowledgePointId == knowledgePointId && it.questionId == questionId && !it.resolved
        }
        val entry = if (idx >= 0) {
            entries[idx].copy(
                errorCount = entries[idx].errorCount + 1,
                lastErrorAt = t,
                userAnswer = userAnswer,
                correctAnswer = correctAnswer,
                explanation = explanation,
                errorType = errorType
            )
        } else {
            ErrorEntry(
                id = "err_${t.toString(16)}_${entries.size}",
                questionId = questionId,
                knowledgePointId = knowledgePointId,
                userAnswer = userAnswer,
                correctAnswer = correctAnswer,
                explanation = explanation,
                errorType = errorType,
                errorCount = 1,
                lastErrorAt = t
            )
        }
        if (idx >= 0) entries[idx] = entry else entries.add(entry)
        return entry
    }

    /** 同 pointId 的累计错误次数（跨题目累计）。 */
    fun errorCount(pointId: String): Int =
        entries.filter { it.knowledgePointId == pointId && !it.resolved }.sumOf { it.errorCount }

    /** 错 ≥3 次的知识点 = 高危。 */
    fun highRiskPointIds(): List<String> =
        entries.filter { !it.resolved }.map { it.knowledgePointId }.distinct()
            .filter { errorCount(it) >= HIGH_RISK_THRESHOLD }

    fun isHighRisk(pointId: String): Boolean = errorCount(pointId) >= HIGH_RISK_THRESHOLD

    /** 错题复习队列：每知识点一条，nextReviewAt = 最后一次错误 + 1 天。 */
    fun reviewQueue(): List<KnowledgeMastery> =
        entries.filter { !it.resolved }
            .groupBy { it.knowledgePointId }
            .map { (pointId, list) ->
                val last = list.maxOf { it.lastErrorAt }
                KnowledgeMastery(
                    pointId = pointId,
                    status = MasteryStatus.LEARNED,
                    reviewCount = list.sumOf { it.errorCount },
                    correctCount = 0,
                    lastReviewAt = last,
                    nextReviewAt = last + 86_400_000L,
                    recallAccuracy = 0f
                )
            }
            .sortedBy { it.nextReviewAt }

    /** 考前 3 天内。 */
    fun isSprintTime(examAt: Long, now: Long = now()): Boolean =
        examAt - now in 0..THREE_DAYS_MS

    /** 错题冲刺卷：高危知识点的复习队列。 */
    fun sprintReviewQueue(examAt: Long, now: Long = now()): List<KnowledgeMastery> =
        if (isSprintTime(examAt, now)) reviewQueue().filter { highRiskPointIds().contains(it.pointId) } else emptyList()

    fun resolve(questionId: String) {
        val idx = entries.indexOfFirst { it.questionId == questionId }
        if (idx >= 0) entries[idx] = entries[idx].copy(resolved = true)
    }

    companion object {
        const val HIGH_RISK_THRESHOLD = 3
        const val THREE_DAYS_MS = 3 * 86_400_000L
    }
}
