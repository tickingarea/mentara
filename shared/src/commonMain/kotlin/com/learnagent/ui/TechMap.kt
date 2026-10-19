package com.learnagent.ui

import com.learnagent.data.TechCard
import com.learnagent.data.TechCategory
import com.learnagent.data.TechStatus
import com.learnagent.data.TechUsage
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 技术地图布局算法（两端共用，纯 Kotlin，不依赖 Compose）：
 * - 按 TechCategory 分组，扇区大小 ∝ 组内节点数
 * - 组内节点沿扇区弧线圆形布局，半径随组大小变化
 * - 节点大小 = min(4 + usedCount*2, 20) dp；颜色按状态
 * - relatedConcepts 命中的节点之间连线
 * 平台层负责 Canvas 绘制、hover（桌面）/点击与 TechCard 弹层。
 */
data class TechMapNode(
    val name: String,
    val category: String,
    val status: TechStatus,
    val usedCount: Int,
    val x: Float,
    val y: Float,
    val radiusDp: Float,
    val colorArgb: Long,
    val glow: Boolean
)

data class TechMapEdge(val fromX: Float, val fromY: Float, val toX: Float, val toY: Float)

data class TechMapLayout(val nodes: List<TechMapNode>, val edges: List<TechMapEdge>)

object TechMapLayoutEngine {

    fun build(cards: List<TechCard>, usages: List<TechUsage>, width: Float, height: Float): TechMapLayout {
        if (cards.isEmpty() || width <= 0f || height <= 0f) return TechMapLayout(emptyList(), emptyList())
        val byCategory = cards.groupBy { it.category }
        val present = TechCategory.values().toList().filter { byCategory[it].orEmpty().isNotEmpty() }
        val total = cards.size.coerceAtLeast(1)
        val cx = width / 2f
        val cy = height / 2f
        val outerR = min(width, height) * 0.40f
        val nodes = mutableListOf<TechMapNode>()
        var angleStart = -PI / 2

        for (category in present) {
            val group = byCategory.getValue(category)
            val sweep = (2 * PI * group.size / total).toFloat()
            val mid = (angleStart + sweep / 2).toFloat()
            // 组内节点数越多，"环"越大
            val ringR = outerR * (0.55f + 0.35f * min(group.size, 8) / 8f)
            group.forEachIndexed { i, card ->
                val usage = usages.firstOrNull { it.techName.equals(card.name, ignoreCase = true) }
                val used = usage?.usedCount ?: 0
                val status = usage?.status ?: TechStatus.DISCOVERED
                val sub = if (group.size == 1) 0f else (i.toFloat() / (group.size - 1) - 0.5f)
                val angle = mid + sub * sweep * 0.85f
                val jitter = 0.75f + 0.25f * (((card.name.hashCode() and 0x7FFF) % 10) / 10f)
                val r = ringR * jitter
                nodes.add(
                    TechMapNode(
                        name = card.name,
                        category = category.name,
                        status = status,
                        usedCount = used,
                        x = cx + cos(angle) * r,
                        y = cy + sin(angle) * r,
                        radiusDp = min(4f + used * 2f, 20f),
                        colorArgb = statusColor(status),
                        glow = used >= 5
                    )
                )
            }
            angleStart += sweep
        }

        val nodeByName = nodes.associateBy { it.name }
        val edges = mutableListOf<TechMapEdge>()
        val seenPairs = mutableSetOf<String>()
        cards.forEach { card ->
            val a = nodeByName[card.name] ?: return@forEach
            card.relatedConcepts.forEach { concept ->
                val b = nodeByName[concept] ?: return@forEach
                val key = listOf(a.name.lowercase(), b.name.lowercase()).sorted().joinToString("|")
                if (seenPairs.add(key)) edges.add(TechMapEdge(a.x, a.y, b.x, b.y))
            }
        }
        return TechMapLayout(nodes, edges)
    }

    /** 命中测试（像素坐标；padPx 为额外容差）。 */
    fun hitTest(nodes: List<TechMapNode>, x: Float, y: Float, padPx: Float = 0f): TechMapNode? {
        var best: TechMapNode? = null
        var bestDist = Float.MAX_VALUE
        nodes.forEach { n ->
            val dx = n.x - x
            val dy = n.y - y
            val dist = sqrt(dx * dx + dy * dy)
            if (dist <= n.radiusDp + padPx && dist < bestDist) {
                bestDist = dist
                best = n
            }
        }
        return best
    }

    fun statusColor(status: TechStatus): Long = when (status) {
        TechStatus.DISCOVERED -> 0xFF4A5260
        TechStatus.FAMILIAR -> 0xFF4A7AB8
        TechStatus.USED -> 0xFF7FB89C
        TechStatus.FLUENT -> 0xFFE8C87A
        TechStatus.EXPERT -> 0xFFB8A8D4
    }
}
