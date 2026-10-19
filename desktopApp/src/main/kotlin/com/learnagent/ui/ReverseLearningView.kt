package com.learnagent.ui

import androidx.compose.foundation.background
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
import com.learnagent.core.KnowledgeNode
import com.learnagent.core.ReverseLearningEngine
import com.learnagent.platform.TechStore
import kotlinx.coroutines.launch

/**
 * 逆向学习（Petersson）：输入目标 → 递归拆解前置知识 → 学习路径（先学最深的未掌握叶子）。
 */
@Composable
fun ReverseLearningView(onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    var goal by remember { mutableStateOf("") }
    var path by remember { mutableStateOf("") }
    var root by remember { mutableStateOf<KnowledgeNode?>(null) }
    var busy by remember { mutableStateOf(false) }

    fun run() {
        if (goal.isBlank() || busy) return
        busy = true
        scope.launch {
            runCatching {
                val engine = ReverseLearningEngine(null, TechStore())
                val n = engine.analyze(goal.trim())
                root = n
                path = engine.learningPathText(n)
            }.onFailure { path = "失败：${it.message}" }
            busy = false
        }
    }

    fun render(n: KnowledgeNode, depth: Int, sb: StringBuilder) {
        repeat(depth) { sb.append("    ") }
        sb.append(if (depth == 0) "" else "└─ ").append(n.name)
        sb.append(if (n.mastered) "（已掌握）" else "（未掌握）").append('\n')
        n.children.forEach { render(it, depth + 1, sb) }
    }

    Dialog(onDismissRequest = onClose) {
        Surface(shape = RoundedCornerShape(18.dp), color = Veil.copy(alpha = 0.98f)) {
            Column(Modifier.width(780.dp).height(600.dp).padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("逆向学习", style = MaterialTheme.typography.titleMedium, color = CinematicGold)
                    Spacer(Modifier.width(10.dp))
                    Text("从目标反推前置知识，先跑通再补理论", style = MaterialTheme.typography.labelSmall, color = TextMid)
                    Spacer(Modifier.weight(1f))
                    FilledTonalButton(onClick = onClose, shape = RoundedCornerShape(12.dp)) { Text("关闭") }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = goal, onValueChange = { goal = it },
                        placeholder = { Text("我想做：视频生成模型 / 分布式锁 / RAG…") },
                        singleLine = true, modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledTonalButton(onClick = { run() }, shape = RoundedCornerShape(12.dp)) { Text(if (busy) "拆解中…" else "生成学习路径") }
                }
                Spacer(Modifier.height(12.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Glimmer.copy(alpha = 0.28f)).padding(12.dp).verticalScroll(rememberScrollState())) {
                    if (root == null) {
                        Text("输入目标，生成「前置知识树 + 学习路径」。已掌握的节点不会再往下展开。", color = TextMid)
                    } else {
                        val sb = StringBuilder(); render(root!!, 0, sb)
                        Text(sb.toString(), style = MaterialTheme.typography.bodyMedium, color = TextHigh)
                        Spacer(Modifier.height(10.dp))
                        Text(path, style = MaterialTheme.typography.bodyMedium, color = Aureate)
                    }
                }
            }
        }
    }
}
