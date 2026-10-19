package com.learnagent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VideoIndexTest {

    @Test
    fun testSampleTimelineBuilt() {
        val items = VideoIndex.sampleTimeline()
        assertTrue(items.isNotEmpty(), "示例时间线不应为空")
        assertTrue(items.any { it.speech.isNotBlank() }, "应含语音内容")
        assertTrue(items.any { it.screenText.isNotBlank() }, "应含画面文字")
    }

    @Test
    fun testSearchGradient() {
        val items = VideoIndex.sampleTimeline()
        val results = VideoIndex.search(items, "梯度")
        assertTrue(results.isNotEmpty(), "检索'梯度'应有结果")
        assertTrue(results.any { it.start == 0.0 }, "应命中第一段（0s）")
    }

    @Test
    fun testSearchBackprop() {
        val items = VideoIndex.sampleTimeline()
        val results = VideoIndex.search(items, "反向传播")
        assertTrue(results.isNotEmpty(), "检索'反向传播'应有结果")
    }

    @Test
    fun testSearchNoMatch() {
        val items = VideoIndex.sampleTimeline()
        val results = VideoIndex.search(items, "量子力学")
        assertEquals(0, results.size, "检索'量子力学'不应有结果")
    }

    @Test
    fun testFormatTime() {
        assertEquals("00:00", VideoIndex.formatTime(0.0))
        assertEquals("00:12", VideoIndex.formatTime(12.9))
        assertEquals("01:05", VideoIndex.formatTime(65.0))
    }
}
