package com.learnagent.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * 图标系统 V3（20 个）：
 *  - 1.5dp 细线、圆角端点、不填充、不对称但平衡
 *  - 渐变光照：左下暗 → 右上亮，悬停 300ms 流动偏移（桌面 hover / Android 固定相位）
 *  - 禁止：闭合整圆、靶心、准星、齿轮、放大镜、纸飞机、实心星
 */
private fun stroke(w: Float) = Stroke(width = w, cap = StrokeCap.Round, join = StrokeJoin.Round)

private fun DrawScope.iconBrush(color: Color, phase: Float = 0f): Brush {
    val s = this.size
    val shift = phase * 0.10f * s.width
    return Brush.linearGradient(
        colors = listOf(color.copy(alpha = 0.45f), color.copy(alpha = 0.95f)),
        start = Offset(s.width * 0.08f - shift, s.height * 0.92f + shift),
        end = Offset(s.width * 0.92f + shift, s.height * 0.08f - shift)
    )
}

/** 图标渲染壳：24dp 基准，渐变笔刷 + 悬停光效。 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun IconCanvas(
    sizeDp: Int,
    modifier: Modifier,
    draw: DrawScope.(Color, Float) -> Unit
) {
    var hovered by remember { mutableStateOf(false) }
    val phase by animateFloatAsState(if (hovered) 1f else 0f, tween(300), label = "iconGlow")
    val content = androidx.compose.material3.LocalContentColor.current
    Canvas(
        modifier
            .size(sizeDp.dp)
            .onPointerEvent(PointerEventType.Enter) { hovered = true }
            .onPointerEvent(PointerEventType.Exit) { hovered = false }
    ) {
        draw(content, phase)
    }
}

private fun DrawScope.arcRect(cx: Float, cy: Float, r: Float): Pair<Offset, Size> =
    Offset(cx - r, cy - r) to Size(r * 2f, r * 2f)

// ============================== 20 ICONS ==============================

/** 1. MentaraLogo：三个大小不一的圆非对称 + 缺口曲线连接。 */
@Composable
fun MentaraLogo(sizeDp: Int = 24, modifier: Modifier = Modifier) {
    IconCanvas(sizeDp, modifier) { c, p ->
        val b = iconBrush(c, p)
        val sw = 1.5.dp.toPx()
        val w = size.width
        val h = size.height
        val n1 = Offset(w * 0.28f, h * 0.26f)
        val n2 = Offset(w * 0.76f, h * 0.34f)
        val n3 = Offset(w * 0.44f, h * 0.74f)
        fun gapLink(a: Offset, b2: Offset, gapStart: Float) {
            val mid = Offset((a.x + b2.x) / 2, (a.y + b2.y) / 2)
            val dx = b2.x - a.x
            val dy = b2.y - a.y
            val len = hypot(dx, dy).coerceAtLeast(1f)
            val ctrl = Offset(mid.x - dy / len * len * 0.14f, mid.y + dx / len * len * 0.14f)
            val steps = 24
            for (k in 0 until steps) {
                val t0 = k / steps.toFloat()
                val t1 = (k + 1) / steps.toFloat()
                if (t0 in gapStart..gapStart + 0.16f) continue
                val u0 = 1f - t0
                val u1 = 1f - t1
                val p0 = Offset(u0 * u0 * a.x + 2 * u0 * t0 * ctrl.x + t0 * t0 * b2.x, u0 * u0 * a.y + 2 * u0 * t0 * ctrl.y + t0 * t0 * b2.y)
                val p1 = Offset(u1 * u1 * a.x + 2 * u1 * t1 * ctrl.x + t1 * t1 * b2.x, u1 * u1 * a.y + 2 * u1 * t1 * ctrl.y + t1 * t1 * b2.y)
                drawLine(b, p0, p1, sw, StrokeCap.Round)
            }
        }
        gapLink(n1, n2, 0.30f)
        gapLink(n2, n3, 0.55f)
        gapLink(n1, n3, 0.72f)
        drawCircle(b, radius = size.minDimension * 0.085f, center = n1)
        drawCircle(b, radius = size.minDimension * 0.055f, center = n2)
        drawCircle(b, radius = size.minDimension * 0.115f, center = n3)
    }
}

/** 2. SearchIcon：向内收缩的螺旋（指纹意象）。 */
@Composable
fun SearchIcon(sizeDp: Int = 24, modifier: Modifier = Modifier) {
    IconCanvas(sizeDp, modifier) { c, p ->
        val b = iconBrush(c, p)
        val sw = 1.5.dp.toPx()
        val cx = size.width * 0.5f
        val cy = size.height * 0.5f
        listOf(0.36f, 0.22f, 0.10f).forEachIndexed { i, r ->
            val (tl, sz) = arcRect(cx, cy, size.minDimension * r)
            drawArc(b, 20f + i * 30f, 300f - i * 50f, false, topLeft = tl, size = sz, style = stroke(sw))
        }
        drawCircle(b, radius = sw * 0.6f, center = Offset(cx, cy))
    }
}

/** 3. ImportIcon：从上往下的瀑布曲线 + 底部水面。 */
@Composable
fun ImportIcon(sizeDp: Int = 24, modifier: Modifier = Modifier) {
    IconCanvas(sizeDp, modifier) { c, p ->
        val b = iconBrush(c, p)
        val sw = 1.5.dp.toPx()
        val fall = Path().apply {
            moveTo(size.width * 0.42f, size.height * 0.12f)
            cubicTo(size.width * 0.60f, size.height * 0.30f, size.width * 0.24f, size.height * 0.52f, size.width * 0.46f, size.height * 0.72f)
        }
        drawPath(fall, b, style = stroke(sw))
        val wave = Path().apply {
            moveTo(size.width * 0.14f, size.height * 0.86f)
            cubicTo(size.width * 0.32f, size.height * 0.78f, size.width * 0.44f, size.height * 0.94f, size.width * 0.60f, size.height * 0.86f)
        }
        drawPath(wave, b, style = stroke(sw))
        drawLine(b, Offset(size.width * 0.66f, size.height * 0.88f), Offset(size.width * 0.84f, size.height * 0.84f), sw, StrokeCap.Round)
    }
}

/** 4. GraphIcon：三圆错落 + S 形曲线连接。 */
@Composable
fun GraphIcon(sizeDp: Int = 24, modifier: Modifier = Modifier) {
    IconCanvas(sizeDp, modifier) { c, p ->
        val b = iconBrush(c, p)
        val sw = 1.5.dp.toPx()
        val w = size.width
        val h = size.height
        val n1 = Offset(w * 0.22f, h * 0.28f)
        val n2 = Offset(w * 0.74f, h * 0.42f)
        val n3 = Offset(w * 0.46f, h * 0.76f)
        val s1 = Path().apply {
            moveTo(n1.x, n1.y)
            cubicTo(w * 0.42f, h * 0.18f, w * 0.56f, h * 0.28f, n2.x, n2.y)
        }
        drawPath(s1, b, style = stroke(sw))
        val s2 = Path().apply {
            moveTo(n2.x, n2.y)
            cubicTo(w * 0.62f, h * 0.62f, w * 0.30f, h * 0.58f, n3.x, n3.y)
        }
        drawPath(s2, b, style = stroke(sw))
        drawCircle(b, radius = size.minDimension * 0.075f, center = n1)
        drawCircle(b, radius = size.minDimension * 0.050f, center = n2)
        drawCircle(b, radius = size.minDimension * 0.095f, center = n3)
    }
}

/** 5. TimelineIcon：微微起伏的波浪线 + 3 个大小渐变的点。 */
@Composable
fun TimelineIcon(sizeDp: Int = 24, modifier: Modifier = Modifier) {
    IconCanvas(sizeDp, modifier) { c, p ->
        val b = iconBrush(c, p)
        val sw = 1.5.dp.toPx()
        val wave = Path().apply {
            moveTo(size.width * 0.12f, size.height * 0.5f)
            cubicTo(size.width * 0.26f, size.height * 0.34f, size.width * 0.42f, size.height * 0.66f, size.width * 0.56f, size.height * 0.5f)
            cubicTo(size.width * 0.68f, size.height * 0.36f, size.width * 0.78f, size.height * 0.46f, size.width * 0.88f, size.height * 0.5f)
        }
        drawPath(wave, b, style = stroke(sw))
        drawCircle(b, radius = sw * 1.4f, center = Offset(size.width * 0.28f, size.height * 0.42f))
        drawCircle(b, radius = sw * 2.0f, center = Offset(size.width * 0.50f, size.height * 0.52f))
        drawCircle(b, radius = sw * 2.7f, center = Offset(size.width * 0.74f, size.height * 0.46f))
    }
}

/** 6. RecordIcon：未闭合的圆弧（开口约 60°），内侧有微小点。 */
@Composable
fun RecordIcon(sizeDp: Int = 24, modifier: Modifier = Modifier) {
    IconCanvas(sizeDp, modifier) { c, p ->
        val b = iconBrush(c, p)
        val sw = 1.5.dp.toPx()
        val r = size.minDimension * 0.32f
        val cx = size.width * 0.5f
        val cy = size.height * 0.5f
        val (tl, sz) = arcRect(cx, cy, r)
        drawArc(b, 30f, 270f, false, topLeft = tl, size = sz, style = stroke(sw))
        drawCircle(b, radius = sw * 1.1f, center = Offset(cx + r * 0.30f, cy + r * 0.30f))
    }
}

/** 7. SettingsIcon：三条粗细不同的横线，端点有微小圆点。 */
@Composable
fun SettingsIcon(sizeDp: Int = 24, modifier: Modifier = Modifier) {
    IconCanvas(sizeDp, modifier) { c, p ->
        val b = iconBrush(c, p)
        val sw = 1.5.dp.toPx()
        drawLine(b, Offset(size.width * 0.16f, size.height * 0.36f), Offset(size.width * 0.84f, size.height * 0.36f), sw, StrokeCap.Round)
        drawLine(b, Offset(size.width * 0.16f, size.height * 0.60f), Offset(size.width * 0.60f, size.height * 0.60f), sw * 1.7f, StrokeCap.Round)
        drawLine(b, Offset(size.width * 0.30f, size.height * 0.82f), Offset(size.width * 0.84f, size.height * 0.82f), sw, StrokeCap.Round)
        drawCircle(b, radius = sw * 0.9f, center = Offset(size.width * 0.10f, size.height * 0.36f))
        drawCircle(b, radius = sw * 0.9f, center = Offset(size.width * 0.90f, size.height * 0.82f))
    }
}

/** 8. SendIcon：从下往上的曲线，上端分叉成两条短曲线。 */
@Composable
fun SendIcon(sizeDp: Int = 24, modifier: Modifier = Modifier) {
    IconCanvas(sizeDp, modifier) { c, p ->
        val b = iconBrush(c, p)
        val sw = 1.5.dp.toPx()
        val stem = Path().apply {
            moveTo(size.width * 0.20f, size.height * 0.84f)
            cubicTo(size.width * 0.46f, size.height * 0.66f, size.width * 0.56f, size.height * 0.40f, size.width * 0.62f, size.height * 0.16f)
        }
        drawPath(stem, b, style = stroke(sw))
        val l1 = Path().apply {
            moveTo(size.width * 0.62f, size.height * 0.16f)
            cubicTo(size.width * 0.50f, size.height * 0.20f, size.width * 0.42f, size.height * 0.26f, size.width * 0.36f, size.height * 0.34f)
        }
        drawPath(l1, b, style = stroke(sw))
        val l2 = Path().apply {
            moveTo(size.width * 0.62f, size.height * 0.16f)
            cubicTo(size.width * 0.72f, size.height * 0.26f, size.width * 0.80f, size.height * 0.34f, size.width * 0.86f, size.height * 0.44f)
        }
        drawPath(l2, b, style = stroke(sw))
    }
}

/** 9. CloseIcon：两条曲线从不同方向汇合但不相交，中间留小缝隙。 */
@Composable
fun CloseIcon(sizeDp: Int = 24, modifier: Modifier = Modifier) {
    IconCanvas(sizeDp, modifier) { c, p ->
        val b = iconBrush(c, p)
        val sw = 1.5.dp.toPx()
        val a1 = Path().apply {
            moveTo(size.width * 0.20f, size.height * 0.20f)
            cubicTo(size.width * 0.42f, size.height * 0.42f, size.width * 0.48f, size.height * 0.50f, size.width * 0.46f, size.height * 0.56f)
        }
        drawPath(a1, b, style = stroke(sw))
        val a2 = Path().apply {
            moveTo(size.width * 0.80f, size.height * 0.80f)
            cubicTo(size.width * 0.58f, size.height * 0.58f, size.width * 0.52f, size.height * 0.50f, size.width * 0.54f, size.height * 0.44f)
        }
        drawPath(a2, b, style = stroke(sw))
    }
}

/** 10. ExpandIcon：三条横线长度依次递增。 */
@Composable
fun ExpandIcon(sizeDp: Int = 24, modifier: Modifier = Modifier) {
    IconCanvas(sizeDp, modifier) { c, p ->
        val b = iconBrush(c, p)
        val sw = 1.5.dp.toPx()
        drawLine(b, Offset(size.width * 0.18f, size.height * 0.30f), Offset(size.width * 0.46f, size.height * 0.30f), sw, StrokeCap.Round)
        drawLine(b, Offset(size.width * 0.18f, size.height * 0.50f), Offset(size.width * 0.66f, size.height * 0.50f), sw, StrokeCap.Round)
        drawLine(b, Offset(size.width * 0.18f, size.height * 0.70f), Offset(size.width * 0.86f, size.height * 0.70f), sw, StrokeCap.Round)
    }
}

/** 11. PlayIcon：被压扁的椭圆开口向右，内部小三角。 */
@Composable
fun PlayIcon(sizeDp: Int = 24, modifier: Modifier = Modifier) {
    IconCanvas(sizeDp, modifier) { c, p ->
        val b = iconBrush(c, p)
        val sw = 1.5.dp.toPx()
        val cx = size.width * 0.36f
        val cy = size.height * 0.5f
        val rx = size.width * 0.26f
        val ry = size.height * 0.32f
        drawArc(b, -55f, 235f, false, topLeft = Offset(cx - rx, cy - ry), size = Size(rx * 2f, ry * 2f), style = stroke(sw))
        val tri = Path().apply {
            moveTo(size.width * 0.32f, size.height * 0.34f)
            lineTo(size.width * 0.48f, size.height * 0.50f)
            lineTo(size.width * 0.32f, size.height * 0.66f)
        }
        drawPath(tri, b, style = stroke(sw * 0.9f))
    }
}

/** 12. PauseIcon：两条粗细不一的竖线。 */
@Composable
fun PauseIcon(sizeDp: Int = 24, modifier: Modifier = Modifier) {
    IconCanvas(sizeDp, modifier) { c, p ->
        val b = iconBrush(c, p)
        val sw = 1.5.dp.toPx()
        drawLine(b, Offset(size.width * 0.36f, size.height * 0.24f), Offset(size.width * 0.36f, size.height * 0.76f), sw * 1.8f, StrokeCap.Round)
        drawLine(b, Offset(size.width * 0.64f, size.height * 0.32f), Offset(size.width * 0.64f, size.height * 0.68f), sw, StrokeCap.Round)
    }
}

/** 13. VideoIcon：略微倾斜的矩形，右下角有小凸起。 */
@Composable
fun VideoIcon(sizeDp: Int = 24, modifier: Modifier = Modifier) {
    IconCanvas(sizeDp, modifier) { c, p ->
        val b = iconBrush(c, p)
        val sw = 1.5.dp.toPx()
        val frame = Path().apply {
            moveTo(size.width * 0.14f, size.height * 0.34f)
            lineTo(size.width * 0.70f, size.height * 0.22f)
            lineTo(size.width * 0.78f, size.height * 0.70f)
            lineTo(size.width * 0.22f, size.height * 0.82f)
            close()
        }
        drawPath(frame, b, style = stroke(sw))
        val bump = Path().apply {
            moveTo(size.width * 0.78f, size.height * 0.62f)
            lineTo(size.width * 0.88f, size.height * 0.56f)
            lineTo(size.width * 0.86f, size.height * 0.76f)
        }
        drawPath(bump, b, style = stroke(sw * 0.9f))
    }
}

/** 14. LinkIcon：两条不相交的曲线（两个半开链环）。 */
@Composable
fun LinkIcon(sizeDp: Int = 24, modifier: Modifier = Modifier) {
    IconCanvas(sizeDp, modifier) { c, p ->
        val b = iconBrush(c, p)
        val sw = 1.5.dp.toPx()
        drawArc(b, -60f, 250f, false, topLeft = Offset(size.width * 0.10f, size.height * 0.24f), size = Size(size.width * 0.42f, size.height * 0.52f), style = stroke(sw))
        drawArc(b, 110f, 260f, false, topLeft = Offset(size.width * 0.48f, size.height * 0.24f), size = Size(size.width * 0.42f, size.height * 0.52f), style = stroke(sw))
    }
}

/** 15. StarIcon：五个渐变的点（不是实心星）。 */
@Composable
fun StarIcon(sizeDp: Int = 24, modifier: Modifier = Modifier) {
    IconCanvas(sizeDp, modifier) { c, p ->
        val b = iconBrush(c, p)
        val cc = c
        val sw = 1.5.dp.toPx()
        val pts = listOf(
            Offset(size.width * 0.14f, size.height * 0.68f) to 0.3f,
            Offset(size.width * 0.34f, size.height * 0.36f) to 0.5f,
            Offset(size.width * 0.52f, size.height * 0.72f) to 0.65f,
            Offset(size.width * 0.70f, size.height * 0.30f) to 0.8f,
            Offset(size.width * 0.88f, size.height * 0.58f) to 1.0f
        )
        pts.forEachIndexed { i, (pos, a) ->
            drawCircle(cc.copy(alpha = 0.3f + 0.7f * a), radius = sw * (0.8f + i * 0.28f), center = pos)
        }
    }
}

/** 16. CheckIcon：从左下到右上的曲线，末端有小勾。 */
@Composable
fun CheckIcon(sizeDp: Int = 24, modifier: Modifier = Modifier) {
    IconCanvas(sizeDp, modifier) { c, p ->
        val b = iconBrush(c, p)
        val sw = 1.5.dp.toPx()
        val line = Path().apply {
            moveTo(size.width * 0.14f, size.height * 0.78f)
            cubicTo(size.width * 0.34f, size.height * 0.66f, size.width * 0.50f, size.height * 0.48f, size.width * 0.58f, size.height * 0.30f)
        }
        drawPath(line, b, style = stroke(sw))
        drawLine(b, Offset(size.width * 0.58f, size.height * 0.30f), Offset(size.width * 0.86f, size.height * 0.16f), sw * 1.3f, StrokeCap.Round)
    }
}

/** 17. WarningIcon：未闭合的三角，内部一个小点。 */
@Composable
fun WarningIcon(sizeDp: Int = 24, modifier: Modifier = Modifier) {
    IconCanvas(sizeDp, modifier) { c, p ->
        val b = iconBrush(c, p)
        val sw = 1.5.dp.toPx()
        val tri = Path().apply {
            moveTo(size.width * 0.16f, size.height * 0.80f)
            lineTo(size.width * 0.46f, size.height * 0.20f)
            lineTo(size.width * 0.76f, size.height * 0.80f)
        }
        drawPath(tri, b, style = stroke(sw))
        drawCircle(b, radius = sw * 0.9f, center = Offset(size.width * 0.46f, size.height * 0.62f))
    }
}

/** 18. ArrowLeftIcon：一条向下的曲线 + 一条向左的曲线。 */
@Composable
fun ArrowLeftIcon(sizeDp: Int = 24, modifier: Modifier = Modifier) {
    IconCanvas(sizeDp, modifier) { c, p ->
        val b = iconBrush(c, p)
        val sw = 1.5.dp.toPx()
        val line = Path().apply {
            moveTo(size.width * 0.78f, size.height * 0.18f)
            cubicTo(size.width * 0.70f, size.height * 0.44f, size.width * 0.58f, size.height * 0.62f, size.width * 0.40f, size.height * 0.72f)
        }
        drawPath(line, b, style = stroke(sw))
        val head = Path().apply {
            moveTo(size.width * 0.44f, size.height * 0.20f)
            cubicTo(size.width * 0.40f, size.height * 0.44f, size.width * 0.38f, size.height * 0.60f, size.width * 0.40f, size.height * 0.72f)
        }
        drawPath(head, b, style = stroke(sw))
    }
}

/** 19. ArrowRightIcon：镜像。 */
@Composable
fun ArrowRightIcon(sizeDp: Int = 24, modifier: Modifier = Modifier) {
    IconCanvas(sizeDp, modifier) { c, p ->
        val b = iconBrush(c, p)
        val sw = 1.5.dp.toPx()
        val line = Path().apply {
            moveTo(size.width * 0.22f, size.height * 0.18f)
            cubicTo(size.width * 0.30f, size.height * 0.44f, size.width * 0.42f, size.height * 0.62f, size.width * 0.60f, size.height * 0.72f)
        }
        drawPath(line, b, style = stroke(sw))
        val head = Path().apply {
            moveTo(size.width * 0.56f, size.height * 0.20f)
            cubicTo(size.width * 0.60f, size.height * 0.44f, size.width * 0.62f, size.height * 0.60f, size.width * 0.60f, size.height * 0.72f)
        }
        drawPath(head, b, style = stroke(sw))
    }
}

/** 20. RefreshIcon：两条环绕的弧线，端点不相交。 */
@Composable
fun RefreshIcon(sizeDp: Int = 24, modifier: Modifier = Modifier) {
    IconCanvas(sizeDp, modifier) { c, p ->
        val b = iconBrush(c, p)
        val sw = 1.5.dp.toPx()
        drawArc(b, 130f, 260f, false, topLeft = Offset(size.width * 0.16f, size.height * 0.16f), size = Size(size.width * 0.68f, size.height * 0.68f), style = stroke(sw))
        drawArc(b, -50f, 260f, false, topLeft = Offset(size.width * 0.26f, size.height * 0.26f), size = Size(size.width * 0.48f, size.height * 0.48f), style = stroke(sw))
    }
}

/** 附加：辩论图标（三圆三角，V3 渐变）。 */
@Composable
fun DebateIcon(sizeDp: Int = 24, modifier: Modifier = Modifier) {
    IconCanvas(sizeDp, modifier) { c, p ->
        val b = iconBrush(c, p)
        val sw = 1.5.dp.toPx()
        val w = size.width
        val h = size.height
        val a = Offset(w * 0.26f, h * 0.68f)
        val b2 = Offset(w * 0.50f, h * 0.22f)
        val d = Offset(w * 0.76f, h * 0.68f)
        val p1 = Path().apply { moveTo(a.x, a.y); cubicTo(w * 0.34f, h * 0.52f, w * 0.40f, h * 0.34f, b2.x, b2.y) }
        val p2 = Path().apply { moveTo(b2.x, b2.y); cubicTo(w * 0.60f, h * 0.34f, w * 0.68f, h * 0.52f, d.x, d.y) }
        val p3 = Path().apply { moveTo(a.x, a.y); cubicTo(w * 0.42f, h * 0.76f, w * 0.60f, h * 0.76f, d.x, d.y) }
        drawPath(p1, b, style = stroke(sw))
        drawPath(p2, b, style = stroke(sw))
        drawPath(p3, b, style = stroke(sw))
        drawCircle(b, radius = sw * 1.6f, center = a)
        drawCircle(b, radius = sw * 1.3f, center = b2)
        drawCircle(b, radius = sw * 1.6f, center = d)
    }
}

/** 附加：星级评分（五渐点，replaces the star text）。 */
@Composable
fun StarRating(importance: Int, sizeDp: Int = 24, modifier: Modifier = Modifier) {
    val c = androidx.compose.material3.LocalContentColor.current
    Canvas(modifier.size(sizeDp.dp)) {
        val sw = 1.5.dp.toPx()
        val step = size.width / 5f
        for (i in 0 until 5) {
            val lit = i < importance
            drawCircle(
                c.copy(alpha = if (lit) 0.9f else 0.25f),
                radius = sw * (0.9f + i * 0.18f) * (if (lit) 1f else 0.6f),
                center = Offset(step * (i + 0.5f), size.height * 0.5f)
            )
        }
    }
}
