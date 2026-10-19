package com.learnagent.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.learnagent.ui.theme.Aureate
import com.learnagent.ui.theme.Glimmer
import com.learnagent.ui.theme.Mist
import com.learnagent.ui.theme.MoonSilver
import com.learnagent.ui.theme.Veil
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 幻域感组件（Android 精简版，与桌面同语义）：
 * pressScale（触屏替代 hover）、idleTilt（持续微倾斜）、floatIn、GlowBox、
 * BreathingOrb、BurstParticles、MistDivider、RotatingLogo。
 */

/** 三次贝塞尔取点（轨道曲线绘制，Android 端几何工具）。 */
internal fun cubicPoint(p0: Offset, p1: Offset, p2: Offset, p3: Offset, t: Float): Offset {
    val u = 1f - t
    val w0 = u * u * u
    val w1 = 3f * u * u * t
    val w2 = 3f * u * t * t
    val w3 = t * t * t
    return Offset(
        p0.x * w0 + p1.x * w1 + p2.x * w2 + p3.x * w3,
        p0.y * w0 + p1.y * w1 + p2.y * w2 + p3.y * w3
    )
}

/** 预点击反馈：按住缩小 0.98，松开恢复。 */
@Composable
fun Modifier.pressScale(interaction: MutableInteractionSource): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = Motion.fast,
        label = "pressScale"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * 卡片激活态统一封装（Compose 通用 API，三端可用）：
 * hover（桌面/外接鼠标）+ pressed（Android/iOS 触屏回退）→ 0..1，200ms。
 * 只监听传入的 interactionSource，每张卡片独立维护，互不影响。
 */
@Composable
fun rememberCardActivation(interaction: MutableInteractionSource): Float {
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val fraction by animateFloatAsState(
        targetValue = if (hovered || pressed) 1f else 0f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "cardActivation"
    )
    return fraction
}

/** 触屏没有 hover：用"持续微倾斜"保持 3D 悬浮感（桌面 tilt3d 的静止态）。 */
@Composable
fun Modifier.idleTilt(degrees: Float = 1.5f): Modifier =
    this.graphicsLayer {
        rotationX = -degrees
        cameraDistance = 18f * density
    }

/** 入场：从下方浮起 + 淡入（spring + fadeIn，≈400ms）。 */
@Composable
fun Modifier.floatIn(delayMillis: Int = 0, distanceDp: Dp = 22.dp): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (delayMillis > 0) delay(delayMillis.toLong())
        progress.animateTo(1f, Motion.softSpring)
    }
    return this.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * distanceDp.toPx()
    }
}

/** 卡片光晕（萤火虫感）：无边框，用主色 0.2 透明度径向光晕代替。 */
@Composable
fun GlowBox(
    glow: Color,
    modifier: Modifier = Modifier,
    intensity: Float = 1f,
    radius: Dp = 180.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val animated by animateFloatAsState(
        targetValue = intensity,
        animationSpec = Motion.standard,
        label = "glowIntensity"
    )
    Box(
        modifier = modifier.drawBehind {
            val r = radius.toPx()
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glow.copy(alpha = 0.20f * animated),
                        glow.copy(alpha = 0.07f * animated),
                        Color.Transparent
                    ),
                    center = center,
                    radius = r
                ),
                radius = r,
                center = center
            )
        },
        content = content
    )
}

/** 等待状态：呼吸光球（替代转圈）。 */
@Composable
fun BreathingOrb(size: Dp = 48.dp, modifier: Modifier = Modifier, color: Color = Accent) {
    val transition = rememberInfiniteTransition(label = "orb")
    val scale by transition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbScale"
    )
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbAlpha"
    )
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = this.size.minDimension / 2f
            val c = Offset(r, r)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = alpha * 0.55f),
                        color.copy(alpha = alpha * 0.12f),
                        Color.Transparent
                    ),
                    center = c,
                    radius = r
                ),
                radius = r,
                center = c
            )
            drawCircle(color.copy(alpha = alpha * 0.55f), radius = r * 0.40f * scale, center = c)
            drawCircle(color.copy(alpha = alpha), radius = r * 0.20f * scale, center = c)
        }
    }
}

/** 完成时"炸开"：光球完成后化作 16 个粒子四散（620ms 淡出）。 */
@Composable
fun BurstParticles(trigger: Int, modifier: Modifier = Modifier, color: Color = Accent) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        p.snapTo(0f)
        p.animateTo(1f, tween(620, easing = FastOutSlowInEasing))
    }
    val v = p.value
    if (trigger > 0 && v > 0.001f && v < 0.999f) {
        Canvas(modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val maxR = size.minDimension * 0.34f
            repeat(16) { i ->
                val ang = i * (2f * PI.toFloat() / 16f) + 0.2f
                val dist = v * maxR * (0.55f + (i % 4) * 0.15f)
                val alpha = (1f - v) * 0.8f
                val pos = Offset(cx + cos(ang) * dist, cy + sin(ang) * dist)
                drawCircle(color.copy(alpha = alpha * 0.4f), radius = 3.2f, center = pos)
                drawCircle(Aureate.copy(alpha = alpha), radius = 1.7f, center = pos)
            }
        }
    }
}

/** 极淡渐变分割线：从透明到 0.1 透明度主色再到透明。 */
@Composable
fun MistDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        Accent.copy(alpha = 0.10f),
                        Color.Transparent
                    )
                )
            )
    )
}

/** 空状态 / 等待区：中央巨大、缓缓旋转、半透明 Logo。 */
@Composable
fun RotatingLogo(size: Dp = 160.dp, modifier: Modifier = Modifier, alpha: Float = 0.45f) {
    val transition = rememberInfiniteTransition(label = "rotatingLogo")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "logoRotation"
    )
    val bob by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoBob"
    )
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                rotationZ = rotation
                this.alpha = alpha
                translationY = bob * 6f
            }
    ) {
        LearnAgentLogo(size = size.value.toInt())
    }
}

/** 当前时间段的 accent 色（AmbienceCore 同源）。 */
val Accent: Color
    @Composable
    get() = Color(com.learnagent.ambience.AmbienceCore.accentRaw(
        java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    ))
