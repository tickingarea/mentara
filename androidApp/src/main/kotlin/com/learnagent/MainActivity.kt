package com.learnagent

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items as staggeredItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.learnagent.core.AndroidVideoActions
import com.learnagent.core.ContextAnalyzer
import com.learnagent.core.DebateModels
import com.learnagent.core.ImportProgress
import com.learnagent.core.ImportQueue
import com.learnagent.core.ImportState
import com.learnagent.core.TechCardExtractor
import com.learnagent.core.TechDecisionService
import com.learnagent.core.WeeklyReportGenerator
import com.learnagent.data.FrameOcr
import com.learnagent.data.ResultsDir
import com.learnagent.data.TimelineItem
import com.learnagent.data.TranscriptSegment
import com.learnagent.data.VideoMeta
import com.learnagent.design.DesignTokens
import com.learnagent.llm.AndroidNimClient
import com.learnagent.platform.TechStore
import com.learnagent.ui.Accent
import com.learnagent.ui.t
import com.learnagent.ui.AudioFx
import com.learnagent.ui.BreathingOrb
import com.learnagent.ui.ChatPanel
import com.learnagent.ui.DebateIcon
import com.learnagent.ui.DebatePanel
import com.learnagent.ui.DecisionsViewAndroid
import com.learnagent.ui.DevAlertChipAndroid
import com.learnagent.ui.DynamicBackground
import com.learnagent.ui.FollowUpDialogAndroid
import com.learnagent.ui.TechMapViewAndroid
import com.learnagent.ui.WeeklyReportDialogAndroid
import com.learnagent.ui.GlassImportButton
import com.learnagent.ui.LayoutBreakpoints
import com.learnagent.ui.LayoutMode
import com.learnagent.ui.GraphIcon
import com.learnagent.ui.AndroidStudyTimeline
import com.learnagent.ui.Haptics
import com.learnagent.ui.Language
import com.learnagent.ui.LearnAgentLogo
import com.learnagent.ui.MiniThumb
import com.learnagent.ui.RotatingLogo
import com.learnagent.ui.SceneTransitions
import com.learnagent.ui.SendIcon
import com.learnagent.ui.SplashScreen
import com.learnagent.ui.TimelineCardAndroid
import com.learnagent.ui.TimelineIcon
import com.learnagent.ui.VideoCardAndroid
import com.learnagent.ui.idleTilt
import com.learnagent.ui.pressScale
import com.learnagent.ui.theme.Abyss
import com.learnagent.ui.theme.Glimmer
import com.learnagent.ui.theme.LearnAgentTheme
import com.learnagent.ui.theme.Mist
import com.learnagent.ui.theme.MoonSilver
import com.learnagent.ui.theme.Phantom
import com.learnagent.ui.theme.TextHigh
import com.learnagent.ui.theme.TextLow
import com.learnagent.ui.theme.TextMid
import com.learnagent.ui.theme.Veil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TechStore.init(applicationContext.filesDir.path)
        setContent {
            val store = remember { ResultsStore(applicationContext) }
            var themeMode by remember { mutableStateOf(store.pref("themeMode", "system")) }
            LearnAgentTheme(darkTheme = themeMode != "light") {
                MentaraApp(store = store, themeMode = themeMode, onThemeMode = { themeMode = it })
            }
        }
    }
}

/**
 * Android 结果目录与偏好：`filesDir/results/`（与桌面 ~/.mentara/results 同构，见 SYNC.md）。
 */
class ResultsStore(context: Context) {
    private val appContext = context.applicationContext
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val prefs = appContext.getSharedPreferences("mentara", Context.MODE_PRIVATE)

    private fun root(): File = File(appContext.filesDir, ResultsDir.RESULTS_ROOT)

    /** 清空本机全部导入数据（结果/索引/转写/OCR/导入临时文件）。 */
    fun wipeAll() {
        runCatching { root().deleteRecursively(); root().mkdirs() }
        runCatching { File(appContext.filesDir, "import").deleteRecursively() }
        runCatching { File(appContext.filesDir, "import").mkdirs() }
    }

    fun videoDir(videoId: String): File = File(root(), videoId).apply { mkdirs() }

    // ---------- 偏好 ----------
    fun pref(key: String, default: String): String = prefs.getString(key, default) ?: default
    fun putPref(key: String, value: String) = prefs.edit().putString(key, value).apply()
    fun bool(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)

    val filesDir: File get() = appContext.filesDir
    fun putBool(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()

    // ---------- 视频与时间线 ----------
    fun listVideos(): List<String> = runCatching {
        root().listFiles { f -> f.isDirectory }.orEmpty().map { it.name }.sorted()
    }.getOrDefault(emptyList())

    fun loadTimeline(videoId: String): List<TimelineItem> {
        val dir = File(root(), videoId)
        val transcript = runCatching {
            val f = File(dir, ResultsDir.TRANSCRIPT_FILE)
            if (f.isFile) json.decodeFromString<List<TranscriptSegment>>(f.readText()) else emptyList()
        }.getOrDefault(emptyList())
        val frames = runCatching {
            val f = File(dir, ResultsDir.FRAMES_FILE)
            if (f.isFile) json.decodeFromString<List<FrameOcr>>(f.readText()) else emptyList()
        }.getOrDefault(emptyList())
        return if (transcript.isEmpty() && frames.isEmpty()) emptyList() else com.learnagent.core.merge(transcript, frames)
    }

    private val countCache = java.util.concurrent.ConcurrentHashMap<String, Pair<Long, Int>>()

    fun unitCount(videoId: String): Int {
        val dir = File(root(), videoId)
        val t = File(dir, "transcript.json")
        val f = File(dir, "frames.json")
        val stamp = t.lastModified() + t.length() + f.lastModified() + f.length()
        countCache[videoId]?.let { if (it.first == stamp) return it.second }
        val n = runCatching { loadTimeline(videoId).size }.getOrDefault(0)
        countCache[videoId] = stamp to n
        return n
    }

    // ---------- SAF 导入（对齐桌面 ConfigStore.importVideo 同构规则） ----------
    fun importVideo(uri: Uri): String? {
        val resolver = appContext.contentResolver
        var name = "video"
        runCatching {
            resolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) name = c.getString(idx) ?: name
            }
        }
        val importDir = File(appContext.filesDir, "import").apply { mkdirs() }
        val dest = File(importDir, name)
        runCatching {
            resolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { input.copyTo(it) }
            } ?: return null
        }.getOrElse { return null }
        val base = name.substringBeforeLast('.')
            .replace(Regex("[^A-Za-z0-9_\\-\\u4e00-\\u9fa5]"), "_")
            .take(40)
            .ifBlank { "video" }
        val id = "$base-${Integer.toHexString(dest.absolutePath.hashCode())}"
        val dir = File(root(), id).apply { mkdirs() }
        val meta = VideoMeta(
            videoId = id, deviceId = "android",
            lastModified = dest.lastModified(), schemaVersion = 1,
            title = name, duration = 0.0
        )
        File(dir, ResultsDir.META_FILE).writeText(json.encodeToString(meta))
        File(dir, "source.txt").writeText(dest.absolutePath)
        return id
    }

    fun sourcePath(videoId: String): String? {
        val f = File(File(root(), videoId), "source.txt")
        return if (f.isFile) f.readText().trim() else null
    }

    // ---------- 浏览记录（新鲜度条数据源） ----------
    fun markViewed(videoId: String) {
        prefs.edit().putLong("viewed_$videoId", System.currentTimeMillis()).apply()
    }

    fun daysSinceViewed(videoId: String): Long? {
        val t = prefs.getLong("viewed_$videoId", 0L)
        return if (t == 0L) null else (System.currentTimeMillis() - t) / 86_400_000L
    }
}

@Composable
fun MentaraApp(store: ResultsStore, themeMode: String, onThemeMode: (String) -> Unit) {
    val context = LocalContext.current
    var showSplash by remember { mutableStateOf(true) }
    // 首启强制安装语音引擎（带进度条；失败可重试；装完才进主界面）
    var asrReady by remember { mutableStateOf(com.learnagent.asr.SenseVoiceEngine.ready(context.filesDir)) }
    var asrPct by remember { mutableStateOf(0) }
    var asrFail by remember { mutableStateOf<String?>(null) }
    var asrRetry by remember { mutableStateOf(0) }
    LaunchedEffect(asrRetry) {
        if (!asrReady) {
            asrFail = null
            val freeMb = runCatching { context.filesDir.usableSpace / 1048576 }.getOrDefault(0L)
            if (freeMb in 1..599L) {
                asrFail = "存储空间不足（剩余约 ${freeMb}MB，需约 600MB）——请清理空间后点「重试」"
            } else {
                var tries = 0
                while (!asrReady && tries < 3) {
                    tries++
                    runCatching {
                        withContext(Dispatchers.IO) {
                            com.learnagent.asr.SenseVoiceEngine.installFromAssets(context, context.filesDir) { d, t ->
                                if (t > 0) asrPct = ((d * 100) / t).coerceIn(0L, 100L).toInt()
                            }
                        }
                    }
                    asrReady = com.learnagent.asr.SenseVoiceEngine.ready(context.filesDir)
                    if (!asrReady) kotlinx.coroutines.delay(1500)
                }
                if (asrReady) asrPct = 100
                else asrFail = "安装失败（常见原因：存储空间不足）——清理空间后点「重试」"
            }
        }
    }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(6000); showSplash = false } // 闪屏看门狗：任何初始化卡住也能进主界面
    var tab by remember { mutableStateOf(0) }
    var videos by remember { mutableStateOf(store.listVideos()) }
    var selected by remember { mutableStateOf<String?>(null) }
    var tick by remember { mutableStateOf(0) }
    var status by remember { mutableStateOf(t("status_ready")) }
    var focusMode by remember { mutableStateOf(0) }
    var successTick by remember { mutableStateOf(0) }
    var onboarded by remember { mutableStateOf(store.bool("onboarded", false)) }
    var userMode by remember { mutableStateOf(store.pref("userMode", "student")) }
    var taskTick by remember { mutableStateOf(0L) }
    LaunchedEffect(com.learnagent.ui.TaskHost.busy) {
        while (com.learnagent.ui.TaskHost.busy) { taskTick = System.currentTimeMillis(); kotlinx.coroutines.delay(1000) }
    }

    // ---- Developer Mode 第 2 批：技术地图 / 周报 / 决策 ----
    val devStore = remember { TechStore() }
    var devRefresh by remember { mutableStateOf(0) }
    var weeklyOpen by remember { mutableStateOf(false) }
    var followUpOpen by remember { mutableStateOf(false) }
    var decisionsOpen by remember { mutableStateOf(false) }
    var techMapOpen by remember { mutableStateOf(false) }
    val weeklyUnread = remember(devRefresh) {
        runCatching { WeeklyReportGenerator(devStore).isUnread() }.getOrDefault(false)
    }
    val pendingFollowUps = remember(devRefresh) {
        runCatching { TechDecisionService.dueFollowUps(devStore).size }.getOrDefault(0)
    }

    // 异步加载（V5 修复闪退）：10 万帧视频在主线程解码合并会 ANR/OOM → 全部移到后台线程
    var timeline by remember { mutableStateOf(emptyList<TimelineItem>()) }
    var unitCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    LaunchedEffect(selected, tick) {
        timeline = emptyList()
        val v = selected
        timeline = if (v == null) emptyList() else withContext(Dispatchers.Default) {
            runCatching { store.loadTimeline(v) }.getOrDefault(emptyList())
        }
    }
    LaunchedEffect(videos, tick) {
        unitCounts = withContext(Dispatchers.Default) {
            videos.associateWith { runCatching { store.unitCount(it) }.getOrDefault(0) }
        }
    }
    val unitCount = unitCounts.values.sum()

    // 音效/触感全局开关（设置页控制）
    LaunchedEffect(Unit) {
        Language.current = store.pref("language", "zh")
        val sound = store.bool("soundEnabled", true)
        AudioFx.enabled = sound
        Haptics.enabled = sound
    }

    // 解析/长任务期间屏幕常亮：任务开始→亮；完成/报错/取消→灭（TaskHost/ModelHost/DebateHost 任一在跑即保持）
    LaunchedEffect(com.learnagent.ui.TaskHost.busy, com.learnagent.ui.ModelHost.busy, com.learnagent.ui.DebateHost.busy) {
        val act = context as? android.app.Activity
        val anyBusy = com.learnagent.ui.TaskHost.busy || com.learnagent.ui.ModelHost.busy || com.learnagent.ui.DebateHost.busy
        act?.runOnUiThread {
            if (anyBusy) act.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            else act.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            (context as? android.app.Activity)?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // 人为因素：每秒 tick + 内容量粒子 + 后台保活同步（熄屏继续跑）
    LaunchedEffect(Unit) {
        runCatching {
            if (!(com.learnagent.ui.TaskHost.busy || com.learnagent.ui.ModelHost.busy || com.learnagent.ui.DebateHost.busy)) {
                context.stopService(android.content.Intent(context, MentaraService::class.java))
            }
        }
        runCatching { com.learnagent.voice.MentaraVoice.init(context) }
        runCatching {
            if (!com.learnagent.asr.SenseVoiceEngine.ready(context.filesDir)) {
                withContext(Dispatchers.IO) { com.learnagent.asr.SenseVoiceEngine.installFromAssets(context, context.filesDir) }
            }
        }
        while (true) {
            delay(1000)
            com.learnagent.ui.AmbienceState.tick()
            MentaraService.sync(context)
        }
    }
    LaunchedEffect(videos.size) { com.learnagent.ui.AmbienceState.updateContentVolume(videos.size) }

    Crossfade(targetState = showSplash, animationSpec = tween(500), label = "splash") { splash ->
        if (splash) {
            SplashScreen(onDone = { showSplash = false })
        } else {
            Box(Modifier.fillMaxSize()) {
                DynamicBackground(Modifier.fillMaxSize())
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    // V7：回归自适应（竖屏手机=单栏；横屏平板=双栏）。真机上缩放方案偏移/闪退，已撤。
                    val layoutMode = LayoutBreakpoints.forWidth(maxWidth.value.toInt())
                    Column(Modifier.fillMaxSize()) {
                        BrandBar()
                        val homePane: @Composable () -> Unit = {
                            HomeTab(
                                videos = videos, store = store,
                                selected = selected,
                                counts = unitCounts,
                                weeklyUnread = weeklyUnread,
                                pendingFollowUps = pendingFollowUps,
                                onOpenWeekly = { weeklyOpen = true },
                                onOpenFollowUps = { followUpOpen = true },
                                onOpenDecisions = { decisionsOpen = true },
                                onOpenTechMap = { techMapOpen = true },
                                onSelect = { id ->
                                    selected = id
                                    store.markViewed(id)
                                    tab = 1
                                    focusMode = 0
                                },
                                onImported = { id ->
                                    videos = store.listVideos()
                                    selected = id
                                    store.markViewed(id)
                                    status = t("status_imported", id)
                                    successTick++
                                    tab = 1
                                    focusMode = 0
                                }
                            )
                        }
                        val focusPane: @Composable () -> Unit = {
                            FocusTab(
                                store = store,
                                selected = selected,
                                timeline = timeline,
                                mode = focusMode,
                                onMode = { focusMode = it },
                                onStatus = { status = it },
                                onChanged = { videos = store.listVideos(); tick++ }
                            )
                        }
                        val chatPane: @Composable () -> Unit = {
                            ChatPanel(videoId = selected, timeline = timeline, modifier = Modifier.fillMaxSize(), apiKey = store.pref("apiKey", ""), model = store.pref("qaModel", "z-ai/glm-5.3-flash"))
                        }
                        val settingsPane: @Composable () -> Unit = {
                            SettingsTab(
                                store = store,
                                videoId = selected,
                                timeline = timeline,
                                themeMode = themeMode,
                                onThemeMode = onThemeMode,
                                userMode = userMode,
                                onUserMode = { userMode = it; store.putPref("userMode", it) },
                                onOpenTechMap = { techMapOpen = true },
                                onOpenWeekly = { weeklyOpen = true },
                                onOpenDecisions = { decisionsOpen = true },
                                onRefresh = { videos = store.listVideos(); tick++; status = t("status_refreshed") }
                            )
                        }

                        Box(Modifier.weight(1f).fillMaxWidth()) {
                            if (layoutMode == LayoutMode.PHONE) {
                                AnimatedContent(
                                    targetState = tab,
                                    transitionSpec = { SceneTransitions.tabSwitch() },
                                    label = "tabSwitch"
                                ) { t ->
                                when (t) {
                                    0 -> homePane()
                                    1 -> focusPane()
                                    2 -> chatPane()
                                    else -> settingsPane()
                                }
                                }
                            } else {
                                // 平板：左「资料库」+ 右「当前标签内容」；底部导航同样可用，避免重复工具条
                                Row(Modifier.fillMaxSize()) {
                                    Box(Modifier.weight(0.38f).fillMaxSize()) { homePane() }
                                    Box(Modifier.weight(1f).fillMaxSize()) {
                                        when (tab) {
                                            2 -> chatPane()
                                            3 -> settingsPane()
                                            else -> focusPane()
                                        }
                                    }
                                }
                            }
                            if (successTick > 0) {
                                com.learnagent.ui.SuccessFlashAndroid(successTick, Modifier.align(Alignment.TopCenter))
                            }
                        }
                        EtherealStatusBar(status = status, videoCount = videos.size, unitCount = unitCount)
                        // 底部导航：所有档都保留（平板用它切标签）
                        run {
                            NavigationBar(containerColor = Veil.copy(alpha = 0.92f)) {
                                listOf(t("nav_library"), t("nav_focus"), t("nav_chat"), t("nav_settings")).forEachIndexed { i, label ->
                                    NavigationBarItem(
                                        selected = tab == i,
                                        onClick = {
                                            AudioFx.tap(); Haptics.tap(context)
                                            com.learnagent.ui.AmbienceState.noteInteraction()
                                            tab = i
                                        },
                                        icon = {
                                            val dotColor = if (tab == i) Accent else Mist
                                            Canvas(Modifier.size(10.dp)) {
                                                drawCircle(
                                                    dotColor,
                                                    radius = size.minDimension / 2f
                                                )
                                            }
                                        },
                                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                        colors = NavigationBarItemDefaults.colors(indicatorColor = Accent.copy(alpha = 0.16f))
                                    )
                                }
                            }
                        }
                    }
                }

                // 首启强制安装页（SenseVoice 预装；失败可重试；装完自动进入）
                if (!asrReady) {
                    Box(
                        Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFF0B0710)).clickable { },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                            if (asrFail == null) {
                                Text("正在安装语音引擎（SenseVoice）", style = MaterialTheme.typography.titleMedium, color = TextHigh)
                                Spacer(Modifier.height(16.dp))
                                LinearProgressIndicator(progress = { (asrPct / 100f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(0.62f))
                                Spacer(Modifier.height(12.dp))
                                Text(if (asrPct >= 99) "正在完成最后校验…" else "已完成 $asrPct% · 仅首次安装，完成后自动进入", style = MaterialTheme.typography.bodyMedium, color = TextMid)
                            } else {
                                Text(asrFail ?: "", style = MaterialTheme.typography.bodyMedium, color = com.learnagent.ui.theme.Coral, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                Spacer(Modifier.height(14.dp))
                                Surface(
                                    color = Accent.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.clickable { asrRetry++ }
                                ) {
                                    Text("重试安装", style = MaterialTheme.typography.labelLarge, color = Accent, modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp))
                                }
                            }
                        }
                    }
                }

                // 停止播报按钮（少女音朗读时可点停）
                if (com.learnagent.voice.MentaraVoice.speaking) {
                    Box(Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(top = 8.dp), contentAlignment = Alignment.Center) {
                        Surface(
                            color = Veil.copy(alpha = 0.95f), shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.clickable { com.learnagent.voice.MentaraVoice.stop() }
                        ) {
                            Text("停止播报", style = MaterialTheme.typography.labelMedium, color = TextHigh, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                        }
                    }
                }

                // 首次引导（入门）
                if (!onboarded) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.55f))
                            .clickable { },
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            color = Veil.copy(alpha = 0.98f),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth(0.92f).padding(16.dp)
                        ) {
                            Column(Modifier.padding(22.dp)) {
                                Text("Mentara", style = MaterialTheme.typography.headlineSmall, color = Accent)
                                Spacer(Modifier.height(6.dp))
                                Text("导入网课 → AI 替你看完 → 随时问它", style = MaterialTheme.typography.titleMedium, color = TextHigh)
                                Spacer(Modifier.height(14.dp))
                                listOf(
                                    "① 点「导入视频」，选一门网课视频",
                                    "② 进「专注」跑 转写 / 抽帧 OCR / 时间线",
                                    "③ 去「问答」直接提问，答案带时间戳出处"
                                ).forEach { line ->
                                    Text(line, style = MaterialTheme.typography.bodyMedium, color = TextMid)
                                    Spacer(Modifier.height(8.dp))
                                }
                                Spacer(Modifier.height(6.dp))
                                Surface(
                                    color = Accent.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.clickable {
                                        AudioFx.success()
                                        store.putPref("onboarded", "true")
                                        onboarded = true
                                    }
                                ) {
                                    Text("开始使用", style = MaterialTheme.typography.labelLarge, color = Accent, modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp))
                                }
                            }
                        }
                    }
                }

                // 后台任务面板（切页面不丢）
                if (com.learnagent.ui.TaskHost.active) {
                    val th = com.learnagent.ui.TaskHost
                    Box(Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(top = 64.dp, start = 16.dp, end = 16.dp)) {
                        Surface(color = Veil.copy(alpha = 0.96f), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("后台任务 · ${th.title}", style = MaterialTheme.typography.labelLarge, color = Accent, modifier = Modifier.weight(1f))
                                    if (th.busy && th.startedAt > 0L) {
                                        val el = ((taskTick - th.startedAt).coerceAtLeast(0L)) / 1000L
                                        Text("已用 %d:%02d".format(el / 60, el % 60), style = MaterialTheme.typography.labelSmall, color = TextMid)
                                    }
                                }
                                if (th.busy) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(th.stage, style = MaterialTheme.typography.bodySmall, color = TextMid)
                                    Spacer(Modifier.height(6.dp))
                                    if (th.progress in 0f..1f) LinearProgressIndicator(progress = { th.progress }, modifier = Modifier.fillMaxWidth())
                                    else LinearProgressIndicator(Modifier.fillMaxWidth())
                                    Spacer(Modifier.height(8.dp))
                                    Surface(color = Mist.copy(alpha = 0.4f), shape = RoundedCornerShape(10.dp), modifier = Modifier.clickable { th.cancel() }) {
                                        Text("取消", style = MaterialTheme.typography.labelSmall, color = TextHigh, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                                    }
                                } else {
                                    LazyColumn(Modifier.height(180.dp)) {
                                        item {
                                            Text(
                                                th.error ?: th.result ?: "",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (th.error != null) MaterialTheme.colorScheme.error else TextHigh
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Surface(color = Mist.copy(alpha = 0.4f), shape = RoundedCornerShape(10.dp), modifier = Modifier.clickable { th.clear() }) {
                                        Text("关闭", style = MaterialTheme.typography.labelSmall, color = TextHigh, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // 休息提醒（人为因素）
                if (com.learnagent.ui.AmbienceState.breakNotice) {
                    LaunchedEffect(Unit) { AudioFx.rest() }
                    Box(Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(top = 52.dp)) {
                        Surface(
                            color = Glimmer.copy(alpha = 0.92f),
                            shape = RoundedCornerShape(22.dp)
                        ) {
                            Text(
                                t("notice_rest"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MoonSilver,
                                modifier = Modifier
                                    .clickable {
                                        AudioFx.tap()
                                        com.learnagent.ui.AmbienceState.dismissBreakNotice()
                                    }
                                    .padding(horizontal = 24.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
                if (weeklyOpen) WeeklyReportDialogAndroid(devStore) { weeklyOpen = false; devRefresh++ }
                if (followUpOpen) FollowUpDialogAndroid(devStore) { followUpOpen = false; devRefresh++ }
                if (decisionsOpen) {
                    Dialog(
                        onDismissRequest = { decisionsOpen = false },
                        properties = DialogProperties(usePlatformDefaultWidth = false)
                    ) {
                        Box(Modifier.fillMaxSize().padding(12.dp)) {
                            DecisionsViewAndroid(devStore) { decisionsOpen = false; devRefresh++ }
                        }
                    }
                }
                if (techMapOpen) {
                    Dialog(
                        onDismissRequest = { techMapOpen = false },
                        properties = DialogProperties(usePlatformDefaultWidth = false)
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize().padding(12.dp),
                            color = Veil.copy(alpha = 0.97f),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Column(Modifier.fillMaxSize().padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Tech Map", style = MaterialTheme.typography.titleMedium, color = Accent)
                                    Spacer(Modifier.weight(1f))
                                    TextButton(onClick = { techMapOpen = false }) { Text("关闭") }
                                }
                                Spacer(Modifier.height(8.dp))
                                TechMapViewAndroid(devStore, Modifier.weight(1f).fillMaxWidth())
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 品牌顶栏：Logo + Mentara（32sp Light 4sp，时间 accent）+ 状态呼吸点。 */
@Composable
private fun BrandBar() {
    val transition = rememberInfiniteTransition(label = "brandBreathe")
    val breathe by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "brandDot"
    )
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = DesignTokens.SpacePageDp.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LearnAgentLogo(30)
        Spacer(Modifier.width(12.dp))
        Text(
            "Mentara",
            style = MaterialTheme.typography.displaySmall,
            color = Accent
        )
        Spacer(Modifier.width(10.dp))
        Text(" ", style = MaterialTheme.typography.bodyMedium, color = MoonSilver)
        Spacer(Modifier.weight(1f))
        val dotAccent = Accent
        Canvas(Modifier.size(12.dp)) {
            drawCircle(dotAccent.copy(alpha = 0.22f * breathe + 0.10f), radius = size.minDimension / 2f)
            drawCircle(dotAccent.copy(alpha = 0.6f * breathe + 0.2f), radius = size.minDimension / 4f)
        }
    }
}

/** 状态栏：底部渐变遮罩 + 状态 + 计数（monospace）。 */
@Composable
private fun EtherealStatusBar(status: String, videoCount: Int, unitCount: Int) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(
                Brush.verticalGradient(listOf(Color.Transparent, Color(0xB80B0710)))
            )
    )
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = DesignTokens.SpacePageDp.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(status, style = MaterialTheme.typography.labelSmall, color = MoonSilver, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text(
            t("status_counts", videoCount.toString(), unitCount.toString()),
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = MoonSilver
        )
    }
}

// ============================================================
//  内容库（瀑布流 + 缩略图 + 新鲜度 + ±4dp 错落 + 玻璃 IMPORT）
// ============================================================

@Composable
private fun HomeTab(
    videos: List<String>,
    store: ResultsStore,
    selected: String?,
    counts: Map<String, Int>,
    weeklyUnread: Boolean,
    pendingFollowUps: Int,
    onOpenWeekly: () -> Unit,
    onOpenFollowUps: () -> Unit,
    onOpenDecisions: () -> Unit,
    onOpenTechMap: () -> Unit,
    onSelect: (String) -> Unit,
    onImported: (String) -> Unit
) {
    val context = LocalContext.current
    // 平台补强 A：SAF 多选 → 持久化权限 → 共享 ImportQueue（串行、可取消）
    val importedIds = remember { mutableListOf<String>() }
    val importQueue = remember {
        ImportQueue { uriString ->
            val uri = Uri.parse(uriString)
            val id = withContext(Dispatchers.IO) { store.importVideo(uri) }
                ?: throw IllegalStateException("import failed: $uriString")
            synchronized(importedIds) { importedIds.add(id) }
        }
    }
    val importProgress by importQueue.progress.collectAsState(
        initial = ImportProgress(0, 0, "", ImportState.PENDING)
    )
    var batchDialogVisible by remember { mutableStateOf(false) }
    val batchActive = importProgress.state == ImportState.PENDING || importProgress.state == ImportState.PROCESSING
    LaunchedEffect(importProgress.state, importProgress.doneCount) {
        when (importProgress.state) {
            ImportState.PENDING, ImportState.PROCESSING -> batchDialogVisible = true
            ImportState.DONE -> {
                batchDialogVisible = true
                if (importProgress.doneCount > 0) {
                    AudioFx.success(); Haptics.success(context)
                    synchronized(importedIds) { importedIds.lastOrNull() }?.let { onImported(it) }
                } else {
                    AudioFx.error(); Haptics.error(context)
                }
            }
            ImportState.CANCELLED -> batchDialogVisible = true
            else -> {}
        }
    }
    val batchLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                runCatching {
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
            importQueue.enqueue(uris.map { it.toString() })
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = DesignTokens.SpacePageDp.dp)) {
        GlassImportButton { batchLauncher.launch(arrayOf("video/*")) }
        Spacer(Modifier.height(10.dp))
        // ---- Developer Mode：周报 / 决策回访 提醒 + 入口 ----
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (weeklyUnread) DevAlertChipAndroid("本周报告已生成", "查看", onOpenWeekly)
            if (pendingFollowUps > 0) DevAlertChipAndroid("$pendingFollowUps 个决策待回访", "回访", onOpenFollowUps)
        }
        if (weeklyUnread || pendingFollowUps > 0) Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DevAlertChipAndroid("Decisions", "打开", onOpenDecisions)
            DevAlertChipAndroid("Tech Map", "打开", onOpenTechMap)
        }
        Spacer(Modifier.height(DesignTokens.SpaceGroupDp.dp))
        if (videos.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    RotatingLogo(size = 170.dp)
                    Spacer(Modifier.height(22.dp))
                    Text("Begin Your Journey", style = MaterialTheme.typography.headlineMedium, color = MoonSilver)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        t("library_sync_hint"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMid
                    )
                }
            }
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Adaptive(minSize = 160.dp), horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpaceGroupDp.dp), verticalItemSpacing = DesignTokens.SpaceGroupDp.dp,
                modifier = Modifier.fillMaxSize()
            ) {
                staggeredItems(videos) { id ->
                    VideoCardAndroid(
                        id = id,
                        segments = counts[id] ?: 0,
                        freshness = when (val d = store.daysSinceViewed(id)) {
                            null -> 1f
                            else -> (1f - d / 7f).coerceIn(0.10f, 1f)
                        },
                        selected = id == selected,
                        onClick = { onSelect(id) }
                    )
                }
            }
        }
    }
    if (batchDialogVisible && importProgress.total > 0) {
        AlertDialog(
            onDismissRequest = { if (!batchActive) batchDialogVisible = false },
            title = { Text("Batch Import", color = Accent) },
            text = {
                Column {
                    when (importProgress.state) {
                        ImportState.PENDING, ImportState.PROCESSING -> {
                            val finished = importProgress.doneCount + importProgress.failedCount
                            Text(
                                "Importing ${(finished + 1).coerceAtMost(importProgress.total)} of ${importProgress.total}...",
                                style = MaterialTheme.typography.bodyMedium, color = TextHigh
                            )
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { if (importProgress.total > 0) finished.toFloat() / importProgress.total else 0f },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Processing: " + importProgress.currentFile.substringAfterLast('/'),
                                style = MaterialTheme.typography.bodySmall, color = MoonSilver,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                        ImportState.CANCELLED -> Text(
                            "Import cancelled",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        ImportState.DONE -> Text(
                            "${importProgress.doneCount} done, ${importProgress.failedCount} failed, " +
                                "${(importProgress.total - importProgress.doneCount - importProgress.failedCount).coerceAtLeast(0)} pending",
                            style = MaterialTheme.typography.bodyMedium, color = TextHigh
                        )
                        else -> {}
                    }
                }
            },
            confirmButton = {
                if (batchActive) {
                    TextButton(onClick = { AudioFx.tap(); importQueue.cancelAll() }) { Text("Cancel") }
                } else {
                    TextButton(onClick = { batchDialogVisible = false }) { Text("Close") }
                }
            }
        )
    }
}

// ============================================================
//  专注区（四模式：时间线 / 图谱 / 问答 / 辩论）
// ============================================================

@Composable
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
private fun FocusTab(
    store: ResultsStore,
    selected: String?,
    timeline: List<TimelineItem>,
    mode: Int,
    onMode: (Int) -> Unit,
    onStatus: (String) -> Unit,
    onChanged: () -> Unit
) {
    val context = LocalContext.current
    val fScope = rememberCoroutineScope()
    val wReady = com.learnagent.asr.SenseVoiceEngine.ready(store.filesDir) // 语音引擎就绪（=SenseVoice）
    var keyTags by remember(timeline) { mutableStateOf(com.learnagent.core.KeyKnowledge.tags(timeline)) }
    LaunchedEffect(timeline) {
        val vid = selected ?: return@LaunchedEffect
        val kf = java.io.File(store.videoDir(vid), "keypoints.txt")
        if (!kf.isFile) return@LaunchedEffect
        runCatching {
            val map = HashMap<Int, String>()
            kf.readLines().forEach { l -> val p = l.split("|"); val i = p.getOrNull(0)?.toIntOrNull(); if (p.size == 2 && i != null) map[i] = p[1] }
            if (map.isNotEmpty()) keyTags = keyTags.mapIndexed { i, h -> map[i] ?: h }
        }
    }
    val sideTags = remember(timeline) { com.learnagent.core.SideNote.tags(timeline) }
    var sumOpen by remember { mutableStateOf(false) }
    var sumFrom by remember { mutableStateOf("00:00") }
    var sumTo by remember { mutableStateOf("05:00") }
    var fBusy by remember { mutableStateOf(false) }
    var tlOpen by remember { mutableStateOf(false) } // 时间线已改为主视图（右侧常驻）
    var pendingMic by remember { mutableStateOf<(() -> Unit)?>(null) }
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) pendingMic?.invoke() else onStatus("未授权麦克风，无法语音转写")
        pendingMic = null
    }
    fun ensureMic(then: () -> Unit) {
        val ok = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (ok) then() else { pendingMic = then; micLauncher.launch(android.Manifest.permission.RECORD_AUDIO) }
    }
    fun fmtStamp(s: Double): String {
        val t = s.toInt()
        return "%d:%02d".format(t / 60, t % 60)
    }
    Column(Modifier.fillMaxSize().padding(horizontal = DesignTokens.SpacePageDp.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            if (selected == null) {
                Text(t("focus_pick_hint"), style = MaterialTheme.typography.bodyMedium, color = TextMid)
            } else {
                Text(
                    selected,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextHigh,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            ModeChipAndroid(selected = mode == 0, onClick = { onMode(0) }) { TimelineIcon(20) }
            ModeChipAndroid(selected = mode == 1, onClick = { onMode(1) }) { GraphIcon(20) }
            ModeChipAndroid(selected = mode == 2, onClick = { onMode(2) }) { SendIcon(20) }
            ModeChipAndroid(selected = mode == 3, onClick = { onMode(3) }) { DebateIcon(20) }
        }
        if (selected != null) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                Surface(
                    color = Mist.copy(alpha = 0.4f), shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.clickable(enabled = !com.learnagent.ui.TaskHost.busy) {
                        if (com.learnagent.ui.TaskHost.busy) return@clickable
                        val sel = selected ?: return@clickable
                        val src = store.sourcePath(sel) ?: run { onStatus("找不到源文件（请重新导入）"); return@clickable }
                        fun doParse() {
                            AudioFx.tap()
                            com.learnagent.ui.TaskHost.run("解析（语音+画面）") { report ->
                                val fps = 1.0 // 固定 1fps；相似画面（>=90%）自动跳过 OCR
                                report("抽帧 OCR 中…（${if (fps >= 19.9) "20fps 全量，一秒20帧不省略" else "每秒 $fps 帧"}）", -1f)
                                val frames = AndroidVideoActions.ocrFrames(context, src, store.videoDir(sel), 1.0 / fps) { d, tt -> report("抽帧 OCR $d/$tt", if (tt > 0) d.toFloat() / tt else -1f); if (d % 20 == 0) onChanged() } // 边解析边刷新：随时可去时间线学
                                onChanged()
                                var speechNote: String
                                val asrKey = store.pref("asrApiKey", "")
                                val senseReadyGate = com.learnagent.asr.SenseVoiceEngine.ready(store.filesDir)
                                if (asrKey.isNotBlank() || senseReadyGate) {
                                    report(if (asrKey.isNotBlank()) "云端语音识别中…（上传音频，通常几秒）" else "SenseVoice 语音识别…（首次含模型加载 1-5 分钟）", -1f)
                                    val wStart = System.currentTimeMillis()
                                    var spErr: String? = null
                                    val sp = runCatching {
                                        AndroidVideoActions.transcribe(context, src, store.videoDir(sel)) { pct ->
                                            val remain = if (pct in 2..99) {
                                                ((System.currentTimeMillis() - wStart) * (100 - pct) / (pct * 60000L))
                                            } else -1L
                                            report(
                                                "语音识别 $pct%" + (if (remain in 1..5999) "（约剩 $remain 分钟）" else ""),
                                                (pct / 100f).coerceIn(0f, 1f)
                                            )
                                        }.size
                                    }.getOrElse { spErr = it.message ?: it.javaClass.simpleName; 0 }
                                    onChanged()
                                    speechNote = if (spErr != null) "语音失败：$spErr（画面/永久知识不受影响）"
                                    else "语音 $sp 段（" + (if (asrKey.isNotBlank()) "云端 ASR" else "SenseVoice") + "）"
                                } else {
                                    if (store.pref("localAsrEngine", "sensevoice") == "sensevoice") {
                                        speechNote = "SenseVoice 模型未安装 → 已自动开始下载（228MB，一次即可，下完重新点解析）"
                                        runCatching { com.learnagent.ui.ModelHost.downloadSenseVoice(store.filesDir) }
                                    } else {
                                        speechNote = "语音引擎未就绪：请重启 App 完成语音引擎安装"
                                    }
                                }
                                var capNote = ""
                                val key3 = store.pref("apiKey", "")
                                if (key3.isNotBlank() && com.learnagent.ui.isOnline(context) && frames.isNotEmpty()) {
                                    val everyNth = ((frames.size + 299) / 300).coerceAtLeast(1)
                                    report("画面描述（视觉模型，自动采样≤300帧）… 可随时取消（已完成会保留）", -1f)
                                    val n = AndroidVideoActions.captionFrames(
                                        store.videoDir(sel), key3,
                                        store.pref("visionModel", "meta/llama-3.2-11b-vision-instruct"),
                                        everyNth = everyNth
                                    ) { d, t -> report("画面描述 $d/$t 帧", if (t > 0) d.toFloat() / t else -1f) }
                                    onChanged()
                                    capNote = if (n >= 0) " · 画面描述 $n 帧（自动采样上限 300；手动『画面描述』= 全量续跑）" else ""
                                }
                                var proofNote = if (key3.isBlank()) " · 未配置 API Key：已跳过纠错/看图改字（设置里填 Key）" else ""
                                if (key3.isNotBlank() && com.learnagent.ui.isOnline(context) && frames.isNotEmpty()) {
                                    report("画面原图校对（模型看图改字）…", -1f)
                                    val c = AndroidVideoActions.proofreadFramesWithVision(
                                        store.videoDir(sel), key3,
                                        store.pref("visionModel", "meta/llama-3.2-11b-vision-instruct")
                                    ) { d, t -> report("原图校对 $d/$t", if (t > 0) d.toFloat() / t else -1f) }
                                    onChanged()
                                    proofNote = if (c > 0) " · 原图校对改字 $c 处" else " · 原图校对完成"
                                }
                                report("提炼永久知识（skill.md，未删减完整档案）…", -1f)
                                val skillN = AndroidVideoActions.buildSkill(store.videoDir(sel))
                                onChanged()
                                var noteN = ""
                                if (key3.isNotBlank() && com.learnagent.ui.isOnline(context)) {
                                    report("整理最终笔记（OCR 纠错 + 去重）…", -1f)
                                    val raw = AndroidVideoActions.rawEvidence(store.videoDir(sel))
                                    if (raw.isNotBlank()) {
                                        val parts = AndroidVideoActions.rawEvidence(store.videoDir(sel), 60000).chunked(2500)
                                        val sb = StringBuilder()
                                        parts.forEachIndexed { pi, part ->
                                            report("纠错中 " + (pi + 1) + "/" + parts.size + " 段…", (pi + 1).toFloat() / parts.size)
                                            val fixed = AndroidNimClient(key3, store.pref("qaModel", "z-ai/glm-5.3-flash"))
                                                .generateResponse("这是从网课中提取的原始文本，包含 ASR 语音识别和画面 OCR 的错别字。请你结合上下文，帮我修正错别字（特别是专业术语），去除重复词，输出通顺的文本。严格要求：一律使用简体中文常用字，严禁繁体字与生僻异体字；不得凭空捏造原文没有的字词。如果某行文本毫无逻辑（乱码、弹幕残影、播放器残留字样），请直接删除该行，绝对不要尝试拼凑或脑补其含义！\n\n" + part, com.learnagent.core.LlmConfig(provider = "nvidia", apiKey = "", baseUrl = "", model = "", temperature = 0.2, maxTokens = 4096))
                                            if (fixed.isNotBlank()) sb.append(fixed).append("\n\n")
                                        }
                                        val note = sb.toString().trim()
                                        // （已由分段纠错生成 note）
                                        if (note.isNotBlank()) {
                                            java.io.File(store.videoDir(sel), "notes.md").appendText("\n\n## 修正后的学习笔记（OCR 纠错版）\n\n" + note + "\n")
                                            noteN = " · 笔记已纠错生成"
                                        }
                                    }
                                }
                                "解析完成：$speechNote · 画面 ${frames.size} 帧$capNote$proofNote · 永久知识 $skillN 条$noteN"
                            }
                        }
                        if (wReady) doParse() else ensureMic { doParse() }
                    }
                ) { Text(if (com.learnagent.ui.TaskHost.busy) "处理中…" else "解析（语音+画面）", style = MaterialTheme.typography.labelMedium, color = TextHigh, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) }
                // （旧「时间线」按钮已删：功能并入主页主视图）
                Surface(
                    color = Accent.copy(alpha = 0.18f), shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.clickable(enabled = !com.learnagent.ui.TaskHost.busy) {
                        if (com.learnagent.ui.TaskHost.busy) return@clickable
                        val key = store.pref("apiKey", "")
                        if (key.isBlank()) { onStatus("需在设置填 API Key"); return@clickable }
                        if (!com.learnagent.ui.isOnline(context)) { onStatus("无网络，无法调用大模型"); return@clickable }
                        if (!com.learnagent.core.Evidence.ready(timeline)) { onStatus("请先『解析（语音+画面）』：语音与画面齐全后判定更准"); return@clickable }
                        AudioFx.tap()
                        val snap = timeline
                        com.learnagent.ui.TaskHost.run("AI 重点判定") { report ->
                            report("大模型判定重点中…", -1f)
                            val client = AndroidNimClient(key, store.pref("qaModel", "z-ai/glm-5.3-flash"))
                            val tags = com.learnagent.core.SemanticKeyJudge(client).judge(snap)
                            keyTags = tags
                            selected?.let { v ->
                                runCatching { java.io.File(store.videoDir(v), "keypoints.txt").writeText(tags.mapIndexedNotNull { i, tg -> tg?.let { "$i|$it" } }.joinToString("\n")) }
                            }
                            "AI 已判定 ${tags.count { it != null }} 处重点，已永久保存（重启仍在时间线高亮）"
                        }
                    }
                ) { Text("AI 重点", style = MaterialTheme.typography.labelMedium, color = Accent, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) }
                Surface(
                    color = Accent.copy(alpha = 0.20f), shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.clickable(enabled = !com.learnagent.ui.TaskHost.busy) {
                        if (com.learnagent.ui.TaskHost.busy) return@clickable
                        val key = store.pref("apiKey", "")
                        if (key.isBlank()) { onStatus("需在设置填 API Key"); return@clickable }
                        if (!com.learnagent.ui.isOnline(context)) { onStatus("无网络，无法调用大模型"); return@clickable }
                        if (!com.learnagent.core.Evidence.ready(timeline)) { onStatus("请先『解析（语音+画面）』：语音与画面齐全后判定更准"); return@clickable }
                        AudioFx.tap()
                        val snap = timeline
                        com.learnagent.ui.TaskHost.run("AI 老师（全知全能）") { report ->
                            report("判定重点…", -1f)
                            val client = AndroidNimClient(key, store.pref("qaModel", "z-ai/glm-5.3-flash"))
                            keyTags = com.learnagent.core.SemanticKeyJudge(client).judge(snap)
                            report("撰写学习笔记…", -1f)
                            val numbered = com.learnagent.core.Evidence.aiDigest(snap, 200, 12000)
                            val prompt = "你是一位全知全能的老师。基于下面这门网课的内容，写一份学习笔记：\n" +
                                "1) 3-6 个要点，每条附 [mm:ss] 时间戳；\n2) 末尾用「必学重点」列出最关键 2-4 点。\n" +
                                "只依据给定内容，不要编造。用中文，条理清晰。\n\n" + numbered
                            val note = client.generateResponse(prompt, com.learnagent.core.LlmConfig(provider = "nvidia", apiKey = "", baseUrl = "", model = ""))
                            if (note.isNotBlank()) {
                                selected?.let { v ->
                                    runCatching { java.io.File(store.videoDir(v), "notes.md").appendText("\n\n## AI 老师笔记（" + java.text.SimpleDateFormat("MM-dd HH:mm").format(java.util.Date()) + "）\n\n" + note + "\n") }
                                }
                            }
                            note.ifBlank { "（模型未返回内容，请稍后重试）" }
                        }
                    }
                ) { Text(if (com.learnagent.ui.TaskHost.busy) "处理中…" else "AI 老师（一键）", style = MaterialTheme.typography.labelMedium, color = Accent, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) }
                Surface(
                    color = Mist.copy(alpha = 0.4f), shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.clickable(enabled = !com.learnagent.ui.TaskHost.busy) {
                        if (com.learnagent.ui.TaskHost.busy) return@clickable
                        val sel2 = selected ?: return@clickable
                        val key2 = store.pref("apiKey", "")
                        if (key2.isBlank()) { onStatus("需在设置填 API Key"); return@clickable }
                        if (!com.learnagent.ui.isOnline(context)) { onStatus("无网络，无法纠错"); return@clickable }
                        AudioFx.tap()
                        com.learnagent.ui.TaskHost.run("三模型纠错") { report ->
                            val dm = com.learnagent.core.DebateModels
                            val n = AndroidVideoActions.polishTranscript(
                                sel2, store.videoDir(sel2), key2,
                                dm.resolve(store.pref("debateModelA", ""), dm.DEFAULT_A),
                                dm.resolve(store.pref("debateModelB", ""), dm.DEFAULT_B),
                                dm.resolve(store.pref("debateModelC", ""), dm.DEFAULT_C)
                            ) { d, tt -> report("纠错 $d/$tt", if (tt > 0) d.toFloat() / tt else -1f) }
                            onChanged()
                            if (n < 0) "没有转写文件，请先『解析（语音）』" else "纠错完成：$n 句"
                        }
                    }
                ) { Text(if (com.learnagent.ui.TaskHost.busy) "处理中…" else "纠错", style = MaterialTheme.typography.labelMedium, color = TextHigh, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) }
                Surface(
                    color = Mist.copy(alpha = 0.4f), shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.clickable { AudioFx.tap(); sumOpen = true }
                ) { Text("段落概括", style = MaterialTheme.typography.labelMedium, color = TextHigh, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) }
                Surface(
                    color = Mist.copy(alpha = 0.4f), shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.clickable(enabled = !com.learnagent.ui.TaskHost.busy) {
                        if (com.learnagent.ui.TaskHost.busy) return@clickable
                        val sel3 = selected ?: return@clickable
                        val key3 = store.pref("apiKey", "")
                        if (key3.isBlank()) { onStatus("需在设置填 API Key"); return@clickable }
                        if (!com.learnagent.ui.isOnline(context)) { onStatus("无网络，无法调用视觉模型"); return@clickable }
                        AudioFx.tap()
                        com.learnagent.ui.TaskHost.run("画面描述（视觉模型）") { report ->
                            val n = AndroidVideoActions.captionFrames(
                                store.videoDir(sel3), key3,
                                store.pref("visionModel", "meta/llama-3.2-11b-vision-instruct")
                            ) { d, t -> report("画面描述 $d/$t 帧", if (t > 0) d.toFloat() / t else -1f) }
                            onChanged()
                            if (n < 0) "没有帧数据：请先点『解析（语音+画面）』" else "画面描述完成：$n 帧（已并入证据：语音+画面+画面描述）"
                        }
                    }
                ) { Text(if (com.learnagent.ui.TaskHost.busy) "处理中…" else "画面描述", style = MaterialTheme.typography.labelMedium, color = TextHigh, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) }
            }
        }
        // （旧时间线弹窗已删：全部功能在主视图）
        if (sumOpen) {
            androidx.compose.ui.window.Dialog(onDismissRequest = { sumOpen = false }) {
                Surface(color = Veil.copy(alpha = 0.98f), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth(0.9f)) {
                    Column(Modifier.padding(18.dp)) {
                        Text("概括时间段", style = MaterialTheme.typography.titleMedium, color = Accent)
                        Spacer(Modifier.height(4.dp))
                        Text("输入开始/结束时间（mm:ss），AI 概括这段时间讲了哪些方面", style = MaterialTheme.typography.bodySmall, color = TextMid)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextField(value = sumFrom, onValueChange = { sumFrom = it }, singleLine = true, label = { Text("开始 mm:ss") }, shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f))
                            TextField(value = sumTo, onValueChange = { sumTo = it }, singleLine = true, label = { Text("结束 mm:ss") }, shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                color = Accent.copy(alpha = 0.18f), shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.clickable {
                                    val a = com.learnagent.core.RangeSlice.parseStamp(sumFrom)
                                    val b = com.learnagent.core.RangeSlice.parseStamp(sumTo)
                                    if (a == null || b == null) { onStatus("时间格式应为 mm:ss"); return@clickable }
                                    val key = store.pref("apiKey", "")
                                    if (key.isBlank()) { onStatus("需在设置填 API Key"); return@clickable }
                                    if (!com.learnagent.ui.isOnline(context)) { onStatus("无网络，无法调用大模型"); return@clickable }
                                    val seg = com.learnagent.core.RangeSlice.slice(timeline, a, b)
                                    if (seg.isEmpty()) { onStatus("该时间段没有内容"); return@clickable }
                                    sumOpen = false
                                    AudioFx.tap()
                                    val label = com.learnagent.core.RangeSlice.fmt(a) + "–" + com.learnagent.core.RangeSlice.fmt(b)
                                    com.learnagent.ui.TaskHost.run("段落概括 $label") { report ->
                                        report("整理这段时间讲了什么…", -1f)
                                        val body = com.learnagent.core.Evidence.render(seg, 10000)
                                        val prompt = "下面是某门网课在时间段 $label 的内容（带时间戳）。\n" +
                                            "请概括这段时间【讲了哪些方面】：先给 1 句总述，再分 3-6 个要点，每个要点前标 [mm:ss]。只依据给定内容，用中文。\n\n" + body
                                        val sum = AndroidNimClient(key, store.pref("qaModel", "z-ai/glm-5.3-flash"))
                                            .generateResponse(prompt, com.learnagent.core.LlmConfig(provider = "nvidia", apiKey = "", baseUrl = "", model = "", temperature = 0.3, maxTokens = 4096))
                                        if (sum.isNotBlank()) {
                                            selected?.let { v ->
                                                runCatching { java.io.File(store.videoDir(v), "notes.md").appendText("\n\n## 段落概括 $label\n\n" + sum + "\n") }
                                            }
                                        }
                                        sum.ifBlank { "（模型未返回内容）" }
                                    }
                                }
                            ) { Text("开始概括", style = MaterialTheme.typography.labelLarge, color = Accent, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) }
                            Surface(
                                color = Mist.copy(alpha = 0.4f), shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.clickable { sumOpen = false }
                            ) { Text("取消", style = MaterialTheme.typography.labelLarge, color = TextHigh, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) }
                        }
                    }
                }
            }
        }
        if (selected == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(t("focus_pick_sub"), style = MaterialTheme.typography.bodyMedium, color = TextMid)
            }
        } else {
            Box(Modifier.fillMaxSize().then(SceneTransitions.modeSwitch(mode))) {
                when (mode) {
                    1 -> com.learnagent.ui.KnowledgeGraph(
                        items = timeline,
                        selectedIndex = null,
                        onNodeClick = { idx ->
                            AudioFx.tap(); Haptics.tap(context)
                            onStatus("定位到片段 $idx")
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    2 -> ChatPanel(videoId = selected, timeline = timeline, modifier = Modifier.fillMaxSize(), apiKey = store.pref("apiKey", ""), model = store.pref("qaModel", "z-ai/glm-5.3-flash"))
                    3 -> DebatePanel(
                        videoId = selected,
                        timeline = timeline,
                        apiKey = store.pref("apiKey", ""),
                        debateModelA = store.pref("debateModelA", ""),
                        debateModelB = store.pref("debateModelB", ""),
                        debateModelC = store.pref("debateModelC", ""),
                        modifier = Modifier.fillMaxSize()
                    )
                    else -> com.learnagent.ui.AndroidTimelinePane(
                        videoFile = selected?.let { s -> store.sourcePath(s)?.let { File(it) } },
                        timeline = timeline
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeChipAndroid(selected: Boolean, onClick: () -> Unit, icon: @Composable () -> Unit) {
    val context = LocalContext.current
    val interaction = remember { MutableInteractionSource() }
    Surface(
        color = if (selected) Accent.copy(alpha = 0.18f) else Mist.copy(alpha = 0.25f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .pressScale(interaction)
            .clickable(interaction, indication = null) { AudioFx.tap(); Haptics.tap(context); onClick() }
    ) {
        Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
            val tint = if (selected) Accent else MoonSilver
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides tint
            ) { icon() }
        }
    }
}

// ============================================================
//  设置（主题 / 音效 / 目录 / Key / 辩论模型 / 工具）
// ============================================================

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun SettingsTab(
    store: ResultsStore,
    videoId: String?,
    timeline: List<TimelineItem>,
    themeMode: String,
    onThemeMode: (String) -> Unit,
    userMode: String,
    onUserMode: (String) -> Unit,
    onOpenTechMap: () -> Unit,
    onOpenWeekly: () -> Unit,
    onOpenDecisions: () -> Unit,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    var apiKey by remember { mutableStateOf(store.pref("apiKey", "")) }
    var qaModel by remember { mutableStateOf(store.pref("qaModel", "z-ai/glm-5.3-flash")) }
    var tavilyKey by remember { mutableStateOf(store.pref("tavilyApiKey", "")) }
    var bochaKey by remember { mutableStateOf(store.pref("bochaApiKey", "")) }
    var voiceOn by remember { mutableStateOf(com.learnagent.voice.MentaraVoice.enabled) }
    var modelA by remember { mutableStateOf(store.pref("debateModelA", DebateModels.DEFAULT_A)) }
    var modelB by remember { mutableStateOf(store.pref("debateModelB", DebateModels.DEFAULT_B)) }
    var modelC by remember { mutableStateOf(store.pref("debateModelC", DebateModels.DEFAULT_C)) }
    var soundOn by remember { mutableStateOf(store.bool("soundEnabled", true)) }
    var wipeOpen by remember { mutableStateOf(false) }
    var whisperBusy by remember { mutableStateOf(false) }
    var whisperPct by remember { mutableStateOf(0) }
    var whisperMsg by remember { mutableStateOf<String?>(null) }

    // ---------- 学习引擎：Rebuild Tech Cards ----------
    var techDialog by remember { mutableStateOf(false) }
    var techBusy by remember { mutableStateOf(false) }
    var techDone by remember { mutableStateOf(0) }
    var techTotal by remember { mutableStateOf(0) }
    var techResult by remember { mutableStateOf<String?>(null) }
    var techError by remember { mutableStateOf<String?>(null) }
    val techScope = rememberCoroutineScope()

    fun rebuildTechCards() {
        techResult = null
        techError = null
        techDone = 0
        techTotal = 0
        techDialog = true
        val vid = videoId
        if (vid == null) {
            techError = "请先选择一个视频"
            return
        }
        if (timeline.isEmpty()) {
            techError = "当前视频没有可处理的 LearningUnit"
            return
        }
        techBusy = true
        techScope.launch {
            try {
                val units = withContext(Dispatchers.IO) { ContextAnalyzer(null).analyze(timeline, vid) }
                if (units.isEmpty()) throw IllegalStateException("当前视频没有可处理的 LearningUnit")
                techTotal = units.size
                val key = store.pref("apiKey", "")
                val llm = if (key.isNotBlank()) {
                    AndroidNimClient(key, store.pref("model", "deepseek-ai/deepseek-v4.1-flash"))
                } else null
                val extractor = TechCardExtractor(llm)
                val techStore = TechStore()
                var count = 0
                units.forEachIndexed { idx, unit ->
                    val cards = withContext(Dispatchers.IO) { extractor.extract(unit, vid) }
                    withContext(Dispatchers.IO) { cards.forEach { techStore.saveTechCard(it) } }
                    count += cards.size
                    techDone = idx + 1
                }
                techResult = "提取了 $count 张技术卡"
            } catch (e: Exception) {
                techError = "失败：${e.message ?: e.javaClass.simpleName}"
            } finally {
                techBusy = false
            }
        }
    }

    if (techDialog) {
        AlertDialog(
            onDismissRequest = { if (!techBusy) techDialog = false },
            title = { Text("Rebuild Tech Cards") },
            text = {
                Column {
                    if (techBusy) {
                        Text("正在处理当前视频...", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { if (techTotal > 0) techDone.toFloat() / techTotal else 0f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("$techDone / $techTotal LearningUnit", style = MaterialTheme.typography.bodySmall, color = TextMid)
                    } else {
                        Text(
                            techError ?: techResult ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (techError != null) MaterialTheme.colorScheme.error else TextHigh
                        )
                    }
                }
            },
            confirmButton = {
                if (!techBusy) {
                    TextButton(onClick = { AudioFx.tap(); techDialog = false }) { Text("关闭") }
                }
            }
        )
    }

    if (wipeOpen) {
        AlertDialog(
            onDismissRequest = { wipeOpen = false },
            title = { Text("清空内容库") },
            text = { Text("将删除本机全部已导入视频、转写、OCR、索引等数据，且不可恢复。确定继续？") },
            confirmButton = {
                TextButton(onClick = { AudioFx.tap(); store.wipeAll(); wipeOpen = false; onRefresh() }) { Text("清空") }
            },
            dismissButton = { TextButton(onClick = { wipeOpen = false }) { Text("取消") } }
        )
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = DesignTokens.SpacePageDp.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Text(t("settings_title"), style = MaterialTheme.typography.titleLarge, color = TextHigh) }

        item {
            Text("使用模式", style = MaterialTheme.typography.titleSmall, color = TextHigh)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("学生模式" to "student", "开发者模式" to "developer").forEach { (label, v) ->
                    Surface(
                        color = if (userMode == v) Accent.copy(alpha = 0.18f) else Mist.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.clickable { AudioFx.tap(); onUserMode(v) }
                    ) {
                        Text(label, style = MaterialTheme.typography.labelMedium, color = if (userMode == v) Accent else TextHigh, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                if (userMode == "developer") "开发者模式：显示 技术地图 / 周报 / 决策 / 技术卡" else "学生模式：聚焦重点标注与问答，界面更简洁",
                style = MaterialTheme.typography.bodySmall, color = TextMid
            )
        }

        item {
            Text(t("settings_theme"), style = MaterialTheme.typography.titleSmall, color = TextHigh)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(t("theme_system") to "system", t("theme_light") to "light", t("theme_dark") to "dark").forEach { (label, v) ->
                    Surface(
                        color = if (themeMode == v) Accent.copy(alpha = 0.18f) else Mist.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.clickable { onThemeMode(v); store.putPref("themeMode", v) }
                    ) {
                        Text(label, style = MaterialTheme.typography.labelMedium, color = if (themeMode == v) Accent else TextHigh, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                    }
                }
            }
        }

        item {
            Text(t("settings_language"), style = MaterialTheme.typography.titleSmall, color = TextHigh)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(t("lang_en") to "en", t("lang_zh") to "zh").forEach { (label, v) ->
                    Surface(
                        color = if (Language.current == v) Accent.copy(alpha = 0.18f) else Mist.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.clickable { store.putPref("language", v); Language.current = v }
                    ) {
                        Text(label, style = MaterialTheme.typography.labelMedium, color = if (Language.current == v) Accent else TextHigh, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                    }
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(t("settings_sound"), style = MaterialTheme.typography.titleSmall, color = TextHigh)
                    Text(t("settings_sound_desc"), style = MaterialTheme.typography.bodySmall, color = TextMid)
                }
                Switch(checked = soundOn, onCheckedChange = {
                    soundOn = it
                    store.putBool("soundEnabled", it)
                    AudioFx.enabled = it
                    Haptics.enabled = it
                })
            }
        }

        item {
            Text(t("settings_api_key") , style = MaterialTheme.typography.titleSmall, color = TextHigh)
            Spacer(Modifier.height(6.dp))
            Field(apiKey, { apiKey = it; store.putPref("apiKey", it) }, "API Key")
            var keyMsg by remember { mutableStateOf<String?>(null) }
            var keyChecking by remember { mutableStateOf(false) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    color = Mist.copy(alpha = 0.4f), shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.clickable {
                        AudioFx.tap()
                        runCatching { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://build.nvidia.com/settings/api-keys"))) }
                    }
                ) { Text("去注册 NVIDIA（网页）", style = MaterialTheme.typography.labelSmall, color = TextHigh, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) }
                Surface(
                    color = Accent.copy(alpha = 0.18f), shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.clickable(enabled = !keyChecking) {
                        if (apiKey.isBlank()) { keyMsg = "还没填 Key"; return@clickable }
                        keyChecking = true; keyMsg = null
                        techScope.launch {
                            val ok = com.learnagent.ui.nimReachable(apiKey.trim(), force = true)
                            keyMsg = if (ok) "Key 有效：NIM 连通 ✓" else "连不上 NIM：Key 无效，或网络到 NVIDIA 不通"
                            keyChecking = false
                        }
                    }
                ) { Text(if (keyChecking) "检测中…" else "检测 Key", style = MaterialTheme.typography.labelSmall, color = Accent, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) }
            }
            keyMsg?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = TextMid)
            }
        }

        item {
            Text("问答模型", style = MaterialTheme.typography.titleSmall, color = TextHigh)
            Spacer(Modifier.height(4.dp))
            Text("有 API Key 时用它回答（默认 GLM 5.3）", style = MaterialTheme.typography.bodySmall, color = TextMid)
            Spacer(Modifier.height(6.dp))
            Field(qaModel, { qaModel = it; store.putPref("qaModel", it) }, "z-ai/glm-5.3-flash")
            Field(bochaKey, { bochaKey = it; store.putPref("bochaApiKey", it) }, "博查 Bocha Key（可选，国内付费；默认已用免费的 AnySearch）")
            Surface(
                color = Mist.copy(alpha = 0.4f), shape = RoundedCornerShape(10.dp),
                modifier = Modifier.clickable { AudioFx.tap(); runCatching { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://open.bochaai.com"))) } }
            ) { Text("去注册 博查（网页）", style = MaterialTheme.typography.labelSmall, color = TextHigh, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) }
            Field(tavilyKey, { tavilyKey = it; store.putPref("tavilyApiKey", it) }, "Tavily API Key（海外备用，需 Google 登录）")
            Surface(
                color = Mist.copy(alpha = 0.4f), shape = RoundedCornerShape(10.dp),
                modifier = Modifier.clickable { AudioFx.tap(); runCatching { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://app.tavily.com"))) } }
            ) { Text("去注册 Tavily（网页）", style = MaterialTheme.typography.labelSmall, color = TextHigh, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) }
        }

        item {
            Text(t("settings_debate_models"), style = MaterialTheme.typography.titleSmall, color = TextHigh)
            Spacer(Modifier.height(6.dp))
            Field(modelA, { modelA = it; store.putPref("debateModelA", it) }, t("settings_model_a"))
            Spacer(Modifier.height(6.dp))
            Field(modelB, { modelB = it; store.putPref("debateModelB", it) }, t("settings_model_b"))
            Spacer(Modifier.height(6.dp))
            Field(modelC, { modelC = it; store.putPref("debateModelC", it) }, t("settings_model_c"))
        }

        item {
            Text(t("settings_results_dir"), style = MaterialTheme.typography.titleSmall, color = TextHigh)
            Spacer(Modifier.height(4.dp))
            Text(t("settings_dir_note"), style = MaterialTheme.typography.bodySmall, color = MoonSilver)
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    color = Mist.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.clickable { AudioFx.tap(); onRefresh() }
                ) {
                    Text(t("btn_refresh"), style = MaterialTheme.typography.labelMedium, color = TextHigh, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                }
                Surface(
                    color = Mist.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.clickable {
                        AudioFx.tap()
                        context.startActivity(Intent(context, TranscriberActivity::class.java))
                    }
                ) {
                    Text(t("tools_whisper"), style = MaterialTheme.typography.labelMedium, color = TextHigh, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                }
                Surface(
                    color = Mist.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.clickable {
                        AudioFx.tap()
                        context.startActivity(Intent(context, StorageTestActivity::class.java))
                    }
                ) {
                    Text(t("tools_storage"), style = MaterialTheme.typography.labelMedium, color = TextHigh, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                }
            }
        }

        if (userMode == "developer") {
        item {
            Text("学习引擎", style = MaterialTheme.typography.titleSmall, color = TextHigh)
            Spacer(Modifier.height(6.dp))
            Surface(
                color = Mist.copy(alpha = 0.4f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.clickable { AudioFx.tap(); rebuildTechCards() }
            ) {
                Text("Rebuild Tech Cards", style = MaterialTheme.typography.labelMedium, color = TextHigh, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
            }
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    "Tech Map" to onOpenTechMap,
                    "Weekly Report" to onOpenWeekly,
                    "Decisions" to onOpenDecisions
                ).forEach { (label, action) ->
                    Surface(
                        color = Mist.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.clickable { AudioFx.tap(); action() }
                    ) {
                        Text(label, style = MaterialTheme.typography.labelMedium, color = TextHigh, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                    }
                }
            }
            if (videoId == null) {
                Spacer(Modifier.height(4.dp))
                Text("请先选择一个视频", style = MaterialTheme.typography.bodySmall, color = TextMid)
            }
        }
        }

        item {
            Text("联网搜索（专家模式用，默认免费）", style = MaterialTheme.typography.titleSmall, color = TextHigh)
            Spacer(Modifier.height(6.dp))
            Text("默认走 AnySearch 匿名免费访问；注册（只需邮箱，每天免费 1000 次）可提高限额。失败自动回落必应。", style = MaterialTheme.typography.bodySmall, color = TextMid)
            Spacer(Modifier.height(8.dp))
            var anysEmail by remember { mutableStateOf("") }
            var anysMsg by remember { mutableStateOf<String?>(null) }
            var anysBusy by remember { mutableStateOf(false) }
            var anysKey by remember { mutableStateOf(store.pref("anysearchApiKey", "")) }
            TextField(
                value = anysEmail, onValueChange = { anysEmail = it }, singleLine = true,
                label = { Text("邮箱（一键注册 AnySearch 免费 Key）") },
                shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    color = Accent.copy(alpha = 0.18f), shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.clickable(enabled = !anysBusy) {
                        // 中文输入法兜底：全角＠和。都转半角
                        val email = anysEmail.trim().replace("＠", "@").replace("。", ".")
                        if (email.isBlank() || !email.contains("@") || !email.contains(".")) { anysMsg = "邮箱格式不对（应为 name@domain.com，注意半角 @ 和 .）"; return@clickable }
                        if (!com.learnagent.ui.isOnline(context)) { anysMsg = "无网络，无法注册"; return@clickable }
                        anysBusy = true; anysMsg = null
                        techScope.launch {
                            com.learnagent.llm.AndroidAnySearcher.register(email)
                                .onSuccess { k ->
                                    anysKey = k; store.putPref("anysearchApiKey", k)
                                    anysMsg = "注册成功，Key 已自动保存（账号=你的邮箱，无需再登录网页）"
                                }
                                .onFailure {
                                    val m = it.message ?: it.javaClass.simpleName
                                    val low = m.lowercase()
                                    anysMsg = when {
                                        low.contains("already") || low.contains("已注册") -> "该邮箱已注册过且 Key 无法自动找回（实测 409 无返回）。最快解法：换一个没用过的邮箱点一键注册，秒发 Key；或点「登录官网」输入邮箱→去邮箱点登录链接→Console→API Keys 复制"
                                        low.contains("rate") && low.contains("limit") -> {
                                            val sec = Regex("(\\d+)\\s*(秒|seconds?|secs?|s)\\b", RegexOption.IGNORE_CASE).find(m)?.groupValues?.get(1)
                                            "被限流（点太频繁）：等" + (if (sec != null) "约 $sec 秒" else "几分钟") + "再点，或切换 WiFi/流量换网络。注册不着急：匿名搜索现在就能用；也可点「登录官网」直接拿 Key"
                                        }
                                        low.contains("key creation failed") -> "账号已创建但 Key 生成失败：点「登录官网」登录后创建 Key"
                                        low.contains("http") || low.contains("timeout") || low.contains("resolve") || low.contains("failed to connect") -> "连不上 api.anysearch.com（网络问题）：切换 WiFi/流量后重试"
                                        else -> "注册失败：" + m
                                    }
                                }
                            anysBusy = false
                        }
                    }
                ) { Text(if (anysBusy) "注册中…" else "一键注册拿 Key", style = MaterialTheme.typography.labelMedium, color = Accent, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) }
                Surface(
                    color = Mist.copy(alpha = 0.4f), shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.clickable {
                        AudioFx.tap()
                        runCatching { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.anysearch.com/login"))) }
                    }
                ) { Text("登录官网拿 Key", style = MaterialTheme.typography.labelMedium, color = TextHigh, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) }
            }
            if (anysKey.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text("已有 Key ✓（联网搜索已启用；不注册也能匿名用，只是限额低）", style = MaterialTheme.typography.bodySmall, color = TextMid)
            }
            Spacer(Modifier.height(6.dp))
            Field(anysKey, { anysKey = it; store.putPref("anysearchApiKey", it) }, "AnySearch Key（可选；也可手动到 anysearch.com/console/api-keys）")
            anysMsg?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = TextMid)
            }
        }

        item {
            Text("云端语音识别（推荐，免本地模型）", style = MaterialTheme.typography.titleSmall, color = TextHigh)
            Spacer(Modifier.height(6.dp))
            Text("填写后语音走云端 API（OpenAI 兼容 /audio/transcriptions）→ 手机不再加载模型，更快更省电；留空则回退本地 Whisper。", style = MaterialTheme.typography.bodySmall, color = TextMid)
            Spacer(Modifier.height(6.dp))
            var asrKey by remember { mutableStateOf(store.pref("asrApiKey", "")) }
            var asrBase by remember { mutableStateOf(store.pref("asrBaseUrl", "https://api.siliconflow.cn/v1")) }
            var asrModel by remember { mutableStateOf(store.pref("asrModel", "TeleAI/TeleSpeechASR")) }
            Field(asrKey, { asrKey = it; store.putPref("asrApiKey", it) }, "ASR API Key（硅基流动 sk-… / 阿里云百炼 sk-… / 任何 OpenAI 兼容服务）")
            Field(asrBase, { asrBase = it; store.putPref("asrBaseUrl", it) }, "ASR Base URL（默认 https://api.siliconflow.cn/v1）")
            Field(asrModel, { asrModel = it; store.putPref("asrModel", it) }, "ASR 模型：硅基流动填 TeleAI/TeleSpeechASR（默认，免费）或 FunAudioLLM/SenseVoiceSmall；换阿里云百炼则 Base 填 https://dashscope.aliyuncs.com/compatible-mode/v1、模型填 qwen3-asr-flash（中文最强）")
        }

        item {
            Text("语音播报（少女音）", style = MaterialTheme.typography.titleSmall, color = TextHigh)
            Spacer(Modifier.height(6.dp))
            Text(
                "回答/解析结果自动朗读 · 状态：${com.learnagent.voice.MentaraVoice.status}。" +
                    "想要更「萝莉」的音色：系统设置→TTS/无障碍→下载少女语音包，App 会自动优先选中。",
                style = MaterialTheme.typography.bodySmall, color = TextMid
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("开启" to true, "关闭" to false).forEach { (label, v) ->
                    Surface(
                        color = if (voiceOn == v) Accent.copy(alpha = 0.18f) else Mist.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.clickable { AudioFx.tap(); voiceOn = v; com.learnagent.voice.MentaraVoice.setEnabled(context, v) }
                    ) { Text(label, style = MaterialTheme.typography.labelMedium, color = if (voiceOn == v) Accent else TextHigh, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) }
                }
            }
        }

        item {
            Text("后台保活（熄屏继续运行）", style = MaterialTheme.typography.titleSmall, color = TextHigh)
            Spacer(Modifier.height(6.dp))
            val pm = context.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
            val ignoring = runCatching { pm.isIgnoringBatteryOptimizations(context.packageName) }.getOrDefault(false)
            val svcRunning = com.learnagent.MentaraService.running
            Text(
                when {
                    svcRunning -> "保活服务：运行中 ✓（任务跑时状态栏应有「Mentara 后台学习中」通知）"
                    else -> "保活服务：未运行（有任务在跑时才启动）"
                } + (com.learnagent.MentaraService.lastStartError?.let { "；上次启动失败：$it" } ?: ""),
                style = MaterialTheme.typography.bodySmall, color = if (svcRunning) TextMid else MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (ignoring) "电池优化已忽略 ✓。注意：熄屏后任务仍停 → 一定是 ROM 省电策略：小米（应用信息→省电策略→无限制 + 锁屏不清理）；华为（应用启动管理→手动管理三项全开）；OPPO/vivo（允许后台运行）。解析大视频时插着充电线可彻底绕开深度休眠。"
                else "长任务已用「前台服务」保活，熄屏也会继续。建议再点下面按钮申请「忽略电池优化」；国产 ROM 还需在系统设置里允许 Mentara 后台运行/无限制。",
                style = MaterialTheme.typography.bodySmall, color = TextMid
            )
            Spacer(Modifier.height(6.dp))
            Surface(
                color = if (ignoring) Mist.copy(alpha = 0.3f) else Accent.copy(alpha = 0.18f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.clickable(enabled = !ignoring) {
                    AudioFx.tap()
                    runCatching {
                        context.startActivity(
                            android.content.Intent(
                                android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                android.net.Uri.parse("package:" + context.packageName)
                            )
                        )
                    }
                }
            ) { Text(if (ignoring) "已忽略电池优化 ✓" else "申请忽略电池优化（推荐）", style = MaterialTheme.typography.labelMedium, color = if (ignoring) TextMid else Accent, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) }
        }

        item {
            Text("数据", style = MaterialTheme.typography.titleSmall, color = TextHigh)
            Spacer(Modifier.height(6.dp))
            Surface(
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.16f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.clickable { AudioFx.tap(); wipeOpen = true }
            ) {
                Text("清空内容库（删除全部导入视频）", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
            }
        }
    }
}

@Composable
private fun Field(value: String, onValue: (String) -> Unit, label: String) {
    TextField(
        value = value,
        onValueChange = onValue,
        singleLine = true,
        placeholder = { Text(label, color = MoonSilver.copy(alpha = 0.6f)) },
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Mist.copy(alpha = 0.35f),
            unfocusedContainerColor = Mist.copy(alpha = 0.22f),
            focusedIndicatorColor = Accent,
            unfocusedIndicatorColor = Mist,
            cursorColor = Accent
        ),
        modifier = Modifier.fillMaxWidth()
    )
}
