package com.learnagent.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.learnagent.core.AnswerStandardLibrary
import com.learnagent.core.AnswerStandardTeacher
import com.learnagent.core.HighlightType
import com.learnagent.core.PracticeResult
import com.learnagent.core.StandardPracticeEngine
import com.learnagent.data.AnswerStandard
import kotlinx.coroutines.launch

/**
 * 规范答题训练：选学科/题型 → 载入内置答题规范 → 输入题目与你的作答 → 批改（红/黄/绿高亮 + 改进建议）。
 * 红=缺失必写步骤/禁止项；黄=顺序有误；绿=命中的规范步骤。
 */
@Composable
fun StandardPracticeView(onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    var subject by remember { mutableStateOf("数学") }
    var standard by remember { mutableStateOf<AnswerStandard?>(null) }
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<PracticeResult?>(null) }
    val subjects = listOf("数学", "物理", "化学", "语文", "英语")
    val standards = remember(subject) { AnswerStandardLibrary.all.filter { it.subject == subject } }

    fun hlColor(type: HighlightType) = when (type) {
        HighlightType.GOOD -> SuccessGreen
        HighlightType.WRONG_ORDER -> Aureate
        HighlightType.MISSING_REQUIRED, HighlightType.FORBIDDEN -> Coral
    }

    Dialog(onDismissRequest = onClose) {
        Surface(shape = RoundedCornerShape(18.dp), color = Veil.copy(alpha = 0.98f)) {
            Column(Modifier.width(820.dp).height(640.dp).padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("规范答题训练", style = MaterialTheme.typography.titleMedium, color = CinematicGold)
                    Spacer(Modifier.width(10.dp))
                    Text("知道哪些步骤必须写 / 可省 / 不能写", style = MaterialTheme.typography.labelSmall, color = TextMid)
                    Spacer(Modifier.weight(1f))
                    FilledTonalButton(onClick = onClose, shape = RoundedCornerShape(12.dp)) { Text("关闭") }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    subjects.forEach { s ->
                        Chip(s, selected = s == subject) { subject = s; standard = null; result = null }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    standards.forEach { std ->
                        Chip(std.id.ifBlank { std.questionType.name }, selected = std.id == standard?.id) { standard = std; result = null }
                    }
                }
                Spacer(Modifier.height(8.dp))
                standard?.let { std ->
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Glimmer.copy(alpha = 0.28f)).padding(10.dp)) {
                        Text("必写步骤：" + std.requiredSteps.joinToString(" → ") { it.content }, style = MaterialTheme.typography.bodySmall, color = TextHigh)
                        if (std.forbiddenSteps.isNotEmpty()) Text("禁止：" + std.forbiddenSteps.joinToString("；"), style = MaterialTheme.typography.bodySmall, color = Coral)
                    }
                    Spacer(Modifier.height(8.dp))
                }
                OutlinedTextField(value = question, onValueChange = { question = it }, label = { Text("题目（可选）") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = answer, onValueChange = { answer = it }, label = { Text("你的作答") }, modifier = Modifier.fillMaxWidth().height(150.dp))
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(onClick = {
                        val std = standard ?: return@FilledTonalButton
                        scope.launch {
                            runCatching {
                                val engine = StandardPracticeEngine(AnswerStandardTeacher(null))
                                result = engine.evaluate(question, std, answer)
                            }.onFailure { result = PracticeResult(question, answer, "", emptyList(), listOf("批改失败：${it.message}")) }
                        }
                    }, shape = RoundedCornerShape(12.dp)) { Text("批改") }
                    result?.let { r ->
                        Text("得分：${standard?.scoringPoints?.sumOf { it.score }?.let { "—" } ?: "—"} · 建议 ${r.suggestions.size} 条", style = MaterialTheme.typography.labelSmall, color = TextMid)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Veil.copy(alpha = 0.5f)).padding(10.dp).verticalScroll(rememberScrollState())) {
                    val r = result
                    if (r == null) {
                        Text("选好学科/题型后填写作答，点「批改」查看红黄绿高亮与改进建议。", color = TextMid)
                    } else {
                        r.highlights.forEach { h ->
                            Text("● ${h.type.name}：" + h.text.take(80), style = MaterialTheme.typography.bodySmall, color = hlColor(h.type))
                        }
                        if (r.highlights.isEmpty()) Text("未发现明显规范问题（或作答过短）。", color = TextMid)
                        Spacer(Modifier.height(8.dp))
                        r.suggestions.forEach { s -> Text("· $s", style = MaterialTheme.typography.bodySmall, color = TextHigh) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = if (selected) CinematicGold else TextMid,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Aureate.copy(alpha = 0.18f) else Glimmer.copy(alpha = 0.30f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}
