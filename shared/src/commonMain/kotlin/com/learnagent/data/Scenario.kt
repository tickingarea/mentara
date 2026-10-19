package com.learnagent.data

import kotlinx.serialization.Serializable

/** 场景：一个由视频内容引发的真实问题/情境。 */
@Serializable
data class Scenario(
    val id: String,
    val question: String,
    val context: String = "",
    val createdAt: Long = 0L,
    val relatedTimelineIds: List<String> = emptyList()
)
