package com.learnagent.core

import com.learnagent.data.FrameOcr

actual class FrameExtractor {
    
    actual suspend fun extract(videoPath: String): List<FrameOcr> {
        // TODO: 实现桌面平台的帧提取和OCR
        return emptyList()
    }
}