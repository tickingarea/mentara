package com.learnagent.core

import com.learnagent.data.TranscriptSegment

actual class Transcriber {
    
    actual suspend fun transcribe(videoPath: String): List<TranscriptSegment> {
        // TODO: 实现桌面平台的视频转录
        return emptyList()
    }
}