package com.learnagent.data

import com.learnagent.core.Indexer
import com.learnagent.core.merge
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EndToEndTest {

    /**
     * 模拟一个真实场景：
     * 一个 30 秒的视频，讲了"梯度下降"和"反向传播"
     * 有 3 段语音 + 3 个画面
     * 期望：融合成 timeline，能被索引，能按关键词检索到正确时间戳
     */
    private fun buildSampleVideo() = Pair(
        // 语音
        listOf(
            TranscriptSegment(start = 0.0, end = 10.0, text = "这节课我们讲梯度下降", speaker = null),
            TranscriptSegment(start = 10.0, end = 20.0, text = "它是优化算法里的基础方法", speaker = null),
            TranscriptSegment(start = 20.0, end = 30.0, text = "接下来讲反向传播", speaker = null)
        ),
        // 画面
        listOf(
            FrameOcr(timestamp = 2.0, text = "梯度下降 = 沿负梯度更新参数", imagePath = "frame_001.jpg"),
            FrameOcr(timestamp = 12.0, text = "学习率控制步长", imagePath = "frame_002.jpg"),
            FrameOcr(timestamp = 22.0, text = "反向传播计算梯度", imagePath = "frame_003.jpg")
        )
    )

    @Test
    fun testFullPipeline() {
        val (transcript, frames) = buildSampleVideo()

        // 1. 融合
        val timeline = merge(transcript, frames)
        assertTrue(timeline.isNotEmpty(), "融合结果不应为空")

        // 2. 索引
        val indexer = Indexer()
        indexer.index(timeline)

        // 3. 检索"梯度"
        val results = indexer.search("梯度", topK = 5)
        assertTrue(results.isNotEmpty(), "检索'梯度'应有结果")

        // 4. 检索"反向传播"
        val results2 = indexer.search("反向传播", topK = 5)
        assertTrue(results2.isNotEmpty(), "检索'反向传播'应有结果")

        // 5. 检索不存在的词
        val results3 = indexer.search("量子力学", topK = 5)
        assertEquals(0, results3.size, "检索'量子力学'不应有结果")
    }

    @Test
    fun testTimelineHasBothSpeechAndScreen() {
        val (transcript, frames) = buildSampleVideo()
        val timeline = merge(transcript, frames)

        // 至少有一条 timeline item 同时有 speech 和 screenText
        val hasBoth = timeline.any { it.speech.isNotBlank() && it.screenText.isNotBlank() }
        assertTrue(hasBoth, "至少一条应同时有语音和画面文字")
    }

    @Test
    fun testTimelineTimestampsMonotonic() {
        val (transcript, frames) = buildSampleVideo()
        val timeline = merge(transcript, frames)

        // 时间戳应该递增
        for (i in 1 until timeline.size) {
            assertTrue(
                timeline[i].start >= timeline[i - 1].start,
                "时间戳应该递增：${timeline[i-1].start} -> ${timeline[i].start}"
            )
        }
    }
}