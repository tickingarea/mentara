package com.learnagent.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.learnagent.core.DebateResult

private val debateAvatars = listOf(Phantom, Aureate, Coral)

private fun agreementColor(a: Double): Color = when {
    a > 0.66 -> SuccessGreen
    a >= 0.40 -> Aureate
    else -> Coral
}

/**
 * 四轮辩论可视化：
 *  - 议题行：可编辑议题 + 重新辩论 / 取消辩论
 *  - 流式：onRound 部分结果逐轮滑入（提案 → 交叉批判 → 中立裁决），每节 floatIn
 *  - 第 1 轮 三栏提案（彩色头像 + 模型名）
 *  - 三方一致性渐变条（绿 >66% · 金 40~66% · 珊瑚 <40%）
 *  - 第 2 轮 交叉批判 3 条（注明"谁批谁"，默认折叠）
 *  - 第 3 轮 中立裁判全文
 *  - 少数派报告（平均相似度最低方，自动产生）
 */
@Composable
fun DebatePanel(
    result: DebateResult?,
    busy: Boolean,
    phase: Int,
    modelNames: Triple<String, String, String>,
    online: Boolean,
    topic: String,
    onTopicChange: (String) -> Unit,
    onRerun: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlowBox(
        glow = MaterialTheme.colorScheme.primary,
        intensity = if (busy) 0.9f else 0.35f,
        radius = 260.dp,
        modifier = modifier.fillMaxSize()
    ) {
        Surface(
            color = Veil.copy(alpha = 0.55f),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(Modifier.fillMaxSize().padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DebateIcon(26)
                    Spacer(Modifier.width(10.dp))
                    Text(t("debate_title"), style = MaterialTheme.typography.titleLarge, color = TextHigh)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (online) "NIM · DeepSeek / Llama / Mistral" else t("debate_offline"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MoonSilver
                    )
                    Spacer(Modifier.weight(1f))
                    if (busy) {
                        Text(
                            when (phase) {
                                1 -> t("debate_short_1")
                                2 -> t("debate_short_2")
                                else -> t("debate_short_3")
                            },
                            style = SubtitleStyle,
                            color = MoonSilver
                        )
                    }
                }

                if (busy) {
                    Spacer(Modifier.height(14.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(Mist.copy(alpha = 0.5f), RoundedCornerShape(1.dp))
                    ) {
                        val progress by animateFloatAsState(
                            targetValue = (phase.coerceIn(0, 3) / 3f).coerceIn(0.05f, 1f),
                            animationSpec = Motion.standard,
                            label = "debateProgress"
                        )
                        Box(
                            Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .background(
                                    Brush.horizontalGradient(listOf(Phantom, Aureate)),
                                    RoundedCornerShape(1.dp)
                                )
                        )
                    }
                }

                // ---- 议题行：自定义议题 + 重新辩论 / 取消 ----
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        TextField(
                            value = topic,
                            onValueChange = onTopicChange,
                            singleLine = true,
                            placeholder = { Text(t("debate_topic_ph"), color = MoonSilver.copy(alpha = 0.6f)) },
                            shape = RoundedCornerShape(12.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Mist.copy(alpha = 0.35f),
                                unfocusedContainerColor = Mist.copy(alpha = 0.22f),
                                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                unfocusedIndicatorColor = Mist,
                                cursorColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Surface(
                        color = if (busy) Coral.copy(alpha = 0.16f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.clickable { if (busy) onCancel() else onRerun() }
                    ) {
                        Text(
                            if (busy) t("debate_cancel") else t("debate_rerun"),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (busy) Coral else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (result == null && !busy) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            RotatingLogo(size = 140.dp, alpha = 0.35f)
                            Spacer(Modifier.height(24.dp))
                            Text(t("debate_waiting_title"), style = SubtitleStyle, color = MoonSilver)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                t("debate_waiting_sub"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMid
                            )
                        }
                    }
                } else if (result != null) {
                    // ---- 第 1 轮：提案（三栏，流式滑入） ----
                    Text(
                        t("debate_phase_1"),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextHigh,
                        modifier = Modifier.floatIn(distanceDp = 12.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val proposals = listOf(result.proposerA, result.proposerB, result.proposerC)
                        val names = listOf(modelNames.first, modelNames.second, modelNames.third)
                        proposals.forEachIndexed { i, proposal ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .floatIn(delayMillis = i * 60, distanceDp = 12.dp)
                                    .background(Veil.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                                    .padding(14.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Canvas(Modifier.size(14.dp)) {
                                            drawCircle(
                                                debateAvatars[i].copy(alpha = 0.35f),
                                                radius = size.minDimension / 2f
                                            )
                                            drawCircle(
                                                debateAvatars[i],
                                                radius = size.minDimension / 3.4f,
                                                center = Offset(size.width / 2f, size.height / 2f)
                                            )
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Text(names[i], style = TimestampStyle, color = MoonSilver)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(proposal, style = MaterialTheme.typography.bodyMedium, color = TextHigh)
                                }
                            }
                        }
                    }

                    // ---- 三方一致性（连续值渐变条） ----
                    if (result.agreement >= 0.0) {
                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(t("debate_agreement"), style = MaterialTheme.typography.titleSmall, color = TextHigh)
                            Spacer(Modifier.width(10.dp))
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .background(Mist.copy(alpha = 0.4f), RoundedCornerShape(3.dp))
                            ) {
                                val frac by animateFloatAsState(
                                    targetValue = result.agreement.toFloat().coerceIn(0f, 1f),
                                    animationSpec = Motion.standard,
                                    label = "agreementBar"
                                )
                                Box(
                                    Modifier
                                        .fillMaxWidth(frac)
                                        .fillMaxHeight()
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(
                                    agreementColor(result.agreement).copy(alpha = 0.5f),
                                                    agreementColor(result.agreement)
                                                )
                                            ),
                                            RoundedCornerShape(3.dp)
                                        )
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "${(result.agreement * 100).toInt()}%",
                                style = TimestampStyle,
                                color = agreementColor(result.agreement)
                            )
                        }
                    }

                    // ---- 第 2 轮：交叉批判（3 条，默认折叠） ----
                    if (result.critiques.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        var showCritiques by remember { mutableStateOf(false) }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .floatIn(distanceDp = 10.dp)
                                .clickable { showCritiques = !showCritiques; Feedback.tap() }
                        ) {
                            Text(t("debate_phase_2"), style = MaterialTheme.typography.titleMedium, color = TextHigh)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (showCritiques) t("debate_collapse") else t("debate_expand"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        AnimatedVisibility(
                            visible = showCritiques,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                Modifier.padding(top = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                result.critiques.forEachIndexed { i, critique ->
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .floatIn(delayMillis = i * 60, distanceDp = 8.dp)
                                            .background(Veil.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                            .padding(12.dp)
                                    ) {
                                        Text(critique, style = MaterialTheme.typography.bodyMedium, color = TextMid)
                                    }
                                }
                            }
                        }
                    }

                    // ---- 第 3 轮：中立裁决 ----
                    if (result.judge.isNotBlank()) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            t("debate_phase_3"),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextHigh,
                            modifier = Modifier.floatIn(distanceDp = 10.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .floatIn(distanceDp = 12.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        ) {
                            Text(result.judge, style = MaterialTheme.typography.bodyMedium, color = TextHigh)
                        }
                    }

                    // ---- 少数派报告 ----
                    if (result.minorityReport.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            t("debate_minority"),
                            style = MaterialTheme.typography.titleSmall,
                            color = Coral,
                            modifier = Modifier.floatIn(distanceDp = 8.dp)
                        )
                        Spacer(Modifier.height(6.dp))
                        Box(
                            Modifier
                                .floatIn(distanceDp = 10.dp)
                                .background(Coral.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(result.minorityReport, style = MaterialTheme.typography.bodyMedium, color = TextMid)
                        }
                    }
                } else {
                    // busy 且尚无任何结果
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            BreathingOrb(size = 96.dp)
                            Spacer(Modifier.height(18.dp))
                            Text(t("debate_running"), style = SubtitleStyle, color = MoonSilver)
                        }
                    }
                }
            }
        }
    }
}
