package com.learnagent.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 代码片段：OCR 提取的代码，可复制。
 * language 为规范化语言名（python / java / kotlin / sql / ...），无法识别时为 text。
 */
@Serializable
data class CodeSnippet(
    @SerialName("id") val id: String,
    @SerialName("language") val language: String,
    @SerialName("purpose") val purpose: String,
    @SerialName("code") val code: String,
    @SerialName("comment") val comment: String = "",
    @SerialName("sourceVideoId") val sourceVideoId: String = "",
    @SerialName("sourceTimestamp") val sourceTimestamp: Double = 0.0,
    @SerialName("tags") val tags: List<String> = emptyList()
)
