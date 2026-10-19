package com.learnagent.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * 电影级运镜（8 种镜头）：
 * dollyIn / dollyOut / parallax / rackFocus / rotation3D / breathing / shake / panTransition。
 * 协调规则：每次只用一种镜头；时长 300-500ms；缓动 FastOutSlowIn / Spring。
 *
 * 性能保护：帧率低时 parallax 幅度 0.08 → 0.04，关闭 blur（rackFocus 直接清零）。
 */
object PerfGuard {
    /** 由性能采样置位（帧率 < 50fps 时 true）。 */
    var lowFps: Boolean = false
    val parallaxScale: Float get() = if (lowFps) 0.04f else 0.08f
    fun shouldUseBlur(): Boolean = !lowFps
}

/** 镜头 1：推进（进入详情页）scale 1.0 → 1.08，400ms。 */
@Composable
fun Modifier.dollyIn(active: Boolean = true): Modifier {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(active) {
        scale.animateTo(
            targetValue = if (active) 1.08f else 1f,
            animationSpec = tween(if (active) 400 else 350, easing = FastOutSlowInEasing)
        )
    }
    return this.graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}

/** 镜头 2：拉远（返回列表）scale 1.08 → 1.0，350ms。 */
@Composable
fun Modifier.dollyOut(active: Boolean): Modifier {
    val scale = remember { Animatable(1.08f) }
    LaunchedEffect(active) {
        scale.animateTo(
            targetValue = if (active) 1f else 1.08f,
            animationSpec = tween(350, easing = FastOutSlowInEasing)
        )
    }
    return this.graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}

/** 镜头 4：视差（鼠标 ±100px → 图层 ±8px；低帧率降为 ±4px）。 */
@Composable
fun Modifier.parallax(mouseX: Float, centerX: Float): Modifier =
    this.graphicsLayer {
        translationX = (mouseX - centerX) * PerfGuard.parallaxScale
    }

/** 镜头 5：景深（focused=false 时 blur 0 → 4dp，500ms；低帧率关闭）。 */
@Composable
fun Modifier.rackFocus(focused: Boolean): Modifier {
    val blurDp by animateDpAsState(
        targetValue = if (focused || !PerfGuard.shouldUseBlur()) 0.dp else 4.dp,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "rackFocus"
    )
    return if (blurDp.value > 0f) this.blur(blurDp) else this
}

/** 镜头 6：3D 旋转（卡片翻面）。 */
@Composable
fun Modifier.rotation3D(angle: Float): Modifier =
    this.graphicsLayer {
        rotationY = angle
        cameraDistance = 12f * density
    }

/** 镜头 7：呼吸（空状态/加载态，scale 1.0 → 1.02，周期 3s）。 */
@Composable
fun Modifier.breathing(): Modifier {
    val transition = rememberInfiniteTransition(label = "breathing")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingScale"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** 镜头 8：抖动（错误提示 -4 → +4 → -4 → 0，300ms）。 */
@Composable
fun Modifier.shake(active: Boolean): Modifier {
    val tx = remember { Animatable(0f) }
    LaunchedEffect(active) {
        if (active) {
            tx.animateTo(-4f, tween(60, easing = FastOutSlowInEasing))
            tx.animateTo(4f, tween(60, easing = FastOutSlowInEasing))
            tx.animateTo(-4f, tween(60, easing = FastOutSlowInEasing))
            tx.animateTo(0f, tween(120, easing = FastOutSlowInEasing))
        }
    }
    return this.graphicsLayer { translationX = tx.value }
}

/** 镜头 3：平移（Tab/页面切换的位移量）。 */
@Composable
fun Modifier.panTransition(offset: Float): Modifier =
    this.graphicsLayer { translationX = offset }
