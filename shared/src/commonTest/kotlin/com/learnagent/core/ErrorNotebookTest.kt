package com.learnagent.core

import com.learnagent.data.ErrorType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ErrorNotebookTest {

    /** 同 pointId 错 3 次 → 标记高危。 */
    @Test
    fun testHighRiskMarking() {
        var t = 1_000_000L
        val notebook = ErrorNotebook { t }
        notebook.record("q1", "kp1", "错答", "对答", "解释", ErrorType.CONCEPT_ERROR)
        t += 1000
        notebook.record("q2", "kp1", "错答", "对答", "解释", ErrorType.CONCEPT_ERROR)
        t += 1000
        assertFalse(notebook.isHighRisk("kp1"), "错 2 次不算高危")
        notebook.record("q3", "kp1", "错答", "对答", "解释", ErrorType.MISUNDERSTANDING)
        assertTrue(notebook.isHighRisk("kp1"), "错 3 次应高危")
        assertTrue(notebook.highRiskPointIds().contains("kp1"))
    }

    /** 同一题重复错 3 次也累计（errorCount 累加）。 */
    @Test
    fun testSameQuestionCounts() {
        var t = 2_000_000L
        val notebook = ErrorNotebook { t }
        repeat(3) {
            notebook.record("q1", "kp2", "错答", "对答", "解释", ErrorType.CARELESS_ERROR)
            t += 1000
        }
        assertEquals(3, notebook.errorCount("kp2"))
        assertTrue(notebook.isHighRisk("kp2"))
    }

    /** 错题入队 → nextReviewAt = 最后错误 + 1 天。 */
    @Test
    fun testAddToReviewQueue() {
        val t = 5_000_000L
        val notebook = ErrorNotebook { t }
        notebook.record("q9", "kp9", "错答", "对答", "解释", ErrorType.CALCULATION_ERROR)
        val queued = notebook.reviewQueue().first { it.pointId == "kp9" }
        assertEquals(t + 86_400_000L, queued.nextReviewAt)
    }

    /** 考前 3 天内可生成错题冲刺卷。 */
    @Test
    fun testSprintTime() {
        val now = 10_000_000_000L
        val notebook = ErrorNotebook { now }
        assertTrue(notebook.isSprintTime(now + 2 * 86_400_000L, now))
        assertFalse(notebook.isSprintTime(now + 10 * 86_400_000L, now))
    }
}
