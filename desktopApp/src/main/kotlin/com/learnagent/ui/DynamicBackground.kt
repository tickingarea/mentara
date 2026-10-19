package com.learnagent.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.learnagent.design.DesignTokens
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

private fun appFocused(): Boolean = runCatching {
    java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow?.isShowing == true
}.getOrDefault(true)

/**
 * 动态背景（DESIGN.md · 第四节）三层叠加：
 *  1. 星空径向渐变 #0F0A18 → #060308 + 噪点抖动 + 程序化纹理
 *  2. 知识流粒子（36–80 个，独立缓慢漂移，<150px 连线）
 *  3. 呼吸光晕（400px 半径、10 秒周期、0.05–0.10 呼吸）
 *
 * 性能设计：
 *  - 静态层（渐变/噪点/纹理）放入独立 graphicsLayer，不随粒子每帧重绘；
 *  - 帧率自适应：窗口失焦 ≈5fps、空闲沉睡 30fps、活跃 60fps；
 *  - 位置缓冲预分配（FloatArray），消除每帧 GC 压力。
 */
@Composable
fun DynamicBackground(modifier: Modifier = Modifier) {
    val particleCount = Ambience.particleCount
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

    // 3D 同源轨道（与 Splash 三圆环同一运动词汇 OrbitMotion）
    val wanderBuf = remember { FloatArray(3) }
    val wandData = remember { FloatArray(9) }   // 每个游走光灵的 x/y/伪深度
    // 帧率自适应：掉帧 → 减少绘制粒子数（不减少轨道与光灵）
    val perf = remember { floatArrayOf(1f) }

    val noise = artImage("art/noise.png")
    val art = artImage("art/main_bg.png")

    var flowTime by remember { mutableFloatStateOf(0f) }
    var clockTime by remember { mutableFloatStateOf(0f) }

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
                    // 帧率采样：每 45 帧评估一次平均帧时长
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
                    val sleeping = Ambience.sleeping
                    val every = when {
                        !appFocused() -> 12
                        sleeping -> 2
                        else -> 1
                    }
                    if (frameIndex % every == 0) {
                        val factor = Ambience.speedFactor * (if (sleeping) 0.35f else 1f)
                        flowTime += accumulated * factor
                        clockTime += accumulated
                        accumulated = 0f
                    }
                }
            }
        }
    }

    Box(modifier) {
        // ---- 第 1 层：静态星空（独立图层，只在夜间模式切换时重绘） ----
        Box(Modifier.fillMaxSize().graphicsLayer()) {
            Canvas(Modifier.fillMaxSize()) {
                val night = Ambience.nightMode
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(CenterGlow, EdgeDark),
                        center = Offset(size.width / 2f, size.height * 0.42f),
                        radius = size.maxDimension * 0.78f
                    )
                )
                noise?.let {
                    drawRect(
                        ShaderBrush(ImageShader(it, TileMode.Repeated, TileMode.Repeated)),
                        alpha = if (night) 0.55f else 0.85f
                    )
                }
                art?.let {
                    drawImage(
                        image = it,
                        dstOffset = IntOffset.Zero,
                        dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                        alpha = if (night) 0.16f else 0.26f
                    )
                }
            }
        }

        // ---- 第 2 层：知识流粒子 + 第 3 层：呼吸光晕 ----
        Canvas(Modifier.fillMaxSize()) {
            val night = Ambience.nightMode
            val sleeping = Ambience.sleeping
            val accent = AmbientAccent
            val lily = Color(0xFF7FB89C)
            val particleAlpha = when {
                sleeping -> 0.45f
                night -> 0.70f
                else -> 1f
            }

            // ---- V3 雾状三层叠加（紫灰 / 绿灰 / 金灰），方向随时间缓转 ----
            val cx = size.width / 2f
            val cy = size.height / 2f
            val fogMax = kotlin.math.max(size.width, size.height) * 1.2f
            val fogColors = listOf(accent, lily, Aureate)
            val fogAlphas = listOf(0.14f, 0.10f, 0.08f)
            for (k in 0..2) {
                val ang = clockTime * (0.024f + k * 0.011f) + k * 2.1f
                val endX = cx + kotlin.math.cos(ang) * fogMax
                val endY = cy + kotlin.math.sin(ang) * fogMax
                val a = fogAlphas[k] * (if (night) 0.7f else 1f)
                drawRect(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(DesignTokens.ColorVoid),
                            fogColors[k].copy(alpha = a),
                            Color.Transparent
                        ),
                        start = Offset(cx - (endX - cx), cy - (endY - cy)),
                        end = Offset(endX, endY)
                    )
                )
            }
            // ---- 2.5 层：3D 同源轨道（三条淡曲线 + 三个游走光灵；光灵经过处轨道增亮） ----
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
            // 游走光灵（同源轨道上的"守护者"：近亮远暗）
            for (i in 0..2) {
                val near = ((wandData[i * 3 + 2] / 2.4f + 1f) / 2f).coerceIn(0f, 1f)
                val c = if (i == 1) Aureate else accent
                val wpos = Offset(wandData[i * 3], wandData[i * 3 + 1])
                val rr = 3f + 5f * near
                drawCircle(c.copy(alpha = (0.10f + 0.16f * near) * particleAlpha), radius = rr * 3.2f, center = wpos)
                drawCircle(c.copy(alpha = (0.45f + 0.40f * near) * particleAlpha), radius = rr * 0.45f, center = wpos)
            }

            val count = (particles.size * perf[0]).toInt().coerceAtLeast(18)
            if (count * 2 > positionBuffer.size) return@Canvas

            for (i in 0 until count) {
                val p = particles[i]
                val x = (p.baseX + p.ampX * sin(2f * PI.toFloat() * flowTime / p.periodA + p.phaseA)) * size.width
                val y = (p.baseY + p.ampY * cos(2f * PI.toFloat() * flowTime / p.periodB + p.phaseB)) * size.height
                positionBuffer[i * 2] = x
                positionBuffer[i * 2 + 1] = y
            }

            // <150px 连线（先平方距离筛选，避免开方）
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

            // 呼吸光晕（10 秒周期移动，0.05–0.10 呼吸）
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
}
