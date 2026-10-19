package com.learnagent.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.learnagent.core.DebateEngine
import com.learnagent.core.DebateModels
import com.learnagent.core.DebateResult
import com.learnagent.core.Indexer
import com.learnagent.core.LocalDebater
import com.learnagent.core.LocalQaEngine
import com.learnagent.core.LlmClient
import com.learnagent.data.TechCard
import com.learnagent.platform.TechStore
import com.learnagent.core.QaAnswer
import com.learnagent.core.RemoteQaEngine
import com.learnagent.llm.AndroidNimClient
import com.learnagent.data.TimelineItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

import com.learnagent.ui.theme.Aureate
import com.learnagent.ui.theme.Coral
import com.learnagent.ui.theme.Mist
import com.learnagent.ui.theme.MoonSilver
import com.learnagent.ui.theme.Phantom
import com.learnagent.ui.theme.SuccessGreen
import com.learnagent.ui.theme.TextHigh
import com.learnagent.ui.theme.TextMid
import com.learnagent.ui.theme.TextLow
import com.learnagent.ui.theme.Veil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Serializable
private data class ChatEntry(val q: String, val answer: String, val citations: List<String> = emptyList())

private val chatJson = Json { ignoreUnknownKeys = true; isLenient = true }


/** mm:ss。 */
internal fun formatTime(seconds: Double): String {
    val t = seconds.toInt()
    fun two(v: Int) = if (v < 10) "0$v" else "$v"
    return two(t / 60) + ":" + two(t % 60)
}

/** 时间戳 chip。 */
@Composable
internal fun TimeChip(text: String) {
    Surface(color = Phantom.copy(alpha = 0.16f), shape = RoundedCornerShape(6.dp)) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Phantom
        )
    }
}

// ============================================================
//  问答面板（气泡 ±1° 微歪斜 + BreathingOrb 等待 + 完成炸开）
// ============================================================

@Composable
fun ChatPanel(
    videoId: String?,
    timeline: List<TimelineItem>,
    modifier: Modifier = Modifier,
    apiKey: String = "",
    model: String = "z-ai/glm-5.3-flash"
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("mentara", android.content.Context.MODE_PRIVATE) }
    var answerMode by remember { mutableStateOf(prefs.getString("answerMode", "expert") ?: "expert") }
    val tavilyKey = prefs.getString("tavilyApiKey", "") ?: ""
    val bochaKey = prefs.getString("bochaApiKey", "") ?: ""
    val anysKey = prefs.getString("anysearchApiKey", "") ?: ""
    val skillText = remember(videoId) {
        runCatching {
            val rootDir = java.io.File(context.filesDir, com.learnagent.data.ResultsDir.RESULTS_ROOT)
            val sb = StringBuilder()
            videoId?.let { vid ->
                val dir = java.io.File(rootDir, vid)
                java.io.File(dir, "skill.md").takeIf { it.isFile }?.let { sb.append("【当前视频：").append(vid).append("】\n").append(it.readText()).append("\n\n") }
                java.io.File(dir, "notes.md").takeIf { it.isFile }?.let { sb.append("【当前视频·学习笔记】\n").append(it.readText().take(4000)).append("\n\n") }
            }
            rootDir.listFiles().orEmpty().filter { it.isDirectory }.forEach { dir ->
                if (dir.name == videoId) return@forEach
                java.io.File(dir, "skill.md").takeIf { it.isFile }?.let {
                    sb.append("【已学视频：").append(dir.name).append("（摘要）】\n").append(it.readText().take(3000)).append("\n\n")
                }
            }
            sb.toString().trim().take(16000)
        }.getOrDefault("")
    }
    val engine = remember(timeline, apiKey, model, tavilyKey, bochaKey, anysKey) {
        val indexer = Indexer()
        indexer.index(timeline)
        val search: suspend (String, Int) -> List<TimelineItem> = { q, k -> indexer.search(q, k) }
        if (apiKey.isBlank()) LocalQaEngine(allItems = { timeline }, search = search)
        else RemoteQaEngine(
            search, AndroidNimClient(apiKey, model),
            fallbackAll = { timeline },
            mode = { if (answerMode == "expert") com.learnagent.core.AnswerMode.EXPERT else com.learnagent.core.AnswerMode.STRICT },
            webSearch = when {
                anysKey.isNotBlank() -> com.learnagent.llm.AndroidAnySearcher(anysKey)
                bochaKey.isNotBlank() -> com.learnagent.llm.AndroidBochaSearcher(bochaKey)
                tavilyKey.isNotBlank() -> com.learnagent.llm.AndroidTavilySearcher(tavilyKey)
                else -> com.learnagent.llm.AndroidAnySearcher("") // 匿名免费（低限流），失败自动回落必应
            },
            extraContext = { skillText }
        )
    }
    val scope = rememberCoroutineScope()
    var question by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var streamText by remember { mutableStateOf("") }
    var reasoningText by remember { mutableStateOf("") }
    var showReasoning by remember { mutableStateOf(false) }
    val chatFile = remember(videoId) {
        videoId?.let { vid ->
            java.io.File(java.io.File(java.io.File(context.filesDir, com.learnagent.data.ResultsDir.RESULTS_ROOT), vid), "chat.json")
        }
    }
    val messages = remember(videoId) { mutableStateListOf<Pair<String, QaAnswer>>() }
    LaunchedEffect(videoId) {
        messages.clear()
        runCatching {
            chatFile?.takeIf { it.isFile }?.let { f ->
                chatJson.decodeFromString(ListSerializer(ChatEntry.serializer()), f.readText()).forEach {
                    messages.add(it.q to QaAnswer(it.answer, it.citations))
                }
            }
        }
    }
    fun persistChat() {
        runCatching {
            chatFile?.let { f ->
                f.parentFile?.mkdirs()
                f.writeText(chatJson.encodeToString(ListSerializer(ChatEntry.serializer()), messages.map { (q, a) -> ChatEntry(q, a.answer, a.citations) }))
            }
        }
    }

    // ---- Developer Mode：回答 chips + 记录决策 ----
    val devStore = remember { TechStore() }
    val knownTechCards = remember(devStore) { devStore.getAllTechCards() }
    var selectedTech by remember { mutableStateOf<TechCard?>(null) }
    var decisionOpen by remember { mutableStateOf(false) }
    var decisionPrefill by remember { mutableStateOf("") }

    var lastBusy by remember { mutableStateOf(false) }
    var burstTick by remember { mutableStateOf(0) }
    LaunchedEffect(busy) {
        if (!busy && lastBusy) burstTick++
        lastBusy = busy
    }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(t("qa_title"), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            listOf("严谨" to "strict", "专家" to "expert").forEach { (label, v) ->
                Surface(
                    color = if (answerMode == v) Accent.copy(alpha = 0.18f) else Mist.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { AudioFx.tap(); answerMode = v; prefs.edit().putString("answerMode", v).apply() }
                ) {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = if (answerMode == v) Accent else TextMid, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
                Spacer(Modifier.width(6.dp))
            }
            if (messages.isNotEmpty()) {
                Surface(
                    color = Mist.copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { AudioFx.tap(); messages.clear(); runCatching { chatFile?.delete() } }
                ) { Text("清空", style = MaterialTheme.typography.labelSmall, color = TextMid, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) }
                Spacer(Modifier.width(6.dp))
            }
            Text(
                if (videoId == null) t("qa_unselected") else if (apiKey.isBlank()) t("qa_offline") else model,
                style = MaterialTheme.typography.labelSmall,
                color = MoonSilver
            )
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (messages.isEmpty() && busy && streamText.isEmpty() && reasoningText.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        BreathingOrb(size = 84.dp)
                        Spacer(Modifier.height(18.dp))
                        Text(t("qa_thinking"), style = MaterialTheme.typography.bodyMedium, color = MoonSilver)
                    }
                }
            } else if (messages.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(t("qa_ask_hint"), style = MaterialTheme.typography.titleMedium, color = TextMid)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (timeline.isEmpty()) t("qa_empty_retrieval") else t("qa_ready"),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextLow
                        )
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(messages) { (q, a) ->
                        Column {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Surface(
                                    color = Phantom,
                                    shape = CardShapes.userBubble(),
                                    modifier = Modifier.graphicsLayer { rotationZ = 0.8f }
                                ) {
                                    Text(
                                        q,
                                        Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF0B0710)
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Surface(
                                color = Veil.copy(alpha = 0.72f),
                                shape = CardShapes.aiBubble(),
                                modifier = Modifier.graphicsLayer { rotationZ = -0.6f }
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(a.answer, style = MaterialTheme.typography.bodyMedium, color = TextHigh)
                                    if (a.citations.isNotEmpty()) {
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            a.citations.joinToString(" · "),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MoonSilver
                                        )
                                    }
                                }
                            }
                            val chips = knownTechCards.map { it.name }
                                .filter { a.answer.contains(it, ignoreCase = true) }
                                .take(6)
                            if (chips.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                TechChipsRowAndroid(chips) { name ->
                                    selectedTech = knownTechCards.firstOrNull { it.name == name }
                                }
                            }
                        }
                    }
                    if (busy && (streamText.isNotEmpty() || reasoningText.isNotEmpty())) {
                        item {
                            Surface(color = Veil.copy(alpha = 0.72f), shape = CardShapes.aiBubble()) {
                                Column(Modifier.padding(12.dp)) {
                                    if (reasoningText.isNotBlank()) {
                                        Text(
                                            if (showReasoning) "▾ 思考过程" else "▸ 思考过程（${reasoningText.length} 字）",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MoonSilver,
                                            modifier = Modifier.clickable { showReasoning = !showReasoning }
                                        )
                                        if (showReasoning) {
                                            Spacer(Modifier.height(4.dp))
                                            Text(reasoningText, style = MaterialTheme.typography.bodySmall, color = TextMid, maxLines = 14, overflow = TextOverflow.Ellipsis)
                                            Spacer(Modifier.height(8.dp))
                                        }
                                    }
                                    Text(if (streamText.isBlank()) "…" else streamText, style = MaterialTheme.typography.bodyMedium, color = TextHigh)
                                }
                            }
                        }
                    }
                }
            }
            if (burstTick > 0) {
                BurstParticles(burstTick, Modifier.fillMaxSize())
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = question,
                onValueChange = { question = it },
                placeholder = { Text(t("qa_placeholder"), color = MoonSilver.copy(alpha = 0.6f)) },
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Mist.copy(alpha = 0.35f),
                    unfocusedContainerColor = Mist.copy(alpha = 0.22f),
                    focusedIndicatorColor = Phantom,
                    unfocusedIndicatorColor = Mist,
                    cursorColor = Phantom
                ),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(10.dp))
            val interaction = remember { MutableInteractionSource() }
            Surface(
                color = if (question.isNotBlank() && !busy) Phantom.copy(alpha = 0.2f) else Mist.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .pressScale(interaction)
                    .clickable(interaction, indication = null) {
                        val q = question
                        if (q.isBlank() || busy) return@clickable
                        AudioFx.tap(); Haptics.tap(context)
                        question = ""
                        busy = true
                        scope.launch {
                            streamText = ""; reasoningText = ""; showReasoning = false
                            val ans = if (apiKey.isNotBlank() && !isOnline(context))
                                QaAnswer("当前无网络连接：在线问答需要联网，请连网后重试。", emptyList())
                            else if (apiKey.isNotBlank() && !com.learnagent.ui.nimReachable(apiKey))
                                QaAnswer("连不上 NVIDIA NIM（模型服务）：设备有网但到 NIM 不通（代理/防火墙/运营商拦截）。请换网络后重试。", emptyList())
                            else runCatching {
                                if (engine is RemoteQaEngine)
                                    engine.askStream(q, videoId, onToken = { streamText += it }, onReasoning = { reasoningText += it })
                                else engine.ask(q)
                            }.getOrElse { QaAnswer("错误：${it.message ?: it.javaClass.simpleName}", emptyList()) }
                            messages.add(q to ans)
                            persistChat()
                            com.learnagent.voice.MentaraVoice.speak(ans.answer)
                            streamText = ""; reasoningText = ""
                            busy = false
                        }
                    }
            ) {
                Text(
                    if (busy) "..." else t("btn_send"),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (question.isNotBlank() && !busy) TextHigh else TextMid,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
            if (messages.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = Mist.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.clickable {
                        AudioFx.tap()
                        decisionPrefill = messages.last().first
                        decisionOpen = true
                    }
                ) {
                    Text(
                        "[ 记录决策 ]",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextHigh,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
    selectedTech?.let { TechCardCompactDialogAndroid(it, devStore) { selectedTech = null } }
    if (decisionOpen) {
        RecordDecisionDialogAndroid(devStore, decisionPrefill) { decisionOpen = false }
    }
}

// ============================================================
//  四轮辩论面板（自定义议题 + 流式轮次 + 一致性渐变条 + 折叠批判 + 取消）
// ============================================================

@Composable
fun DebatePanel(
    videoId: String?,
    timeline: List<TimelineItem>,
    apiKey: String,
    debateModelA: String,
    debateModelB: String,
    debateModelC: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val online = apiKey.isNotBlank()
    val onThisVideo = DebateHost.videoId == videoId
    val result = if (onThisVideo) DebateHost.result else null
    val busy = onThisVideo && DebateHost.busy
    val phase = if (busy) DebateHost.phase else 0
    var topic by remember(videoId) { mutableStateOf(t("debate_topic_default", videoId ?: "")) }
    var tick by remember { mutableStateOf(0L) }
    LaunchedEffect(busy) {
        while (busy) { tick = System.currentTimeMillis(); kotlinx.coroutines.delay(1000) }
    }
    val elapsedSec = if (busy && DebateHost.startedAt > 0L) ((tick - DebateHost.startedAt).coerceAtLeast(0L)) / 1000L else 0L

    fun cancel() { DebateHost.cancel() }

    fun start(t: String) {
        if (busy || timeline.isEmpty()) return
        AudioFx.tap(); Haptics.tap(context)
        DebateHost.start(videoId, t, timeline, apiKey, debateModelA, debateModelB, debateModelC, isOnline(context))
    }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DebateIcon(24)
            Spacer(Modifier.width(8.dp))
            Text(t("debate_title"), style = MaterialTheme.typography.titleMedium, color = TextHigh, modifier = Modifier.weight(1f))
            Text(
                if (online) "NIM" else t("debate_offline"),
                style = MaterialTheme.typography.labelSmall,
                color = MoonSilver
            )
            if (busy) {
                Spacer(Modifier.width(8.dp))
                Text(
                    when (phase) { 1 -> t("debate_short_1"); 2 -> t("debate_short_2"); else -> t("debate_short_3") } +
                        " · " + "已用 %d:%02d".format(elapsedSec / 60, elapsedSec % 60),
                    style = MaterialTheme.typography.labelSmall,
                    color = Phantom
                )
            }
        }
        if (busy) {
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(2.dp).background(Mist.copy(alpha = 0.5f), RoundedCornerShape(1.dp))) {
                Box(
                    Modifier
                        .fillMaxWidth((phase.coerceIn(0, 3) / 3f).coerceIn(0.05f, 1f))
                        .height(2.dp)
                        .background(Brush.horizontalGradient(listOf(Phantom, Aureate)), RoundedCornerShape(1.dp))
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        if (busy || result != null) {
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(1 to "① 提案", 2 to "② 交叉批判", 3 to "③ 中立裁决").forEach { (ph, label) ->
                    val done = !busy && result != null || phase > ph
                    val cur = busy && phase == ph
                    Text(
                        (if (done) "✓ " else if (cur) "● " else "○ ") + label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (done) SuccessGreen else if (cur) Aureate else MoonSilver
                    )
                }
            }
        }
        // 议题行
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = topic,
                onValueChange = { topic = it },
                singleLine = true,
                placeholder = { Text(t("debate_topic_ph"), color = MoonSilver.copy(alpha = 0.6f)) },
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Mist.copy(alpha = 0.35f),
                    unfocusedContainerColor = Mist.copy(alpha = 0.22f),
                    focusedIndicatorColor = Phantom,
                    unfocusedIndicatorColor = Mist,
                    cursorColor = Phantom
                ),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(10.dp))
            Surface(
                color = if (busy) Coral.copy(alpha = 0.16f) else Phantom.copy(alpha = 0.14f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.clickable {
                    if (busy) cancel() else if (timeline.isNotEmpty()) start(topic) else { AudioFx.error() }
                }
            ) {
                Text(
                    if (busy) t("debate_cancel") else t("debate_rerun"),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (busy) Coral else Phantom,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        val r = result
        if (r == null && !busy) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    RotatingLogo(size = 130.dp, alpha = 0.35f)
                    Spacer(Modifier.height(20.dp))
                    Text(
                        if (onThisVideo && DebateHost.error != null) DebateHost.error!! else t("debate_waiting_title"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (onThisVideo && DebateHost.error != null) Coral else MoonSilver
                    )
                }
            }
        } else if (r != null) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Text(t("debate_phase_1"), style = MaterialTheme.typography.titleSmall, color = TextHigh)
                }
                val proposals = listOf(r.proposerA, r.proposerB, r.proposerC)
                val names = if (online) listOf("Kimi K3", "DeepSeek V4.1", "GLM 5.3") else listOf("Kimi K3（本地）", "DeepSeek V4.1（本地）", "GLM 5.3（本地）")
                val avatars = listOf(Phantom, Aureate, Coral)
                items(proposals.indices.toList()) { i ->
                    Row(
                        Modifier
                            .floatIn(delayMillis = i * 60, distanceDp = 12.dp)
                            .background(Veil.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Canvas(Modifier.size(14.dp)) {
                            drawCircle(avatars[i].copy(alpha = 0.35f), radius = size.minDimension / 2f)
                            drawCircle(avatars[i], radius = size.minDimension / 3.4f, center = Offset(size.width / 2f, size.height / 2f))
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(names[i], style = MaterialTheme.typography.labelSmall, color = MoonSilver)
                            Spacer(Modifier.height(4.dp))
                            Text(proposals[i], style = MaterialTheme.typography.bodyMedium, color = TextHigh)
                        }
                    }
                }
                if (r.agreement >= 0.0) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(t("debate_agreement"), style = MaterialTheme.typography.labelLarge, color = TextHigh)
                            Spacer(Modifier.width(8.dp))
                            Box(Modifier.weight(1f).height(6.dp).background(Mist.copy(alpha = 0.4f), RoundedCornerShape(3.dp))) {
                                val frac = r.agreement.toFloat().coerceIn(0f, 1f)
                                val color = when {
                                    r.agreement > 0.66 -> SuccessGreen
                                    r.agreement >= 0.40 -> Aureate
                                    else -> Coral
                                }
                                Box(
                                    Modifier
                                        .fillMaxWidth(frac)
                                        .height(6.dp)
                                        .background(color, RoundedCornerShape(3.dp))
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "${(r.agreement * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = when {
                                    r.agreement > 0.66 -> SuccessGreen
                                    r.agreement >= 0.40 -> Aureate
                                    else -> Coral
                                }
                            )
                        }
                    }
                }
                if (r.critiques.isNotEmpty()) {
                    item {
                        var show by remember { mutableStateOf(false) }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { show = !show; AudioFx.tap() }
                        ) {
                            Text(t("debate_phase_2"), style = MaterialTheme.typography.titleSmall, color = TextHigh)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (show) t("debate_collapse") else t("debate_expand"),
                                style = MaterialTheme.typography.labelSmall,
                                color = Phantom
                            )
                        }
                        if (show) {
                            Spacer(Modifier.height(6.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                r.critiques.forEach { critique ->
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .background(Veil.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                            .padding(10.dp)
                                    ) {
                                        Text(critique, style = MaterialTheme.typography.bodySmall, color = TextMid)
                                    }
                                }
                            }
                        }
                    }
                }
                if (r.judge.isNotBlank()) {
                    item {
                        Text(t("debate_phase_3"), style = MaterialTheme.typography.titleSmall, color = TextHigh)
                        Spacer(Modifier.height(6.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .background(Phantom.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Text(r.judge, style = MaterialTheme.typography.bodyMedium, color = TextHigh)
                        }
                    }
                }
                if (r.minorityReport.isNotBlank()) {
                    item {
                        Text(t("debate_minority"), style = MaterialTheme.typography.labelLarge, color = Coral)
                        Spacer(Modifier.height(4.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .background(Coral.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Text(r.minorityReport, style = MaterialTheme.typography.bodySmall, color = TextMid)
                        }
                    }
                }
            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    BreathingOrb(size = 80.dp)
                    Spacer(Modifier.height(14.dp))
                    Text(t("debate_running"), style = MaterialTheme.typography.bodyMedium, color = MoonSilver)
                }
            }
        }
    }
}

// ============================================================
//  时间线卡片（20dp 光晕 + 星级 + 标签 + 展开原文 + 记录经验）
// ============================================================

@Composable
fun TimelineCardAndroid(item: TimelineItem, keyTag: String? = null, sideTag: String? = null) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var noted by remember { mutableStateOf(false) }
    val text = (item.speech + " " + item.screenText).trim()
    val importance = remember(item) { (3 + (text.length / 20)).coerceIn(3, 5) }
    val importanceColor = when (importance) {
        5 -> Coral
        4 -> Aureate
        else -> Phantom
    }
    GlowBox(
        glow = importanceColor,
        intensity = if (expanded) 0.9f else 0.32f,
        radius = 150.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            color = Veil.copy(alpha = 0.62f),
            shape = CardShapes.learningUnitCard(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TimeChip("${formatTime(item.start)} - ${formatTime(item.end)}")
                    StarRating(importance = importance, sizeDp = 18)
                    Text(
                        if (item.speech.isNotBlank()) t("chip_speech") else t("chip_screen"),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMid
                    )
                }
                if (keyTag != null) {
                    Spacer(Modifier.height(8.dp))
                    Surface(color = Aureate.copy(alpha = 0.18f), shape = RoundedCornerShape(8.dp)) {
                        Text(
                            "★ 必学 · $keyTag",
                            style = MaterialTheme.typography.labelSmall,
                            color = Aureate,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                if (sideTag != null) {
                    Spacer(Modifier.height(8.dp))
                    Surface(color = MoonSilver.copy(alpha = 0.16f), shape = RoundedCornerShape(8.dp)) {
                        Text(
                            "⤼ $sideTag",
                            style = MaterialTheme.typography.labelSmall,
                            color = MoonSilver,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    item.speech.ifBlank { item.screenText }.ifBlank { t("card_no_content") },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextHigh
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = Mist.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.clickable {
                            AudioFx.tap()
                            expanded = !expanded
                        }
                    ) {
                        Text(
                            if (expanded) "收起原文" else "展开原文",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextHigh,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                    Surface(
                        color = if (noted) Phantom.copy(alpha = 0.2f) else Mist.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.clickable {
                            noted = !noted
                            if (noted) { AudioFx.success(); Haptics.success(context) }
                        }
                    ) {
                        Text(if (noted) t("btn_noted") else t("btn_note"),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (noted) Phantom else TextHigh,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
                if (expanded) {
                    Spacer(Modifier.height(10.dp))
                    MistDivider()
                    Spacer(Modifier.height(10.dp))
                    if (item.speech.isNotBlank()) {
                        Text(t("card_speech", item.speech), style = MaterialTheme.typography.bodySmall, color = TextMid, maxLines = 6, overflow = TextOverflow.Ellipsis)
                    }
                    if (item.screenText.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(t("card_screen", item.screenText), style = MaterialTheme.typography.bodySmall, color = TextMid, maxLines = 6, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}