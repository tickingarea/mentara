package com.learnagent.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TimelineItem(
    @SerialName("start") val start: Double,
    @SerialName("end") val end: Double,
    @SerialName("speech") val speech: String = "",
    @SerialName("screenText") val screenText: String = "",
    @SerialName("framePath") val framePath: String = "",
    /** 画面描述（视觉模型生成，可为空）。 */
    @SerialName("caption") val caption: String = ""
)
