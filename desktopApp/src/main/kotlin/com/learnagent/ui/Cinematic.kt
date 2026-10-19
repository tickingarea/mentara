package com.learnagent.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

// ============================================================
//  电影质感基建：光源 / 光晕 / 暗角 / 虚线 / 代码高亮
// ============================================================

/**
 * 主光源：从右上 30° 照射的柔和高光。
 * 卡片内部 drawBehind 使用——右上角亮，左下角暗。
 */
fun Modifier.cinematicLight(intensity: Float = 0.08f): Modifier = drawBehind {
    val w = size.width
    val h = size.height
    drawRect(
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF0F1419),  // Abyss Blue
                Color(0xFF0A0E14),  // Deep Void
            ),
            start = Offset(w * 0.8f, h * 0.1f),
            end = Offset(w * 0.2f, h * 0.9f)
        )
    )
    // 暖光源（右上 30°）
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Aureate.copy(alpha = intensity),
                Color.Transparent
            ),
            center = Offset(w * 0.85f, h * 0.1f),
            radius = w * 0.5f
        ),
        radius = w * 0.5f,
        center = Offset(w * 0.85f, h * 0.1f)
    )
}

/**
 * 暗角：四角变暗（vignette effect）。
 */
fun Modifier.vignette(strength: Float = 0.3f): Modifier = drawBehind {
    val c = Offset(size.width / 2f, size.height / 2f)
    val r = size.maxDimension * 0.7f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Transparent,
                Color.Transparent,
                DeepVoid.copy(alpha = strength)
            ),
            center = c,
            radius = r
        ),
        radius = r,
        center = c
    )
}

/**
 * 光晕：元素背后的柔和 radial gradient。
 * 用 drawBehind 画在文字/缩略图后面。
 */
fun Modifier.haloGlow(color: Color, radiusPx: Float = 80f, alpha: Float = 0.15f): Modifier = drawBehind {
    val c = Offset(size.width / 2f, size.height / 2f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha), Color.Transparent),
            center = c,
            radius = radiusPx
        ),
        radius = radiusPx,
        center = c
    )
}

/**
 * 暖色调覆盖：0.05 透明度的暖色全卡片覆盖。
 */
fun Modifier.warmOverlay(): Modifier = drawBehind {
    drawRect(
        Brush.radialGradient(
            colors = listOf(
                Aureate.copy(alpha = 0.03f),
                Color.Transparent
            ),
            center = Offset(size.width * 0.6f, size.height * 0.3f),
            radius = size.maxDimension * 0.8f
        )
    )
}

/**
 * 虚线分隔（不是实线——电影字幕风格）。
 */
@Composable
fun CinematicDivider(modifier: Modifier = Modifier, color: Color = TextLow.copy(alpha = 0.3f)) {
    Canvas(
        modifier
            .fillMaxWidth()
            .height(1.dp)
    ) {
        val path = Path().apply {
            moveTo(0f, size.height / 2)
            lineTo(size.width, size.height / 2)
        }
        drawPath(
            path,
            color,
            style = Stroke(
                width = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
            )
        )
    }
}

/**
 * 金色短线：标题下方的装饰性渐变横线。
 */
@Composable
fun GoldUnderline(width: Dp = 48.dp, modifier: Modifier = Modifier) {
    Canvas(
        modifier
            .height(2.dp)
            .padding(bottom = 0.dp)
    ) {
        drawRect(
            Brush.horizontalGradient(
                colors = listOf(Aureate.copy(alpha = 0.7f), Aureate.copy(alpha = 0.1f), Color.Transparent),
                startX = 0f,
                endX = width.toPx()
            ),
            size = Size(width.toPx(), 2.dp.toPx())
        )
    }
}

// ============================================================
//  电影级文字样式（速查）
// ============================================================

/** 主角标题：24sp Thin + 金色 + 光晕感 */
val HeroTitle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Thin,
    fontSize = 24.sp,
    letterSpacing = 4.sp
)

/** 副角标题：18sp Light + 冷银 */
val SupportingTitle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Light,
    fontSize = 18.sp,
    letterSpacing = 1.sp
)

/** 标签：11sp Medium 全大写 3sp 字距 */
val CinematicTag = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    letterSpacing = 3.sp
)

/** 微标数字：Mono 11sp */
val MicroMono = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Normal,
    fontSize = 11.sp,
    letterSpacing = 0.5.sp
)
