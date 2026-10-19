package com.learnagent

import com.learnagent.core.Indexer
import com.learnagent.core.LocalQaEngine
import com.learnagent.core.merge
import com.learnagent.data.FrameOcr
import com.learnagent.data.TimelineItem
import com.learnagent.data.TranscriptSegment
import com.learnagent.data.VideoMeta
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 完整流程测试：视频元数据 → 融合 → 落盘/读回 → 索引 → 检索 → 问答。
 */
class PipelineTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val transcript = listOf(
        TranscriptSegment(start = 0.0, end = 10.0, text = "这节课我们讲梯度下降", speaker = null),
        TranscriptSegment(start = 10.0, end = 20.0, text = "它是优化算法里的基础方法", speaker = null),
        TranscriptSegment(start = 20.0, end = 30.0, text = "接下来讲反向传播", speaker = null),
        TranscriptSegment(start = 30.0, end = 40.0, text = "最后讲卷积神经网络处理图像", speaker = null)
    )

    private val frames = listOf(
        FrameOcr(timestamp = 2.0, text = "梯度下降 = 沿负梯度更新参数", imagePath = "frame_001.jpg"),
        FrameOcr(timestamp = 22.0, text = "反向传播计算梯度", imagePath = "frame_003.jpg"),
        FrameOcr(timestamp = 32.0, text = "CNN 用于图像识别", imagePath = "frame_004.jpg")
    )

    @Test
    fun testFullPipelineVideoToAnswer() = runBlocking {
        // 1) 视频元数据序列化往返
        val meta = VideoMeta(
            videoId = "v1", deviceId = "d1", lastModified = 0L,
            schemaVersion = 1, title = "梯度下降课", duration = 40.0
        )
        val metaJson = json.encodeToString(meta)
        assertEquals("v1", json.decodeFromString<VideoMeta>(metaJson).videoId)

        // 2) 融合
        val timeline = merge(transcript, frames)
        assertTrue(timeline.isNotEmpty(), "融合结果不应为空")

        // 3) 落盘 + 读回（模拟 results 目录）
        val file = File.createTempFile("learnagent_timeline", ".json")
        try {
            file.writeText(json.encodeToString(timeline))
            val loaded = json.decodeFromString<List<TimelineItem>>(file.readText())
            assertEquals(timeline.size, loaded.size, "落盘读回后条目数应一致")

            // 4) 索引 + 检索
            val indexer = Indexer()
            indexer.index(loaded)
            val hits = indexer.search("反向传播")
            assertTrue(hits.isNotEmpty(), "应检索到'反向传播'")
            assertTrue(hits.any { it.speech.contains("反向传播") }, "命中项应包含关键词")

            // 5) 问答
            val engine = LocalQaEngine { q, k -> indexer.search(q, k) }
            val answer = engine.ask("反向传播")
            assertTrue(answer.answer.contains("反向传播"), "回答应包含相关内容")
            assertTrue(answer.citations.isNotEmpty(), "回答应有时间戳引用")

            // 6) 不存在的词
            assertEquals(0, indexer.search("量子力学").size, "不应检索到无关词")
        } finally {
            file.delete()
        }
    }

    @Test
    fun testMonotonicTimelineAfterRoundTrip() {
        val timeline = merge(transcript, frames)
        val file = File.createTempFile("learnagent_timeline2", ".json")
        try {
            file.writeText(json.encodeToString(timeline))
            val loaded = json.decodeFromString<List<TimelineItem>>(file.readText())
            for (i in 1 until loaded.size) {
                assertTrue(
                    loaded[i].start >= loaded[i - 1].start,
                    "时间戳应递增：${loaded[i - 1].start} -> ${loaded[i].start}"
                )
            }
        } finally {
            file.delete()
        }
    }
}
