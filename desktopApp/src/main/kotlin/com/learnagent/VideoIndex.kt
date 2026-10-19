package com.learnagent

import com.learnagent.core.Indexer
import com.learnagent.core.merge
import com.learnagent.data.FrameOcr
import com.learnagent.data.TimelineItem
import com.learnagent.data.TranscriptSegment

/**
 * 桌面端可测试的检索逻辑。
 *
 * 先用 Merger 把"语音 + 画面"融合成时间线，再用 shared 的 Indexer 做关键词检索。
 * UI 只负责渲染，所有逻辑放在这里以便单元测试。
 */
object VideoIndex {

    /**
     * 构造一个代表"视频内容"的时间线（30-40 秒的课程片段）。
     * 这是本地示例数据，用于在未接入真实转写/OCR 前驱动 UI。
     */
    fun sampleTimeline(): List<TimelineItem> {
        val transcript = listOf(
            TranscriptSegment(start = 0.0, end = 10.0, text = "这节课我们讲梯度下降", speaker = null),
            TranscriptSegment(start = 10.0, end = 20.0, text = "它是优化算法里的基础方法", speaker = null),
            TranscriptSegment(start = 20.0, end = 30.0, text = "接下来讲反向传播", speaker = null),
            TranscriptSegment(start = 30.0, end = 40.0, text = "最后讲卷积神经网络处理图像", speaker = null)
        )
        val frames = listOf(
            FrameOcr(timestamp = 2.0, text = "梯度下降 = 沿负梯度更新参数", imagePath = "frame_001.jpg"),
            FrameOcr(timestamp = 12.0, text = "学习率控制步长", imagePath = "frame_002.jpg"),
            FrameOcr(timestamp = 22.0, text = "反向传播计算梯度", imagePath = "frame_003.jpg"),
            FrameOcr(timestamp = 32.0, text = "CNN 用于图像识别", imagePath = "frame_004.jpg")
        )
        return merge(transcript, frames)
    }

    /** 在给定时间线上按关键词检索，返回按相关性排序的条目。 */
    fun search(items: List<TimelineItem>, query: String, topK: Int = 5): List<TimelineItem> {
        val indexer = Indexer()
        indexer.index(items)
        return indexer.search(query, topK)
    }

    /** 把秒数格式化成 mm:ss。 */
    fun formatTime(seconds: Double): String {
        val total = seconds.toInt()
        val m = total / 60
        val s = total % 60
        return "%02d:%02d".format(m, s)
    }

    /** 把一条时间线渲染成可读文本，包含时间戳与内容。 */
    fun format(item: TimelineItem): String {
        val range = "[${formatTime(item.start)} - ${formatTime(item.end)}]"
        val speech = item.speech.ifBlank { "（无语音）" }
        val screen = item.screenText.ifBlank { "（无画面文字）" }
        return "$range 语音: $speech | 画面: $screen"
    }
}
