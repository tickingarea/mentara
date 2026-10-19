package com.learnagent.core

import com.learnagent.data.TranscriptSegment

/**
 * 视频转录接口
 * 
 * 负责将视频文件转录为文本内容
 */
interface Transcriber {
    
    /**
     * 转录视频文件
     * 
     * @param videoPath 视频文件路径
     * @return 转录文本片段列表
     */
    suspend fun transcribe(videoPath: String): List<TranscriptSegment>
}