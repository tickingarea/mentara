package com.learnagent.core

import com.learnagent.data.FrameOcr

/**
 * 帧提取接口
 * 
 * 负责从视频中提取关键帧并进行OCR识别
 */
interface FrameExtractor {
    
    /**
     * 从视频中提取帧并进行OCR识别
     * 
     * @param videoPath 视频文件路径
     * @return 帧OCR识别结果列表
     */
    suspend fun extract(videoPath: String): List<FrameOcr>
}