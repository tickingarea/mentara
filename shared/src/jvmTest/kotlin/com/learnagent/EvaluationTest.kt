package com.learnagent

import com.learnagent.core.Indexer
import com.learnagent.core.LocalQaEngine
import com.learnagent.core.merge
import com.learnagent.data.FrameOcr
import com.learnagent.data.TimelineItem
import com.learnagent.data.TranscriptSegment
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * 评测：用黄金集驱动 LocalQaEngine，统计答案命中率。
 *
 * 黄金集位于 `src/jvmTest/resources/golden_set.json`，共 10 题。
 * 判定方式：回答中是否包含期望的关键词（无匹配题期望包含“未提及”）。
 */
class EvaluationTest {

    @Serializable
    data class GoldenCase(val question: String, val expect: String)

    @Serializable
    data class GoldenSet(val cases: List<GoldenCase>)

    private val json = Json { ignoreUnknownKeys = true }

    private fun timeline(): List<TimelineItem> = merge(
        listOf(
            TranscriptSegment(0.0, 10.0, "这节课我们讲梯度下降", null),
            TranscriptSegment(10.0, 20.0, "它是优化算法里的基础方法", null),
            TranscriptSegment(20.0, 30.0, "接下来讲反向传播", null),
            TranscriptSegment(30.0, 40.0, "最后讲卷积神经网络处理图像", null)
        ),
        listOf(
            FrameOcr(2.0, "梯度下降 = 沿负梯度更新参数", "frame_001.jpg"),
            FrameOcr(12.0, "学习率控制步长", "frame_002.jpg"),
            FrameOcr(22.0, "反向传播计算梯度", "frame_003.jpg"),
            FrameOcr(32.0, "CNN 用于图像识别", "frame_004.jpg")
        )
    )

    @Test
    fun testGoldenSetAccuracy() = runBlocking {
        val raw = javaClass.getResourceAsStream("/golden_set.json")
        assertNotNull(raw, "缺少 golden_set.json")
        val set = json.decodeFromString<GoldenSet>(raw.readBytes().decodeToString())

        val indexer = Indexer()
        indexer.index(timeline())
        val engine = LocalQaEngine { q, k -> indexer.search(q, k) }

        var matched = 0
        for (case in set.cases) {
            val answer = engine.ask(case.question)
            val hit = answer.answer.contains(case.expect)
            if (hit) matched++
            println("[${if (hit) "PASS" else "FAIL"}] Q='${case.question}' expect='${case.expect}' -> ${answer.answer.replace("\n", " ")}")
        }

        val accuracy = matched.toDouble() / set.cases.size
        println("EVAL_ACCURACY=$matched/${set.cases.size} (${(accuracy * 100).toInt()}%)")
        println("EVAL_BASELINE=${(accuracy * 100).toInt()}%")

        assertEquals(set.cases.size, matched, "黄金集应全部命中，实际 $matched/${set.cases.size}")
    }
}
