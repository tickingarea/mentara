package com.learnagent.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DataModelTest {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; isLenient = true }

    @Test
    fun testVideoMetaSerialization() {
        val video = VideoMeta(
            videoId = "v1",
            deviceId = "d1",
            lastModified = 0L,
            schemaVersion = 1,
            title = "Test",
            duration = 100.0
        )
        val text = json.encodeToString(video)
        val decoded = json.decodeFromString<VideoMeta>(text)
        assertEquals(video.videoId, decoded.videoId)
        assertEquals(video.deviceId, decoded.deviceId)
        assertEquals(video.title, decoded.title)
        assertEquals(video.duration, decoded.duration)
    }

    @Test
    fun testTranscriptSegmentSerialization() {
        val seg = TranscriptSegment(
            start = 0.0,
            end = 5.0,
            text = "hello",
            speaker = null
        )
        val text = json.encodeToString(seg)
        val decoded = json.decodeFromString<TranscriptSegment>(text)
        assertEquals(seg.start, decoded.start)
        assertEquals(seg.end, decoded.end)
        assertEquals(seg.text, decoded.text)
    }

    @Test
    fun testFrameOcrSerialization() {
        val frame = FrameOcr(
            timestamp = 1.5,
            text = "PPT Title",
            imagePath = "/path/frame.jpg"
        )
        val text = json.encodeToString(frame)
        val decoded = json.decodeFromString<FrameOcr>(text)
        assertEquals(frame.timestamp, decoded.timestamp)
        assertEquals(frame.text, decoded.text)
        assertEquals(frame.imagePath, decoded.imagePath)
    }

    @Test
    fun testTimelineItemSerialization() {
        val timeline = TimelineItem(
            start = 0.0,
            end = 5.0,
            speech = "hello world",
            screenText = "PPT text",
            framePath = "/path/f.jpg"
        )
        val text = json.encodeToString(timeline)
        val decoded = json.decodeFromString<TimelineItem>(text)
        assertEquals(timeline.start, decoded.start)
        assertEquals(timeline.end, decoded.end)
        assertEquals(timeline.speech, decoded.speech)
        assertEquals(timeline.screenText, decoded.screenText)
    }

    @Test
    fun testTimelineItemListSerialization() {
        val items = listOf(
            TimelineItem(start = 0.0, end = 5.0, speech = "a", screenText = "A", framePath = ""),
            TimelineItem(start = 5.0, end = 10.0, speech = "b", screenText = "B", framePath = "")
        )
        val text = json.encodeToString(items)
        val decoded = json.decodeFromString<List<TimelineItem>>(text)
        assertEquals(items.size, decoded.size)
        assertEquals(items[0].speech, decoded[0].speech)
    }
}