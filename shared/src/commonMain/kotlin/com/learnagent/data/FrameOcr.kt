package com.learnagent.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * 帧OCR识别结果
 * 
 * @property timestamp 时间戳（秒）
 * @property text OCR识别的文本内容
 * @property imagePath 帧图片保存路径
 */
@Serializable
data class FrameOcr(
    @SerialName("timestamp")
    val timestamp: Double,
    
    @SerialName("text")
    val text: String,
    
    @SerialName("imagePath")
    val imagePath: String,

    /** 画面氛围（量化判定，如：昏暗·冷调·高对比）。 */
    @SerialName("mood")
    val mood: String = "",

    /** 画面描述（视觉模型生成，可能含推测）。 */
    @SerialName("caption")
    val caption: String = ""
)