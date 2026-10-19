package com.learnagent.data

import com.learnagent.core.merge
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MergerTest {

    @Test
    fun testMergeBasic() {
        val transcript = listOf(
            TranscriptSegment(start = 0.0, end = 5.0, text = "hello", speaker = null),
            TranscriptSegment(start = 5.0, end = 10.0, text = "world", speaker = null)
        )
        val frames = listOf(
            FrameOcr(timestamp = 2.0, text = "PPT-1", imagePath = ""),
            FrameOcr(timestamp = 7.0, text = "PPT-2", imagePath = "")
        )
        val merged = merge(transcript, frames)
        assertTrue(merged.isNotEmpty())
        assertTrue(merged.any { it.speech.contains("hello") })
    }

    @Test
    fun testMergeOnlyTranscript() {
        val transcript = listOf(
            TranscriptSegment(start = 0.0, end = 5.0, text = "only speech", speaker = null)
        )
        val merged = merge(transcript, emptyList())
        assertEquals(1, merged.size)
        assertEquals("only speech", merged[0].speech)
    }

    @Test
    fun testMergeOnlyFrames() {
        val frames = listOf(
            FrameOcr(timestamp = 1.0, text = "only screen", imagePath = "")
        )
        val merged = merge(emptyList(), frames)
        assertTrue(merged.isNotEmpty())
        assertTrue(merged.any { it.screenText.contains("only screen") })
    }
}