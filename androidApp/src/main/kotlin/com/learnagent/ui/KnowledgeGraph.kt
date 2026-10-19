package com.learnagent.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.learnagent.data.TimelineItem
import kotlin.math.cos
import kotlin.math.sin

/** 从文本提取关键词（长度 >= 2 的词）。 */
fun extractKeywords(text: String): Set<String> {
    val cleaned = text.lowercase().replace(Regex("[\\s,，。、/！？；：\"'`()（）：]"), " ")
    return cleaned.split(" ").filter { it.length >= 2 }.toSet()
}

private fun nodePositions(canvas: Size, count: Int): List<Offset> {
    if (count <= 0) return emptyList()
    val cx = canvas.width / 2f
    val cy = canvas.height / 2f
    val rx = canvas.width * 0.34f
    val ry = canvas.height * 0.34f
    return (0 until count).map { i ->
        val ang = (2.0 * Math.PI * i / count) - Math.PI / 2
        Offset((cx + rx * cos(ang)).toFloat(), (cy + ry * sin(ang)).toFloat())
    }
}

/**
 * 知识图谱：每个 TimelineItem 一个节点；共享关键词的节点连线；
 * 节点大小按度数决定；点击节点回调其索引。
 */
@Composable
fun KnowledgeGraph(
    items: List<TimelineItem>,
    selectedIndex: Int?,
    onNodeClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val outline = MaterialTheme.colorScheme.onSurface

    val keywords = remember(items) {
        items.map { extractKeywords(it.speech + " " + it.screenText) }
    }
    val edges = remember(items) {
        buildList {
            for (i in items.indices) {
                for (j in i + 1 until items.size) {
                    if (keywords[i].intersect(keywords[j]).isNotEmpty()) add(i to j)
                }
            }
        }
    }
    val degree = remember(items) {
        IntArray(items.size).also { d -> edges.forEach { (a, b) -> d[a]++; d[b]++ } }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(items) {
                detectTapGestures { off ->
                    val pos = nodePositions(Size(size.width.toFloat(), size.height.toFloat()), items.size)
                    val idx = pos.indices.minByOrNull { (pos[it] - off).getDistance() }
                    if (idx != null && (pos[idx] - off).getDistance() < 48f) onNodeClick(idx)
                }
            }
    ) {
        val pos = nodePositions(size, items.size)
        edges.forEach { (a, b) ->
            drawLine(primary.copy(alpha = 0.25f), pos[a], pos[b], strokeWidth = 2.5f)
        }
        pos.forEachIndexed { i, p ->
            val r = 12f + degree[i] * 5f
            val color = if (i == selectedIndex) secondary else primary
            if (i == selectedIndex) drawCircle(secondary.copy(alpha = 0.25f), r + 10f, p)
            drawCircle(color, r, p)
            drawCircle(outline.copy(alpha = 0.15f), r, p, style = Stroke(width = 1.5f))
        }
    }
}
