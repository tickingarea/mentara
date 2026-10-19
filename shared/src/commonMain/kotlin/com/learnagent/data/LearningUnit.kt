package com.learnagent.data

import kotlinx.serialization.Serializable

enum class Emotion { NEUTRAL, EXCITED, CALM, CONFUSED, EMPHATIC, HUMOROUS }

enum class Tone { NORMAL, DEFINITION, EXAMPLE, EMPHASIS, TRANSITION, SUMMARY, WARNING }

enum class Pace { NORMAL, FAST, SLOW, PAUSE }

enum class SemanticType { DEFINITION, EXAMPLE, DERIVATION, CONCLUSION, COMPARISON, WARNING, BACKGROUND, UNKNOWN }

/**
 * 学习单元：TimelineItem 的"升华"结果。
 * 四层 + 升华层，供学习卡片 / 问答 / 图谱使用。
 */
@Serializable
data class LearningUnit(
    val id: String,
    val videoId: String,
    val start: Double,
    val end: Double,
    // 第 1 层：语音
    val speech: String = "",
    val speaker: String? = null,
    // 第 2 层：画面
    val screenText: String = "",
    val visualDescription: String = "",
    val framePath: String = "",
    // 第 3 层：表达
    val emotion: Emotion = Emotion.NEUTRAL,
    val tone: Tone = Tone.NORMAL,
    val pace: Pace = Pace.NORMAL,
    // 第 4 层：语义
    val semanticType: SemanticType = SemanticType.UNKNOWN,
    val importance: Int = 3,
    val keywords: List<String> = emptyList(),
    val relatedIds: List<String> = emptyList(),
    // 升华层
    val oneLineSummary: String = "",
    val intuition: String = "",
    val whenToUse: String = "",
    val commonPitfalls: String = ""
)
