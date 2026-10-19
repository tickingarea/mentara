package com.learnagent.core

import com.learnagent.data.KnowledgeMastery
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpacedRepetitionEngineTest {

    private val engine = SpacedRepetitionEngine()
    private fun fresh() = KnowledgeMastery(pointId = "p1")

    /** q=5 连续 5 次（每天一次）→ EF 收敛在 [2.5, 2.7]。 */
    @Test
    fun testEFConvergence() {
        var m = fresh()
        var day = 10L * 86_400_000L
        repeat(5) {
            day += 86_400_000L + 1
            m = engine.update(m, 5, now = day)
        }
        assertTrue(m.easeFactor in 2.5f..2.7f, "EF 应收敛，实际=${m.easeFactor}")
    }

    /** q=0 连续 5 次 → EF 不低于 1.3（恰好 1.3）。 */
    @Test
    fun testEFLowerBound() {
        var m = fresh()
        var day = 10L * 86_400_000L
        repeat(5) {
            day += 86_400_000L + 1
            m = engine.update(m, 0, now = day)
        }
        assertEquals(1.3f, m.easeFactor)
    }

    /** q=5 时 interval 序列 = 1, 6, 15, 39, 101。 */
    @Test
    fun testIntervalSequence() {
        var m = fresh()
        val seq = mutableListOf<Int>()
        var day = 10L * 86_400_000L
        repeat(5) {
            day += 86_400_000L + 1
            m = engine.update(m, 5, now = day)
            seq.add(m.interval)
        }
        assertEquals(listOf(1, 6, 15, 39, 101), seq)
    }

    /** 同一天两次 q=5 → reviewCount 只 +1。 */
    @Test
    fun testSameDayDebounce() {
        val t0 = 1_700_000_000_000L
        var m = engine.update(fresh(), 5, now = t0)
        m = engine.update(m, 5, now = t0 + 60_000L)
        assertEquals(1, m.reviewCount)
    }

    /** 答错 q=0 → interval 重置为 1。 */
    @Test
    fun testWrongResetInterval() {
        var m = fresh()
        var day = 10L * 86_400_000L
        day += 86_400_000L + 1
        m = engine.update(m, 5, now = day)     // interval=1
        day += 86_400_000L + 1
        m = engine.update(m, 5, now = day)     // interval=6
        day += 86_400_000L + 1
        m = engine.update(m, 0, now = day)     // 答错
        assertEquals(1, m.interval)
    }

    /** getDueCards 只返回到期且非 NOT_LEARNED 的卡。 */
    @Test
    fun testDueCards() {
        val now = 1_700_000_000_000L
        val learned = KnowledgeMastery(pointId = "a", status = com.learnagent.data.MasteryStatus.LEARNED, nextReviewAt = now - 1)
        val future = KnowledgeMastery(pointId = "b", status = com.learnagent.data.MasteryStatus.LEARNED, nextReviewAt = now + 86_400_000L)
        val newbie = KnowledgeMastery(pointId = "c", status = com.learnagent.data.MasteryStatus.NOT_LEARNED, nextReviewAt = now - 1)
        val due = engine.getDueCards(listOf(learned, future, newbie), now)
        assertEquals(listOf("a"), due.map { it.pointId })
    }
}
