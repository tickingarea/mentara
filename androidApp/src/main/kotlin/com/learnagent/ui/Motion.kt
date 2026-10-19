package com.learnagent.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.Dp

/** Android 端微动态规格（与桌面 Motion 同值）。 */
object Motion {
    val fast = tween<Float>(durationMillis = 120, easing = FastOutSlowInEasing)
    val fastDp = tween<Dp>(durationMillis = 120, easing = FastOutSlowInEasing)
    val standard = tween<Float>(durationMillis = 220, easing = FastOutSlowInEasing)
    val slow = tween<Float>(durationMillis = 320, easing = FastOutSlowInEasing)
    val spring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
    val softSpring = spring<Float>(
        dampingRatio = 0.75f,
        stiffness = Spring.StiffnessLow
    )
}
