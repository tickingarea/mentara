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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.learnagent.core.ContextualRecall
import com.learnagent.core.RecallBlock
import com.learnagent.core.TechDecisionService
import com.learnagent.core.TechUsageTracker
import com.learnagent.core.WeeklyReport
import com.learnagent.core.WeeklyReportGenerator
import com.learnagent.data.Decision
import com.learnagent.data.TechCard
import com.learnagent.platform.TechStore
import com.learnagent.platform.currentTimeMillis
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// ============================================================
//  Developer Mode 第 2 批 UI（桌面）：
//  技术地图 / 周报 / 决策日志 + 回访 / 记录决策表单 / 召回块 / 技术 chips
// ============================================================

private fun relativeTime(ts: Long): String {
    if (ts <= 0L) return ""
    val diff = currentTimeMillis() - ts
    return when {
        diff < 60_000 -> "刚刚"
        diff < 3_600_000 -> "${diff / 60_000} 分钟前"
        diff < 86_400_000 -> "${diff / 3_600_000} 小时前"
        else -> "${diff / 86_400_000} 天前"
    }
}

private fun formatDate(ts: Long): String =
    if (ts <= 0L) "—" else SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(ts))

/** 紧凑技术卡：3 秒可读完；打开时 seenCount +1。 */
@Composable
fun TechCardCompactDialog(card: TechCard, store: TechStore, onClose: () -> Unit) {
    LaunchedEffect(card.id) { TechUsageTracker(store).onTechCardOpened(card.name) }
    Dialog(onDismissRequest = onClose) {
        Surface(shape = CardShapes.techCard(), color = Veil.copy(alpha = 0.98f)) {
            Column(Modifier.width(460.dp).padding(22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(card.name, style = MaterialTheme.typography.titleMedium, color = CinematicGold)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        card.category.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = Phantom,
                        modifier = Modifier
                            .border(1.dp, Mist, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(card.whatItIs, style = MaterialTheme.typography.bodyMedium, color = TextHigh)
                if (card.whatItSolves.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text("解决：${card.whatItSolves}", style = MaterialTheme.typography.bodySmall, color = MoonSilver)
                }
                if (card.whenToUse.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text("适用：${card.whenToUse}", style = MaterialTheme.typography.bodySmall, color = MoonSilver)
                }
                if (card.whenNotToUse.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text("不适用：${card.whenNotToUse}", style = MaterialTheme.typography.bodySmall, color = Coral)
                }
                if (card.howItWorks.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text("原理：${card.howItWorks}", style = MaterialTheme.typography.bodySmall, color = TextMid)
                }
                if (card.codeSnippet.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        card.codeSnippet,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFont),
                        color = SuccessGreen
                    )
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    FilledTonalButton(onClick = onClose, shape = RoundedCornerShape(12.dp)) { Text("关闭") }
                }
            }
        }
    }
}

/** 技术 chips：问答回答下方；点击弹出技术卡。 */
@Composable
fun TechChipsRow(names: List<String>, onChip: (String) -> Unit) {
    if (names.isEmpty()) return
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
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

/** 搜索页顶部召回块。 */
@Composable
fun RecallBlockSection(blocks: List<RecallBlock>, onOpen: (RecallBlock) -> Unit) {
    if (blocks.isEmpty()) return
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Glimmer.copy(alpha = 0.35f))
            .padding(12.dp)
    ) {
        Text("You've seen this before", style = MaterialTheme.typography.labelMedium, color = Aureate)
        Spacer(Modifier.height(8.dp))
        blocks.take(3).forEach { block ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpen(block) }
                    .padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    when (block.type) {
                        "decision" -> "决策"
                        "tech_card" -> "技术卡"
                        else -> "使用"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Phantom,
                    modifier = Modifier.width(44.dp)
                )
                Text(
                    block.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextHigh,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (block.timestamp > 0) {
                    Spacer(Modifier.width(8.dp))
                    Text(relativeTime(block.timestamp), style = MaterialTheme.typography.labelSmall, color = TextMid)
                }
            }
        }
    }
}

/** 记录决策表单（入口：问答面板 [Record Decision]）。 */
@Composable
fun RecordDecisionDialog(
    store: TechStore,
    prefillProblem: String,
    onSaved: (Decision) -> Unit,
    onClose: () -> Unit
) {
    var problem by remember { mutableStateOf(prefillProblem) }
    var options by remember { mutableStateOf("") }
    var chosen by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var constraints by remember { mutableStateOf("") }
    var followUpDays by remember { mutableStateOf("7") }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onClose) {
        Surface(shape = RoundedCornerShape(18.dp), color = Veil.copy(alpha = 0.98f)) {
            Column(Modifier.width(520.dp).padding(22.dp)) {
                Text("Record Decision", style = MaterialTheme.typography.titleMedium, color = CinematicGold)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = problem, onValueChange = { problem = it },
                    label = { Text("Problem") }, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = options, onValueChange = { options = it },
                    label = { Text("Options（每行一个）") }, modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = chosen, onValueChange = { chosen = it },
                    label = { Text("Chosen") }, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = reason, onValueChange = { reason = it },
                    label = { Text("Reason") }, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = constraints, onValueChange = { constraints = it },
                        label = { Text("Constraints（逗号分隔）") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = followUpDays, onValueChange = { followUpDays = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("Follow-up days") },
                        modifier = Modifier.width(150.dp)
                    )
                }
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = Coral)
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    FilledTonalButton(
                        onClick = {
                            if (problem.isBlank() || chosen.isBlank() || reason.isBlank()) {
                                error = "Problem / Chosen / Reason 必填"
                                return@FilledTonalButton
                            }
                            val decision = TechDecisionService.record(
                                store = store,
                                problem = problem.trim(),
                                chosen = chosen.trim(),
                                reason = reason.trim(),
                                options = options.lines().map { it.trim() }.filter { it.isNotBlank() },
                                constraints = constraints.split(',', '，').map { it.trim() }.filter { it.isNotBlank() },
                                followUpDays = followUpDays.toIntOrNull() ?: 7
                            )
                            onSaved(decision)
                            onClose()
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("保存") }
                    Spacer(Modifier.width(8.dp))
                    FilledTonalButton(onClick = onClose, shape = RoundedCornerShape(12.dp)) { Text("取消") }
                }
            }
        }
    }
}

/** 回访：逐个填写 result + resultGood。 */
@Composable
fun FollowUpDialog(store: TechStore, onClose: () -> Unit) {
    var index by remember { mutableStateOf(0) }
    val due = remember { TechDecisionService.dueFollowUps(store) }
    var result by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onClose) {
        Surface(shape = RoundedCornerShape(18.dp), color = Veil.copy(alpha = 0.98f)) {
            Column(Modifier.width(520.dp).padding(22.dp)) {
                Text("决策回访（${index + 1}/${due.size}）", style = MaterialTheme.typography.titleMedium, color = CinematicGold)
                Spacer(Modifier.height(12.dp))
                val current = due.getOrNull(index)
                if (current == null) {
                    Text("没有待回访的决策。", color = TextMid)
                } else {
                    Text(current.problem, style = MaterialTheme.typography.bodyMedium, color = TextHigh)
                    Spacer(Modifier.height(4.dp))
                    Text("选择：${current.chosen}｜理由：${current.reason}", style = MaterialTheme.typography.bodySmall, color = MoonSilver)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = result, onValueChange = { result = it },
                        label = { Text("结果如何？（多行）") }, modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FollowUpChoice("是", SuccessGreen) {
                            TechDecisionService.saveFollowUp(store, current, result, true)
                            result = ""
                            if (index + 1 >= due.size) onClose() else index++
                        }
                        FollowUpChoice("部分", Aureate) {
                            TechDecisionService.saveFollowUp(store, current, result, null)
                            result = ""
                            if (index + 1 >= due.size) onClose() else index++
                        }
                        FollowUpChoice("否", Coral) {
                            TechDecisionService.saveFollowUp(store, current, result, false)
                            result = ""
                            if (index + 1 >= due.size) onClose() else index++
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    FilledTonalButton(onClick = onClose, shape = RoundedCornerShape(12.dp)) { Text("关闭") }
                }
            }
        }
    }
}

@Composable
private fun FollowUpChoice(label: String, color: Color, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

/** 周报页。 */
@Composable
fun WeeklyReportView(store: TechStore, onClose: () -> Unit) {
    val report = remember { WeeklyReportGenerator(store).generate() }
    LaunchedEffect(Unit) { WeeklyReportGenerator(store).markRead() }
    Dialog(onDismissRequest = onClose) {
        Surface(shape = RoundedCornerShape(18.dp), color = Veil.copy(alpha = 0.98f)) {
            Column(Modifier.width(520.dp).padding(22.dp)) {
                Text("Weekly Report", style = MaterialTheme.typography.titleMedium, color = CinematicGold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${formatDate(report.weekStart)} ~ ${formatDate(report.weekEnd)}",
                    style = MaterialTheme.typography.labelSmall, color = TextMid
                )
                Spacer(Modifier.height(14.dp))
                WeeklyRow("本周新增技术", report.newTechsCount.toString())
                WeeklyRow("活跃使用总次数", report.activeUseCount.toString())
                WeeklyRow("新增决策", report.newDecisionsCount.toString())
                WeeklyRow("强项领域", report.strongDomains.joinToString(" / ").ifBlank { "—" })
                WeeklyRow("见过未用", report.weakDomains.joinToString(" / ").ifBlank { "—" })
                WeeklyRow("本周焦点", report.focusTech ?: "—")
                Spacer(Modifier.height(12.dp))
                Text(report.suggestion, style = MaterialTheme.typography.bodySmall, color = TextHigh)
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    FilledTonalButton(onClick = onClose, shape = RoundedCornerShape(12.dp)) { Text("关闭") }
                }
            }
        }
    }
}

@Composable
private fun WeeklyRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextMid, modifier = Modifier.width(130.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, color = TextHigh)
    }
}

/** 决策日志页（按时间倒序）。 */
@Composable
fun DecisionsView(store: TechStore, onBack: () -> Unit) {
    val decisions = remember { store.getAllDecisions().sortedByDescending { it.createdAt } }
    Surface(shape = RoundedCornerShape(18.dp), color = Veil.copy(alpha = 0.96f)) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Decisions", style = MaterialTheme.typography.titleMedium, color = CinematicGold)
                Spacer(Modifier.weight(1f))
                FilledTonalButton(onClick = onBack, shape = RoundedCornerShape(12.dp)) { Text("返回") }
            }
            Spacer(Modifier.height(12.dp))
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
                            Text(formatDate(d.createdAt), style = MaterialTheme.typography.labelSmall, color = TextMid)
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

/** 决策详情（召回块点击）。 */
@Composable
fun DecisionDetailDialog(decision: Decision, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        Surface(shape = RoundedCornerShape(18.dp), color = Veil.copy(alpha = 0.98f)) {
            Column(Modifier.width(480.dp).padding(22.dp)) {
                Text("决策详情", style = MaterialTheme.typography.titleMedium, color = CinematicGold)
                Spacer(Modifier.height(10.dp))
                Text(formatDate(decision.createdAt), style = MaterialTheme.typography.labelSmall, color = TextMid)
                Spacer(Modifier.height(6.dp))
                Text(decision.problem, style = MaterialTheme.typography.bodyMedium, color = TextHigh)
                Spacer(Modifier.height(8.dp))
                Text("选择：${decision.chosen}", style = MaterialTheme.typography.bodySmall, color = Aureate)
                Spacer(Modifier.height(4.dp))
                Text("理由：${decision.reason}", style = MaterialTheme.typography.bodySmall, color = MoonSilver)
                if (decision.constraints.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text("约束：${decision.constraints.joinToString("、")}", style = MaterialTheme.typography.bodySmall, color = TextMid)
                }
                if (decision.result.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "结果：${decision.result}",
                        style = MaterialTheme.typography.bodySmall,
                        color = when (decision.resultGood) {
                            true -> SuccessGreen
                            false -> Coral
                            null -> MoonSilver
                        }
                    )
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    FilledTonalButton(onClick = onClose, shape = RoundedCornerShape(12.dp)) { Text("关闭") }
                }
            }
        }
    }
}

/** 首页提醒卡（周报 / 回访）。 */
@Composable
fun DevAlertCard(title: String, action: String, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Glimmer.copy(alpha = 0.45f))
            .border(1.dp, Aureate.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodySmall, color = TextHigh)
        Spacer(Modifier.width(10.dp))
        Text(action, style = MaterialTheme.typography.labelMedium, color = Aureate)
    }
}

/** 技术地图（Canvas + hover 放大 + 点击弹卡）。 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TechMapView(store: TechStore, modifier: Modifier = Modifier) {
    val cards = remember { store.getAllTechCards() }
    val usages = remember { store.getAllTechUsage() }
    val density = LocalDensity.current
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val layout = remember(cards, usages, canvasSize) {
        TechMapLayoutEngine.build(cards, usages, canvasSize.width.toFloat(), canvasSize.height.toFloat())
    }
    var hovered by remember { mutableStateOf<TechMapNode?>(null) }
    var selected by remember { mutableStateOf<TechCard?>(null) }

    Box(
        modifier = modifier
            .onSizeChanged { canvasSize = it }
            .onPointerEvent(PointerEventType.Move) { e ->
                val p = e.changes.firstOrNull()?.position ?: return@onPointerEvent
                hovered = TechMapLayoutEngine.hitTest(layout.nodes, p.x, p.y, 10f * density.density)
            }
            .onPointerEvent(PointerEventType.Exit) { hovered = null }
            .pointerInput(layout) {
                detectTapGestures { p ->
                    val hit = TechMapLayoutEngine.hitTest(layout.nodes, p.x, p.y, 10f * density.density)
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
                if (hovered?.name == n.name) drawCircle(color.copy(alpha = 0.35f), radius = r * 1.6f, center = Offset(n.x, n.y))
                drawCircle(color, radius = r, center = Offset(n.x, n.y))
                if (hovered?.name == n.name) {
                    drawCircle(CinematicGold, radius = r * 1.2f, center = Offset(n.x, n.y), style = Stroke(width = 1.dp.toPx()))
                }
            }
        }
        if (cards.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无技术卡：设置页 → Rebuild Tech Cards", color = TextMid)
            }
        }
        hovered?.let { n ->
            Text(
                n.name,
                style = MaterialTheme.typography.labelMedium,
                color = TextHigh,
                modifier = Modifier
                    .offset { IntOffset(n.x.roundToInt() + 14, n.y.roundToInt() - 20) }
                    .background(Veil.copy(alpha = 0.95f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
    selected?.let { TechCardCompactDialog(it, store) { selected = null } }
}
