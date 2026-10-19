package com.learnagent.core

import com.learnagent.data.TechCard
import com.learnagent.data.TechUsage
import com.learnagent.platform.TechStore
import com.learnagent.platform.currentTimeMillis

/** 周报数据聚合结果（打开应用时生成，不是推送）。 */
data class WeeklyReport(
    val weekStart: Long,
    val weekEnd: Long,
    val newTechsCount: Int,          // 本周新增的 DISCOVERED
    val activeUseCount: Int,         // 本周 usedCount 增加的总和（近似：本周活跃技术的当前 usedCount 之和）
    val newDecisionsCount: Int,      // 本周新增的 Decision
    val strongDomains: List<String>, // usedCount >= 3 的类别
    val weakDomains: List<String>,   // seenCount >= 3 但 usedCount = 0 的类别
    val focusTech: String?,          // 本周 usedCount 增长最多的（近似：本周活跃且 usedCount 最大）
    val suggestion: String
)

/**
 * 周报生成器（每周日 00:00 起的新一周，打开时按需生成）。
 * 周起点按 UTC 近似（commonMain 无时区 API）；已读状态存 TechStore.meta。
 */
class WeeklyReportGenerator(
    private val techStore: TechStore,
    private val nowMillis: Long = currentTimeMillis()
) {

    fun generate(): WeeklyReport {
        val start = weekStart(nowMillis)
        val end = start + MS_PER_WEEK
        val inWeek: (Long) -> Boolean = { it in start until end }

        val usages = techStore.getAllTechUsage()
        val decisions = techStore.getAllDecisions()
        val cards = techStore.getAllTechCards()

        val newTechsCount = usages.count { inWeek(it.firstSeenAt) }
        val weekActive = usages.filter { inWeek(it.lastSeenAt) }
        val activeUseCount = weekActive.sumOf { it.usedCount }
        val newDecisionsCount = decisions.count { inWeek(it.createdAt) }

        val nameToCategory: (String) -> String = { name ->
            cards.firstOrNull { it.name.equals(name, ignoreCase = true) }?.category?.name ?: "OTHER"
        }
        val strongDomains = usages.filter { it.usedCount >= 3 }.map { nameToCategory(it.techName) }.distinct()
        val weakDomains = usages.filter { it.seenCount >= 3 && it.usedCount == 0 }
            .map { nameToCategory(it.techName) }.distinct()

        val focusTech = weekActive.maxByOrNull { it.usedCount }?.techName
            ?: usages.maxByOrNull { it.usedCount }?.techName

        val suggestion = buildString {
            append(if (focusTech != null) "本周最活跃：$focusTech。" else "本周暂无使用痕迹。")
            if (strongDomains.isNotEmpty()) append("强项领域：${strongDomains.joinToString("/")}。")
            if (weakDomains.isNotEmpty()) append("「见过但没用过」：${weakDomains.joinToString("/")}，建议写一个最小实现练手。")
            if (newDecisionsCount > 0) append("本周记录了 $newDecisionsCount 个决策。")
        }

        return WeeklyReport(
            weekStart = start,
            weekEnd = end,
            newTechsCount = newTechsCount,
            activeUseCount = activeUseCount,
            newDecisionsCount = newDecisionsCount,
            strongDomains = strongDomains,
            weakDomains = weakDomains,
            focusTech = focusTech,
            suggestion = suggestion
        )
    }

    fun isUnread(): Boolean = techStore.getMeta(META_KEY) != weekKey(nowMillis)

    fun markRead() = techStore.saveMeta(META_KEY, weekKey(nowMillis))

    companion object {
        const val MS_PER_WEEK = 7 * 86_400_000L
        private const val META_KEY = "weekly_report_seen"

        /** 周日 00:00（UTC 近似）。 */
        fun weekStart(now: Long): Long {
            val day = now.floorDiv(86_400_000L)
            val offset = ((day - 3) % 7 + 7) % 7   // 1970-01-01 是周四，+3 对齐周日
            return (day - offset) * 86_400_000L
        }

        fun weekKey(now: Long): String = "w" + (weekStart(now) / 86_400_000L)
    }
}
