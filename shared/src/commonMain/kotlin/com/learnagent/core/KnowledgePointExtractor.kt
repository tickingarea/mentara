package com.learnagent.core

import com.learnagent.data.ExamLevel
import com.learnagent.data.KnowledgePoint
import com.learnagent.data.LearningUnit
import com.learnagent.data.MasteryStatus

/**
 * 知识点提取器（规则版）：
 * 每个 LearningUnit 生成 1-3 个知识点；考纲等级由 importance 映射；掌握状态初始 NOT_LEARNED。
 */
class KnowledgePointExtractor {

    fun extract(unit: LearningUnit, videoId: String): List<KnowledgePoint> {
        val text = (unit.speech + "。" + unit.screenText).trim()
        if (text.isBlank()) return emptyList()

        val names = LinkedHashSet<String>()
        unit.keywords.forEach { kw -> if (kw.isNotBlank()) names.add(kw.trim()) }
        PROPER_NOUN.findAll(text).forEach { m -> names.add(m.value) }
        if (names.isEmpty()) names.add(text.take(12))

        val examLevel = when {
            unit.importance >= 5 -> ExamLevel.MUST_KNOW
            unit.importance == 4 -> ExamLevel.OFTEN
            unit.importance == 3 -> ExamLevel.UNDERSTAND
            else -> ExamLevel.OPTIONAL
        }
        val definition = unit.oneLineSummary.ifBlank { text.take(50) }

        return names.take(3).mapIndexed { i, name ->
            KnowledgePoint(
                id = "${videoId}_kp_${unit.start.toInt()}_$i",
                name = name,
                definition = definition,
                sourceVideoId = videoId,
                sourceTimestamp = unit.start,
                examLevel = examLevel,
                masteryStatus = MasteryStatus.NOT_LEARNED,
                relatedIds = unit.relatedIds
            )
        }
    }

    private companion object {
        val PROPER_NOUN = Regex("\\b[A-Z][A-Za-z0-9+#._-]{1,}\\b")
    }
}
