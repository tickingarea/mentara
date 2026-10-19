package com.learnagent.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.learnagent.ui.theme.Abyss
import com.learnagent.ui.theme.Aureate
import com.learnagent.ui.theme.Glimmer
import com.learnagent.ui.theme.Mist
import com.learnagent.ui.theme.Phantom
import com.learnagent.ui.theme.SuccessGreen
import com.learnagent.ui.theme.TextHigh
import com.learnagent.ui.theme.TextMid
import com.learnagent.design.DesignTokens
import com.learnagent.ui.theme.Veil

// ============================================================
//  玻璃 IMPORT 按钮 / 视频卡片（缩略图+新鲜度+错落）/ 成功光带
// ============================================================

/** 导入按钮协调化：玻璃渐变 + 全大写 + 2sp 字距 + 按压 TAP 音与触感。 */
@Composable
fun GlassImportButton(onClick: () -> Unit) {
    val context = LocalContext.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shift by animateFloatAsState(if (pressed) 1f else 0f, Motion.standard, label = "importShift")
    Box(
        modifier = Modifier
            .pressScale(interaction)
            .clip(CardShapes.importCard())
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Accent.copy(alpha = 0.10f),
                        Aureate.copy(alpha = 0.16f),
                        Accent.copy(alpha = 0.10f),
                        Aureate.copy(alpha = 0.16f)
                    ),
                    startX = -200f + shift * 300f,
                    endX = 300f + shift * 300f
                )
            )
            .clickable(interaction, indication = null) {
                AudioFx.tap(); Haptics.tap(context)
                onClick()
            }
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ImportIcon(18)
            Spacer(Modifier.width(10.dp))
            Text(
                t("btn_import"),
                style = MaterialTheme.typography.labelLarge.copy(
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = TextHigh
            )
        }
    }
}

/** 视频卡片：20dp 无边框光晕 + 微缩略图 + 单元数 + 新鲜度条 + ±4dp 错落 + 持续微倾斜。 */
@Composable
fun VideoCardAndroid(
    id: String,
    segments: Int,
    freshness: Float,
    selected: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val interaction = remember { MutableInteractionSource() }
    // 卡片激活态：hover（外接鼠标/触控笔）/pressed（触屏回退）统一 0..1，200ms
    val activation = rememberCardActivation(interaction)
    val goldBorder by animateDpAsState(
        targetValue = if (activation > 0f) 1.dp else 0.dp,
        animationSpec = tween(200),
        label = "cardGoldBorder"
    )
    val wobbleDp = ((id.hashCode() and 0x7FFF) % 9) - 4
    val accentPalette = listOf(
        Color(0xFF4A7AB8), Color(0xFFB8A8D4), Color(0xFF7FB89C), Color(0xFFE8C87A), Color(0xFFE89A9A)
    )
    val accentColor = accentPalette[(id.hashCode() and 0x7FFFFFFF) % accentPalette.size]
    val enterRotation = remember { Animatable(-15f) }
    LaunchedEffect(Unit) { enterRotation.animateTo(0f, tween(400, easing = FastOutSlowInEasing)) }
    GlowBox(
        glow = if (selected) Accent else Aureate,
        intensity = if (selected) 1f else 0.30f + 0.70f * activation,
        radius = 130.dp,
        modifier = Modifier.fillMaxWidth().floatIn(distanceDp = 14.dp)
    ) {
        Surface(
            color = Color(0xFF243044).copy(alpha = 0.95f),
            shape = CardShapes.videoCard(),
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationY = wobbleDp.dp.toPx()
                    rotationX = -1.5f
                    cameraDistance = 18f * density
                    scaleX = 1f + 0.025f * activation
                    scaleY = 1f + 0.025f * activation
                }
                .rotation3D(enterRotation.value)
                .border(goldBorder, Aureate.copy(alpha = 0.55f * activation), CardShapes.videoCard())
                .pressScale(interaction)
                .clickable(interaction, indication = null) {
                    AudioFx.tap(); Haptics.tap(context)
                    onClick()
                }
                .hoverable(interaction)
        ) {
            Column(
                Modifier
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF060A0F).copy(alpha = 0.55f - 0.47f * activation),
                                Color(0xFF060A0F).copy(alpha = 0.16f - 0.14f * activation)
                            )
                        )
                    )
                    .padding(DesignTokens.SpaceCardInnerDp.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 封面：默认暗滤镜（能看出轮廓）→ 按压/悬停明显提亮 + 金色描边/光晕
                    Box(
                        Modifier
                            .size(width = 52.dp, height = 38.dp)
                            .clip(RoundedCornerShape(9.dp))
                    ) {
                        // 程序化封面底：冷调画面 + 柔光，避免纯黑
                        Canvas(
                            Modifier
                                .matchParentSize()
                                .graphicsLayer { alpha = 0.85f + 0.15f * activation }
                        ) {
                            drawRect(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF3C5878),
                                        Color(0xFF243449),
                                        Color(0xFF17202E)
                                    )
                                )
                            )
                            drawRect(
                                brush = Brush.verticalGradient(
                                    listOf(accentColor.copy(alpha = 0.85f), accentColor.copy(alpha = 0.12f))
                                ),
                                size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.34f)
                            )
                            drawCircle(
                                brush = Brush.radialGradient(
                                    listOf(Color.White.copy(alpha = 0.22f), Color.Transparent)
                                ),
                                radius = size.width * 0.50f,
                                center = Offset(size.width * 0.30f, size.height * 0.30f)
                            )
                            drawCircle(
                                brush = Brush.radialGradient(
                                    listOf(Aureate.copy(alpha = 0.26f), Color.Transparent)
                                ),
                                radius = size.width * 0.42f,
                                center = Offset(size.width * 0.80f, size.height * 0.72f)
                            )
                        }
                        // 原三点缩略图（批次 1 视觉不变）
                        MiniThumb(seed = id)
                        // 暗滤镜：Abyss 0.65 → 0.20（按压/悬停）
                        Box(
                            Modifier
                                .matchParentSize()
                                .background(Abyss.copy(alpha = 0.45f - 0.40f * activation))
                        )
                        // 金色描边：0dp → 1dp
                        Box(
                            Modifier
                                .matchParentSize()
                                .border(goldBorder, Aureate.copy(alpha = 0.90f * activation), RoundedCornerShape(9.dp))
                        )
                        // 径向金色 halo：强度 0 → 1
                        Canvas(Modifier.matchParentSize()) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    listOf(Aureate.copy(alpha = 0.30f * activation), Color.Transparent),
                                    center = center,
                                    radius = size.maxDimension * 0.75f
                                ),
                                radius = size.maxDimension * 0.75f,
                                center = center
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            id,
                            style = MaterialTheme.typography.titleMedium.copy(
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.55f),
                                    offset = Offset(0f, 1f),
                                    blurRadius = 3f
                                )
                            ),
                            color = TextHigh,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (segments > 0) t("home_indexed", "", segments.toString()) else t("home_not_indexed"),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = TextMid,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                // 新鲜度条
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Mist.copy(alpha = 0.4f))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(freshness)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Accent.copy(alpha = 0.8f), Aureate.copy(alpha = 0.8f))
                                )
                            )
                    )
                }
            }
        }
    }
}

/** 微妙的程序化"缩略图"：Logo 同源三点结构微缩视觉（accent 由组合层取好传入）。 */
@Composable
fun MiniThumb(seed: String) {
    val accent = Accent
    Canvas(Modifier.size(width = 52.dp, height = 38.dp).clip(RoundedCornerShape(9.dp))) {
        drawRect(Brush.linearGradient(listOf(Glimmer, Abyss)))
        val t1 = Offset(size.width * 0.28f, size.height * 0.36f)
        val t2 = Offset(size.width * 0.64f, size.height * 0.30f)
        val t3 = Offset(size.width * 0.44f, size.height * 0.72f)
        drawLine(accent.copy(alpha = 0.4f), t1, t3, strokeWidth = 1f)
        drawLine(accent.copy(alpha = 0.4f), t2, t3, strokeWidth = 1f)
        val a = 0.5f + ((seed.hashCode() and 0x7FFF) % 5) * 0.1f
        drawCircle(accent.copy(alpha = a), radius = 3.2f, center = t1)
        drawCircle(Aureate.copy(alpha = a), radius = 2.4f, center = t2)
        drawCircle(accent.copy(alpha = a), radius = 4f, center = t3)
    }
}

/** 成功光带：顶部中心向两侧扩散绿色光带，2 秒消散。 */
@Composable
fun SuccessFlashAndroid(trigger: Int, modifier: Modifier = Modifier) {
    var visible by remember { androidx.compose.runtime.mutableStateOf(false) }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        visible = true
        kotlinx.coroutines.delay(2000)
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


