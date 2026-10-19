package com.learnagent.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 决策记录：选择 + 理由 + 结果，经验层核心。
 * resultGood 为 null 表示结果未回填；followUpAt 为复盘提醒时间（0 = 未设置）。
 */
@Serializable
data class Decision(
    @SerialName("id") val id: String,
    @SerialName("problem") val problem: String,
    @SerialName("options") val options: List<String> = emptyList(),
    @SerialName("chosen") val chosen: String,
    @SerialName("reason") val reason: String,
    @SerialName("constraints") val constraints: List<String> = emptyList(),
    @SerialName("result") val result: String = "",
    @SerialName("resultGood") val resultGood: Boolean? = null,
    @SerialName("createdAt") val createdAt: Long,
    @SerialName("followUpAt") val followUpAt: Long = 0L,
    @SerialName("followUpDone") val followUpDone: Boolean = false
)
