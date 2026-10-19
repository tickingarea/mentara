package com.learnagent.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * V3 杂志级字体层级（Android，与桌面同规格）：
 * 每级字号差 ≥ 4sp，相邻层级字重差 ≥ 100（Thin→Light→Regular→Medium）。
 */
private val Light = FontFamily.SansSerif
private val Mono = FontFamily.Monospace

val Typography = Typography(
    // DisplayLarge：品牌/数字标牌 48sp Thin 8sp
    displayLarge = TextStyle(
        fontFamily = Light, fontWeight = FontWeight.Thin,
        fontSize = 48.sp, lineHeight = 52.sp, letterSpacing = 8.sp
    ),
    // DisplaySmall：品牌标题 32sp Light 4sp
    displaySmall = TextStyle(
        fontFamily = Light, fontWeight = FontWeight.Light,
        fontSize = 32.sp, lineHeight = 36.sp, letterSpacing = 4.sp
    ),
    // HeadlineLarge：页面主标 28sp Thin 3sp
    headlineLarge = TextStyle(
        fontFamily = Light, fontWeight = FontWeight.Thin,
        fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = 3.sp
    ),
    // HeadlineMedium：区块标 20sp Light 2sp
    headlineMedium = TextStyle(
        fontFamily = Light, fontWeight = FontWeight.Light,
        fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = 2.sp
    ),
    // HeadlineSmall：子区块 18sp Light 1.5sp
    headlineSmall = TextStyle(
        fontFamily = Light, fontWeight = FontWeight.Light,
        fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = 1.5.sp
    ),
    // TitleLarge：条目标 16sp Medium 1sp
    titleLarge = TextStyle(
        fontFamily = Light, fontWeight = FontWeight.Medium,
        fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 1.sp
    ),
    // TitleMedium：条目 15sp Medium 0.5sp
    titleMedium = TextStyle(
        fontFamily = Light, fontWeight = FontWeight.Medium,
        fontSize = 15.sp, lineHeight = 21.sp, letterSpacing = 0.5.sp
    ),
    // TitleSmall：小条目 14sp Medium 0.5sp
    titleSmall = TextStyle(
        fontFamily = Light, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.5.sp
    ),
    // BodyLarge：正文 15sp Regular 1.6× 行高
    bodyLarge = TextStyle(
        fontFamily = Light, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 24.sp, letterSpacing = 0.3.sp
    ),
    // BodyMedium：次要 14sp Regular 1.5× 行高
    bodyMedium = TextStyle(
        fontFamily = Light, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 21.sp, letterSpacing = 0.3.sp
    ),
    // BodySmall：辅助 12sp Light 1.4× 行高
    bodySmall = TextStyle(
        fontFamily = Light, fontWeight = FontWeight.Light,
        fontSize = 12.sp, lineHeight = 17.sp, letterSpacing = 0.3.sp
    ),
    // LabelLarge：按钮 14sp Medium 2sp 全大写
    labelLarge = TextStyle(
        fontFamily = Light, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 2.sp
    ),
    // LabelMedium：标签 12sp Light 2sp
    labelMedium = TextStyle(
        fontFamily = Light, fontWeight = FontWeight.Light,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 2.sp
    ),
    // LabelSmall：微标 13sp Mono 1sp
    labelSmall = TextStyle(
        fontFamily = Mono, fontWeight = FontWeight.Normal,
        fontSize = 13.sp, lineHeight = 17.sp, letterSpacing = 1.sp
    )
)
