package com.learnagent.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.learnagent.config.AppConfig
import com.learnagent.config.ConfigStore
import com.learnagent.core.ContextAnalyzer
import com.learnagent.core.KnowledgePointExtractor
import com.learnagent.core.SpacedRepetitionEngine
import com.learnagent.data.KnowledgeMastery
import com.learnagent.data.KnowledgePoint
import com.learnagent.data.MasteryStatus
import com.learnagent.data.TechCard
import com.learnagent.data.TechStatus
import com.learnagent.platform.TechStore

private fun masteryColor(s: MasteryStatus): Color = when (s) {
    MasteryStatus.NOT_LEARNED -> Color(0xFF4A5260)
    MasteryStatus.LEARNED -> Color(0xFF4A7AB8)
    MasteryStatus.FAMILIAR -> Color(0xFF7FB89C)
    MasteryStatus.MASTERED -> Color(0xFFE8C87A)
    MasteryStatus.EXPERT -> Color(0xFFB8A8D4)
}

private fun techColor(s: TechStatus): Color = when (s) {
    TechStatus.DISCOVERED -> Color(0xFF4A5260)
    TechStatus.FAMILIAR -> Color(0xFF4A7AB8)
    TechStatus.USED -> Color(0xFF7FB89C)
    TechStatus.FLUENT -> Color(0xFFE8C87A)
    TechStatus.EXPERT -> Color(0xFFB8A8D4)
}

/**
 * 掌握度热力 / 技术地图联动（#1）：
 * - 上半：当前视频知识点的 SM-2 掌握热力（颜色=状态，排序=准确率升序 → 弱项在前）
 * - 下半：技术卡使用热力（颜色=TechStatus），点击直接弹 3 秒技术卡
 */
@Composable
fun MasteryHeatView(config: AppConfig, videoId: String?, onClose: () -> Unit) {
    val store = remember { TechStore() }
    var points by remember { mutableStateOf<List<KnowledgePoint>>(emptyList()) }
    var mastery by remember { mutableStateOf<Map<String, KnowledgeMastery>>(emptyMap()) }
    var techs by remember { mutableStateOf<List<TechCard>>(emptyList()) }
    var selected by remember { mutableStateOf<TechCard?>(null) }

    LaunchedEffect(videoId) {
        runCatching {
            val vid = videoId
            if (vid != null) {
                val timeline = ConfigStore.loadTimeline(config, vid)
                val units = ContextAnalyzer(null).analyze(timeline, vid)
                val ex = KnowledgePointExtractor()
                points = units.flatMap { u -> ex.extract(u, vid) }.distinctBy { it.id }
            }
            mastery = runCatching { store.getAllMastery().associateBy { it.pointId } }.getOrDefault(emptyMap())
            techs = runCatching { store.getAllTechCards() }.getOrDefault(emptyList())
        }
    }

    val due = remember(mastery) {
        runCatching { SpacedRepetitionEngine().getDueCards(mastery.values.toList(), System.currentTimeMillis()).size }.getOrDefault(0)
    }
    val sortedPoints = points.sortedWith(compareBy({ mastery[it.id]?.recallAccuracy ?: 0f }, { mastery[it.id]?.status?.ordinal ?: 0 }))
    val usageByName = remember(techs) { runCatching { store.getAllTechUsage().associateBy { it.techName } }.getOrDefault(emptyMap()) }

    Dialog(onDismissRequest = onClose) {
        Surface(shape = RoundedCornerShape(18.dp), color = Veil.copy(alpha = 0.98f)) {
            Column(Modifier.width(860.dp).height(660.dp).padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("掌握度热力 / 技术地图", style = MaterialTheme.typography.titleMedium, color = CinematicGold)
                    Spacer(Modifier.width(10.dp))
                    Text("知识点 SM-2 · 技术卡使用痕迹", style = MaterialTheme.typography.labelSmall, color = TextMid)
                    Spacer(Modifier.weight(1f))
                    Text("待复习 $due", style = MaterialTheme.typography.labelMedium, color = Aureate)
                    Spacer(Modifier.width(10.dp))
                    FilledTonalButton(onClick = onClose, shape = RoundedCornerShape(12.dp)) { Text("关闭") }
                }
                Spacer(Modifier.height(10.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Text("知识点掌握热力（弱项在前）", style = MaterialTheme.typography.labelLarge, color = TextHigh)
                    }
                    if (sortedPoints.isEmpty()) {
                        item { Text("暂无知识点：先选中视频并执行抽帧/深度整理。", color = TextMid) }
                    } else {
                        items(sortedPoints) { p ->
                            val m = mastery[p.id]
                            val status = m?.status ?: MasteryStatus.NOT_LEARNED
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Glimmer.copy(alpha = 0.25f)).padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(14.dp).clip(RoundedCornerShape(4.dp)).background(masteryColor(status)))
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(p.name, style = MaterialTheme.typography.bodyMedium, color = TextHigh)
                                    Text("${status.name} · 准确率 ${((m?.recallAccuracy ?: 0f) * 100).toInt()}% · 复习 ${m?.reviewCount ?: 0} 次",
                                        style = MaterialTheme.typography.labelSmall, color = TextMid)
                                }
                            }
                        }
                    }
                    item {
                        Spacer(Modifier.height(6.dp))
                        Text("技术卡使用热力（点击弹卡）", style = MaterialTheme.typography.labelLarge, color = TextHigh)
                    }
                    if (techs.isEmpty()) {
                        item { Text("暂无技术卡：设置页『Rebuild Tech Cards』生成。", color = TextMid) }
                    } else {
                        items(techs) { c ->
                            val u = usageByName[c.name]
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Glimmer.copy(alpha = 0.25f))
                                    .clickable { selected = c }.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(14.dp).clip(RoundedCornerShape(4.dp)).background(techColor(u?.status ?: TechStatus.DISCOVERED)))
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(c.name, style = MaterialTheme.typography.bodyMedium, color = TextHigh)
                                    Text("${(u?.status ?: TechStatus.DISCOVERED).name} · 遇见 ${u?.seenCount ?: 0} · 使用 ${u?.usedCount ?: 0}",
                                        style = MaterialTheme.typography.labelSmall, color = TextMid)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    selected?.let { TechCardCompactDialog(it, store) { selected = null } }
}
