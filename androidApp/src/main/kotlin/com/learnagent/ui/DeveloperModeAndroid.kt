package com.learnagent.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.learnagent.core.TechDecisionService
import com.learnagent.core.TechUsageTracker
import com.learnagent.core.WeeklyReportGenerator
import com.learnagent.data.Decision
import com.learnagent.data.TechCard
import com.learnagent.platform.TechStore
import com.learnagent.ui.theme.Abyss
import com.learnagent.ui.theme.Aureate
import com.learnagent.ui.theme.Coral
import com.learnagent.ui.theme.Glimmer
import com.learnagent.ui.theme.Mist
import com.learnagent.ui.theme.MoonSilver
import com.learnagent.ui.theme.Phantom
import com.learnagent.ui.theme.SuccessGreen
import com.learnagent.ui.theme.TextHigh
import com.learnagent.ui.theme.TextMid
import com.learnagent.ui.theme.Veil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ============================================================
//  Developer Mode 第 2 批 UI（Android）：
//  技术地图 / 周报 / 决策日志 + 回访 / 记录决策 / chips
//  布局算法复用 shared commonMain ui/TechMap.kt
// ============================================================

private fun dateText(ts: Long): String =
    if (ts <= 0L) "—" else SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(ts))

@Composable
fun TechCardCompactDialogAndroid(card: TechCard, store: TechStore, onClose: () -> Unit) {
    LaunchedEffect(card.id) { TechUsageTracker(store).onTechCardOpened(card.name) }
    AlertDialog(
        onDismissRequest = onClose,
        shape = CardShapes.techCard(),
        title = { Text(card.name, color = Accent) },
        text = {
            Column {
                Text(card.category.name, style = MaterialTheme.typography.labelSmall, color = Aureate)
                Spacer(Modifier.height(6.dp))
                Text(card.whatItIs, style = MaterialTheme.typography.bodyMedium, color = TextHigh)
                if (card.whatItSolves.isNotBlank()) Text("解决：${card.whatItSolves}", style = MaterialTheme.typography.bodySmall, color = MoonSilver)
                if (card.whenToUse.isNotBlank()) Text("适用：${card.whenToUse}", style = MaterialTheme.typography.bodySmall, color = MoonSilver)
                if (card.whenNotToUse.isNotBlank()) Text("不适用：${card.whenNotToUse}", style = MaterialTheme.typography.bodySmall, color = Coral)
                if (card.codeSnippet.isNotBlank()) Text(card.codeSnippet, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = SuccessGreen)
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("关闭") } }
    )
}

@Composable
fun TechChipsRowAndroid(names: List<String>, onChip: (String) -> Unit) {
    if (names.isEmpty()) return
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        names.forEach { name ->
            Text(
                "# $name",
                style = MaterialTheme.typography.labelSmall,
                color = Aureate,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Glimmer.copy(alpha = 0.6f))
                    .border(1.dp, Aureate.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                    .clickable { onChip(name) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

/** 首页提醒 chip（周报 / 回访）。 */
@Composable
fun DevAlertChipAndroid(title: String, action: String, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Glimmer.copy(alpha = 0.45f))
            .border(1.dp, Aureate.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodySmall, color = TextHigh, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.width(8.dp))
        Text(action, style = MaterialTheme.typography.labelMedium, color = Aureate)
    }
}

@Composable
fun RecordDecisionDialogAndroid(store: TechStore, prefillProblem: String, onClose: () -> Unit) {
    var problem by remember { mutableStateOf(prefillProblem) }
    var options by remember { mutableStateOf("") }
    var chosen by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var constraints by remember { mutableStateOf("") }
    var followUpDays by remember { mutableStateOf("7") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("记录决策", color = Accent) },
        text = {
            Column {
                OutlinedTextField(problem, { problem = it }, label = { Text("问题") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(options, { options = it }, label = { Text("可选方案（每行一个）") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(chosen, { chosen = it }, label = { Text("最终选择") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(reason, { reason = it }, label = { Text("理由") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(constraints, { constraints = it }, label = { Text("约束条件（逗号分隔）") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    followUpDays,
                    { followUpDays = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("回访天数") },
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = Coral)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (problem.isBlank() || chosen.isBlank() || reason.isBlank()) {
                    error = "问题 / 最终选择 / 理由 必填"
                } else {
                    TechDecisionService.record(
                        store = store,
                        problem = problem.trim(),
                        chosen = chosen.trim(),
                        reason = reason.trim(),
                        options = options.lines().map { it.trim() }.filter { it.isNotBlank() },
                        constraints = constraints.split(',', '，').map { it.trim() }.filter { it.isNotBlank() },
                        followUpDays = followUpDays.toIntOrNull() ?: 7
                    )
                    onClose()
                }
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("取消") } }
    )
}

@Composable
fun FollowUpDialogAndroid(store: TechStore, onClose: () -> Unit) {
    var index by remember { mutableStateOf(0) }
    val due = remember { TechDecisionService.dueFollowUps(store) }
    var result by remember { mutableStateOf("") }
    val current = due.getOrNull(index)

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("决策回访（${index + 1}/${due.size}）", color = Accent) },
        text = {
            Column {
                if (current == null) {
                    Text("没有待回访的决策。", color = TextMid)
                } else {
                    Text(current.problem, style = MaterialTheme.typography.bodyMedium, color = TextHigh)
                    Spacer(Modifier.height(4.dp))
                    Text("选择：${current.chosen}｜理由：${current.reason}", style = MaterialTheme.typography.bodySmall, color = MoonSilver)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(result, { result = it }, label = { Text("结果如何？") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("是" to true, "部分" to null, "否" to false).forEach { (label, good) ->
                            Text(
                                label,
                                style = MaterialTheme.typography.labelLarge,
                                color = when (good) {
                                    true -> SuccessGreen
                                    false -> Coral
                                    null -> Aureate
                                },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, Mist, RoundedCornerShape(10.dp))
                                    .clickable {
                                        TechDecisionService.saveFollowUp(store, current, result, good)
                                        result = ""
                                        if (index + 1 >= due.size) onClose() else index++
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("关闭") } }
    )
}

@Composable
fun WeeklyReportDialogAndroid(store: TechStore, onClose: () -> Unit) {
    val report = remember { WeeklyReportGenerator(store).generate() }
    LaunchedEffect(Unit) { WeeklyReportGenerator(store).markRead() }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Weekly Report", color = Accent) },
        text = {
            Column {
                Text("${dateText(report.weekStart)} ~ ${dateText(report.weekEnd)}", style = MaterialTheme.typography.labelSmall, color = TextMid)
                Spacer(Modifier.height(8.dp))
                Text("本周新增技术：${report.newTechsCount}", style = MaterialTheme.typography.bodySmall, color = TextHigh)
                Text("活跃使用：${report.activeUseCount}｜新增决策：${report.newDecisionsCount}", style = MaterialTheme.typography.bodySmall, color = TextHigh)
                Text("强项：${report.strongDomains.joinToString("/").ifBlank { "—" }}", style = MaterialTheme.typography.bodySmall, color = TextHigh)
                Text("见过未用：${report.weakDomains.joinToString("/").ifBlank { "—" }}", style = MaterialTheme.typography.bodySmall, color = TextHigh)
                Text("焦点：${report.focusTech ?: "—"}", style = MaterialTheme.typography.bodySmall, color = TextHigh)
                Spacer(Modifier.height(8.dp))
                Text(report.suggestion, style = MaterialTheme.typography.bodySmall, color = MoonSilver)
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("关闭") } }
    )
}

@Composable
fun DecisionsViewAndroid(store: TechStore, onBack: () -> Unit) {
    val decisions = remember { store.getAllDecisions().sortedByDescending { it.createdAt } }
    Surface(shape = RoundedCornerShape(18.dp), color = Veil.copy(alpha = 0.96f), modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("决策记录", style = MaterialTheme.typography.titleMedium, color = Accent)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onBack) { Text("返回") }
            }
            if (decisions.isEmpty()) {
                Text("还没有决策记录。问答面板 → Record Decision 可创建。", color = TextMid)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(decisions) { d ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(CardShapes.decisionCard())
                                .background(Glimmer.copy(alpha = 0.35f))
                                .padding(12.dp)
                        ) {
                            Text(dateText(d.createdAt), style = MaterialTheme.typography.labelSmall, color = TextMid)
                            Spacer(Modifier.height(4.dp))
                            Text(d.problem, style = MaterialTheme.typography.bodyMedium, color = TextHigh)
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    d.chosen,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF0B0710),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Aureate)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                                if (d.result.isNotBlank()) {
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "结果：${d.result}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = when (d.resultGood) {
                                            true -> SuccessGreen
                                            false -> Coral
                                            null -> MoonSilver
                                        },
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 技术地图（Android：点击节点弹技术卡；布局算法来自 shared）。 */
@Composable
fun TechMapViewAndroid(store: TechStore, modifier: Modifier = Modifier) {
    val cards = remember { store.getAllTechCards() }
    val usages = remember { store.getAllTechUsage() }
    val density = LocalDensity.current
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val layout = remember(cards, usages, canvasSize) {
        TechMapLayoutEngine.build(cards, usages, canvasSize.width.toFloat(), canvasSize.height.toFloat())
    }
    var selected by remember { mutableStateOf<TechCard?>(null) }

    Box(
        modifier = modifier
            .onSizeChanged { canvasSize = it }
            .pointerInput(layout) {
                detectTapGestures { p ->
                    val hit = TechMapLayoutEngine.hitTest(layout.nodes, p.x, p.y, 12f * density.density)
                    if (hit != null) selected = cards.firstOrNull { it.name == hit.name }
                }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            layout.edges.forEach { e ->
                drawLine(Color.White.copy(alpha = 0.15f), Offset(e.fromX, e.fromY), Offset(e.toX, e.toY), strokeWidth = 0.5.dp.toPx())
            }
            layout.nodes.forEach { n ->
                val color = Color(n.colorArgb)
                val r = n.radiusDp * this.density
                if (n.glow) drawCircle(color.copy(alpha = 0.20f), radius = r * 1.9f, center = Offset(n.x, n.y))
                drawCircle(color, radius = r, center = Offset(n.x, n.y))
                drawCircle(Color.White.copy(alpha = 0.25f), radius = r * 1.25f, center = Offset(n.x, n.y), style = Stroke(width = 0.8.dp.toPx()))
            }
        }
        if (cards.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无技术卡：设置页 → Rebuild Tech Cards", color = TextMid)
            }
        }
    }
    selected?.let { TechCardCompactDialogAndroid(it, store) { selected = null } }
}
