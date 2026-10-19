package com.learnagent.core

import com.learnagent.data.KnowledgeMastery
import com.learnagent.data.MasteryStatus
import com.learnagent.platform.currentTimeMillis

/**
 * SM-2 间隔重复引擎。
 *
 * q: 回忆质量 0-5（0-2 完全忘了 / 3 想起一部分 / 4 想起但有犹豫 / 5 瞬间想起）
 * EF 上限 2.6、下限 1.3（验收：q=5 ×5 收敛到 2.6；q=0 ×5 不低于 1.3）
 */
class SpacedRepetitionEngine {

    fun update(mastery: KnowledgeMastery, q: Int, now: Long = currentTimeMillis()): KnowledgeMastery {
        val quality = q.coerceIn(0, 5)
        // 1. 计算新 EF（含上下限）
        val newEF = (mastery.easeFactor + (0.1f - (5 - quality) * (0.08f + (5 - quality) * 0.02f)))
            .coerceIn(MIN_EF, MAX_EF)

        // 2. 计算新 interval
        val newInterval = when {
            quality < 3 -> 1                       // 答错，明天再来
            mastery.interval == 0 -> 1             // 第一次
            mastery.interval == 1 -> 6             // 第二次
            else -> (mastery.interval * newEF).toInt()
        }

        // 3. 防抖动：同一天内多次复习，只取最后一次 q（reviewCount 不增，更新 EF/interval）
        val lastReviewDay = mastery.lastReviewAt / 86_400_000L
        val today = now / 86_400_000L
        if (lastReviewDay == today && mastery.reviewCount > 0) {
            return mastery.copy(easeFactor = newEF, interval = newInterval)
        }

        return mastery.copy(
            easeFactor = newEF,
            interval = newInterval,
            reviewCount = mastery.reviewCount + 1,
            correctCount = if (quality >= 3) mastery.correctCount + 1 else mastery.correctCount,
            lastReviewAt = now,
            nextReviewAt = now + newInterval * 86_400_000L,
            recallAccuracy = mastery.correctCount.toFloat() / (mastery.reviewCount + 1),
            status = upgradeStatus(mastery, quality)
        )
    }

    private fun upgradeStatus(m: KnowledgeMastery, q: Int): MasteryStatus = when {
        q < 3 -> MasteryStatus.FAMILIAR
        m.correctCount >= 5 && m.interval >= 21 -> MasteryStatus.EXPERT
        m.correctCount >= 3 -> MasteryStatus.MASTERED
        else -> MasteryStatus.LEARNED
    }

    fun getDueCards(all: List<KnowledgeMastery>, now: Long = currentTimeMillis()): List<KnowledgeMastery> =
        all.filter { it.status != MasteryStatus.NOT_LEARNED && it.nextReviewAt in 1..now }
            .sortedBy { it.nextReviewAt }

    companion object {
        const val MIN_EF = 1.3f
        /** EF 上限：对应验收断言「连续 5 次 q=5 → EF ∈ [2.5, 2.7]」与间隔序列 1/6/15/39/101。 */
        const val MAX_EF = 2.6f
    }
}
