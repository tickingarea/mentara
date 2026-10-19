package com.learnagent.core

import com.learnagent.data.KnowledgePoint

/**
 * 交错学习：按 chapterId 分组后 Round-Robin 轮询，避免同章节连续。
 * 单章节 → 打乱返回（无交错可用）。
 */
class InterleavingEngine {

    fun interleave(points: List<KnowledgePoint>, count: Int): List<KnowledgePoint> {
        if (points.isEmpty() || count <= 0) return emptyList()
        val groups = points.groupBy { it.chapterId }.values.map { it.shuffled() }
        if (groups.size <= 1) return points.shuffled().take(count)

        val result = mutableListOf<KnowledgePoint>()
        var idx = 0
        while (result.size < count) {
            var added = false
            for (group in groups) {
                if (idx < group.size && result.size < count) {
                    result.add(group[idx])
                    added = true
                }
            }
            if (!added) break   // 所有组都抽完
            idx++
        }
        return result
    }
}
