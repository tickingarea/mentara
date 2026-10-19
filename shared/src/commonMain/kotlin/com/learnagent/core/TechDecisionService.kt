package com.learnagent.core

import com.learnagent.data.Decision
import com.learnagent.platform.TechStore
import com.learnagent.platform.currentTimeMillis

/**
 * 决策服务：记录决策 + 回访。
 * 记录后同步使用痕迹：相关技术 decisionCount+1、usedCount+2。
 */
object TechDecisionService {

    const val MS_PER_DAY = 86_400_000L

    fun record(
        store: TechStore,
        problem: String,
        chosen: String,
        reason: String,
        options: List<String> = emptyList(),
        constraints: List<String> = emptyList(),
        followUpDays: Int = 7,
        now: Long = currentTimeMillis()
    ): Decision {
        val decision = Decision(
            id = "dec_" + now.toString(16),
            problem = problem,
            options = options,
            chosen = chosen,
            reason = reason,
            constraints = constraints,
            createdAt = now,
            followUpAt = now + followUpDays.coerceIn(1, 90) * MS_PER_DAY
        )
        store.saveDecision(decision)
        val tracker = TechUsageTracker(store) { now }
        tracker.onDecisionRecorded(techNamesIn(problem + " " + chosen, store))
        return decision
    }

    fun dueFollowUps(store: TechStore, now: Long = currentTimeMillis()): List<Decision> =
        store.getAllDecisions().filter { !it.followUpDone && it.followUpAt in 1..now }

    fun saveFollowUp(
        store: TechStore,
        decision: Decision,
        result: String,
        resultGood: Boolean?,
        now: Long = currentTimeMillis()
    ) {
        store.saveDecision(decision.copy(result = result, resultGood = resultGood, followUpDone = true, followUpAt = now))
    }

    /** 文本中出现的已知技术（TechCard 名 + TechUsage 名）。 */
    fun techNamesIn(text: String, store: TechStore): List<String> {
        if (text.isBlank()) return emptyList()
        val known = (store.getAllTechCards().map { it.name } + store.getAllTechUsage().map { it.techName })
            .filter { it.isNotBlank() }
            .distinct()
        return known.filter { text.contains(it, ignoreCase = true) }
    }
}
