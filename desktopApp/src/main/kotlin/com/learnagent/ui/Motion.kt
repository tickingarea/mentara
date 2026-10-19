package com.learnagent.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.Dp

/**
 * 微动态规格（DESIGN.md · 幻域级）。
 * tween/spring 属于 Compose 动画 API；因 shared 的 android 目标无法引入 Compose（离线缓存限制），
 * 本文件位于 desktopApp 层。
 */
object Motion {
    /** 快速响应：hover、按压（120ms） */
    val fast = tween<Float>(durationMillis = 120, easing = FastOutSlowInEasing)

    /** 快速响应（Dp，用于卡片上浮） */
    val fastDp = tween<Dp>(durationMillis = 120, easing = FastOutSlowInEasing)

    /** 标准过渡：卡片展开、面板切换（220ms） */
    val standard = tween<Float>(durationMillis = 220, easing = FastOutSlowInEasing)

    /** 慢速：页面切换（320ms） */
    val slow = tween<Float>(durationMillis = 320, easing = FastOutSlowInEasing)

    /** 弹簧：元素出现 */
    val spring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    /** 柔和弹簧：3D 回弹、浮入 */
    val softSpring = spring<Float>(
        dampingRatio = 0.75f,
        stiffness = Spring.StiffnessLow
    )
}
