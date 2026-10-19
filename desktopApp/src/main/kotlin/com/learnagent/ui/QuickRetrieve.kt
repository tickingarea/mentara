package com.learnagent.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.learnagent.core.ProblemParser
import com.learnagent.data.Decision
import com.learnagent.data.TechCard
import com.learnagent.platform.TechStore

private data class QuickHit(
    val kind: String,
    val title: String,
    val sub: String,
    val score: Int,
    val tech: TechCard? = null,
    val decision: Decision? = null
)

private fun quickQuery(store: TechStore, q: String): List<QuickHit> {
    val hits = ArrayList<QuickHit>()
    if (q.isBlank()) {
        store.getAllDecisions().sortedByDescending { it.createdAt }.take(6).forEach {
            hits.add(QuickHit("决策", it.problem, it.chosen + "｜" + it.reason, 0, decision = it))
        }
        store.getAllTechCards().take(6).forEach {
            hits.add(QuickHit("技术卡", it.name, it.whatItIs, 0, tech = it))
        }
        return hits
    }
    val keys = ProblemParser(null).parseWithDictionary(q).keywords
    fun scoreOf(text: String) = keys.count { text.contains(it, ignoreCase = true) }
    store.getAllDecisions().forEach { d ->
        val s = scoreOf(d.problem) * 3 + scoreOf(d.chosen) * 2 + scoreOf(d.reason)
        if (s > 0) hits.add(QuickHit("决策", d.problem, "选择：" + d.chosen + "｜" + d.reason.take(60), s + 5, decision = d))
    }
    store.getAllTechCards().forEach { c ->
        val s = scoreOf(c.name) * 3 + scoreOf(c.whatItIs) + scoreOf(c.whatItSolves) + scoreOf(c.relatedConcepts.joinToString(" "))
        if (s > 0) hits.add(QuickHit("技术卡", c.name, c.whatItIs + "｜" + c.whatItSolves.take(50), s, tech = c))
    }
    store.getAllCodeSnippets().forEach { sn ->
        val s = scoreOf(sn.purpose) + scoreOf(sn.tags.joinToString(" ")) + scoreOf(sn.code)
        if (s > 0) hits.add(QuickHit("代码", sn.purpose.ifBlank { sn.language }, sn.code.take(60), s - 2))
    }
    return hits.sortedByDescending { it.score }.take(30)
}

/**
 * 知识调用器（理念：能调出来就是你的知识）：
 * 一个输入框 → 先经验层（你的决策）后理论层（技术卡/代码），命中即弹"3 秒可读完"的紧凑卡。
 */
@Composable
fun QuickRetrieveDialog(onClose: () -> Unit) {
    val store = remember { TechStore() }
    var q by remember { mutableStateOf("") }
    val hits = remember(q) { runCatching { quickQuery(store, q) }.getOrDefault(emptyList()) }
    var tech by remember { mutableStateOf<TechCard?>(null) }
    var decision by remember { mutableStateOf<Decision?>(null) }

    Dialog(onDismissRequest = onClose) {
        Surface(shape = RoundedCornerShape(18.dp), color = Veil.copy(alpha = 0.98f)) {
            Column(Modifier.width(720.dp).height(560.dp).padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("速查台", style = MaterialTheme.typography.titleMedium, color = CinematicGold)
                    Spacer(Modifier.width(10.dp))
                    Text("先经验层（你的决策）→ 后理论层（技术卡/代码）", style = MaterialTheme.typography.labelSmall, color = TextMid)
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = q, onValueChange = { q = it },
                    placeholder = { Text("输入问题或技术名，如：高并发下的分布式锁") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                if (hits.isEmpty()) {
                    Text(if (q.isBlank()) "输入关键词开始检索" else "没有命中；换个说法或先积累技术卡/决策", color = TextMid)
                } else {
                    LazyColumn(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
                        items(hits) { h ->
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                    .background(Glimmer.copy(alpha = 0.30f))
                                    .clickable {
                                        if (h.tech != null) tech = h.tech
                                        else if (h.decision != null) decision = h.decision
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(h.kind, style = MaterialTheme.typography.labelSmall, color = Aureate, modifier = Modifier.width(52.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(h.title, style = MaterialTheme.typography.bodyMedium, color = TextHigh, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (h.sub.isNotBlank()) Text(h.sub, style = MaterialTheme.typography.bodySmall, color = TextMid, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    tech?.let { TechCardCompactDialog(it, store) { tech = null } }
    decision?.let { DecisionDetailDialog(it) { decision = null } }
}
