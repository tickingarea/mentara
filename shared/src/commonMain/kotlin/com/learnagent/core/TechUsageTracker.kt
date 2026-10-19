package com.learnagent.core

import com.learnagent.data.TechStatus
import com.learnagent.data.TechUsage
import com.learnagent.platform.TechStore
import com.learnagent.platform.currentTimeMillis

/**
 * 使用痕迹状态机。
 *
 * 触发：打开 TechCard → seen+1；搜索点击 → used+1；
 * 记录 Decision → decisionCount+1 / usedCount+2；完成 Project → used+3（接口预留）。
 *
 * 升级（单向只升不降）：
 * DISCOVERED(seen≥1) → FAMILIAR(seen≥3 或 used≥1) → USED(used≥3) → FLUENT(used≥5) → EXPERT(used≥10)
 *
 * 防抖：同一技术 1 小时内重复 seen 只算 1 次。
 */
class TechUsageTracker(
    private val techStore: TechStore,
    private val now: () -> Long = { currentTimeMillis() }
) {

    fun onTechCardOpened(techName: String) = mutate(techName) { u, t ->
        if (t - u.lastSeenAt >= SEEN_DEDUPE_MS) u.copy(seenCount = u.seenCount + 1, lastSeenAt = t) else u
    }

    fun onTechCardUsed(techName: String) = mutate(techName) { u, t ->
        u.copy(usedCount = u.usedCount + 1, lastSeenAt = t)
    }

    fun onDecisionRecorded(techNames: List<String>) {
        techNames.filter { it.isNotBlank() }.forEach { name ->
            mutate(name) { u, t -> u.copy(decisionCount = u.decisionCount + 1, usedCount = u.usedCount + 2, lastSeenAt = t) }
        }
    }

    /** 完成 Project（预留接口，UI 尚未接入）。 */
    fun onProjectCompleted(techNames: List<String>) {
        techNames.filter { it.isNotBlank() }.forEach { name ->
            mutate(name) { u, t -> u.copy(usedCount = u.usedCount + 3, lastSeenAt = t) }
        }
    }

    fun statusOf(usage: TechUsage): TechStatus = when {
        usage.usedCount >= 10 -> TechStatus.EXPERT
        usage.usedCount >= 5 -> TechStatus.FLUENT
        usage.usedCount >= 3 -> TechStatus.USED
        usage.seenCount >= 3 || usage.usedCount >= 1 -> TechStatus.FAMILIAR
        else -> TechStatus.DISCOVERED
    }

    private fun mutate(name: String, f: (TechUsage, Long) -> TechUsage) {
        val t = now()
        val existing = techStore.getAllTechUsage().firstOrNull { it.techName == name }
        // 新建记录 lastSeenAt=0，保证首次 seen 一定计数（1h 去重只约束后续重复）
        val base = existing ?: TechUsage(techName = name, firstSeenAt = t, lastSeenAt = 0L)
        val updated = f(base, t)
        val candidate = statusOf(updated)
        val finalStatus = if (candidate.ordinal > updated.status.ordinal) candidate else updated.status
        techStore.saveTechUsage(updated.copy(status = finalStatus))
    }

    companion object {
        const val SEEN_DEDUPE_MS = 3_600_000L
    }
}
