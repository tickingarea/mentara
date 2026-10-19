package com.learnagent.core

import com.learnagent.data.TranscriptSegment
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StudyPrecisionTest {

    @Test
    fun atmosphereClassifiesQuantitatively() {
        // 昏暗 / 冷调 / 低饱和
        val dark = Atmosphere.fromAverages(rAvg = 18f, gAvg = 22f, bAvg = 70f, minLuma = 8f, maxLuma = 60f)
        assertTrue(dark.labels.contains("昏暗"), "${dark.labels}")
        assertTrue(dark.labels.contains("冷调"), "${dark.labels}")

        // 明亮 / 暖调 / 鲜艳
        val bright = Atmosphere.fromAverages(rAvg = 230f, gAvg = 190f, bAvg = 90f, minLuma = 20f, maxLuma = 250f)
        assertTrue(bright.labels.contains("明亮"), "${bright.labels}")
        assertTrue(bright.labels.contains("暖调"), "${bright.labels}")
    }

    private class Fake(private val reply: String) : LlmClient {
        override suspend fun generateResponse(prompt: String, config: LlmConfig): String = reply
    }

    private fun seg(text: String) = TranscriptSegment(start = 12.0, end = 15.0, text = text, speaker = null)

    @Test
    fun polisherConsensusHighConfidence() = runBlocking {
        val p = TranscriptPolisher(
            listOf("A" to Fake("梯度下降"), "B" to Fake("梯度下降"), "C" to Fake("梯度下降"))
        )
        val r = p.polish("v1", listOf(seg("梯渡下降")))
        val s = r.segments[0]
        assertEquals("梯度下降", s.final)
        assertEquals(1.0f, s.confidence)
        assertEquals(12.0, s.start)
        assertEquals(15.0, s.end)
        assertTrue(p.markdown(r).contains("00:12"))
    }

    @Test
    fun polisherTwoVotesMediumConfidence() = runBlocking {
        val p = TranscriptPolisher(
            listOf("A" to Fake("梯度下降"), "B" to Fake("梯度下降"), "C" to Fake("剃度下降"))
        )
        val s = p.polish("v1", listOf(seg("梯渡下降"))).segments[0]
        assertEquals("梯度下降", s.final)
        assertEquals(0.7f, s.confidence)
        assertTrue(s.alternatives.contains("剃度下降"))
    }

    @Test
    fun polisherAllDisagreeKeepsOriginal() = runBlocking {
        val p = TranscriptPolisher(
            listOf("A" to Fake("甲"), "B" to Fake("乙"), "C" to Fake("丙"))
        )
        val s = p.polish("v1", listOf(seg("原句"))).segments[0]
        assertEquals("原句", s.final)
        assertEquals(0.3f, s.confidence)
        assertEquals(3, s.alternatives.size)
    }

    private fun item(start: Double, speech: String, screen: String = "") =
        com.learnagent.data.TimelineItem(start = start, end = start + 5, speech = speech, screenText = screen)

    @Test
    fun keyKnowledgeMarksEmphasisHeadingAndRepeat() {
        val timeline = listOf(
            item(0.0, "这节课的重点是反向传播"),
            item(10.0, "随便聊两句天气怎么样呢"),
            item(20.0, "", "梯度下降"),
            item(30.0, "梯度下降的直觉是沿着最陡方向走"),
            item(40.0, "梯度下降的直觉是沿着最陡方向走")
        )
        val tags = KeyKnowledge.tags(timeline)
        assertEquals("强调", tags[0])
        assertEquals(null, tags[1])
        assertEquals("标题", tags[2])
        assertEquals("重复", tags[3])
        assertEquals("重复", tags[4])
    }
}
