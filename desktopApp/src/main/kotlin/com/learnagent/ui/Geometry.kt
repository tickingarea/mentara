package com.learnagent.ui

import androidx.compose.ui.geometry.Offset

/** 三次贝塞尔取点（轨道曲线绘制，桌面端几何工具）。 */
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
