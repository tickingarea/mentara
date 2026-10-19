package com.learnagent.core

import com.learnagent.platform.TechStore

/** 召回块：搜索/问答时主动浮现的历史相关记录。 */
data class RecallBlock(
    val type: String,        // "decision" / "tech_card" / "usage"
    val summary: String,     // 一句话摘要
    val timestamp: Long,
    val referenceId: String  // 关联的实体 id（decision.id / card.id / techName）
)

/**
 * 上下文召回（"反复遇见"机制）：
 * - Decision.problem/chosen/reason 相似
 * - TechCard.relatedConcepts / name 命中
 * - TechUsage.usedCount ≥ 3
 * 混合排序取 Top N；搜索 topN=5，回答 topN=3。
 */
class ContextualRecall(private val techStore: TechStore) {

    fun recallForSearch(query: String, topN: Int = 5): List<RecallBlock> = recall(query, topN)

    fun recallForAnswer(question: String, topN: Int = 3): List<RecallBlock> = recall(question, topN)

    private fun recall(query: String, topN: Int): List<RecallBlock> {
        if (query.isBlank()) return emptyList()
        val keywords = ProblemParser(null).parseWithDictionary(query).keywords
        if (keywords.isEmpty()) return emptyList()
        fun hits(text: String): Int = keywords.count { text.contains(it, ignoreCase = true) }

        val scored = mutableListOf<Pair<Int, RecallBlock>>()

        techStore.getAllDecisions().forEach { d ->
            val score = hits(d.problem) + hits(d.chosen) + hits(d.reason)
            if (score > 0) {
                scored.add(score to RecallBlock("decision", "决策：${d.problem} → ${d.chosen}", d.createdAt, d.id))
            }
        }

        val usageByName = techStore.getAllTechUsage().associateBy { it.techName }
        techStore.getAllTechCards().forEach { c ->
            val score = hits(c.name) * 2 + hits(c.relatedConcepts.joinToString(" ")) + hits(c.whatItSolves)
            if (score > 0) {
                val ts = usageByName[c.name]?.lastSeenAt ?: 0L
                scored.add(score to RecallBlock("tech_card", "技术卡：${c.name} —— ${c.whatItIs}", ts, c.id))
            }
        }

        techStore.getAllTechUsage().filter { it.usedCount >= 3 }.forEach { u ->
            if (hits(u.techName) > 0) {
                scored.add(2 to RecallBlock("usage", "反复使用：${u.techName}（${u.usedCount} 次 · ${u.status}）", u.lastSeenAt, u.techName))
            }
        }

        return scored
            .sortedWith(compareByDescending<Pair<Int, RecallBlock>> { it.first }.thenByDescending { it.second.timestamp })
            .map { it.second }
            .distinctBy { it.type + it.referenceId }
            .take(topN)
    }
}
