package com.learnagent.core

import com.learnagent.data.KnowledgeMastery
import com.learnagent.data.KnowledgePoint
import com.learnagent.platform.currentTimeMillis
import kotlin.math.roundToInt

data class ScoreEstimate(
    val estimatedTotal: Int,           // 0-100
    val chapterScores: Map<String, Int>,
    val weakChapters: List<String>,
    val suggestedFocus: List<String>
)

/**
 * 分数估算：
 * 权重 MUST_KNOW ×3 / OFTEN ×2 / UNDERSTAND ×1 / OPTIONAL ×0.5；无考纲信息 → ×1。
 * 总分 = Σ(权重 × recallAccuracy) / Σ(权重) × 100。
 */
class ScoreEstimator {

    fun estimate(
        masteries: List<KnowledgeMastery>,
        points: List<KnowledgePoint>,
        syllabusInfo: Map<String, SyllabusInfo>
    ): ScoreEstimate {
        if (points.isEmpty()) return ScoreEstimate(0, emptyMap(), emptyList(), emptyList())
        val masteryByPoint = masteries.associateBy { it.pointId }
        var totalWeight = 0f
        var totalWeighted = 0f
        val chapterAgg = mutableMapOf<String, Pair<Float, Float>>()

        points.forEach { point ->
            val weight = syllabusInfo[point.id]?.let { levelWeight(it.level) } ?: 1f
            val rate = masteryByPoint[point.id]?.recallAccuracy ?: 0f
            totalWeight += weight
            totalWeighted += weight * rate
            val chapter = point.chapterId.ifBlank { point.sourceVideoId.ifBlank { "未分章" } }
            val (w, wr) = chapterAgg[chapter] ?: (0f to 0f)
            chapterAgg[chapter] = (w + weight) to (wr + weight * rate)
        }

        val total = if (totalWeight <= 0f) 0 else (totalWeighted / totalWeight * 100f).roundToInt().coerceIn(0, 100)
        val chapterScores = chapterAgg.mapValues { (_, v) ->
            if (v.first <= 0f) 0 else (v.second / v.first * 100f).roundToInt().coerceIn(0, 100)
        }
        val weakChapters = chapterScores.filter { it.value < WEAK_THRESHOLD }
            .entries.sortedBy { it.value }.map { it.key }.take(3)
        val focus = points
            .sortedByDescending { p ->
                val w = syllabusInfo[p.id]?.let { levelWeight(it.level) } ?: 1f
                w * (1f - (masteryByPoint[p.id]?.recallAccuracy ?: 0f))
            }
            .take(3)
            .map { it.name }
        return ScoreEstimate(total, chapterScores, weakChapters, focus)
    }

    companion object {
        const val WEAK_THRESHOLD = 60

        fun levelWeight(level: SyllabusLevel): Float = when (level) {
            SyllabusLevel.MUST_KNOW -> 3f
            SyllabusLevel.OFTEN -> 2f
            SyllabusLevel.UNDERSTAND -> 1f
            SyllabusLevel.OPTIONAL -> 0.5f
        }
    }
}

data class DailyReport(
    val newPointsCount: Int,
    val reviewAccuracy: Float,
    val estimatedScore: Int,
    val todayFocus: List<String>,
    val standardWarning: String?    // 规范答题错误率高时提示
)

/** 每日报告：聚合掌握度 + 估分 + 待复习焦点 + 规范警告。 */
class DailyReportGenerator(private val estimator: ScoreEstimator) {

    fun generate(
        masteries: List<KnowledgeMastery> = emptyList(),
        points: List<KnowledgePoint> = emptyList(),
        syllabusInfo: Map<String, SyllabusInfo> = emptyMap(),
        newPointsCount: Int = 0,
        standardErrorRate: Float = 0f,
        now: Long = currentTimeMillis()
    ): DailyReport {
        val reviewed = masteries.filter { it.reviewCount > 0 }
        val accuracy = if (reviewed.isEmpty()) 0f else reviewed.map { it.recallAccuracy }.average().toFloat()
        val estimate = estimator.estimate(masteries, points, syllabusInfo)
        val due = SpacedRepetitionEngine().getDueCards(masteries, now).take(5)
        val dueNames = due.mapNotNull { m -> points.firstOrNull { it.id == m.pointId }?.name }
        val focus = dueNames.ifEmpty { estimate.suggestedFocus }
        val warning = if (standardErrorRate > 0.5f) {
            "规范答题错误率偏高（${(standardErrorRate * 100).roundToInt()}%），建议先对齐答题模板。"
        } else null
        return DailyReport(newPointsCount, accuracy, estimate.estimatedTotal, focus, warning)
    }
}
