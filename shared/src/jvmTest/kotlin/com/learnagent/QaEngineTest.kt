package com.learnagent

import com.learnagent.core.LocalQaEngine
import com.learnagent.core.merge
import com.learnagent.data.FrameOcr
import com.learnagent.data.TimelineItem
import com.learnagent.data.TranscriptSegment
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QaEngineTest {

    private fun timeline(): List<TimelineItem> = merge(
        listOf(
            TranscriptSegment(start = 0.0, end = 10.0, text = "梯度下降是优化算法", speaker = null),
            TranscriptSegment(start = 10.0, end = 20.0, text = "反向传播计算梯度", speaker = null)
        ),
        listOf(FrameOcr(timestamp = 2.0, text = "梯度下降 = 沿负梯度更新参数", imagePath = "f1.jpg"))
    )

    private fun engine(items: List<TimelineItem>) = LocalQaEngine { q, k ->
        val keywords = q.split(" ").filter { it.isNotBlank() }
        items
            .filter { item -> keywords.any { item.speech.contains(it) || item.screenText.contains(it) } }
            .take(k)
    }

    @Test
    fun testLocalQaFindsContent() = runBlocking {
        val answer = engine(timeline()).ask("梯度")
        assertTrue(answer.answer.contains("梯度"), "答案应包含检索到的内容")
        assertTrue(answer.citations.isNotEmpty(), "应返回时间戳引用")
    }

    @Test
    fun testLocalQaNoMatch() = runBlocking {
        val answer = engine(emptyList()).ask("量子力学")
        assertEquals(emptyList(), answer.citations, "无匹配时不应有引用")
        assertTrue(answer.answer.contains("未提及"), "无匹配时应提示未提及")
    }

    @Test
    fun testLocalQaCitationFormat() = runBlocking {
        val answer = engine(timeline()).ask("梯度")
        assertTrue(answer.citations.all { it.matches(Regex("\\d{2}:\\d{2}")) }, "引用应为 mm:ss 格式")
    }
}
