package com.learnagent.data

/**
 * 结果目录结构常量
 */
object ResultsDir {
    /**
     * 结果根目录
     */
    const val RESULTS_ROOT = "results"
    
    /**
     * 元数据文件名
     */
    const val META_FILE = "meta.json"
    
    /**
     * 转录文件名
     */
    const val TRANSCRIPT_FILE = "transcript.json"
    
    /**
     * 帧OCR文件名
     */
    const val FRAMES_FILE = "frames.json"
    
    /**
     * 时间线文件名
     */
    const val TIMELINE_FILE = "timeline.json"
    
    /**
     * 构建视频目录路径
     */
    fun getVideoDir(videoId: String): String = "$RESULTS_ROOT/$videoId"
    
    /**
     * 构建元数据文件路径
     */
    fun getMetaPath(videoId: String): String = "${getVideoDir(videoId)}/$META_FILE"
    
    /**
     * 构建转录文件路径
     */
    fun getTranscriptPath(videoId: String): String = "${getVideoDir(videoId)}/$TRANSCRIPT_FILE"
    
    /**
     * 构建帧OCR文件路径
     */
    fun getFramesPath(videoId: String): String = "${getVideoDir(videoId)}/$FRAMES_FILE"
    
    /**
     * 构建时间线文件路径
     */
    fun getTimelinePath(videoId: String): String = "${getVideoDir(videoId)}/$TIMELINE_FILE"
}