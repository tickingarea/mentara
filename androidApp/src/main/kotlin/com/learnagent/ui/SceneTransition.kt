package com.learnagent.ui

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * 场景切换（10 种）：
 * listToDetail / contentToGraph / graphToQa / tabSwitch / dialogOpen / dialogClose /
 * modeSwitch / immersiveEnter / scrollParallax / cardExpand。
 *
 * 说明：Compose 1.7 的 SharedTransitionLayout 为实验 API 且需要改造列表/详情结构，
 * 本批按规范允许的替代方案实现"位移 + 缩放 + 淡入淡出"（位置连续性由共享锚点近似）。
 */
object SceneTransitions {

    /** 切换 1：列表 → 详情（卡片位移 + 淡入；共享元素替代方案）。 */
    fun listToDetail(): ContentTransform =
        (slideInHorizontally(tween(400, easing = FastOutSlowInEasing)) { it / 6 } + fadeIn(tween(400))) togetherWith
            (slideOutHorizontally(tween(400, easing = FastOutSlowInEasing)) { -it / 8 } + fadeOut(tween(300)))

    /** 切换 2：内容 → 图谱（缩小淡出 → 中央放大淡入，600ms）。 */
    fun contentToGraph(): ContentTransform =
        (scaleIn(tween(600, easing = FastOutSlowInEasing), initialScale = 0.92f) + fadeIn(tween(600))) togetherWith
            (scaleOut(tween(600, easing = FastOutSlowInEasing), targetScale = 0.90f) + fadeOut(tween(400)))

    /** 切换 3：图谱 → 问答（左滑出 / 右滑入，400ms）。 */
    fun graphToQa(): ContentTransform =
        (slideInHorizontally(tween(400, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(400))) togetherWith
            (slideOutHorizontally(tween(400, easing = FastOutSlowInEasing)) { -it } + fadeOut(tween(300)))

    /** 切换 4：Tab「从下方顶出」（上推 + 淡入，320ms）。 */
    fun tabSwitch(): ContentTransform =
        (slideInVertically(tween(320, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(320))) togetherWith
            (slideOutVertically(tween(240, easing = FastOutSlowInEasing)) { -it / 10 } + fadeOut(tween(200)))

    /** 切换 5：对话框打开（底部弹入 + 缩放 + 淡入，spring，350ms 档）。 */
    fun dialogOpen(): EnterTransition =
        slideInVertically(
            animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)
        ) { it / 3 } +
            scaleIn(animationSpec = spring(dampingRatio = 0.7f), initialScale = 0.92f) +
            fadeIn(tween(350))

    /** 切换 6：对话框关闭（反向，250ms）。 */
    fun dialogClose(): ExitTransition =
        scaleOut(tween(250), targetScale = 0.95f) + fadeOut(tween(250))

    /** 切换 7：模式切换 3D 翻转（0 → 90° → 0°，各 400ms；内容在中点切换）。 */
    @Composable
    fun modeSwitch(trigger: Any?): Modifier {
        val rotation = remember { Animatable(0f) }
        LaunchedEffect(trigger) {
            rotation.snapTo(0f)
            rotation.animateTo(90f, tween(400, easing = FastOutSlowInEasing))
            rotation.animateTo(0f, tween(400, easing = FastOutSlowInEasing))
        }
        return Modifier.graphicsLayer {
            rotationY = rotation.value
            cameraDistance = 12f * density
        }
    }

    /** 切换 8：进入沉浸模式（工具栏上滑出 / 内容上移扩展，400ms）。 */
    @Composable
    fun immersiveEnter(immersive: Boolean): Modifier {
        val dy = remember { Animatable(0f) }
        LaunchedEffect(immersive) {
            dy.animateTo(
                targetValue = if (immersive) -72f else 0f,
                animationSpec = tween(400, easing = FastOutSlowInEasing)
            )
        }
        return Modifier.graphicsLayer { translationY = dy.value }
    }

    /** 切换 9：滚动视差（背景慢 / 内容中 / 前景快，由 speed 控制）。 */
    @Composable
    fun scrollParallax(scrollState: ScrollState, speed: Float = 0.5f): Modifier {
        val offset = remember { derivedStateOf { -scrollState.value * speed } }
        return Modifier.graphicsLayer { translationY = offset.value }
    }

    /** 切换 10：卡片展开（高度 spring，dampingRatio = 0.7）。 */
    fun cardExpand(): Modifier =
        Modifier.animateContentSize(
            animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)
        )
}
