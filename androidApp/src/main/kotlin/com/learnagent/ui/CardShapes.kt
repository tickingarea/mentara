package com.learnagent.ui

import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * 高端异形卡片形状（替代标准 16dp 全圆角）。
 * 每类内容一种形状：斜切角 / 弧顶 / 双层 / 不对称气泡 / 标签切角 / 缺口。
 */
object CardShapes {

    /** 视频卡：左上 + 右下大圆角，右上 + 左下直角（被光斜切）。 */
    fun videoCard(): Shape = RoundedCornerShape(
        topStart = 24.dp,
        topEnd = 0.dp,
        bottomEnd = 24.dp,
        bottomStart = 0.dp
    )

    /** 学习单元卡：弧顶（曲线）+ 左右 20dp 过渡 + 底部直角。 */
    fun learningUnitCard(): Shape = GenericShape { size, _ ->
        val radius = 20f * 2f
        moveTo(0f, size.height)
        lineTo(0f, 40f)
        quadraticBezierTo(size.width / 2f, 0f, size.width, 40f)
        lineTo(size.width, size.height)
        lineTo(radius.coerceAtMost(size.width / 2f), size.height)
        close()
    }

    /** 技术卡：16dp（双层玻璃由使用处叠"影子卡片"实现）。 */
    fun techCard(): Shape = RoundedCornerShape(16.dp)

    /** 用户气泡：右下角 4dp，其余 20dp。 */
    fun userBubble(): Shape = RoundedCornerShape(
        topStart = 20.dp, topEnd = 20.dp, bottomEnd = 4.dp, bottomStart = 20.dp
    )

    /** AI 气泡：左下角 4dp，其余 20dp。 */
    fun aiBubble(): Shape = RoundedCornerShape(
        topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 4.dp
    )

    /** 决策卡：右上斜切 24dp（文件夹标签角）+ 其余三角 12dp 圆角。 */
    fun decisionCard(): Shape = object : Shape {
        override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
            val r = 12f * density.density
            val cut = 24f * density.density
            val w = size.width
            val h = size.height
            val path = Path().apply {
                moveTo(r, 0f)
                lineTo(w - cut, 0f)
                lineTo(w, cut)
                lineTo(w, h - r)
                quadraticBezierTo(w, h, w - r, h)
                lineTo(r, h)
                quadraticBezierTo(0f, h, 0f, h - r)
                lineTo(0f, r)
                quadraticBezierTo(0f, 0f, r, 0f)
                close()
            }
            return Outline.Generic(path)
        }
    }

    /** 导入课程卡：四角 32dp 大圆角 + 左侧中间半圆缺口（被咬一口的饼干）。 */
    fun importCard(): Shape = object : Shape {
        override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
            val r = 32f * density.density
            val notch = 14f * density.density
            val w = size.width
            val h = size.height
            val path = Path().apply {
                moveTo(r, 0f)
                lineTo(w - r, 0f)
                quadraticBezierTo(w, 0f, w, r)
                lineTo(w, h - r)
                quadraticBezierTo(w, h, w - r, h)
                lineTo(r, h)
                quadraticBezierTo(0f, h, 0f, h - r)
                lineTo(0f, h / 2f + notch)
                quadraticBezierTo(notch * 1.6f, h / 2f, 0f, h / 2f - notch)
                lineTo(0f, r)
                quadraticBezierTo(0f, 0f, r, 0f)
                close()
            }
            return Outline.Generic(path)
        }
    }
}
