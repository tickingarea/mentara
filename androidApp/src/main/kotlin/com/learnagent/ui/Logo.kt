package com.learnagent.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import kotlin.math.hypot

/**
 * 内部 Logo（V3 缺口曲线版）：三个不同大小的圆、非对称排列，
 * 连接曲线在每段中点留一个缺口——"知识永不闭合"。
 * 桌面与 Android 共用同一绘制。
 */
@Composable
fun LearnAgentLogo(size: Int = 48) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    Canvas(modifier = Modifier.size(size.dp)) {
        val w = this.size.width
        val h = this.size.height
        val n1 = Offset(w * 0.30f, h * 0.28f)
        val n2 = Offset(w * 0.74f, h * 0.38f)
        val n3 = Offset(w * 0.46f, h * 0.76f)
        val sw = w * 0.028f

        fun gapLink(a: Offset, b: Offset, gapStart: Float, alpha: Float) {
            val mid = Offset((a.x + b.x) / 2, (a.y + b.y) / 2)
            val dx = b.x - a.x
            val dy = b.y - a.y
            val len = hypot(dx, dy).coerceAtLeast(1f)
            val ctrl = Offset(mid.x - dy / len * len * 0.14f, mid.y + dx / len * len * 0.14f)
            val steps = 26
            for (k in 0 until steps) {
                val t0 = k / steps.toFloat()
                val t1 = (k + 1) / steps.toFloat()
                if (t0 in gapStart..gapStart + 0.16f) continue
                val u0 = 1f - t0
                val u1 = 1f - t1
                val p0 = Offset(u0 * u0 * a.x + 2 * u0 * t0 * ctrl.x + t0 * t0 * b.x, u0 * u0 * a.y + 2 * u0 * t0 * ctrl.y + t0 * t0 * b.y)
                val p1 = Offset(u1 * u1 * a.x + 2 * u1 * t1 * ctrl.x + t1 * t1 * b.x, u1 * u1 * a.y + 2 * u1 * t1 * ctrl.y + t1 * t1 * b.y)
                drawLine(primary.copy(alpha = alpha), p0, p1, sw)
            }
        }
        gapLink(n1, n2, 0.32f, 0.55f)
        gapLink(n2, n3, 0.58f, 0.45f)
        gapLink(n1, n3, 0.74f, 0.40f)

        drawCircle(primary, radius = w * 0.105f, center = n1)
        drawCircle(primary, radius = w * 0.070f, center = n2)
        drawCircle(secondary, radius = w * 0.135f, center = n3)
    }
}