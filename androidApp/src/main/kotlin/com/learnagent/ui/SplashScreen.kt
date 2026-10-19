package com.learnagent.ui

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.learnagent.ui.theme.Aureate
import com.learnagent.ui.theme.MorningPurple
import com.learnagent.ui.theme.MoonSilver
import com.learnagent.ui.theme.Phantom
import com.learnagent.ui.theme.TextHigh
import com.learnagent.ui.theme.TextLow
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val SPLASH_SECONDS = 8f

private val RING_SIZES = listOf(64.dp, 50.dp, 78.dp)
private val RING_COLORS = listOf(Phantom, Phantom, Aureate)

private fun frac(v: Float): Float = v - kotlin.math.floor(v)

private fun quadraticPoint(p0: Offset, ctrl: Offset, p1: Offset, t: Float): Offset {
    val u = 1f - t
    return Offset(
        u * u * p0.x + 2 * u * t * ctrl.x + t * t * p1.x,
        u * u * p0.y + 2 * u * t * ctrl.y + t * t * p1.y
    )
}

/**
 * 电影级 Splash（Android 版，8 秒六幕，与桌面同运动词汇 OrbitMotion）：
 *  幕 1 虚空光晕 → 幕 2 三发光圆环 3D 不规则轨道漂入 → 幕 3 贝塞尔思维线
 *  → 幕 4 Mentara 字母 Z 轴穿出 → 幕 5 副标题 + 三角微光点 → 幕 6 淡出
 *  触感：三节点落地各轻震一次；音效：与桌面同时间轴（AudioFx）。
 */
@Composable
fun SplashScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween((SPLASH_SECONDS * 1000).toInt(), easing = LinearEasing))
        onDone()
    }

    var tick by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) {
                    tick += ((now - last) / 1_000_000_000.0).coerceIn(0.0, 0.1).toFloat()
                }
                last = now
            }
        }
    }

    // ---- 音效时间线（与桌面一致；触感：节点落地） ----
    LaunchedEffect(Unit) { delay(120); AudioFx.splashTone(0.22f) }
    LaunchedEffect(Unit) {
        delay(1550); AudioFx.chime(0.95f); Haptics.tap(context)
        delay(500); AudioFx.chime(0.88f); Haptics.tap(context)
        delay(500); AudioFx.chime(0.80f); Haptics.tap(context)
    }
    LaunchedEffect(Unit) { delay(3050); AudioFx.drone(0.9f) }
    LaunchedEffect(Unit) { delay(4500); AudioFx.whoosh(0.85f) }
    LaunchedEffect(Unit) { delay(6080); AudioFx.splashTone(0.45f) }
    LaunchedEffect(Unit) { delay(7520); AudioFx.open() }

    val sec = progress.value * SPLASH_SECONDS
    val glowT = (sec / 1.5f).coerceIn(0f, 1f)
    val logoFade = 1f - ((sec - 7.35f) / 0.65f).coerceIn(0f, 1f)
    val subAlpha = ((sec - 6.0f) / 0.6f).coerceIn(0f, 1f)
    val tagAlpha = ((sec - 6.5f) / 0.6f).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .graphicsLayer { alpha = 1f - ((sec - 7.5f) / 0.5f).coerceIn(0f, 1f) },
        contentAlignment = Alignment.Center
    ) {
        // ---- 电影镜头（自动缓漂；触屏无鼠标视差） ----
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationY = sin(tick * 0.3f) * 3f
                    rotationX = cos(tick * 0.4f) * 2f
                    cameraDistance = 24f * density
                },
            contentAlignment = Alignment.Center
        ) {
            // 幕 1：虚空光晕
            Canvas(Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height * 0.44f)
                val minDim = size.minDimension
                val expand = 1f - (1f - glowT) * (1f - glowT) * (1f - glowT)
                val radius = minDim * (0.06f + 0.80f * expand)
                val afterGlow = 1f - ((sec - 2.2f) / 2.3f).coerceIn(0f, 1f)
                val haloAlpha = (0.55f * expand * (0.35f + 0.65f * afterGlow)) * logoFade
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Phantom.copy(alpha = haloAlpha),
                            MorningPurple.copy(alpha = haloAlpha * 0.45f),
                            Aureate.copy(alpha = haloAlpha * 0.10f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )
                if (sec > 3f) {
                    val breathe = 0.5f + 0.5f * sin(2f * PI.toFloat() * (sec - 3f) / 1.6f)
                    drawCircle(
                        Phantom.copy(alpha = 0.10f * breathe * logoFade),
                        radius = minDim * 0.28f,
                        center = center,
                        style = Stroke(width = 1.5f)
                    )
                }
            }

            // 幕 2-3：三圆环 3D 不规则轨道场
            val pops = List(3) { remember { Animatable(0f) } }
            for (i in 0..2) {
                val started = sec >= 1.5f + i * 0.5f
                LaunchedEffect(started) {
                    if (started) {
                        pops[i].animateTo(
                            0.999f,
                            spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)
                        )
                    }
                }
            }
            Box(
                Modifier
                    .size(360.dp)
                    .align(Alignment.Center)
                    .offset(y = (-56).dp)
                    .graphicsLayer { alpha = logoFade; cameraDistance = 20f * density },
                contentAlignment = Alignment.Center
            ) {
                val ringAlpha = ((sec - 1.8f) / 1.2f).coerceIn(0f, 1f) * logoFade
                GyroRing(tick, ringAlpha, 64f, 9f, Phantom, 1.6f, dash = false)
                GyroRing(tick, ringAlpha * 0.7f, 70f, -6f, Aureate, 1.1f, dash = true)

                OrbitTracksCanvas(sec, tick, ringAlpha, pops.map { it.value })

                RingSprite(0, tick, pops[0].value, RING_SIZES[0], RING_COLORS[0])
                RingSprite(1, tick, pops[1].value, RING_SIZES[1], RING_COLORS[1])
                RingSprite(2, tick, pops[2].value, RING_SIZES[2], RING_COLORS[2])
            }

            // 幕 4/5：标题 + 副标题 + 三角微光点
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.Center).offset(y = 96.dp)
            ) {
                MentaraTitleZ(sec = sec, tick = tick)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Your Second Mind",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Light,
                        fontSize = 18.sp,
                        letterSpacing = 6.sp
                    ),
                    color = MoonSilver.copy(alpha = subAlpha),
                    modifier = Modifier.graphicsLayer { translationY = (1f - subAlpha) * 14f }
                )
                Spacer(Modifier.height(26.dp))
                TrinityDots(sec = sec, tick = tick)
            }
        }

        Text(
            "Where Knowledge Breathes",
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Light,
                fontSize = 12.sp,
                letterSpacing = 4.sp
            ),
            color = TextLow.copy(alpha = tagAlpha),
            modifier = Modifier.align(Alignment.BottomCenter).offset(y = (-36).dp)
        )
    }
}

/** 单个发光圆环（与桌面同公式：OrbitMotion.splashPosition + graphicsLayer 深度）。 */
@Composable
private fun RingSprite(index: Int, tick: Float, pop: Float, sizeDp: Dp, color: Color) {
    val pos = remember { FloatArray(3) }
    OrbitMotion.splashPosition(index, tick, pop, pos)
    Box(
        Modifier
            .size(sizeDp)
            .graphicsLayer {
                val boxPx = OrbitMotion.SPLASH_FIELD.dp.toPx()
                val x = pos[0] * boxPx * OrbitMotion.SPLASH_SCALE_X
                val y = pos[1] * boxPx * OrbitMotion.SPLASH_SCALE_Y
                val z = pos[2] * boxPx * OrbitMotion.SPLASH_SCALE_X
                cameraDistance = 20f * density
                translationX = x
                translationY = y
                rotationX = z * 0.2f
                rotationY = x * 0.15f
                rotationZ = tick * 6f + 25f * sin(tick * 0.17f + index) + index * 120f
                val s = (1f + z * 0.002f).coerceIn(0.55f, 1.5f)
                scaleX = s
                scaleY = s
                alpha = ((0.6f + z * 0.003f) * pop).coerceIn(0.15f, 1f)
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            val c = Offset(r, r)
            val near = ((pos[2] / 2.4f + 1f) / 2f).coerceIn(0f, 1f)
            val shimmer = 0.5f + 0.5f * sin(tick * 0.9f + index * 2f)
            val haloAlpha = (0.08f + 0.20f * near) * pop * (0.85f + 0.15f * shimmer)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(
                        color.copy(alpha = haloAlpha),
                        color.copy(alpha = haloAlpha * 0.35f),
                        Color.Transparent
                    ),
                    center = c,
                    radius = r * (1.7f - 0.4f * near)
                ),
                radius = r * (1.7f - 0.4f * near),
                center = c
            )
            drawCircle(
                color = color.copy(alpha = (0.40f + 0.50f * near) * pop),
                radius = r * 0.86f,
                center = c,
                style = Stroke(width = (1.1f + 0.9f * near).dp.toPx())
            )
            drawCircle(
                color = color.copy(alpha = (0.30f + 0.50f * near) * pop),
                radius = r * 0.10f * (0.75f + 0.5f * near),
                center = c
            )
        }
    }
}

/** 轨道 / 思维线 / 涟漪 / 电流 / 微粒（圆环之下的同源层，与桌面同构）。 */
@Composable
private fun OrbitTracksCanvas(sec: Float, tick: Float, alpha: Float, pops: List<Float>) {
    val buf = remember { FloatArray(3) }
    Canvas(Modifier.fillMaxSize()) {
        if (alpha <= 0.01f) return@Canvas
        val w = size.width
        val h = size.height
        val s = size.minDimension
        val cx = w / 2f
        val cy = h / 2f

        val ringXY = FloatArray(6)
        for (i in 0..2) {
            OrbitMotion.splashPosition(i, tick, pops[i], buf)
            ringXY[i * 2] = cx + buf[0] * s * OrbitMotion.SPLASH_SCALE_X
            ringXY[i * 2 + 1] = cy + buf[1] * s * OrbitMotion.SPLASH_SCALE_Y
        }

        // 三条淡轨道曲线（圆环邻近处增亮）
        for (k in 0..2) {
            val drift = OrbitMotion.noise(tick * 0.1f + k * 7f) * 0.10f
            val yBase = h * (0.28f + k * 0.22f) + drift * h
            val p0 = Offset(-w * 0.06f, yBase + h * 0.10f * sin(tick * 0.07f + k))
            val p1 = Offset(w * 0.34f, yBase - h * 0.24f)
            val p2 = Offset(w * 0.66f, yBase + h * 0.22f)
            val p3 = Offset(w * 1.06f, yBase - h * 0.10f)
            val segments = 22
            var prev = cubicPoint(p0, p1, p2, p3, 0f)
            for (seg in 1..segments) {
                val u = seg / segments.toFloat()
                val pt = cubicPoint(p0, p1, p2, p3, u)
                var boost = 0f
                for (i in 0..2) {
                    val d = kotlin.math.hypot(pt.x - ringXY[i * 2], pt.y - ringXY[i * 2 + 1])
                    boost += (1f - (d / (s * 0.38f)).coerceIn(0f, 1f)) * pops[i]
                }
                val a = (0.04f + 0.11f * boost.coerceAtMost(1.2f)) * alpha
                drawLine(
                    color = (if (k == 1) Aureate else Phantom).copy(alpha = a),
                    start = prev,
                    end = pt,
                    strokeWidth = 1f
                )
                prev = pt
            }
        }

        // 环间思维线
        val lineP = ((sec - 3.0f) / 1.0f).coerceIn(0f, 1f)
        if (lineP > 0f) {
            val pulse = 0.55f + 0.45f * sin(2f * PI.toFloat() * (sec - 3f) / 0.9f)
            val pairs = listOf(0 to 1, 1 to 2, 2 to 0)
            pairs.forEachIndexed { idx, (a, b) ->
                val from = Offset(ringXY[a * 2], ringXY[a * 2 + 1])
                val to = Offset(ringXY[b * 2], ringXY[b * 2 + 1])
                val mid = Offset((from.x + to.x) / 2, (from.y + to.y) / 2)
                val dx = to.x - from.x
                val dy = to.y - from.y
                val len = kotlin.math.hypot(dx, dy).coerceAtLeast(1f)
                val dir = if (idx % 2 == 0) 1f else -1f
                val ctrl = Offset(
                    mid.x - dy / len * 0.16f * len * dir + OrbitMotion.noise(tick * 0.4f + idx) * 6f,
                    mid.y + dx / len * 0.16f * len * dir
                )
                val samples = 40
                val n = (samples * lineP).toInt().coerceAtLeast(1)
                val path = Path()
                path.moveTo(from.x, from.y)
                for (k in 1..n) {
                    val t = k.toFloat() / samples
                    val p = quadraticPoint(from, ctrl, to, t)
                    path.lineTo(p.x, p.y)
                }
                drawPath(
                    path = path,
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Phantom.copy(alpha = 0.72f * pulse * alpha),
                            Phantom.copy(alpha = 0.20f * pulse * alpha),
                            Color.Transparent
                        ),
                        center = ctrl,
                        radius = len * 0.75f
                    ),
                    style = Stroke(width = s * 0.012f)
                )
                if (lineP >= 1f) {
                    val raw = frac((sec - 3f) / 1.6f + a * 0.33f)
                    val eased = 0.5f - 0.5f * cos(PI.toFloat() * raw)
                    val dot = quadraticPoint(from, ctrl, to, eased)
                    val wob = OrbitMotion.noise(tick * 2f + a) * 3f
                    val nx = -dy / len * wob
                    val ny = dx / len * wob
                    drawCircle(Phantom.copy(alpha = 0.65f * alpha), radius = s * 0.012f, center = Offset(dot.x + nx, dot.y + ny))
                    drawCircle(Aureate.copy(alpha = 0.30f * alpha), radius = s * 0.024f, center = Offset(dot.x + nx, dot.y + ny))
                }
            }
        }

        // 涟漪
        for (i in 0..2) {
            val appearAt = 1.5f + i * 0.5f
            val ripple = ((sec - (appearAt + 0.12f)) / 0.9f).coerceIn(0f, 1f)
            if (ripple in 0.01f..0.99f && pops[i] > 0.1f) {
                drawCircle(
                    color = Phantom.copy(alpha = (1f - ripple) * 0.4f * pops[i] * alpha),
                    radius = s * (0.05f + ripple * 0.22f),
                    center = Offset(ringXY[i * 2], ringXY[i * 2 + 1]),
                    style = Stroke(width = 1.2f)
                )
            }
        }

        // 轨道微粒
        val mp = ((sec - 2.6f) / 1.4f).coerceIn(0f, 1f)
        if (mp > 0f) {
            repeat(9) { j ->
                val idx = j % 3
                val tj = tick * (0.35f + 0.06f * j) + j * 1.37f
                OrbitMotion.position(idx, tj, buf)
                val px = cx + (buf[0] * 0.10f + OrbitMotion.noise(j * 3.3f) * 0.06f) * s
                val py = cy + (buf[1] * 0.09f + OrbitMotion.noise(j * 1.9f + 4f) * 0.05f) * s
                val near = ((buf[2] / 2.2f + 1f) / 2f).coerceIn(0f, 1f)
                val r = 1.1f + near * 1.5f
                val alp = (0.14f + 0.30f * near) * mp * alpha
                val col = if (j % 2 == 0) Phantom else Aureate
                drawCircle(col.copy(alpha = alp * 0.35f), radius = r * 2.4f, center = Offset(px, py))
                drawCircle(col.copy(alpha = alp), radius = r, center = Offset(px, py))
            }
        }
    }
}

/** 陀螺环（节律被 sin 调制，非匀速）。 */
@Composable
private fun GyroRing(
    tick: Float,
    alpha: Float,
    rotationBaseX: Float,
    spinSpeed: Float,
    color: Color,
    strokeWidth: Float,
    dash: Boolean
) {
    Canvas(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                this.alpha = alpha
                cameraDistance = 22f * density
                rotationX = rotationBaseX + cos(tick * 0.13f) * 5f
                rotationY = tick * spinSpeed + 20f * sin(tick * 0.19f)
            }
    ) {
        val r = size.minDimension * 0.42f
        val pathEffect = if (dash) PathEffect.dashPathEffect(floatArrayOf(10f, 14f), 0f) else null
        drawCircle(
            color = color.copy(alpha = 0.45f),
            radius = r,
            center = center,
            style = Stroke(width = strokeWidth, pathEffect = pathEffect)
        )
        drawCircle(
            color = color.copy(alpha = 0.16f),
            radius = r * 1.12f,
            center = center,
            style = Stroke(width = strokeWidth * 0.6f)
        )
    }
}

/** 幕 4：Mentara 每字母 Z 轴穿出 + 落定微漂浮。 */
@Composable
private fun MentaraTitleZ(sec: Float, tick: Float) {
    val letters = "Mentara".toList()
    val states = remember { List(letters.size) { Animatable(0f) } }
    letters.forEachIndexed { i, _ ->
        val started = sec >= 4.5f + i * 0.08f
        LaunchedEffect(started) {
            if (started) {
                states[i].animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow))
            }
        }
    }
    Row {
        letters.forEachIndexed { i, ch ->
            val p = states[i].value
            Text(
                text = ch.toString(),
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Light,
                    fontSize = 64.sp,
                    letterSpacing = 10.sp,
                    shadow = Shadow(
                        color = Phantom.copy(alpha = 0.55f * p),
                        offset = Offset(0f, 0f),
                        blurRadius = 28f
                    )
                ),
                color = TextHigh.copy(alpha = p),
                modifier = Modifier.graphicsLayer {
                    scaleX = 0.3f + 0.7f * p
                    scaleY = 0.3f + 0.7f * p
                    rotationX = (1f - p) * -60f + cos(tick * 0.33f + i * 1.7f) * 2.2f * p
                    translationY = sin(tick * 0.4f + i * 0.9f) * 2.2f * p
                    alpha = p
                    cameraDistance = 24f * density
                }
            )
        }
    }
}

/** 幕 5：三角微光点（噪声振荡 + 节律自转）。 */
@Composable
private fun TrinityDots(sec: Float, tick: Float) {
    val groupVisible = ((sec - 6.3f) / 0.6f).coerceIn(0f, 1f)
    Canvas(
        Modifier
            .size(width = 150.dp, height = 30.dp)
            .graphicsLayer {
                alpha = groupVisible
                rotationY = tick * 6f + 10f * sin(tick * 0.21f)
                cameraDistance = 26f * density
            }
    ) {
        if (groupVisible <= 0f) return@Canvas
        val dots = listOf(
            Offset(size.width * 0.25f, size.height * 0.55f),
            Offset(size.width * 0.50f, size.height * 0.20f),
            Offset(size.width * 0.75f, size.height * 0.55f)
        )
        val osc = FloatArray(3) { i -> OrbitMotion.noise(tick * 0.85f + i * 2.7f) }
        val linePairs = listOf(0 to 1, 1 to 2, 2 to 0)
        linePairs.forEach { (a, b) ->
            val la = ((osc[a] + osc[b]) / 2f + 1f) / 2f
            drawLine(
                color = MoonSilver.copy(alpha = 0.08f + 0.16f * la),
                start = dots[a],
                end = dots[b],
                strokeWidth = 1f
            )
        }
        dots.forEachIndexed { i, c ->
            val o = osc[i]
            val haloA = 0.20f + 0.16f * ((o + 1f) / 2f)
            val coreR = 2.2f * (1f + 0.22f * o)
            drawCircle(Phantom.copy(alpha = haloA), radius = coreR * 2.6f, center = c)
            drawCircle(Aureate.copy(alpha = 0.45f + 0.40f * ((o + 1f) / 2f)), radius = coreR, center = c)
        }
    }
}
