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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.learnagent.config.AppConfig
import com.learnagent.config.ConfigStore
import com.learnagent.core.ContextAnalyzer
import com.learnagent.core.Difficulty
import com.learnagent.core.Exam
import com.learnagent.core.ExamGenerator
import com.learnagent.core.ExamGrader
import com.learnagent.core.ExamResult
import com.learnagent.core.ExamScope
import com.learnagent.core.InterleavingEngine
import com.learnagent.core.KnowledgePointExtractor
import com.learnagent.data.KnowledgePoint
import com.learnagent.data.QuestionType
import kotlinx.coroutines.launch

/**
 * 学业出卷 / 批改（批次 #2 延伸）：
 * 从当前视频知识点离线出卷（题型配比 40/15/15/20/10）→ 作答 → 批改（客观精确匹配 + 主观本地启发式）。
 */
@Composable
fun ExamView(config: AppConfig, videoId: String?, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    var points by remember { mutableStateOf<List<KnowledgePoint>>(emptyList()) }
    var exam by remember { mutableStateOf<Exam?>(null) }
    var result by remember { mutableStateOf<ExamResult?>(null) }
    var busy by remember { mutableStateOf(false) }
    val answers = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(videoId) {
        val vid = videoId ?: return@LaunchedEffect
        runCatching {
            val timeline = ConfigStore.loadTimeline(config, vid)
            val units = ContextAnalyzer(null).analyze(timeline, vid)
            val ex = KnowledgePointExtractor()
            points = units.flatMap { u -> ex.extract(u, vid) }.distinctBy { it.id }
        }
    }

    fun makeExam() {
        if (busy) return
        busy = true
        scope.launch {
            runCatching {
                answers.clear(); result = null
                exam = ExamGenerator(null, points).generate(
                    scope = ExamScope.SINGLE_VIDEO,
                    videoIds = videoId?.let { listOf(it) } ?: emptyList(),
                    questionCount = 10,
                    difficulty = Difficulty.MEDIUM,
                    interleaver = InterleavingEngine()
                )
            }.onFailure { result = ExamResult(0, 0, emptyList()) }
            busy = false
        }
    }

    fun submit() {
        val e = exam ?: return
        busy = true
        scope.launch {
            runCatching { result = ExamGrader(null).grade(e, answers.toMap()) }
            busy = false
        }
    }

    Dialog(onDismissRequest = onClose) {
        Surface(shape = RoundedCornerShape(18.dp), color = Veil.copy(alpha = 0.98f)) {
            Column(Modifier.width(840.dp).height(660.dp).padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("学业出卷 / 批改", style = MaterialTheme.typography.titleMedium, color = CinematicGold)
                    Spacer(Modifier.width(10.dp))
                    Text("知识点 ${points.size} · 离线出卷 10 题", style = MaterialTheme.typography.labelSmall, color = TextMid)
                    Spacer(Modifier.weight(1f))
                    FilledTonalButton(onClick = { makeExam() }, shape = RoundedCornerShape(12.dp)) { Text(if (busy) "…" else "出卷") }
                    Spacer(Modifier.width(8.dp))
                    if (exam != null && result == null) {
                        FilledTonalButton(onClick = { submit() }, shape = RoundedCornerShape(12.dp)) { Text("交卷批改") }
                        Spacer(Modifier.width(8.dp))
                    }
                    FilledTonalButton(onClick = onClose, shape = RoundedCornerShape(12.dp)) { Text("关闭") }
                }
                Spacer(Modifier.height(10.dp))
                when {
                    points.isEmpty() -> Text("没有知识点：先选中视频并执行『抽帧 OCR / 深度整理』。", color = TextMid)
                    exam == null -> Text("点右上「出卷」按知识点生成试卷（单选/多选/填空/简答/计算配比 8:3:3:4:2）。", color = TextMid)
                    else -> {
                        result?.let { r ->
                            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Glimmer.copy(alpha = 0.30f)).padding(10.dp)) {
                                Text("总分：${r.totalScore} / ${r.maxScore}", style = MaterialTheme.typography.titleMedium, color = Aureate)
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(exam!!.questions) { q ->
                                val grade = result?.grades?.firstOrNull { it.questionId == q.id }
                                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Veil.copy(alpha = 0.5f)).padding(10.dp)) {
                                    Text("【${q.type.name}】${q.stem}", style = MaterialTheme.typography.bodyMedium, color = TextHigh)
                                    Spacer(Modifier.height(4.dp))
                                    if (q.options.isNotEmpty()) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            q.options.forEachIndexed { i, opt ->
                                                val label = ('A' + i).toString()
                                                val sel = answers[q.id]?.contains(label) == true
                                                Text(
                                                    "$label. ${opt.take(24)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (sel) CinematicGold else TextMid,
                                                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                                        .background(if (sel) Aureate.copy(alpha = 0.18f) else Glimmer.copy(alpha = 0.28f))
                                                        .clickable { answers[q.id] = label }
                                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                                )
                                            }
                                        }
                                    } else {
                                        OutlinedTextField(
                                            value = answers[q.id].orEmpty(),
                                            onValueChange = { answers[q.id] = it },
                                            modifier = Modifier.fillMaxWidth().height(84.dp),
                                            placeholder = { Text("作答…") }
                                        )
                                    }
                                    grade?.let {
                                        Spacer(Modifier.height(4.dp))
                                        Text("得分 ${it.score}/${it.maxScore} · ${it.feedback}", style = MaterialTheme.typography.bodySmall,
                                            color = if (it.score >= it.maxScore) SuccessGreen else Coral)
                                    }
                                    if (result != null) {
                                        Text("参考答案：" + q.correctAnswer.take(120), style = MaterialTheme.typography.bodySmall, color = MoonSilver)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
