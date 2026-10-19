package com.learnagent.core

import com.learnagent.data.KnowledgePoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InterleavingEngineTest {

    private fun kp(chapter: Int, index: Int) = KnowledgePoint(
        id = "p_${chapter}_$index",
        name = "点$chapter-$index",
        definition = "定义$chapter-$index",
        chapterId = "ch$chapter"
    )

    /** 3 章节 × 5 点，抽 9 → 每章节 3 点。 */
    @Test
    fun testInterleave() {
        val points = (0 until 3).flatMap { ch -> (0 until 5).map { i -> kp(ch, i) } }
        val picked = InterleavingEngine().interleave(points, 9)
        assertEquals(9, picked.size)
        val perChapter = picked.groupingBy { it.chapterId }.eachCount()
        assertEquals(3, perChapter.values.size)
        perChapter.values.forEach { assertEquals(3, it, "每章节应抽 3 个") }
    }

    /** 只有 1 章节 → 打乱返回（不崩）。 */
    @Test
    fun testSingleChapter() {
        val points = (0 until 5).map { kp(0, it) }
        val picked = InterleavingEngine().interleave(points, 4)
        assertEquals(4, picked.size)
        assertTrue(picked.all { points.contains(it) })
        // 抽满时内容等价
        assertEquals(points.toSet(), InterleavingEngine().interleave(points, 10).toSet())
    }
}
