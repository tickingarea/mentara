package com.learnagent.core

import com.learnagent.data.TimelineItem

/** 知识图谱节点。 */
data class GraphNode(
    val index: Int,
    val label: String,
    val degree: Int
)

/** 知识图谱边（两个片段共享的关键词）。 */
data class GraphEdge(
    val from: Int,
    val to: Int,
    val shared: List<String>
)

data class KnowledgeGraphData(
    val nodes: List<GraphNode>,
    val edges: List<GraphEdge>
)

/**
 * 知识图谱构建（纯逻辑，跨平台）。
 *
 * 每个 TimelineItem 是一个节点；共享关键词的节点之间连边；
 * 节点 degree = 与之共享关键词的节点数。
 */
object GraphBuilder {

    fun build(items: List<TimelineItem>): KnowledgeGraphData {
        val keywords = items.map { keywordsOf(it.speech + " " + it.screenText) }
        val edges = buildList {
            for (i in items.indices) {
                for (j in i + 1 until items.size) {
                    val shared = keywords[i].intersect(keywords[j])
                    if (shared.isNotEmpty()) add(GraphEdge(i, j, shared.toList().sorted()))
                }
            }
        }
        val degree = IntArray(items.size)
        edges.forEach { degree[it.from]++; degree[it.to]++ }
        val nodes = items.mapIndexed { i, item -> GraphNode(i, labelOf(item), degree[i]) }
        return KnowledgeGraphData(nodes, edges)
    }

    fun keywordsOf(text: String): Set<String> {
        val cleaned = text.lowercase()
            .replace(Regex("[\\s,，。、/！？；：\"'`()（）：]+"), " ")
        return cleaned.split(" ").filter { it.length >= 2 }.toSet()
    }

    private fun labelOf(item: TimelineItem): String {
        val text = item.speech.ifBlank { item.screenText }
        return text.take(16)
    }
}
