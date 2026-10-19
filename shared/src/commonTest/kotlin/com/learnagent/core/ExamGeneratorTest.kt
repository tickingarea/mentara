package com.learnagent.core

import com.learnagent.data.KnowledgePoint
import com.learnagent.data.QuestionType
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class ExamGeneratorTest {

    private fun kp(index: Int, chapter: Int = index % 5) = KnowledgePoint(
        id = "p_${chapter}_$index",
        name = "知识点$index",
        definition = "这是第 $index 个知识点的定义，用于出卷测试",
        chapterId = "ch$chapter",
        sourceVideoId = "video_${chapter}"
    )

    /** 出 20 题 → 单选 8 / 多选 3 / 填空 3 / 简答 4 / 计算 2。 */
    @Test
    fun testQuestionTypeRatio() = runBlocking {
        val points = (0 until 25).map { kp(it) }
        val exam = ExamGenerator(null, points).generate(
            scope = ExamScope.MULTI_VIDEO,
            videoIds = emptyList(),
            questionCount = 20,
            difficulty = Difficulty.MEDIUM,
            interleaver = InterleavingEngine()
        )
        assertEquals(20, exam.questions.size)
        val counts = exam.questions.groupingBy { it.type }.eachCount()
        assertEquals(8, counts[QuestionType.SINGLE_CHOICE], "单选")
        assertEquals(3, counts[QuestionType.MULTIPLE_CHOICE], "多选")
        assertEquals(3, counts[QuestionType.FILL_BLANK], "填空")
        assertEquals(4, counts[QuestionType.SHORT_ANSWER], "简答")
        assertEquals(2, counts[QuestionType.CALCULATION], "计算")
        assertEquals("本地启发式", exam.source)
    }

    /** 只有 3 个知识点、出 10 题 → 返回 3 题不崩。 */
    @Test
    fun testInsufficientPoints() = runBlocking {
        val points = (0 until 3).map { kp(it) }
        val exam = ExamGenerator(null, points).generate(
            scope = ExamScope.SINGLE_VIDEO,
            videoIds = emptyList(),
            questionCount = 10,
            difficulty = Difficulty.EASY,
            interleaver = InterleavingEngine()
        )
        assertEquals(3, exam.questions.size)
        assertEquals(3, exam.questions.map { it.knowledgePointId }.distinct().size)
    }

    /** 配比函数自身：总数守恒。 */
    @Test
    fun testAllocateTypesTotal() {
        val gen = ExamGenerator(null)
        listOf(1, 2, 5, 10, 37).forEach { n ->
            val types = gen.allocateTypes(n)
            assertEquals(n, types.size, "n=$n 类型总数应守恒")
        }
    }
}
