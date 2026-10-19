package com.learnagent.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * 视频元数据信息
 * 
 * @property videoId 视频唯一标识符
 * @property deviceId 设备唯一标识符
 * @property lastModified 最后修改时间戳
 * @property schemaVersion 数据模式版本号
 * @property title 视频标题
 * @property duration 视频时长（秒）
 */
@Serializable
data class VideoMeta(
    @SerialName("videoId")
    val videoId: String,
    
    @SerialName("deviceId")
    val deviceId: String,
    
    @SerialName("lastModified")
    val lastModified: Long,
    
    @SerialName("schemaVersion")
    val schemaVersion: Int = 1,
    
    @SerialName("title")
    val title: String = "",
    
    @SerialName("duration")
    val duration: Double = 0.0
)