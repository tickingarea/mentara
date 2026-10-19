package com.learnagent.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * 转录文本片段
 * 
 * @property start 开始时间（秒）
 * @property end 结束时间（秒）
 * @property text 转录文本内容
 * @property speaker 说话者标识
 */
@Serializable
data class TranscriptSegment(
    @SerialName("start")
    val start: Double,
    
    @SerialName("end")
    val end: Double,
    
    @SerialName("text")
    val text: String,
    
    @SerialName("speaker")
    val speaker: String? = null
)