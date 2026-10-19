package com.learnagent.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import java.awt.GraphicsEnvironment
import java.time.LocalTime

// ============================================================
//  Mentara · 幻域色板（DESIGN.md 2026）
//  虚空 / 深渊 / 轻纱 / 微光 + 幻紫 / 鎏金 / 月银 / 珊瑚
// ============================================================

// 三端共享设计令牌（shared/.../design/DesignTokens.kt）
import com.learnagent.design.DesignTokens as Tokens

val Void = Color(Tokens.ColorVoid)              // 虚空（背景主色）
val Abyss = Color(Tokens.ColorAbyss)            // 深渊（次背景）
val Veil = Color(Tokens.ColorVeil)              // 轻纱（卡片底色）
val Glimmer = Color(Tokens.ColorGlimmer)        // 微光（悬浮层）
val Phantom = Color(Tokens.ColorPhantom)        // 主色·幻紫
val Aureate = Color(Tokens.ColorAureate)        // 主色·鎏金
val MoonSilver = Color(Tokens.ColorMoonSilver)  // 辅助·月银
val Coral = Color(Tokens.ColorCoral)            // 强调·珊瑚
val TextHigh = Color(Tokens.ColorTextHigh)      // 文字·主
val TextMid = Color(Tokens.ColorTextMid)        // 文字·次
val TextLow = Color(Tokens.ColorTextLow)        // 文字·三
val Mist = Color(Tokens.ColorMist)              // 描边·雾

// 空态 / 渐变用色
val CenterGlow = Color(Tokens.ColorCenterGlow)  // 背景径向中心
val EdgeDark = Color(Tokens.ColorEdgeDark)      // 背景径向边缘
val SuccessGreen = Color(Tokens.ColorSuccess)   // 成功绿

// 电影级色板别名（Cinematic.kt 用）
val CinematicGold = Aureate                      // 暖金——主角元素
val DawnSilver = MoonSilver                      // 冷银——次要信息
val Bioluminescence = Color(Tokens.ColorLilyGreen) // 生物荧光——强调
val DeepVoid = Void                               // 最深背景
val ShadowPurple = Veil                           // 阴影里的紫
val AbyssBlue = Abyss                             // 阴影里的蓝
val FogGray = TextMid                             // 雾灰——正文
val Twilight = TextLow                            // 暮色——次要文字
val LilyGreen = Color(Tokens.ColorLilyGreen)      // 睡莲绿（独立引用）

// 旧名兼容（视觉语义已被新色板替换）
val Ink = Void
val InkSurface = Veil
val InkRaised = Glimmer
val Electric = Phantom
val Mint = Aureate
val Rose = Coral
val TextHi = TextHigh
val TextLo = TextMid
val Hairline = Mist

// ---------- 时间幻化：主色随一天时段变化（V3 晨雾紫 / 睡莲绿 / 深海蓝） ----------

val MoonlightPurple = Color(Tokens.ColorMorningPurple)  // 06-12 晨雾紫
val DeepPurple = Color(Tokens.ColorNightPurple)         // 18-06 深海蓝

fun accentForHour(hour: Int): Color = when (hour) {
    in 6..11 -> MoonlightPurple
    in 12..17 -> Color(Tokens.ColorLilyGreen)
    else -> DeepPurple
}

val AmbientAccent: Color get() = accentForHour(LocalTime.now().hour)

// ============================================================
//  字体系统（系统内置，运行时探测，无外部字体文件）
// ============================================================

private val availableFamilies: Set<String> by lazy {
    runCatching {
        GraphicsEnvironment.getLocalGraphicsEnvironment()
            .availableFontFamilyNames.map { it.lowercase() }.toSet()
    }.getOrDefault(emptySet())
}

private fun pickFamily(vararg names: String): String =
    names.firstOrNull { availableFamilies.contains(it.lowercase()) } ?: names.last()

@OptIn(ExperimentalTextApi::class)
val DisplayFont: FontFamily = FontFamily(
    pickFamily("Segoe UI Variable Display", "Segoe UI", "Bahnschrift Light", "sans-serif")
)

@OptIn(ExperimentalTextApi::class)
val BodyFont: FontFamily = FontFamily(
    pickFamily("Segoe UI Variable Text", "Segoe UI", "sans-serif")
)

@OptIn(ExperimentalTextApi::class)
val MonoFont: FontFamily = FontFamily(
    pickFamily("Cascadia Code", "Consolas", "monospace")
)

// 规格样式（DESIGN.md）
val BrandStyle = TextStyle(
    fontFamily = DisplayFont, fontWeight = FontWeight.Light,
    fontSize = 32.sp, letterSpacing = 4.sp
)
val PageTitleStyle = TextStyle(
    fontFamily = DisplayFont, fontWeight = FontWeight.Normal,
    fontSize = 20.sp, letterSpacing = 1.5.sp
)
val SubtitleStyle = TextStyle(
    fontFamily = BodyFont, fontWeight = FontWeight.Light,
    fontSize = 14.sp, letterSpacing = 2.sp
)
val BodyStyle = TextStyle(
    fontFamily = BodyFont, fontWeight = FontWeight.Normal,
    fontSize = 15.sp, lineHeight = 22.sp, letterSpacing = 0.3.sp
)
val TimestampStyle = TextStyle(
    fontFamily = MonoFont, fontWeight = FontWeight.Normal,
    fontSize = 13.sp, letterSpacing = 0.5.sp
)
val ButtonLabelStyle = TextStyle(
    fontFamily = BodyFont, fontWeight = FontWeight.Medium,
    fontSize = 14.sp, letterSpacing = 1.2.sp
)

private val AppTypography = Typography(
    // ---------- V3 杂志级字体层级 ----------
    // DisplayLarge：品牌/数字标牌 48sp Thin 8sp 字距
    displayLarge = TextStyle(
        fontFamily = DisplayFont, fontWeight = FontWeight.Thin,
        fontSize = 48.sp, lineHeight = 52.sp, letterSpacing = 8.sp
    ),
    // DisplaySmall：品牌标题 32sp Light 4sp
    displaySmall = TextStyle(
        fontFamily = DisplayFont, fontWeight = FontWeight.Light,
        fontSize = 32.sp, lineHeight = 36.sp, letterSpacing = 4.sp
    ),
    // HeadlineLarge：页面主标 28sp Thin 3sp
    headlineLarge = TextStyle(
        fontFamily = DisplayFont, fontWeight = FontWeight.Thin,
        fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = 3.sp
    ),
    // HeadlineMedium：区块标 20sp Light 2sp
    headlineMedium = TextStyle(
        fontFamily = DisplayFont, fontWeight = FontWeight.Light,
        fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = 2.sp
    ),
    // HeadlineSmall：子区块 18sp Light 1.5sp
    headlineSmall = TextStyle(
        fontFamily = DisplayFont, fontWeight = FontWeight.Light,
        fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = 1.5.sp
    ),
    // TitleLarge：条目标 16sp Medium 1sp
    titleLarge = TextStyle(
        fontFamily = BodyFont, fontWeight = FontWeight.Medium,
        fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 1.sp
    ),
    // TitleMedium：条目 15sp Medium 0.5sp
    titleMedium = TextStyle(
        fontFamily = BodyFont, fontWeight = FontWeight.Medium,
        fontSize = 15.sp, lineHeight = 21.sp, letterSpacing = 0.5.sp
    ),
    // TitleSmall：小条目 14sp Medium 0.5sp
    titleSmall = TextStyle(
        fontFamily = BodyFont, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.5.sp
    ),
    // BodyLarge：正文 15sp Regular 1.6× 行高
    bodyLarge = TextStyle(
        fontFamily = BodyFont, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 24.sp, letterSpacing = 0.3.sp
    ),
    // BodyMedium：次要 14sp Regular 1.5× 行高
    bodyMedium = TextStyle(
        fontFamily = BodyFont, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 21.sp, letterSpacing = 0.3.sp
    ),
    // BodySmall：辅助 12sp Light 1.4× 行高
    bodySmall = TextStyle(
        fontFamily = BodyFont, fontWeight = FontWeight.Light,
        fontSize = 12.sp, lineHeight = 17.sp, letterSpacing = 0.3.sp
    ),
    // LabelLarge：按钮 14sp Medium 2sp 全大写
    labelLarge = TextStyle(
        fontFamily = BodyFont, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 2.sp
    ),
    // LabelMedium：标签 12sp Light 2sp
    labelMedium = TextStyle(
        fontFamily = BodyFont, fontWeight = FontWeight.Light,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 2.sp
    ),
    // LabelSmall：微标 13sp Mono 1sp
    labelSmall = TextStyle(
        fontFamily = MonoFont, fontWeight = FontWeight.Normal,
        fontSize = 13.sp, lineHeight = 17.sp, letterSpacing = 1.sp
    )
)

// ---------- ColorScheme ----------

private fun darkColors(accent: Color) = darkColorScheme(
    primary = accent,
    onPrimary = Void,
    primaryContainer = Color(0xFF2A1B45),
    onPrimaryContainer = Color(0xFFE8DDFF),
    secondary = Aureate,
    onSecondary = Void,
    secondaryContainer = Color(0xFF2E2410),
    onSecondaryContainer = Color(0xFFF2E3B8),
    tertiary = MoonSilver,
    onTertiary = Void,
    background = Void,
    onBackground = TextHigh,
    surface = Veil,
    onSurface = TextHigh,
    surfaceVariant = Glimmer,
    onSurfaceVariant = TextMid,
    outline = Mist,
    outlineVariant = Mist,
    error = Coral,
    onError = Void
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF7A4FD0),
    onPrimary = Color.White,
    secondary = Color(0xFF9A7B2E),
    onSecondary = Color.White,
    background = Color(0xFFF6F2FA),
    onBackground = Color(0xFF241C30),
    surface = Color(0xFFFDFBFF),
    onSurface = Color(0xFF241C30),
    surfaceVariant = Color(0xFFEDE6F5),
    onSurfaceVariant = Color(0xFF6B6178),
    outline = Color(0xFFD8CFE4),
    error = Color(0xFFC94F63),
    onError = Color.White
)

@Composable
fun LearnAgentTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) darkColors(AmbientAccent) else LightColors,
        typography = AppTypography,
        content = content
    )
}
