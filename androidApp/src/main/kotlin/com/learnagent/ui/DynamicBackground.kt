package com.learnagent.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.learnagent.ui.theme.Aureate
import com.learnagent.ui.theme.CenterGlow
import com.learnagent.ui.theme.EdgeDark
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private class DriftParticle(
    val baseX: Float,
    val baseY: Float,
    val ampX: Float,
    val ampY: Float,
    val periodA: Float,
    val periodB: Float,
    val phaseA: Float,
    val phaseB: Float,
    val radius: Float,
    val golden: Boolean,
    val halo: Float
)

/**
 * 动态背景（Android 版，与桌面同构三层）：
 *  1. 星空径向渐变（双渐变叠加防色带）
 *  2. 知识流粒子（AmbienceState 驱动数量 36~80 / 速度 / 沉睡 / 夜读）
 *  3. 呼吸光晕 + 三条贝塞尔轨道曲线 + 三个游走光灵（OrbitMotion 同源运动）
 *
 * 性能：每 45 帧采样平均帧时长，>26ms → 绘制粒子数 ×0.75（下限 35%）；
 * 窗口失焦降到 ~5fps（ViewTreeObserver 焦点监听，不用 AWT）。
 */
@Composable
fun DynamicBackground(modifier: Modifier = Modifier) {
    val particleCount = AmbienceState.particleCount
    val particles = remember(particleCount) {
        val rnd = Random(19_87)
        List(particleCount) {
            DriftParticle(
                baseX = rnd.nextFloat(),
                baseY = rnd.nextFloat(),
                ampX = 0.012f + rnd.nextFloat() * 0.030f,
                ampY = 0.010f + rnd.nextFloat() * 0.026f,
                periodA = 22f + rnd.nextFloat() * 38f,
                periodB = 26f + rnd.nextFloat() * 42f,
                phaseA = rnd.nextFloat() * 2f * PI.toFloat(),
                phaseB = rnd.nextFloat() * 2f * PI.toFloat(),
                radius = 1f + rnd.nextFloat() * 2f,
                golden = it % 2 == 1,
                halo = 0.35f + rnd.nextFloat() * 0.30f
            )
        }
    }
    val positionBuffer = remember { FloatArray(80 * 2) }
    val wanderBuf = remember { FloatArray(3) }
    val wandData = remember { FloatArray(9) }
    val perf = remember { floatArrayOf(1f) }

    // 窗口焦点（失焦 → 帧循环降到 ~5fps）
    val view = LocalView.current
    var viewActive by remember { mutableStateOf(true) }
    DisposableEffect(view) {
        val listener = android.view.ViewTreeObserver.OnWindowFocusChangeListener { viewActive = it }
        view.viewTreeObserver.addOnWindowFocusChangeListener(listener)
        onDispose { view.viewTreeObserver.removeOnWindowFocusChangeListener(listener) }
    }

    var flowTime by remember { mutableFloatStateOf(0f) }
    var clockTime by remember { mutableFloatStateOf(0f) }
    // Accent 是 @Composable getter：在组合层取好，draw lambda 只捕获局部值
    val localAccent = Accent

    LaunchedEffect(Unit) {
        var last = 0L
        var accumulated = 0f
        var frameIndex = 0
        var accMs = 0.0
        var perfFrames = 0
        while (true) {
            withFrameNanos { now ->
                if (last == 0L) {
                    last = now
                } else {
                    val dt = ((now - last) / 1_000_000_000.0).coerceIn(0.0, 0.1).toFloat()
                    last = now
                    accumulated += dt
                    frameIndex++
                    accMs += dt * 1000.0
                    perfFrames++
                    if (perfFrames >= 45) {
                        val avg = accMs / perfFrames
                        if (avg > 26.0) {
                            perf[0] = (perf[0] * 0.75f).coerceAtLeast(0.35f)
                        } else if (avg < 20.0) {
                            perf[0] = (perf[0] * 1.12f).coerceAtMost(1f)
                        }
                        accMs = 0.0
                        perfFrames = 0
                    }
                    val sleeping = AmbienceState.sleeping
                    val every = when {
                        !viewActive -> 12
                        sleeping -> 2
                        else -> 1
                    }
                    if (frameIndex % every == 0) {
                        val factor = AmbienceState.speedFactor * (if (sleeping) 0.35f else 1f)
                        flowTime += accumulated * factor
                        clockTime += accumulated
                        accumulated = 0f
                    }
                }
            }
        }
    }

    Canvas(modifier.fillMaxSize()) {
        val night = AmbienceState.nightMode
        val sleeping = AmbienceState.sleeping
        val accent = localAccent
        val lily = Color(0xFF7FB89C)
        val particleAlpha = when {
            sleeping -> 0.45f
            night -> 0.70f
            else -> 1f
        }

        // ---- V3 雾状三层叠加（紫灰 / 绿灰 / 金灰），方向随时间缓转 ----
        val fogCx = size.width / 2f
        val fogCy = size.height / 2f
        val fogMax = kotlin.math.max(size.width, size.height) * 1.2f
        val fogColors = listOf(accent, lily, Aureate)
        val fogAlphas = listOf(0.14f, 0.10f, 0.08f)
        for (k in 0..2) {
            val ang = clockTime * (0.024f + k * 0.011f) + k * 2.1f
            val endX = fogCx + kotlin.math.cos(ang) * fogMax
            val endY = fogCy + kotlin.math.sin(ang) * fogMax
            val a = fogAlphas[k] * (if (night) 0.7f else 1f)
            drawRect(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0A0E14),
                        fogColors[k].copy(alpha = a),
                        Color.Transparent
                    ),
                    start = Offset(fogCx - (endX - fogCx), fogCy - (endY - fogCy)),
                    end = Offset(endX, endY)
                )
            )
        }

        // ---- 第 1 层：星空渐变（双渐变叠加防色带） ----
        val center = Offset(size.width / 2f, size.height * 0.42f)
        val radius = size.maxDimension * 0.78f
        drawRect(
            Brush.radialGradient(
                colors = listOf(CenterGlow, EdgeDark),
                center = center,
                radius = radius
            )
        )
        drawRect(
            Brush.radialGradient(
                colors = listOf(Color(0x14100A1C), Color.Transparent),
                center = Offset(size.width * 0.3f, size.height * 0.6f),
                radius = size.maxDimension * 0.5f
            )
        )

        // ---- 2.5 层：3D 同源轨道（三条淡曲线 + 游走光灵） ----
        for (i in 0..2) {
            val t = clockTime * (0.55f + 0.07f * i) + i * 4.7f
            OrbitMotion.position(i, t, wanderBuf)
            wandData[i * 3] = size.width * (0.5f + wanderBuf[0] * 0.17f)
            wandData[i * 3 + 1] = size.height * (0.5f + wanderBuf[1] * 0.15f)
            wandData[i * 3 + 2] = wanderBuf[2]
        }
        val trackNearR = size.minDimension * 0.35f
        val trackNearR2 = trackNearR * trackNearR
        for (k in 0..2) {
            val drift = OrbitMotion.noise(clockTime * 0.05f + k * 9f) * 0.08f
            val yBase = size.height * (0.25f + k * 0.25f) + drift * size.height
            val p0 = Offset(-size.width * 0.06f, yBase)
            val p1 = Offset(size.width * 0.35f, yBase - size.height * 0.22f)
            val p2 = Offset(size.width * 0.65f, yBase + size.height * 0.24f)
            val p3 = Offset(size.width * 1.06f, yBase - size.height * 0.06f)
            val segs = 16
            var prev = cubicPoint(p0, p1, p2, p3, 0f)
            for (seg in 1..segs) {
                val u = seg / segs.toFloat()
                val pt = cubicPoint(p0, p1, p2, p3, u)
                var boost = 0f
                for (i in 0..2) {
                    val dx = pt.x - wandData[i * 3]
                    val dy = pt.y - wandData[i * 3 + 1]
                    val d2 = dx * dx + dy * dy
                    if (d2 < trackNearR2) boost += (1f - d2 / trackNearR2)
                }
                val a = (0.030f + 0.085f * boost.coerceAtMost(1.2f)) * particleAlpha * (if (night) 0.7f else 1f)
                drawLine(
                    color = (if (k == 1) Aureate else accent).copy(alpha = a),
                    start = prev,
                    end = pt,
                    strokeWidth = 1f
                )
                prev = pt
            }
        }
        for (i in 0..2) {
            val near = ((wandData[i * 3 + 2] / 2.4f + 1f) / 2f).coerceIn(0f, 1f)
            val c = if (i == 1) Aureate else accent
            val wpos = Offset(wandData[i * 3], wandData[i * 3 + 1])
            val rr = 3f + 5f * near
            drawCircle(c.copy(alpha = (0.10f + 0.16f * near) * particleAlpha), radius = rr * 3.2f, center = wpos)
            drawCircle(c.copy(alpha = (0.45f + 0.40f * near) * particleAlpha), radius = rr * 0.45f, center = wpos)
        }

        // ---- 第 2 层：知识流粒子 ----
        val count = (particles.size * perf[0]).toInt().coerceAtLeast(18)
        if (count * 2 <= positionBuffer.size) {
            for (i in 0 until count) {
                val p = particles[i]
                val x = (p.baseX + p.ampX * sin(2f * PI.toFloat() * flowTime / p.periodA + p.phaseA)) * size.width
                val y = (p.baseY + p.ampY * cos(2f * PI.toFloat() * flowTime / p.periodB + p.phaseB)) * size.height
                positionBuffer[i * 2] = x
                positionBuffer[i * 2 + 1] = y
            }
            val maxDist2 = 150f * 150f
            for (i in 0 until count) {
                val xi = positionBuffer[i * 2]
                val yi = positionBuffer[i * 2 + 1]
                for (j in i + 1 until count) {
                    val dx = xi - positionBuffer[j * 2]
                    val dy = yi - positionBuffer[j * 2 + 1]
                    val d2 = dx * dx + dy * dy
                    if (d2 < maxDist2) {
                        val d = sqrt(d2)
                        val a = (150f - d) / 150f * 0.15f * particleAlpha
                        val color = if (particles[i].golden) Aureate else accent
                        drawLine(
                            color.copy(alpha = a),
                            Offset(xi, yi),
                            Offset(positionBuffer[j * 2], positionBuffer[j * 2 + 1]),
                            strokeWidth = 0.8f
                        )
                    }
                }
            }
            for (i in 0 until count) {
                val p = particles[i]
                val color = if (p.golden) Aureate else accent
                val pos = Offset(positionBuffer[i * 2], positionBuffer[i * 2 + 1])
                drawCircle(color.copy(alpha = 0.5f * p.halo * particleAlpha), radius = p.radius * 3.2f, center = pos)
                drawCircle(color.copy(alpha = 0.85f * particleAlpha), radius = p.radius, center = pos)
            }
        }

        // ---- 第 3 层：呼吸光晕 ----
        val gx = size.width * (0.5f + 0.25f * sin(2f * PI.toFloat() * clockTime / 10f))
        val gy = size.height * (0.45f + 0.18f * sin(2f * PI.toFloat() * clockTime / 10f + 1.7f))
        val breathe = 0.05f + 0.05f * (0.5f + 0.5f * sin(2f * PI.toFloat() * clockTime / 10f))
        val glowAlpha = when {
            night -> breathe * 0.55f
            sleeping -> breathe * 0.60f
            else -> breathe
        }
        val glowRadius = 400.dp.toPx()
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accent.copy(alpha = glowAlpha),
                    accent.copy(alpha = glowAlpha * 0.35f),
                    Color.Transparent
                ),
                center = Offset(gx, gy),
                radius = glowRadius
            ),
            radius = glowRadius,
            center = Offset(gx, gy)
        )
    }
}
