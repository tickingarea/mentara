package com.learnagent.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.learnagent.config.AppConfig
import com.learnagent.config.ConfigStore
import com.learnagent.core.ContextAnalyzer
import com.learnagent.core.FeynmanCheck
import com.learnagent.core.InterleavingEngine
import com.learnagent.core.KnowledgePointExtractor
import com.learnagent.core.SpacedRepetitionEngine
import com.learnagent.data.KnowledgeMastery
import com.learnagent.data.KnowledgePoint
import com.learnagent.platform.TechStore

/**
 * 学业复习台（批次 #2）：SM-2 待复习队列 + 主动回忆 + 费曼检查 + 交错学习 + 错题式自评。
 * 知识点来自当前视频（KnowledgePointExtractor），掌握度写入 TechStore（与学业模式同一套）。
 */
@Composable
fun StudyReviewView(config: AppConfig, videoId: String?, onClose: () -> Unit) {
    val store = remember { TechStore() }
    val engine = remember { SpacedRepetitionEngine() }
    val feynman = remember { FeynmanCheck(null) }

    var points by remember { mutableStateOf<List<KnowledgePoint>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(videoId) {
        loading = true
        val vid = videoId
        if (vid != null) {
            runCatching {
                val timeline = ConfigStore.loadTimeline(config, vid)
                val units = ContextAnalyzer(null).analyze(timeline, vid)
                val extractor = KnowledgePointExtractor()
                points = units.flatMap { u -> extractor.extract(u, vid) }.distinctBy { it.id }
            }
        }
        loading = false
    }

    val mastery = remember { mutableStateMapOf<String, KnowledgeMastery>() }
    LaunchedEffect(Unit) { runCatching { store.getAllMastery().forEach { mastery[it.pointId] = it } } }

    // 交错学习：跨章节轮询打散
    var session by remember { mutableStateOf<List<KnowledgePoint>>(emptyList()) }
    LaunchedEffect(points) {
        if (points.isNotEmpty()) session = InterleavingEngine().interleave(points, minOf(points.size, 40))
    }
    var idx by remember { mutableStateOf(0) }
    var showAnswer by remember { mutableStateOf(false) }
    var feyn by remember { mutableStateOf("") }
    var feynResult by remember { mutableStateOf<String?>(null) }

    fun grade(q: Int) {
        val p = session.getOrNull(idx) ?: return
        val cur = mastery[p.id] ?: KnowledgeMastery(pointId = p.id)
        val up = engine.update(cur, q, System.currentTimeMillis())
        runCatching { store.saveMastery(up) }
        mastery[p.id] = up
        showAnswer = false; feyn = ""; feynResult = null; idx++
    }

    val current = session.getOrNull(idx)
    val dueCount = remember(points, mastery.size) {
        runCatching { engine.getDueCards(store.getAllMastery(), System.currentTimeMillis()).size }.getOrDefault(0)
    }

    Dialog(onDismissRequest = onClose) {
        Surface(shape = RoundedCornerShape(18.dp), color = Veil.copy(alpha = 0.98f)) {
            Column(Modifier.width(760.dp).height(600.dp).padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("学业复习台", style = MaterialTheme.typography.titleMedium, color = CinematicGold)
                    Spacer(Modifier.width(10.dp))
                    Text("SM-2 · 主动回忆 · 费曼 · 交错学习", style = MaterialTheme.typography.labelSmall, color = TextMid)
                    Spacer(Modifier.weight(1f))
                    Text("待复习 $dueCount", style = MaterialTheme.typography.labelMedium, color = Aureate)
                    Spacer(Modifier.width(10.dp))
                    FilledTonalButton(onClick = onClose, shape = RoundedCornerShape(12.dp)) { Text("关闭") }
                }
                Spacer(Modifier.height(12.dp))
                when {
                    loading -> Text("正在从当前视频生成知识点…", color = TextMid)
                    session.isEmpty() -> Text("没有知识点：先选中视频并执行『抽帧 OCR / 深度整理』，或在设置页重建技术卡。", color = TextMid)
                    current == null -> {
                        Text("本轮完成 🎉 共 ${session.size} 张", color = SuccessGreen)
                        Spacer(Modifier.height(10.dp))
                        FilledTonalButton(onClick = { idx = 0; showAnswer = false; session = session.shuffled() }) { Text("再练一轮") }
                    }
                    else -> {
                        Text("第 ${idx + 1} / ${session.size} 张", style = MaterialTheme.typography.labelSmall, color = TextMid)
                        Spacer(Modifier.height(8.dp))
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Glimmer.copy(alpha = 0.30f)).padding(14.dp)) {
                            Text(current.name, style = MaterialTheme.typography.titleMedium, color = TextHigh)
                            Spacer(Modifier.height(6.dp))
                            if (!showAnswer) {
                                Text("主动回忆：先自己说出它的定义与用途，再对照答案（别偷看）。", style = MaterialTheme.typography.bodySmall, color = MoonSilver)
                            } else {
                                Text(current.definition, style = MaterialTheme.typography.bodyMedium, color = TextHigh)
                                if (current.relatedIds.isNotEmpty()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text("关联：" + current.relatedIds.joinToString("、"), style = MaterialTheme.typography.bodySmall, color = TextMid, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                                Spacer(Modifier.height(10.dp))
                                Text("费曼自检：用最简单的话解释它（给 10 岁小孩讲）。", style = MaterialTheme.typography.bodySmall, color = MoonSilver)
                                Spacer(Modifier.height(6.dp))
                                OutlinedTextField(value = feyn, onValueChange = { feyn = it }, modifier = Modifier.fillMaxWidth().height(110.dp),
                                    placeholder = { Text("写下你的解释…") })
                                Spacer(Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    FilledTonalButton(onClick = {
                                        runCatching {
                                            // 本地启发式费曼检查（离线可用）
                                            val r = kotlinx.coroutines.runBlocking { feynman.check(current, feyn) }
                                            val auto = if (r.passed) 4 else 2
                                            grade(auto)
                                        }
                                    }) { Text("费曼检查并评分") }
                                    feynResult?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = TextMid) }
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (!showAnswer) {
                                FilledTonalButton(onClick = { showAnswer = true }) { Text("显示答案") }
                                FilledTonalButton(onClick = { grade(1) }) { Text("不熟（q=1）") }
                            } else {
                                FilledTonalButton(onClick = { grade(1) }) { Text("不熟") }
                                FilledTonalButton(onClick = { grade(3) }) { Text("模糊") }
                                FilledTonalButton(onClick = { grade(5) }) { Text("掌握") }
                            }
                        }
                    }
                }
            }
        }
    }
}
