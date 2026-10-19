package com.learnagent.data

import kotlinx.serialization.Serializable

/** 经验：面对某个场景时的选择、结果与教训。 */
@Serializable
data class Experience(
    val id: String,
    val scenarioId: String,
    val choice: String = "",
    val result: String = "",
    val lesson: String = "",
    val recordedAt: Long = 0L,
    val followUpAt: Long = 0L,
    val followUpDone: Boolean = false
)
