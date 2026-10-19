package com.learnagent.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ============================================================
//  幻域感交互（DESIGN.md · 第七 / 九节）
//  去边框 · 3D 悬浮 · 预点击反馈 · 入场浮起 · 等待光球
// ============================================================

/**
 * 鼠标滑过 → HOVER 音效。
 * 双重节流：同一元素 500ms 内只响一次（此处）+ 全局 120ms 间隔（Feedback 内），
 * 避免布局刷新产生的重复指针事件导致"自鸣"。
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Modifier.hoverSound(): Modifier {
    var lastPlayAt by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    return onPointerEvent(PointerEventType.Enter) {
        val now = System.currentTimeMillis()
        if (now - lastPlayAt >= 500L) {
            lastPlayAt = now
            Feedback.hover()
        }
    }
}

/**
 * 卡片 3D 悬浮：默认 ±1.5°，hover 时朝鼠标方向倾斜（最多 [maxDegrees]），离开回弹。
 *
 * 性能：直接用普通状态 + animateFloatAsState，而不是每次指针移动都 launch 协程
 * （旧实现每秒会产生上百个协程，是鼠标移动卡顿的主因之一）。
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Modifier.tilt3d(maxDegrees: Float = 5f, idleDegrees: Float = 1.5f): Modifier {
    var targetX by remember { androidx.compose.runtime.mutableFloatStateOf(-idleDegrees) }
    var targetY by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    val rotX by animateFloatAsState(targetX, Motion.fast, label = "tiltX")
    val rotY by animateFloatAsState(targetY, Motion.fast, label = "tiltY")
    return this
        .onPointerEvent(PointerEventType.Move) { event ->
            val change = event.changes.firstOrNull() ?: return@onPointerEvent
            val w = size.width.toFloat().coerceAtLeast(1f)
            val h = size.height.toFloat().coerceAtLeast(1f)
            val nx = (change.position.x / w - 0.5f) * 2f
            val ny = (change.position.y / h - 0.5f) * 2f
            targetY = nx * maxDegrees
            targetX = -ny * maxDegrees
        }
        .onPointerEvent(PointerEventType.Exit) {
            targetY = 0f
            targetX = -idleDegrees
        }
        .graphicsLayer {
            rotationX = rotX
            rotationY = rotY
            cameraDistance = 18f * density
        }
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
fun BreathingOrb(size: Dp = 48.dp, modifier: Modifier = Modifier, color: Color = AmbientAccent) {
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

/** 空状态：中央巨大、缓缓旋转、半透明 Logo。 */
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

/** 成功状态：顶部中心向两侧扩散绿色光带，2 秒后消散。 */
@Composable
fun SuccessFlash(trigger: Int, modifier: Modifier = Modifier) {
    var visible by remember { androidx.compose.runtime.mutableStateOf(false) }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        visible = true
        delay(2000)
        visible = false
    }
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(tween(200)),
        exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(tween(500)),
        modifier = modifier
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, SuccessGreen, SuccessGreen, Color.Transparent)
                    )
                )
        )
    }
}

/** 错误状态：边缘短暂泛珊瑚色。 */
@Composable
fun ErrorVeil(trigger: Int, modifier: Modifier = Modifier) {
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        alpha.snapTo(0.55f)
        alpha.animateTo(0f, tween(700))
    }
    if (alpha.value > 0.001f) {
        val a = alpha.value
        Canvas(modifier.fillMaxSize()) {
            val band = 110.dp.toPx()
            val c = Coral.copy(alpha = a * 0.5f)
            drawRect(
                brush = Brush.verticalGradient(listOf(c, Color.Transparent)),
                size = Size(size.width, band)
            )
            drawRect(
                brush = Brush.verticalGradient(listOf(Color.Transparent, c)),
                topLeft = Offset(0f, size.height - band),
                size = Size(size.width, band)
            )
            drawRect(
                brush = Brush.horizontalGradient(listOf(c, Color.Transparent)),
                size = Size(band, size.height)
            )
            drawRect(
                brush = Brush.horizontalGradient(listOf(Color.Transparent, c)),
                topLeft = Offset(size.width - band, 0f),
                size = Size(band, size.height)
            )
        }
    }
}

/** 错误抖动：±3px。 */
@Composable
fun Modifier.errorShake(trigger: Int): Modifier {
    val shake = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        val sequence = listOf(3f, -3f, 2f, -2f, 1f, 0f)
        for (v in sequence) {
            shake.snapTo(v)
            delay(36)
        }
    }
    return this.graphicsLayer { translationX = shake.value }
}

/** 完成时"炸开"：光球完成后化作 16 个粒子四散（620ms 淡出）。 */
@Composable
fun BurstParticles(trigger: Int, modifier: Modifier = Modifier, color: Color = AmbientAccent) {
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

/** 人为因素：休息提醒浮条。 */
@Composable
fun BreakNotice(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    LaunchedEffect(Unit) {
        Feedback.rest()
        delay(6000)
        onDismiss()
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .floatIn(distanceDp = (-10).dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            Modifier
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, Glimmer.copy(alpha = 0.92f), Color.Transparent)
                    ),
                    RoundedCornerShape(22.dp)
                )
                .padding(horizontal = 32.dp, vertical = 12.dp)
        ) {
            Text(
                t("notice_rest"),
                style = SubtitleStyle,
                color = MoonSilver,
                textAlign = TextAlign.Center
            )
        }
    }
}
