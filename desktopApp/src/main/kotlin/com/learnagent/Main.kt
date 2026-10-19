package com.learnagent

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.State
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items as staggeredItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.learnagent.app.ReviewTracker
import com.learnagent.app.VideoActions
import com.learnagent.audio.PcmSynth
import com.learnagent.audio.SoundSynth
import com.learnagent.config.AppConfig
import com.learnagent.config.ConfigStore
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.derivedStateOf
import com.learnagent.design.DesignTokens
import com.learnagent.core.DebateEngine
import com.learnagent.core.DebateModels
import com.learnagent.core.DebateResult
import com.learnagent.core.Indexer
import com.learnagent.core.ContextAnalyzer
import com.learnagent.core.ContextualRecall
import com.learnagent.core.RecallBlock
import com.learnagent.core.TechCardExtractor
import com.learnagent.core.TechDecisionService
import com.learnagent.core.WeeklyReportGenerator
import com.learnagent.data.Decision
import com.learnagent.data.TechCard
import com.learnagent.core.LocalDebater
import com.learnagent.core.GraphBuilder
import com.learnagent.core.LocalQaEngine
import com.learnagent.core.QaAnswer
import com.learnagent.core.QaEngine
import com.learnagent.core.RemoteQaEngine
import com.learnagent.data.TimelineItem
import com.learnagent.platform.TechStore
import com.learnagent.llm.NvidiaLlmClient
import com.learnagent.ui.Abyss
import com.learnagent.ui.AbyssBlue
import com.learnagent.ui.Ambience
import com.learnagent.ui.Aureate
import com.learnagent.ui.BodyFont
import com.learnagent.ui.DisplayFont
import com.learnagent.ui.MonoFont
import com.learnagent.ui.BrandStyle
import com.learnagent.ui.cinematicLight
import com.learnagent.ui.HeroTitle
import com.learnagent.ui.GoldUnderline
import com.learnagent.ui.CinematicTag
import com.learnagent.ui.MicroMono
import com.learnagent.ui.CinematicDivider
import com.learnagent.ui.haloGlow
import com.learnagent.ui.CinematicDivider
import com.learnagent.ui.CinematicGold
import com.learnagent.ui.DawnSilver
import com.learnagent.ui.Bioluminescence
import com.learnagent.ui.FogGray
import com.learnagent.ui.Twilight
import com.learnagent.ui.DeepVoid
import com.learnagent.ui.BreakNotice
import com.learnagent.ui.BreathingOrb
import com.learnagent.ui.BurstParticles
import com.learnagent.ui.Coral
import com.learnagent.ui.ArrowLeftIcon
import com.learnagent.ui.DebateIcon
import com.learnagent.ui.DebatePanel
import com.learnagent.ui.DynamicBackground
import com.learnagent.ui.ErrorVeil
import com.learnagent.ui.Feedback
import com.learnagent.ui.Glimmer
import com.learnagent.ui.GlowBox
import com.learnagent.ui.GraphIcon
import com.learnagent.ui.ImportIcon
import com.learnagent.ui.Language
import com.learnagent.ui.KnowledgeGraph
import com.learnagent.ui.LearnAgentLogo
import com.learnagent.ui.LearnAgentTheme
import com.learnagent.ui.Mist
import com.learnagent.ui.MoonSilver
import com.learnagent.ui.Motion
import com.learnagent.ui.Phantom
import com.learnagent.ui.RotatingLogo
import com.learnagent.ui.SendIcon
import com.learnagent.ui.SplashScreen
import com.learnagent.ui.StarRating
import com.learnagent.ui.SubtitleStyle
import com.learnagent.ui.SuccessFlash
import com.learnagent.ui.SuccessGreen
import com.learnagent.ui.TextHigh
import com.learnagent.ui.TextMid
import com.learnagent.ui.TimelineIcon
import com.learnagent.ui.TimelineReplay
import com.learnagent.ui.TimestampStyle
import com.learnagent.ui.t
import com.learnagent.ui.Veil
import com.learnagent.ui.Void
import com.learnagent.ui.errorShake
import com.learnagent.ui.floatIn
import com.learnagent.ui.hoverSound
import com.learnagent.ui.pressScale
import com.learnagent.ui.CardShapes
import com.learnagent.ui.dollyIn
import com.learnagent.ui.dollyOut
import com.learnagent.ui.rackFocus
import com.learnagent.ui.rotation3D
import com.learnagent.ui.SceneTransitions
import com.learnagent.ui.breathing
import com.learnagent.ui.parallax
import com.learnagent.ui.rememberCardActivation
import com.learnagent.ui.DecisionDetailDialog
import com.learnagent.ui.DecisionsView
import com.learnagent.ui.DevAlertCard
import com.learnagent.ui.FollowUpDialog
import com.learnagent.ui.RecallBlockSection
import com.learnagent.ui.RecordDecisionDialog
import com.learnagent.ui.TechCardCompactDialog
import com.learnagent.ui.TechChipsRow
import com.learnagent.ui.TechMapView
import com.learnagent.ui.WeeklyReportView
import com.learnagent.ui.StudyTimelineView
import com.learnagent.ui.QuickRetrieveDialog
import com.learnagent.ui.StudyReviewView
import com.learnagent.ui.ReverseLearningView
import com.learnagent.ui.StandardPracticeView
import com.learnagent.ui.ExamView
import com.learnagent.ui.MasteryHeatView
import com.learnagent.ui.tilt3d
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.awt.datatransfer.DataFlavor
import javax.swing.JFileChooser
import javax.swing.TransferHandler
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import com.learnagent.core.ImportProgress
import com.learnagent.core.ImportQueue
import com.learnagent.core.ImportState
import com.learnagent.ui.LayoutBreakpoints
import com.learnagent.ui.LayoutMode

/** 批量导入队列（由 main() 注入；拖拽/文件夹/快捷键共用）。 */
val LocalImportQueue = staticCompositionLocalOf<ImportQueue?> { null }

/** 全局快捷键桥：Ctrl+Shift+K 唤起速查台。 */
object QuickHotkey {
    var tick by mutableStateOf(0)
        private set
    fun fire() { tick++ }
}

/** 跑分：进程内起始时刻（main() 首行附近）。 */
private val PROCESS_START_MS = System.currentTimeMillis()

/** 跑分：粒子压测开关（80 粒子会话时临时改 true，测完改回 false）。 */
private const val PERF_FORCE_PARTICLES_80 = false

private val MEDIA_EXTENSIONS = setOf("mp4", "mkv", "mov", "wav")

private fun isMediaFile(f: File): Boolean =
    f.isFile && !f.isHidden && !f.name.startsWith(".") && !f.name.startsWith("~$") &&
        f.extension.lowercase() in MEDIA_EXTENSIONS

/** 递归扫描媒体文件，过滤隐藏/系统文件，按修改时间排序。 */
private fun scanMediaFiles(dir: File): List<String> =
    dir.walkTopDown().filter { isMediaFile(it) }.sortedBy { it.lastModified() }.map { it.absolutePath }.toList()

/** 文件夹选择（目录模式）。 */
private fun pickFolder(): File? {
    val chooser = JFileChooser().apply {
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        dialogTitle = "Import Folder"
    }
    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
}

/** 拖拽展开：目录递归扫描，文件过滤扩展名。 */
private fun expandDropped(files: List<File>): List<String> =
    files.flatMap { f ->
        when {
            f.isDirectory -> scanMediaFiles(f)
            isMediaFile(f) -> listOf(f.absolutePath)
            else -> emptyList()
        }
    }

fun main() = application {
    val settingsOpen = remember { mutableStateOf(false) }
    val searchOpen = remember { mutableStateOf(false) }
    val importRequest = remember { mutableStateOf(0) }
    // 平台补强 A：批量导入队列（拖拽 / 文件夹 / 快捷键共用；串行处理，可取消）
    val importQueue = remember {
        ImportQueue { path ->
            ConfigStore.importVideo(ConfigStore.load(), File(path))
        }
    }
    Window(
        onCloseRequest = ::exitApplication,
        title = "Mentara",
        icon = painterResource("icon.png"),
        state = rememberWindowState(width = 1200.dp, height = 820.dp),
        onKeyEvent = { e ->
            when {
                e.type == KeyEventType.KeyDown && e.key == Key.Escape && searchOpen.value -> {
                    searchOpen.value = false
                    true
                }
                e.type == KeyEventType.KeyDown && e.key == Key.Escape && settingsOpen.value -> {
                    settingsOpen.value = false
                    true
                }
                e.type == KeyEventType.KeyDown && e.isCtrlPressed && e.key == Key.K -> {
                    searchOpen.value = true
                    true
                }
                e.type == KeyEventType.KeyDown && e.isCtrlPressed && e.isShiftPressed && e.key == Key.K -> {
                    QuickHotkey.fire()
                    true
                }
                e.type == KeyEventType.KeyDown && e.isCtrlPressed && e.key == Key.O -> {
                    importRequest.value++
                    true
                }
                else -> false
            }
        }
    ) {
        val loaded = remember { ConfigStore.load() }
        Feedback.enabled = loaded.soundEnabled
        SoundSynth.enabled = loaded.soundEnabled
        Language.current = loaded.language
        LaunchedEffect(Unit) { Feedback.window = window }
        // ---- 跑分探针：启动耗时（main→首帧）+ 60s 帧采样（只加日志，不改逻辑） ----
        LaunchedEffect(Unit) {
            withFrameNanos { }
            val startup = System.currentTimeMillis() - PROCESS_START_MS
            println("STARTUP_TIME_MS=$startup")
            System.out.flush()
            var frames = 0L
            var firstNs = 0L
            var lastNs = 0L
            while (true) {
                val ns = withFrameNanos { it }
                if (firstNs == 0L) firstNs = ns
                lastNs = ns
                frames++
                if (ns - firstNs >= 60_000_000_000L) break
            }
            val avgMs = (lastNs - firstNs) / 1_000_000.0 / (frames - 1).coerceAtLeast(1)
            println("AVG_FRAME_TIME_MS=" + String.format("%.2f", avgMs))
            println("FPS=" + String.format("%.1f", if (avgMs > 0) 1000.0 / avgMs else 0.0))
            System.out.flush()
        }
        // 平台补强 A：Swing 原生拖拽（多文件/文件夹）→ ImportQueue
        LaunchedEffect(Unit) {
            window.transferHandler = object : TransferHandler() {
                override fun canImport(support: TransferSupport): Boolean =
                    support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)

                override fun importData(support: TransferSupport): Boolean {
                    @Suppress("UNCHECKED_CAST")
                    val files = runCatching {
                        support.transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>
                    }.getOrNull() ?: return false
                    val paths = expandDropped(files)
                    if (paths.isNotEmpty()) importQueue.enqueue(paths)
                    return true
                }
            }
        }
        LaunchedEffect(Unit) {
            withContext(Dispatchers.Default) {
                SoundSynth.preload(
                    PcmSynth.Sound.HOVER, PcmSynth.Sound.TAP, PcmSynth.Sound.SUCCESS,
                    PcmSynth.Sound.ERROR, PcmSynth.Sound.OPEN, PcmSynth.Sound.CLOSE,
                    PcmSynth.Sound.SPLASH_TONE, PcmSynth.Sound.CHIME,
                    PcmSynth.Sound.DRONE, PcmSynth.Sound.WHOOSH
                )
            }
        }
        var themeMode by remember { mutableStateOf(loaded.themeMode) }
        var showSplash by remember { mutableStateOf(true) }
        LearnAgentTheme(darkTheme = themeMode != "light") {
            Crossfade(targetState = showSplash, animationSpec = tween(500), label = "splash") { splash ->
                if (splash) {
                    SplashScreen(onDone = { showSplash = false })
                } else {
                    CompositionLocalProvider(LocalImportQueue provides importQueue) {
                        AppRoot(
                            initialConfig = loaded,
                            onThemeChange = { themeMode = it },
                            settingsOpen = settingsOpen.value,
                            onSettingsOpen = { settingsOpen.value = it },
                            searchOpen = searchOpen.value,
                            onSearchOpen = { searchOpen.value = it },
                            importRequest = importRequest.value
                        )
                    }
                }
            }
        }
    }
}

private fun pickVideo(): File? {
    val dialog = FileDialog(Frame(), t("file_dialog_title"), FileDialog.LOAD)
    dialog.isVisible = true
    val dir = dialog.directory
    val name = dialog.file
    return if (dir != null && name != null) File(dir, name) else null
}

private fun formatTime(seconds: Double): String {
    val t = seconds.toInt()
    fun two(v: Int) = if (v < 10) "0$v" else "$v"
    return two(t / 60) + ":" + two(t % 60)
}

// ---------- 幻域无边框组件（DESIGN.md：卡片无描边，输入框只留底部一线，按钮靠色深分层） ----------

/** 无边框幽灵按钮：半透明微光底 + 透明度分层，不用实边框。 */
@Composable
private fun GhostButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.hoverSound(),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = Glimmer.copy(alpha = 0.48f),
            contentColor = TextHigh,
            disabledContainerColor = Glimmer.copy(alpha = 0.20f),
            disabledContentColor = MoonSilver.copy(alpha = 0.5f)
        ),
        content = content
    )
}

/** 无边框输入框：透明容器 + 只保留底部 1px 淡线（聚焦时变主色）。 */
@Composable
private fun VeilField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    placeholder: @Composable (() -> Unit)? = null,
    label: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = singleLine,
        placeholder = placeholder,
        label = label,
        trailingIcon = trailingIcon,
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Mist.copy(alpha = 0.35f),
            unfocusedContainerColor = Mist.copy(alpha = 0.22f),
            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
            unfocusedIndicatorColor = Mist,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MoonSilver,
            focusedPlaceholderColor = MoonSilver.copy(alpha = 0.6f),
            unfocusedPlaceholderColor = MoonSilver.copy(alpha = 0.6f)
        )
    )
}

/** 极淡渐变分割线：从透明到 0.1 透明度主色再到透明，不用实线。 */
@Composable
private fun MistDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        Color.Transparent
                    )
                )
            )
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AppRoot(
    initialConfig: AppConfig,
    onThemeChange: (String) -> Unit,
    settingsOpen: Boolean,
    onSettingsOpen: (Boolean) -> Unit,
    searchOpen: Boolean,
    onSearchOpen: (Boolean) -> Unit,
    importRequest: Int
) {
    var config by remember { mutableStateOf(initialConfig) }
    var videos by remember { mutableStateOf(ConfigStore.listVideos(initialConfig)) }
    var selected by remember {
        mutableStateOf(ConfigStore.loadLastSelected()?.takeIf { it in videos } ?: videos.firstOrNull())
    }
    var status by remember { mutableStateOf(t("status_ready")) }
    var tick by remember { mutableStateOf(0) }
    var importTick by remember { mutableStateOf(importRequest) }
    var search by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(0) }

    // ---- Developer Mode 第 2 批：技术地图 / 周报 / 决策回访 ----
    val devStore = remember { TechStore() }
    var techMapOpen by remember { mutableStateOf(false) }
    var quickOpen by remember { mutableStateOf(false) }
    var weeklyOpen by remember { mutableStateOf(false) }
    var decisionsOpen by remember { mutableStateOf(false) }
    var followUpOpen by remember { mutableStateOf(false) }
    val weeklyUnread = remember(tick, videos.size, config) {
        runCatching { WeeklyReportGenerator(devStore).isUnread() }.getOrDefault(false)
    }
    val pendingFollowUps = remember(tick, config) {
        runCatching { TechDecisionService.dueFollowUps(devStore).size }.getOrDefault(0)
    }

    // ---- 平台补强 A：批量导入进度（拖拽/文件夹共用队列） ----
    val importQueue = LocalImportQueue.current
    val importProgress = if (importQueue != null) {
        importQueue.progress.collectAsState(initial = ImportProgress(0, 0, "", ImportState.PENDING)).value
    } else ImportProgress(0, 0, "", ImportState.PENDING)
    var importDialogOpen by remember { mutableStateOf(false) }
    LaunchedEffect(importProgress.state, importProgress.doneCount, importProgress.failedCount) {
        when (importProgress.state) {
            ImportState.PENDING, ImportState.PROCESSING -> importDialogOpen = true
            ImportState.DONE, ImportState.CANCELLED -> {
                importDialogOpen = true
                if (importProgress.state == ImportState.DONE) {
                    videos = ConfigStore.listVideos(config)
                    if (selected != null && selected !in videos) selected = videos.firstOrNull()
                    tick++
                    status = "Imported ${importProgress.doneCount} file(s)" +
                        if (importProgress.failedCount > 0) ", ${importProgress.failedCount} failed" else ""
                }
            }
            else -> {}
        }
    }

    val videoMeta = remember(videos, tick, config) {
        videos.associateWith { id -> ConfigStore.loadTimeline(config, id) }
    }
    val unitCount = videoMeta.values.sumOf { it.size }

    fun refresh() {
        videos = ConfigStore.listVideos(config)
        if (selected != null && selected !in videos) selected = videos.firstOrNull()
    }

    fun doImport(file: File) {
        val id = ConfigStore.importVideo(config, file)
        refresh(); selected = id; tick++
        mode = 0
        status = t("status_imported", file.name)
    }

    LaunchedEffect(selected) {
        selected?.let { ConfigStore.saveLastSelected(it); ReviewTracker.markViewed(it) }
    }
    LaunchedEffect(importRequest) {
        if (importRequest > importTick) {
            importTick = importRequest
            pickVideo()?.let { doImport(it) }
        }
    }

    val stale = remember(videos, tick) { ReviewTracker.staleVideos(7) }

    // ---- 人为因素 + 幻域反馈状态（只影响视觉/音效，不影响逻辑） ----
    val rotateScope = rememberCoroutineScope()
    val paneRotation = remember { Animatable(0f) }
    var successTick by remember { mutableStateOf(0) }
    var errorTick by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            Ambience.tick()
        }
    }
    LaunchedEffect(videos.size) {
        Ambience.updateContentVolume(videos.size)
        if (PERF_FORCE_PARTICLES_80) Ambience.previewParticles(80)
    }
    val statusWatch = remember { mutableStateOf(status) }
    LaunchedEffect(status) {
        if (status != statusWatch.value) {
            statusWatch.value = status
            when {
                status.contains("failed") || status.contains("Failed") || status.contains("error") || status.contains("Error") -> {
                    errorTick++
                    Feedback.error()
                }
                status.contains("Imported") || status.contains("saved") || status.contains("done") || status.contains("saved") -> {
                    successTick++
                    Feedback.success()
                }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
    DynamicBackground(Modifier.fillMaxSize())
    Column(
        Modifier
            .fillMaxSize()
            .errorShake(errorTick)
            .graphicsLayer {
                rotationY = paneRotation.value
                cameraDistance = 60f * density
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        rotateScope.launch { paneRotation.animateTo(0f, Motion.softSpring) }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        rotateScope.launch {
                            paneRotation.snapTo((paneRotation.value + dragAmount * 0.05f).coerceIn(-15f, 15f))
                        }
                    }
                )
            }
            .onPointerEvent(PointerEventType.Press) { Ambience.noteInteraction() }
            .rackFocus(focused = !(techMapOpen || weeklyOpen || decisionsOpen || followUpOpen || importDialogOpen))
    ) {
        if (stale.isNotEmpty()) {
            val staleId = stale.first().first
            val days = stale.first().second
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(horizontal = DesignTokens.SpaceSectionDp.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        t("banner_stale", staleId, days.toString()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    FilledTonalButton(shape = RoundedCornerShape(12.dp), onClick = {
                        selected = staleId
                        ReviewTracker.markViewed(staleId)
                        status = t("status_review", staleId)
                    }) { Text(t("btn_review_now")) }
                }
            }
        }
        TopBar(search = search, onSearch = { search = it }, onSettings = { onSettingsOpen(true) })
        AnimatedContent(
            targetState = settingsOpen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) { showSettings ->
            if (showSettings) {
                SettingsScreen(
                    initial = config,
                    currentVideoId = selected,
                    onOpenTechMap = { techMapOpen = true },
                    onOpenWeekly = { weeklyOpen = true },
                    onOpenDecisions = { decisionsOpen = true },
                    onSave = { newCfg ->
                        config = newCfg
                        ConfigStore.save(newCfg)
                        onThemeChange(newCfg.themeMode)
                        refresh()
                        onSettingsOpen(false)
                        status = t("status_saved")
                    },
                    onCancel = { onSettingsOpen(false) }
                )
            } else {
                AnimatedContent(
                    targetState = selected,
                    transitionSpec = { SceneTransitions.listToDetail() },
                    label = "listToDetail"
                ) { sel ->
                if (sel == null) {
                    HomeGrid(
                        videos = videos,
                        videoMeta = videoMeta,
                        search = search,
                        onSelect = { selected = it; mode = 0 },
                        onImport = { file -> doImport(file) },
                        weeklyUnread = weeklyUnread,
                        onOpenWeekly = { weeklyOpen = true },
                        pendingFollowUps = pendingFollowUps,
                        onOpenFollowUps = { followUpOpen = true },
                        onOpenDecisions = { decisionsOpen = true },
                        onOpenTechMap = { techMapOpen = true },
                        onOpenQuick = { quickOpen = true }
                    )
                } else {
                    FocusArea(
                        config = config,
                        videoId = sel,
                        tick = tick,
                        mode = mode,
                        onMode = { mode = it },
                        onBack = { selected = null },
                        onStatus = { status = it },
                        onChanged = { refresh(); tick++; }
                    )
                }
                }
            }
        }
        StatusBar(status = status, videoCount = videos.size, unitCount = unitCount)
        if (searchOpen) {
            SearchDialog(
                config = config,
                videos = videos,
                onClose = { onSearchOpen(false) },
                onPick = { selected = it; mode = 0; onSearchOpen(false) }
            )
        }
        if (techMapOpen) {
            Dialog(onDismissRequest = { techMapOpen = false }) {
                Surface(shape = RoundedCornerShape(18.dp), color = Veil.copy(alpha = 0.97f)) {
                    Column(Modifier.width(900.dp).height(620.dp).padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Tech Map", style = MaterialTheme.typography.titleMedium, color = CinematicGold)
                            Spacer(Modifier.weight(1f))
                            FilledTonalButton(onClick = { techMapOpen = false }, shape = RoundedCornerShape(12.dp)) { Text("关闭") }
                        }
                        Spacer(Modifier.height(8.dp))
                        TechMapView(devStore, Modifier.fillMaxSize())
                    }
                }
            }
        }
        if (weeklyOpen) WeeklyReportView(devStore) { weeklyOpen = false }
        if (followUpOpen) FollowUpDialog(devStore) { followUpOpen = false }
        if (decisionsOpen) {
            Dialog(onDismissRequest = { decisionsOpen = false }) {
                Box(Modifier.width(760.dp).height(560.dp)) {
                    DecisionsView(devStore) { decisionsOpen = false }
                }
            }
        }
        LaunchedEffect(QuickHotkey.tick) { if (QuickHotkey.tick > 0) quickOpen = true }
        if (quickOpen) QuickRetrieveDialog { quickOpen = false }
        if (importDialogOpen) {
            Dialog(onDismissRequest = {
                if (importProgress.state != ImportState.PROCESSING && importProgress.state != ImportState.PENDING) {
                    importDialogOpen = false
                }
            }) {
                Surface(shape = RoundedCornerShape(18.dp), color = Veil.copy(alpha = 0.98f)) {
                    Column(Modifier.width(480.dp).padding(24.dp)) {
                        Text("Batch Import", style = MaterialTheme.typography.titleMedium, color = CinematicGold)
                        Spacer(Modifier.height(12.dp))
                        when (importProgress.state) {
                            ImportState.PENDING, ImportState.PROCESSING -> {
                                val finished = importProgress.doneCount + importProgress.failedCount
                                Text(
                                    "Importing ${(finished + 1).coerceAtMost(importProgress.total)} of ${importProgress.total}...",
                                    style = MaterialTheme.typography.bodyMedium, color = TextHigh
                                )
                                Spacer(Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = {
                                        if (importProgress.total > 0) finished.toFloat() / importProgress.total else 0f
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Processing: ${File(importProgress.currentFile).name.ifBlank { importProgress.currentFile }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MoonSilver,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            ImportState.CANCELLED -> Text(
                                "Import cancelled",
                                style = MaterialTheme.typography.bodyMedium, color = Coral
                            )
                            ImportState.DONE -> Text(
                                "${importProgress.doneCount} done, ${importProgress.failedCount} failed, " +
                                    "${(importProgress.total - importProgress.doneCount - importProgress.failedCount).coerceAtLeast(0)} pending",
                                style = MaterialTheme.typography.bodyMedium, color = TextHigh
                            )
                            else -> {}
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            if (importProgress.state == ImportState.PROCESSING || importProgress.state == ImportState.PENDING) {
                                GhostButton(onClick = { Feedback.tap(); importQueue?.cancelAll() }) { Text("Cancel") }
                            } else {
                                FilledTonalButton(onClick = { importDialogOpen = false }, shape = RoundedCornerShape(12.dp)) { Text("Close") }
                            }
                        }
                    }
                }
            }
        }
    }

    // ---- 幻域遮罩：内容消失在虚空中 ----
    Box(
        Modifier
            .fillMaxWidth()
            .height(72.dp)
            .align(Alignment.TopCenter)
            .background(Brush.verticalGradient(listOf(Void.copy(alpha = 0.72f), Color.Transparent)))
    )
    Box(
        Modifier
            .fillMaxWidth()
            .height(72.dp)
            .align(Alignment.BottomCenter)
            .background(Brush.verticalGradient(listOf(Color.Transparent, Void.copy(alpha = 0.72f))))
    )

    // ---- 成功光带 / 错误泛光 / 休息提醒 ----
    SuccessFlash(successTick, Modifier.align(Alignment.TopCenter))
    ErrorVeil(errorTick, Modifier.align(Alignment.Center))
    if (Ambience.breakNotice) {
        BreakNotice(onDismiss = { Ambience.dismissBreakNotice() }, modifier = Modifier.align(Alignment.TopCenter))
    }
    }
}

@Composable
fun TopBar(search: String, onSearch: (String) -> Unit, onSettings: () -> Unit) {
    Surface(color = Veil.copy(alpha = 0.45f)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LearnAgentLogo(size = 36)
            Spacer(Modifier.width(14.dp))
            Text(
                "Mentara",
                style = BrandStyle,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.weight(1f))
            VeilField(
                value = search,
                onValueChange = onSearch,
                singleLine = true,
                placeholder = { Text(t("search_placeholder")) },
                trailingIcon = {
                    Text(
                        "CTRL+K",
                        style = TimestampStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                },
                modifier = Modifier.width(400.dp)
            )
            Spacer(Modifier.weight(1f))
            FilledTonalButton(
                onClick = onSettings,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.hoverSound()
            ) { Text(t("btn_settings")) }
        }
    }
}

@Composable
fun StatusBar(status: String, videoCount: Int, unitCount: Int) {
    val dot = when {
        status.contains("failed") || status.contains("Failed") || status.contains("error") || status.contains("Error") -> Coral
        status.contains("...") || status.contains("ing") -> Aureate
        else -> SuccessGreen
    }
    Surface(color = Veil.copy(alpha = 0.40f)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = DesignTokens.SpaceSectionDp.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center) {
                    Box(Modifier.size(14.dp).background(dot.copy(alpha = 0.22f), CircleShape))
                    Box(Modifier.size(7.dp).background(dot, CircleShape))
                }
                Spacer(Modifier.width(8.dp))
                Text(status, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(t("status_counts", videoCount.toString(), unitCount.toString()), style = TimestampStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Text("OFFLINE", style = TimestampStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("CTRL+K", style = TimestampStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalLayoutApi::class)
@Composable
fun HomeGrid(
    videos: List<String>,
    videoMeta: Map<String, List<TimelineItem>>,
    search: String,
    onSelect: (String) -> Unit,
    onImport: (File) -> Unit,
    weeklyUnread: Boolean = false,
    onOpenWeekly: () -> Unit = {},
    pendingFollowUps: Int = 0,
    onOpenFollowUps: () -> Unit = {},
    onOpenDecisions: () -> Unit = {},
    onOpenTechMap: () -> Unit = {},
    onOpenQuick: () -> Unit = {}
) {
    val shown = if (search.isBlank()) videos else videos.filter { it.contains(search, ignoreCase = true) }
    var mouseX by remember { mutableStateOf(0f) }
    var gridWidth by remember { mutableStateOf(0f) }
    Column(
        modifier = Modifier.fillMaxSize().padding(DesignTokens.SpaceSectionDp.dp).dollyOut(true),
        horizontalAlignment = Alignment.Start
        ) {
        Spacer(Modifier.height(48.dp))
        Text(
            "Welcome back",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(4.dp))
        Row {
            Text(
                videos.size.toString(),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Text(
                if (videos.size == 1) "video" else "videos",
                style = MaterialTheme.typography.headlineMedium,
                color = MoonSilver
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            t("home_sub"),
            modifier = Modifier.padding(start = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        // ---- Developer Mode：周报 / 决策回访 提醒 + 入口 ----
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (weeklyUnread) DevAlertCard("本周报告已生成", "查看") { onOpenWeekly() }
            if (pendingFollowUps > 0) DevAlertCard("你有 $pendingFollowUps 个决策待回访", "回访") { onOpenFollowUps() }
        }
        if (weeklyUnread || pendingFollowUps > 0) Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            GhostButton(onClick = onOpenDecisions) { Text("Decisions") }
            GhostButton(onClick = onOpenTechMap) { Text("Tech Map") }
            GhostButton(onClick = onOpenQuick) { Text("速查") }
            val importQueue = LocalImportQueue.current
            GhostButton(onClick = {
                Feedback.tap()
                pickFolder()?.let { dir -> importQueue?.enqueue(scanMediaFiles(dir)) }
            }) { Text("Import Folder") }
        }
        Spacer(Modifier.height(24.dp))
        GlassImportButton { pickVideo()?.let(onImport) }
        Spacer(Modifier.height(24.dp))
        if (shown.isEmpty() && search.isBlank()) {
            Spacer(Modifier.weight(0.7f))
            Box(Modifier.breathing()) {
                RotatingLogo(size = 190.dp)
            }
            Spacer(Modifier.height(30.dp))
            Text(
                "Begin Your Journey",
                style = SubtitleStyle.copy(fontSize = 17.sp, letterSpacing = 5.sp),
                color = MoonSilver
            )
            Spacer(Modifier.height(12.dp))
            Text(
                t("home_empty_sub"),
                style = MaterialTheme.typography.bodyMedium,
                color = TextMid
            )
            Spacer(Modifier.weight(1f))
        } else if (shown.isEmpty()) {
            Text(
                t("home_no_match", search),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Adaptive(minSize = 250.dp),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpaceGroupDp.dp),
                verticalItemSpacing = DesignTokens.SpaceGroupDp.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .onSizeChanged { gridWidth = it.width.toFloat() }
                    .onPointerEvent(PointerEventType.Move) { e ->
                        e.changes.firstOrNull()?.let { ch -> mouseX = ch.position.x }
                    }
                    .parallax(mouseX, gridWidth / 2f)
            ) {
                staggeredItems(shown) { id ->
                    val meta = videoMeta[id].orEmpty()
                    VideoCard(
                        id = id,
                        segments = meta.size,
                        seconds = meta.sumOf { (it.end - it.start).coerceAtLeast(0.0) },
                        onClick = { onSelect(id) }
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** 导入按钮协调化：玻璃质感、背景渐变微移（200ms）、字母大写 + 2sp 间距 + TAP 音效。 */
@Composable
private fun GlassImportButton(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val shift by animateFloatAsState(
        targetValue = if (hovered) 1f else 0f,
        animationSpec = Motion.standard,
        label = "importShift"
    )
    Box(
        modifier = Modifier
            .hoverSound()
            .hoverable(interaction)
            .clip(CardShapes.importCard())
            .clickable { Feedback.tap(); onClick() }
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)
                    ),
                    startX = -200f + shift * 300f,
                    endX = 300f + shift * 300f
                )
            )
            .padding(horizontal = 22.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ImportIcon(18)
            Spacer(Modifier.width(10.dp))
            Text(
                t("btn_import"),
                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp),
                color = TextHigh
            )
        }
    }
}

@Composable
fun VideoCard(id: String, segments: Int, seconds: Double, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    // 卡片激活态：hover（桌面）/pressed（触屏回退）统一 0..1，200ms
    val activation = rememberCardActivation(interaction)
    val goldBorder by animateDpAsState(
        targetValue = if (activation > 0f) 1.dp else 0.dp,
        animationSpec = tween(200),
        label = "cardGoldBorder"
    )
    val stagger = (id.hashCode() and 0x7FFFFFFF) % 150
    val wobbleDp = ((id.hashCode() and 0x7FFF) % 17) - 8   // ±8dp 微错落
    val freshness = when (val d = remember(id) { ReviewTracker.daysSince(id) }) {
        null -> 1f
        else -> (1f - d / 7f).coerceIn(0.10f, 1f)
    }
    val accentPalette = listOf(
        Color(0xFF4A7AB8), Color(0xFFB8A8D4), Color(0xFF7FB89C), Color(0xFFE8C87A), Color(0xFFE89A9A)
    )
    val accentColor = accentPalette[(id.hashCode() and 0x7FFFFFFF) % accentPalette.size]
    val enterRotation = remember { Animatable(-15f) }
    LaunchedEffect(Unit) { enterRotation.animateTo(0f, tween(400, easing = FastOutSlowInEasing)) }
    GlowBox(
        glow = if (hovered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
        intensity = 0.30f + 0.70f * activation,
        radius = 170.dp,
        modifier = Modifier.fillMaxWidth().floatIn(delayMillis = stagger)
    ) {
        Surface(
            color = Color(0xFF243044).copy(alpha = 0.95f),
            shape = CardShapes.videoCard(),
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationY = wobbleDp.dp.toPx()
                    rotationZ = ((id.hashCode() and 0x3) - 1) * 0.3f   // ±0.3° 微倾
                    scaleX = 1f + 0.025f * activation
                    scaleY = 1f + 0.025f * activation
                }
                .rotation3D(enterRotation.value)
                .border(goldBorder, CinematicGold.copy(alpha = 0.55f * activation), CardShapes.videoCard())
                .pressScale(interaction)
                .hoverSound()
                .hoverable(interaction)
                .clickable { Feedback.tap(); onClick() }
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF060A0F).copy(alpha = 0.55f - 0.47f * activation),
                                Color(0xFF060A0F).copy(alpha = 0.16f - 0.14f * activation)
                            )
                        )
                    )
            ) {
                // 三分法构图：缩略图左上，标题右上
                Row(
                    Modifier.fillMaxWidth().padding(
                        top = 28.dp, bottom = 28.dp,
                        start = 28.dp, end = 28.dp
                    ),
                    verticalAlignment = Alignment.Top
                ) {
                    // 封面：默认暗滤镜（能看出轮廓）→ 悬停明显提亮 + 金色描边/光晕
                    Box(
                        Modifier
                            .size(width = 140.dp, height = 84.dp)
                            .graphicsLayer {
                                rotationZ = -2f
                                shadowElevation = 4.dp.toPx()
                            }
                            .clip(RoundedCornerShape(10.dp))
                    ) {
                        // 程序化封面底：冷调画面 + 柔光，避免纯黑
                        Canvas(
                            Modifier
                                .matchParentSize()
                                .graphicsLayer { alpha = 0.85f + 0.15f * activation }
                        ) {
                            drawRect(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF3C5878),
                                        Color(0xFF243449),
                                        Color(0xFF17202E)
                                    )
                                )
                            )
                            drawRect(
                                brush = Brush.verticalGradient(
                                    listOf(accentColor.copy(alpha = 0.85f), accentColor.copy(alpha = 0.12f))
                                ),
                                size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.34f)
                            )
                            drawCircle(
                                brush = Brush.radialGradient(
                                    listOf(Color.White.copy(alpha = 0.22f), Color.Transparent)
                                ),
                                radius = size.width * 0.50f,
                                center = Offset(size.width * 0.30f, size.height * 0.30f)
                            )
                            drawCircle(
                                brush = Brush.radialGradient(
                                    listOf(Aureate.copy(alpha = 0.26f), Color.Transparent)
                                ),
                                radius = size.width * 0.42f,
                                center = Offset(size.width * 0.80f, size.height * 0.72f)
                            )
                        }
                        // 原三点缩略图（批次 1 视觉不变）
                        MiniThumb(seed = id)
                        // 原右下角暖金光晕（保留）
                        Canvas(Modifier.fillMaxSize()) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    listOf(Aureate.copy(alpha = 0.12f), Color.Transparent),
                                    center = Offset(size.width * 0.8f, size.height * 0.8f),
                                    radius = size.width * 0.4f
                                ),
                                radius = size.width * 0.4f,
                                center = Offset(size.width * 0.8f, size.height * 0.8f)
                            )
                        }
                        // 暗滤镜：AbyssBlue 0.65 → 0.20（悬停）
                        Box(
                            Modifier
                                .matchParentSize()
                                .background(AbyssBlue.copy(alpha = 0.45f - 0.40f * activation))
                        )
                        // 悬停金色描边：0dp → 1dp
                        Box(
                            Modifier
                                .matchParentSize()
                                .border(goldBorder, CinematicGold.copy(alpha = 0.90f * activation), RoundedCornerShape(10.dp))
                        )
                        // 悬停径向金色 halo：强度 0 → 1
                        Canvas(Modifier.matchParentSize()) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    listOf(Aureate.copy(alpha = 0.30f * activation), Color.Transparent),
                                    center = center,
                                    radius = size.maxDimension * 0.75f
                                ),
                                radius = size.maxDimension * 0.75f,
                                center = center
                            )
                        }
                    }
                    Spacer(Modifier.width(20.dp))
                    // 标题区：缩略图顶部下移 8dp（三分点位置）；标题在暗滤镜上层，保持可读
                    Column(Modifier.weight(1f).padding(top = 8.dp)) {
                        Text(
                            id,
                            style = androidx.compose.ui.text.TextStyle(
                                fontFamily = BodyFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                                letterSpacing = 0.5.sp,
                                shadow = androidx.compose.ui.graphics.Shadow(
                                    color = Color.Black.copy(alpha = 0.6f),
                                    offset = Offset(1f, 1f),
                                    blurRadius = 5f
                                )
                            ),
                            color = Color(0xFFF7E7B4),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.haloGlow(Aureate, radiusPx = 60f, alpha = 0.08f + 0.10f * activation)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (segments > 0) "${segments} segments" else t("home_not_indexed"),
                            style = androidx.compose.ui.text.TextStyle(
                                fontFamily = DisplayFont,
                                fontWeight = FontWeight.Light,
                                fontSize = 13.sp,
                                letterSpacing = 1.sp
                            ),
                            color = DawnSilver,
                            maxLines = 1
                        )
                    }
                }
                // 虚线分隔
                CinematicDivider(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp)
                        .align(Alignment.CenterStart)
                        .offset(y = 100.dp)
                )
                // 底部行：时间戳左下 / 进度条右下 / 标签最右
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 28.dp, end = 28.dp, bottom = 16.dp)
                        .align(Alignment.BottomStart),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // 时长：Mono 左下
                    Text(
                        formatTime(seconds),
                        style = androidx.compose.ui.text.TextStyle(
                            fontFamily = MonoFont,
                            fontWeight = FontWeight.Normal,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = FogGray
                    )
                    Spacer(Modifier.weight(1f))
                    // 新鲜度条：从 40% 开始，占 40%
                    Box(
                        Modifier
                            .weight(0.4f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Mist.copy(alpha = 0.3f))
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(freshness)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Bioluminescence.copy(alpha = 0.6f), Aureate.copy(alpha = 0.8f))
                                    )
                                )
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    // YESTERDAY → 最右下
                    Text(
                        "OPEN →",
                        style = androidx.compose.ui.text.TextStyle(
                            fontFamily = BodyFont,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            letterSpacing = 2.sp
                        ),
                        color = Bioluminescence
                    )
                }
                // 暗角

            }
        }
    }
}

/** 微妙的程序化"缩略图"：用 Logo 同源的三点结构生成微缩视觉，不依赖真实截图。 */
@Composable
private fun MiniThumb(seed: String) {
    val accent = MaterialTheme.colorScheme.primary
    Canvas(Modifier.size(width = 52.dp, height = 38.dp).clip(RoundedCornerShape(9.dp))) {
        drawRect(Brush.linearGradient(listOf(Glimmer, Abyss)))
        val t1 = Offset(size.width * 0.28f, size.height * 0.36f)
        val t2 = Offset(size.width * 0.64f, size.height * 0.30f)
        val t3 = Offset(size.width * 0.44f, size.height * 0.72f)
        drawLine(accent.copy(alpha = 0.4f), t1, t3, strokeWidth = 1f)
        drawLine(accent.copy(alpha = 0.4f), t2, t3, strokeWidth = 1f)
        val a = 0.5f + ((seed.hashCode() and 0x7FFF) % 5) * 0.1f
        drawCircle(accent.copy(alpha = a), radius = 3.2f, center = t1)
        drawCircle(Aureate.copy(alpha = a), radius = 2.4f, center = t2)
        drawCircle(accent.copy(alpha = a), radius = 4f, center = t3)
    }
}

@Composable
fun FocusArea(
    config: AppConfig,
    videoId: String,
    tick: Int,
    mode: Int,
    onMode: (Int) -> Unit,
    onBack: () -> Unit,
    onStatus: (String) -> Unit,
    onChanged: () -> Unit
) {
    var selIdx by remember { mutableStateOf<Int?>(null) }
    val timeline = remember(videoId, tick) { ConfigStore.loadTimeline(config, videoId) }

    // ---- 四轮辩论状态（宿主内持有，切标签不丢失；含持久化恢复与协程取消） ----
    val debateScope = rememberCoroutineScope()
    var debateResult by remember(videoId) {
        mutableStateOf<DebateResult?>(runCatching { ConfigStore.loadDebate(config, videoId) }.getOrNull())
    }
    var debateBusy by remember { mutableStateOf(false) }
    var debatePhase by remember { mutableStateOf(0) }
    var debateJob by remember { mutableStateOf<Job?>(null) }
    var debateTopic by remember(videoId) { mutableStateOf(t("debate_topic_default", videoId)) }
    val debateOnline = config.nvidiaApiKey.isNotBlank()

    fun cancelDebate() {
        debateJob?.cancel()
        debateJob = null
        onStatus(t("status_debate_cancelled"))
    }

    fun startDebate(topic: String) {
        if (debateBusy) return
        if (timeline.isEmpty()) {
            onStatus(t("status_need_content"))
            return
        }
        Feedback.tap()
        val context = timeline
            .sortedByDescending { (it.speech + it.screenText).length }
            .take(8)
            .joinToString("。") { (it.speech + " " + it.screenText).trim() }
        debateBusy = true
        debatePhase = 1
        onStatus(t("status_debate_running"))
        debateJob = debateScope.launch {
            try {
                val engine = withContext(Dispatchers.IO) {
                    if (debateOnline) {
                        DebateEngine(
                            NvidiaLlmClient(
                                config.nvidiaApiKey,
                                model = DebateModels.resolve(config.debateModelA, DebateModels.DEFAULT_A)
                            ),
                            NvidiaLlmClient(
                                config.nvidiaApiKey,
                                model = DebateModels.resolve(config.debateModelB, DebateModels.DEFAULT_B)
                            ),
                            NvidiaLlmClient(
                                config.nvidiaApiKey,
                                model = DebateModels.resolve(config.debateModelC, DebateModels.DEFAULT_C)
                            )
                        )
                    } else {
                        DebateEngine(
                            LocalDebater("概述", timeline),
                            LocalDebater("挑战", timeline),
                            LocalDebater("证据", timeline)
                        )
                    }
                }
                // LLM 调用是阻塞 IO，必须在后台执行；onRound 流式推送部分结果
                val result = withContext(Dispatchers.IO) {
                    engine.debate(
                        topic, context,
                        onPhase = { debatePhase = it },
                        onRound = { round, partial ->
                            debateResult = partial
                            if (round >= 2) Feedback.tap()
                        }
                    )
                }
                debateResult = result
                withContext(Dispatchers.IO) { runCatching { ConfigStore.saveDebate(config, videoId, result) } }
                Feedback.success()
                onStatus(t("status_debate_done", ((result.agreement * 100).toInt()).toString()))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Feedback.error()
                onStatus(t("status_debate_failed", e.message.toString()))
            } finally {
                debateBusy = false
                debatePhase = 0
                debateJob = null
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(DesignTokens.SpaceSectionDp.dp).dollyIn(true)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ModeIconButton(selected = false, onClick = onBack) {
                ArrowLeftIcon(20)
            }
            Text(
                videoId,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            ModeIconButton(selected = mode == 0, onClick = { onMode(0) }) { TimelineIcon(22) }
            ModeIconButton(selected = mode == 1, onClick = { onMode(1) }) { GraphIcon(22) }
            ModeIconButton(selected = mode == 2, onClick = { onMode(2) }) { SendIcon(22) }
            ModeIconButton(selected = mode == 3, onClick = { onMode(3) }) { DebateIcon(22) }
        }
        // 辩论进行时的顶部迷你进度条（不打断其他操作）
        AnimatedVisibility(visible = debateBusy, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(2.dp)
                    .background(Mist.copy(alpha = 0.5f), RoundedCornerShape(1.dp))
            ) {
                val progress = (debatePhase.coerceIn(0, 3) / 3f).coerceIn(0.05f, 1f)
                Box(
                    Modifier
                        .fillMaxWidth(progress)
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(listOf(Phantom, Aureate)),
                            RoundedCornerShape(1.dp)
                        )
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        AnimatedContent(
            targetState = mode,
            transitionSpec = { SceneTransitions.contentToGraph() },
            modifier = Modifier.weight(1f).fillMaxWidth(),
            label = "focusMode"
        ) { m ->
            when (m) {
                1 -> KnowledgeGraph(
                    items = timeline,
                    selectedIndex = selIdx,
                    onNodeClick = { idx -> selIdx = idx; onStatus(t("status_located", formatTime(timeline[idx].start))) },
                    modifier = Modifier.fillMaxSize()
                )
                2 -> QaPanel(config, videoId, tick, Modifier.fillMaxSize()) {
                    onMode(3)
                }
                3 -> DebatePanel(
                    result = debateResult,
                    busy = debateBusy,
                    phase = debatePhase,
                    modelNames = if (debateOnline) {
                        Triple("Kimi K3（NIM）", "DeepSeek V4.1（NIM）", "GLM 5.3（NIM）")
                    } else {
                        Triple(t("debate_local_a"), t("debate_local_b"), t("debate_local_c"))
                    },
                    online = debateOnline,
                    topic = debateTopic,
                    onTopicChange = { debateTopic = it },
                    onRerun = { startDebate(debateTopic) },
                    onCancel = { cancelDebate() },
                    modifier = Modifier.fillMaxSize()
                )
                else -> BoxWithConstraints(Modifier.fillMaxSize()) {
                    // 平台补强 A：TABLET_FULL（≥900dp）才显示右侧问答栏
                    val showQa = LayoutBreakpoints.forWidth(maxWidth.value.toInt()) == LayoutMode.TABLET_FULL
                    Row(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(if (showQa) 0.7f else 1f).fillMaxHeight()) {
                            ContentCards(
                                config, videoId, timeline, onStatus, onChanged,
                                onDeepAnalysis = { if (debateBusy) cancelDebate() else startDebate(debateTopic) },
                                debateBusy = debateBusy
                            )
                        }
                        if (showQa) {
                            Spacer(Modifier.width(16.dp))
                            Box(Modifier.weight(0.3f).fillMaxHeight()) {
                                QaPanel(config, videoId, tick, Modifier.fillMaxSize()) {
                                    onMode(3)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModeIconButton(selected: Boolean, onClick: () -> Unit, icon: @Composable () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val bg = when {
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        hovered -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        else -> Color.Transparent
    }
    val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .size(40.dp)
            .pressScale(interaction)
            .hoverSound()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .hoverable(interaction)
            .clickable { Feedback.tap(); onClick() },
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides tint) { icon() }
    }
}

// ============================================================
//  螺旋线滚动路径（批次 2）
//  条目按视口垂直位置 focus ∈ [-1,1] → 横向正弦偏移 + Z 轴旋转
// ============================================================
private val SpiralAmplitudeX = 20.dp              // 横向正弦幅度（允许 16~24dp）
private const val SpiralAmplitudeRotDeg = 1.5f    // Z 轴旋转幅度（允许 1.2°~1.8°）
private const val SpiralMaxShrink = 0.02f         // 边缘微缩上限
private const val SpiralFastScale = 0.3f          // 快速滚动时衰减到 30%
private const val SpiralFastSpeedPxPerSec = 1200f // “快速滚动”阈值（px/s）
private const val SpiralRestoreDelayMs = 300L     // 停止滚动后恢复延迟
private const val SpiralRestoreMs = 300           // 恢复动画时长
private const val SpiralDemoLongPressMs = 3000L   // 演示模式长按时长
private const val SpiralDemoStepPx = 9f           // 演示自动滚动步长（≈560px/s）

/**
 * 螺旋线路径容器：只做 graphicsLayer 绘制变换，不改布局尺寸、不拦截点击。
 * focus 通过 derivedStateOf 基于可见条目计算；graphicsLayer 内读取 State，
 * 滚动每帧只更新绘制层，不触发 item 重组。
 */
@Composable
private fun SpiralScrollItem(
    listState: LazyListState,
    itemKey: Any,
    amplitudeScale: State<Float>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val focus by remember(listState, itemKey) {
        derivedStateOf {
            val info = listState.layoutInfo
            val viewportHeight = (info.viewportEndOffset - info.viewportStartOffset).toFloat()
            if (viewportHeight <= 0f) return@derivedStateOf 0f
            val viewportCenterY = (info.viewportStartOffset + info.viewportEndOffset) / 2f
            val visible = info.visibleItemsInfo.firstOrNull { it.key == itemKey }
                ?: return@derivedStateOf 0f
            val itemCenterY = visible.offset + visible.size / 2f
            ((itemCenterY - viewportCenterY) / (viewportHeight / 2f)).coerceIn(-1f, 1f)
        }
    }
    Box(
        modifier = modifier.graphicsLayer {
            val scale = amplitudeScale.value
            translationX = sin(focus * PI.toFloat()) * SpiralAmplitudeX.toPx() * scale
            rotationZ = focus * SpiralAmplitudeRotDeg * scale
            val shrink = 1f - abs(focus) * SpiralMaxShrink
            scaleX = shrink
            scaleY = shrink
        }
    ) {
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContentCards(
    config: AppConfig,
    videoId: String,
    timeline: List<TimelineItem>,
    onStatus: (String) -> Unit,
    onChanged: () -> Unit,
    onDeepAnalysis: () -> Unit = {},
    debateBusy: Boolean = false
) {
    val scope = rememberCoroutineScope()
    val source = remember(videoId) { ConfigStore.sourceFile(config, videoId) }
    val listState = rememberLazyListState()
    var timelineOpen by remember { mutableStateOf(false) }

    // ---- 螺旋线速度自适应：快速滚动衰减到 30%，停止 300ms 平滑恢复 ----
    var fastScrolling by remember { mutableStateOf(false) }
    LaunchedEffect(listState) {
        var prevIndex = listState.firstVisibleItemIndex
        var prevOffset = listState.firstVisibleItemScrollOffset
        var prevTime = System.currentTimeMillis()
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                val now = System.currentTimeMillis()
                val dt = (now - prevTime).coerceAtLeast(16L)
                val itemSize = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 1
                val delta = abs((index - prevIndex) * itemSize + (offset - prevOffset))
                if (delta * 1000f / dt > SpiralFastSpeedPxPerSec) fastScrolling = true
                prevIndex = index
                prevOffset = offset
                prevTime = now
            }
    }
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { scrolling ->
                if (!scrolling) {
                    delay(SpiralRestoreDelayMs)
                    if (!listState.isScrollInProgress) fastScrolling = false
                }
            }
    }
    val spiralAmplitudeScale = animateFloatAsState(
        targetValue = if (fastScrolling) SpiralFastScale else 1f,
        animationSpec = tween(SpiralRestoreMs),
        label = "spiralAmplitudeScale"
    )

    // ---- 演示模式：长按 HERO 标题 3 秒 → 自动滚动（循环），便于录屏 ----
    var demoMode by remember { mutableStateOf(false) }
    LaunchedEffect(demoMode) {
        while (demoMode) {
            if (listState.canScrollForward) {
                listState.animateScrollBy(SpiralDemoStepPx, tween(16, easing = LinearEasing))
            } else {
                listState.scrollToItem(0)
                delay(700)
            }
        }
    }

    // ---- 滚动进度（0..1，derivedStateOf 避免每帧重组） ----
    val scrollFraction by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            if (info.totalItemsCount <= 1) 0f
            else listState.firstVisibleItemIndex.toFloat() / (info.totalItemsCount - 1).coerceAtLeast(1)
        }
    }

    // ---- 三层镜头结构 ----
    Box(Modifier.fillMaxSize()) {
        // LAYER 1：固定背景光（反向视差 -16dp..+16dp）
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer {
                    translationY = -scrollFraction * 16.dp.toPx()
                }
                .cinematicLight(intensity = 0.10f)
                .let { it }
        ) {
            // Vignette（固定，不随滚动移动）
            Canvas(Modifier.matchParentSize()) {
                val c = Offset(size.width / 2f, size.height / 2f)
                val r = size.maxDimension * 0.75f
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color.Transparent, Color.Transparent, DeepVoid.copy(alpha = 0.25f)),
                        center = c, radius = r
                    ),
                    radius = r, center = c
                )
            }
        }

        // LAYER 2：中景氛围（0.6 速度——halation 光斑 + 雾）
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer {
                    translationY = -scrollFraction * 8.dp.toPx()
                    alpha = 1f - scrollFraction * 0.3f   // 越滚越淡
                }
        ) {
            Canvas(Modifier.matchParentSize()) {
                // 暖光斑（右上——Deakins 式侧光）
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Aureate.copy(alpha = 0.06f), Color.Transparent),
                        center = Offset(size.width * 0.82f, size.height * 0.15f),
                        radius = size.width * 0.3f
                    ),
                    radius = size.width * 0.3f,
                    center = Offset(size.width * 0.82f, size.height * 0.15f)
                )
                // 冷光补光（左下——Storaro 式冷蓝）
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF4A7AB8).copy(alpha = 0.04f), Color.Transparent),
                        center = Offset(size.width * 0.15f, size.height * 0.85f),
                        radius = size.width * 0.35f
                    ),
                    radius = size.width * 0.35f,
                    center = Offset(size.width * 0.15f, size.height * 0.85f)
                )
                // 荧光点（中上——Blade Runner 式生物荧光）
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Bioluminescence.copy(alpha = 0.03f), Color.Transparent),
                        center = Offset(size.width * 0.55f, size.height * 0.45f),
                        radius = size.width * 0.2f
                    ),
                    radius = size.width * 0.2f,
                    center = Offset(size.width * 0.55f, size.height * 0.45f)
                )
            }
        }

        // LAYER 3：前景内容（正常速度——LazyColumn + 动作按钮）
        Column(Modifier.fillMaxSize()) {
            // ---- 动作按钮行 ----
            if (source != null) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(start = 24.dp, end = 48.dp, top = 8.dp)
                ) {
                    FilledTonalButton(
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.hoverSound(),
                        onClick = {
                            Feedback.tap()
                            onStatus(t("status_transcribing"))
                            scope.launch {
                                val msg = withContext(Dispatchers.IO) {
                                    try { VideoActions.transcribe(config, videoId) } catch (e: Exception) { t("status_transcribe_failed", e.message.toString()) }
                                }
                                onStatus(msg)
                                onChanged()
                            }
                        }
                    ) { Text(t("btn_transcribe")) }
                    FilledTonalButton(
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.hoverSound(),
                        onClick = {
                            Feedback.tap()
                            onStatus(t("status_ocr"))
                            scope.launch {
                                val msg = withContext(Dispatchers.IO) {
                                    try { VideoActions.ocrFrames(config, videoId) } catch (e: Exception) { t("status_ocr_failed", e.message.toString()) }
                                }
                                onStatus(msg)
                                onChanged()
                            }
                        }
                    ) { Text(t("btn_ocr")) }
                    GhostButton(
                        onClick = { if (timeline.isEmpty()) onStatus(t("status_need_content")) else onDeepAnalysis() }
                    ) {
                        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) { DebateIcon(16) }
                        Spacer(Modifier.width(6.dp))
                        Text(if (debateBusy) t("btn_cancel_debate") else t("btn_deep_analysis"))
                    }
                    GhostButton(onClick = { Feedback.tap(); timelineOpen = true }) { Text(t("学习时间线")) }
                }
                // 镜头切换：金线展开
                Spacer(Modifier.height(12.dp))
                Row(Modifier.padding(start = 24.dp, end = 48.dp)) {
                    GoldUnderline(width = 96.dp, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
            }

            if (timeline.isEmpty()) {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(t("content_no_results"), style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (source == null) t("content_source_missing") else t("content_no_results_hint"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // ---- 前景 LazyColumn：非对称 padding + rack focus + 呼吸节奏 ----
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 24.dp, end = 48.dp,
                        top = 16.dp, bottom = 96.dp
                    )
                ) {
                    // HERO：视频标题 + dolly out 效果（外层螺旋线路径）
                    item(key = "hero") {
                        SpiralScrollItem(listState, "hero", spiralAmplitudeScale) {
                            Box(
                                Modifier.graphicsLayer {
                                    // Dolly out：随滚动 scale 1.02→0.98, alpha 1→0.86
                                    val dolly = scrollFraction.coerceIn(0f, 1f)
                                    scaleX = 1.02f - dolly * 0.04f
                                    scaleY = 1.02f - dolly * 0.04f
                                    alpha = 1f - dolly * 0.14f
                                    translationY = -dolly * 12.dp.toPx()
                                }
                            ) {
                                Column(Modifier.padding(bottom = 16.dp)) {
                                    Text(
                                        videoId,
                                        style = HeroTitle,
                                        color = CinematicGold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .haloGlow(Aureate, radiusPx = 90f, alpha = 0.08f)
                                            .breathing()
                                            .pointerInput(Unit) {
                                                // 长按 3 秒：切换演示模式（自动滚动）
                                                detectTapGestures(
                                                    onPress = {
                                                        val early = withTimeoutOrNull(SpiralDemoLongPressMs) {
                                                            awaitRelease()
                                                            false
                                                        }
                                                        if (early == null) {
                                                            demoMode = !demoMode
                                                            Feedback.tap()
                                                        }
                                                    }
                                                )
                                            }
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "${timeline.size} segments",
                                        style = MicroMono,
                                        color = Twilight
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    GoldUnderline(width = 64.dp)
                                    Spacer(Modifier.height(8.dp))
                                    CinematicDivider(color = Twilight.copy(alpha = 0.15f))
                                }
                            }
                        }
                    }
                    // 内容条目：rack focus + 微倾 + 非等距节奏（外层螺旋线路径）
                    itemsIndexed(timeline, key = { _, it -> it.start }) { index, item ->
                        val isKeyItem = index % 4 == 0   // 每 4 个做一次"重点"
                        val microTilt = ((index * 7 + 3) % 5 - 2) * 0.2f   // -0.4°..+0.4° 确定性
                        SpiralScrollItem(listState, item.start, spiralAmplitudeScale) {
                            Box(
                                Modifier
                                    .graphicsLayer {
                                        // Rack focus：微倾 + 微缩放
                                        rotationZ = microTilt
                                        val focusBoost = if (isKeyItem) 1.008f else 1f
                                        scaleX = focusBoost
                                        scaleY = focusBoost
                                    }
                                    .padding(
                                        top = if (isKeyItem) 24.dp else 12.dp,
                                        bottom = if (isKeyItem) 16.dp else 8.dp,
                                        start = if (index % 3 == 1) 16.dp else 0.dp,   // 偶尔左偏
                                        end = if (index % 3 == 2) 8.dp else 0.dp      // 偶尔右偏
                                    )
                            ) {
                                TimelineCard(item)
                            }
                        }
                        // 章节间虚线（每 3 个条目插一条）
                        if (index % 3 == 2 && index < timeline.size - 1) {
                            CinematicDivider(
                                color = Twilight.copy(alpha = 0.12f),
                                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp, start = 48.dp, end = 24.dp)
                            )
                        }
                    }
                    // 底部收束：胶片时间轴（外层螺旋线路径）
                    item(key = "footer") {
                        SpiralScrollItem(listState, "footer", spiralAmplitudeScale) {
                            Column(
                                Modifier.fillMaxWidth().padding(top = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CinematicDivider(color = Twilight.copy(alpha = 0.2f))
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "END OF TRANSCRIPT",
                                    style = CinematicTag.copy(fontSize = 10.sp),
                                    color = Twilight
                                )
                            }
                        }
                    }
                }
            }
        }
        // 演示模式指示（仅录屏时可见，不拦截点击）
        if (demoMode) {
            Text(
                "DEMO ● SPIRAL",
                style = MicroMono,
                color = Aureate,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 14.dp)
                    .graphicsLayer { alpha = 0.85f }
            )
        }
    }
    if (timelineOpen) {
        StudyTimelineView(config, videoId, timeline) { timelineOpen = false }
    }
}

@Composable
fun VideoListItem(
    id: String,
    selected: Boolean,
    segments: Int,
    seconds: Double,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val bg = when {
        selected -> MaterialTheme.colorScheme.secondaryContainer
        hovered -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else -> Color.Transparent
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .hoverable(interaction)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(34.dp)
                .background(
                    if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    RoundedCornerShape(2.dp)
                )
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                id,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            val meta = if (segments > 0) {
                val total = seconds.toInt()
                fun two(v: Int) = if (v < 10) "0$v" else "$v"
                t("home_indexed", formatTime(seconds), segments.toString())
            } else {
                t("home_not_indexed")
            }
            Text(
                meta,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun EmptyState(onImport: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        RotatingLogo(size = 190.dp)
        Spacer(Modifier.height(28.dp))
        Text(
            "Begin Your Journey",
            style = SubtitleStyle.copy(fontSize = 17.sp, letterSpacing = 5.sp),
            color = MoonSilver
        )
        Spacer(Modifier.height(10.dp))
        Text("Where Knowledge Breathes", style = MaterialTheme.typography.bodyMedium, color = TextMid)
        Spacer(Modifier.height(32.dp))
        FilledTonalButton(
            onClick = { Feedback.tap(); onImport() },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.hoverSound()
        ) { Text(t("btn_import")) }
        Spacer(Modifier.height(16.dp))
        Text(
            t("home_empty_sub"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ContentPane(
    config: AppConfig,
    videoId: String?,
    tick: Int,
    onStatus: (String) -> Unit,
    onChanged: () -> Unit
) {
    val scope = rememberCoroutineScope()
    if (videoId == null) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(t("focus_pick_hint"), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val timeline = remember(videoId, tick) { ConfigStore.loadTimeline(config, videoId) }
    val source = remember(videoId) { ConfigStore.sourceFile(config, videoId) }

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(videoId, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (source != null) {
                FilledTonalButton(
                    shape = RoundedCornerShape(12.dp),
                    onClick = {
                        onStatus(t("status_transcribing"))
                        scope.launch {
                            val msg = withContext(Dispatchers.IO) {
                                try { VideoActions.transcribe(config, videoId) } catch (e: Exception) { t("status_transcribe_failed", e.message.toString()) }
                            }
                            onStatus(msg)
                            onChanged()
                        }
                    }
                ) { Text(t("btn_transcribe")) }
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(
                    shape = RoundedCornerShape(12.dp),
                    onClick = {
                        onStatus(t("status_ocr"))
                        scope.launch {
                            val msg = withContext(Dispatchers.IO) {
                                try { VideoActions.ocrFrames(config, videoId) } catch (e: Exception) { t("status_ocr_failed", e.message.toString()) }
                            }
                            onStatus(msg)
                            onChanged()
                        }
                    }
                ) { Text(t("btn_ocr")) }
            }
        }
        Spacer(Modifier.height(12.dp))

        if (timeline.isEmpty()) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(t("content_no_results"), style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                Text(
                    if (source == null) t("content_source_missing") else t("content_no_results_hint"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            var view by remember { mutableStateOf(0) }
            var selIdx by remember { mutableStateOf<Int?>(null) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ViewButton("Content", view == 0) { view = 0 }
                ViewButton("Graph", view == 1) { view = 1 }
                ViewButton("Timeline", view == 2) { view = 2 }
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (view) {
                    1 -> KnowledgeGraph(
                        items = timeline,
                        selectedIndex = selIdx,
                        onNodeClick = { idx ->
                            selIdx = idx
                            onStatus(t("status_located", formatTime(timeline[idx].start)))
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    2 -> TimelineReplay(
                        items = timeline,
                        selectedIndex = selIdx,
                        onSelect = { idx ->
                            selIdx = idx
                            onStatus(t("status_located", formatTime(timeline[idx].start)))
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    else -> LazyColumn(Modifier.fillMaxSize()) {
                        items(timeline) { item -> TimelineCard(item) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewButton(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        FilledTonalButton(
            onClick = { Feedback.tap(); onClick() },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.hoverSound()
        ) { Text(label) }
    } else {
        GhostButton(onClick = { Feedback.tap(); onClick() }) { Text(label) }
    }
}

@Composable
fun TimelineCard(item: TimelineItem) {
    var expanded by remember { mutableStateOf(false) }
    var noted by remember { mutableStateOf(false) }
    val text = (item.speech + " " + item.screenText).trim()
    val importance = remember(item) { (3 + (text.length / 20)).coerceIn(3, 5) }
    val type = remember(item) {
        when {
            text.contains("=") -> "Definition"
            text.contains("note") -> "Key point"
            else -> "Point"
        }
    }
    val importanceColor = when (importance) {
        5 -> Coral
        4 -> Aureate
        else -> MaterialTheme.colorScheme.primary
    }
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    GlowBox(
        glow = importanceColor,
        intensity = if (hovered || expanded) 1f else 0.32f,
        radius = 200.dp,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).floatIn(delayMillis = 40)
    ) {
        Surface(
            color = Color(0xFF0F1419).copy(alpha = if (hovered) 0.78f else 0.62f),
            shape = CardShapes.learningUnitCard(),
            modifier = Modifier
                .fillMaxWidth()
                .cinematicLight(intensity = if (hovered) 0.10f else 0.05f)
                .pressScale(interaction)
                .hoverSound()
                .hoverable(interaction)
        ) {
        Column(Modifier.padding(DesignTokens.SpaceCardInnerDp.dp).animateContentSize()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimeChip("${formatTime(item.start)} - ${formatTime(item.end)}")
                StarRating(importance = importance, sizeDp = 18)
                TagChip(type)
                TagChip(if (item.speech.isNotBlank()) t("chip_speech") else t("chip_screen"))
            }
            Spacer(Modifier.height(18.dp))
            // ---- 黄金分割：主角标题 ----
            Text(
                text.take(30).ifBlank { t("card_no_content") },
                style = HeroTitle,
                color = CinematicGold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.haloGlow(Aureate, radiusPx = 70f, alpha = 0.10f)
            )
            Spacer(Modifier.height(6.dp))
            GoldUnderline(width = 48.dp)
            Spacer(Modifier.height(14.dp))
            // ---- 定义：右偏 48dp（不贴左）----
            Text(
                text.take(80),
                style = MaterialTheme.typography.bodyLarge,
                color = FogGray,
                maxLines = if (expanded) 6 else 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 48.dp)
            )
            Spacer(Modifier.height(14.dp))
            CinematicDivider(color = Twilight.copy(alpha = 0.2f))
            Spacer(Modifier.height(14.dp))
            // ---- INTUITION 右对齐 italic ----
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                Text("INTUITION", style = CinematicTag, color = Bioluminescence)
                Spacer(Modifier.height(4.dp))
                Text(
                    text.take(20).ifBlank { "—" },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Light, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                    color = DawnSilver
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GhostButton(onClick = { Feedback.tap(); expanded = !expanded }) {
                    Text(if (expanded) t("btn_collapse") else t("btn_expand"))
                }
                FilledTonalButton(
                    onClick = { noted = !noted; if (noted) Feedback.success() },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.hoverSound()
                ) {
                    Text(if (noted) t("btn_noted") else t("btn_note"))
                }
            }
            if (expanded) {
                Spacer(Modifier.height(12.dp))
                MistDivider()
                Spacer(Modifier.height(12.dp))
                if (item.speech.isNotBlank()) Text(t("card_speech", item.speech), style = MaterialTheme.typography.bodySmall)
                if (item.screenText.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(t("card_screen", item.screenText), style = MaterialTheme.typography.bodySmall)
                }
                if (item.framePath.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(t("card_frame", item.framePath), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        }
    }
}

@Composable
private fun TagChip(text: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(6.dp)) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Phantom)
        Spacer(Modifier.width(8.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun TimeChip(text: String) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(6.dp)) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun QaPanel(
    config: AppConfig,
    videoId: String?,
    tick: Int,
    modifier: Modifier = Modifier,
    onOpenDebate: (() -> Unit)? = null
) {
    val timeline = remember(videoId, tick) { if (videoId == null) emptyList() else ConfigStore.loadTimeline(config, videoId) }
    val engine: QaEngine = remember(timeline, config.nvidiaApiKey, config.model) {
        val indexer = Indexer()
        indexer.index(timeline)
        val search: suspend (String, Int) -> List<TimelineItem> = { q, k -> indexer.search(q, k) }
        if (config.nvidiaApiKey.isBlank()) {
            LocalQaEngine(allItems = { timeline }, search = search)
        } else {
            RemoteQaEngine(search, NvidiaLlmClient(config.nvidiaApiKey, model = config.model))
        }
    }
    val scope = rememberCoroutineScope()
    var question by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val messages = remember { mutableStateListOf<Pair<String, QaAnswer>>() }

    // ---- Developer Mode：回答 chips + 记录决策 ----
    val devStore = remember { TechStore() }
    val knownTechCards = remember(devStore) { devStore.getAllTechCards() }
    var selectedTech by remember { mutableStateOf<TechCard?>(null) }
    var decisionOpen by remember { mutableStateOf(false) }
    var decisionPrefill by remember { mutableStateOf("") }

    // 完成瞬间"炸开成粒子"
    var lastBusy by remember { mutableStateOf(false) }
    var burstTick by remember { mutableStateOf(0) }
    LaunchedEffect(busy) {
        if (!busy && lastBusy) burstTick++
        lastBusy = busy
    }

    fun runAsk(q: String) {
        val query = q.trim()
        if (query.isBlank() || busy) return
        Feedback.tap()
        busy = true
        scope.launch {
            val ans = try {
                engine.ask(query)
            } catch (e: Exception) {
                QaAnswer("Error: " + e.message, emptyList())
            }
            messages.add(query to ans)
            busy = false
        }
    }

    GlowBox(
        glow = MaterialTheme.colorScheme.secondary,
        intensity = if (busy) 0.9f else 0.35f,
        radius = 240.dp,
        modifier = modifier.fillMaxSize()
    ) {
        Surface(
            color = Veil.copy(alpha = 0.55f),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxSize()
        ) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(3.dp).height(22.dp).background(Brush.verticalGradient(listOf(Phantom, Aureate)), RoundedCornerShape(2.dp)))
                Spacer(Modifier.width(10.dp))
                Text(t("qa_title"), style = MaterialTheme.typography.headlineMedium)
            }
            Spacer(Modifier.weight(1f))
                if (onOpenDebate != null) {
                    Box(
                        Modifier
                            .size(28.dp)
                            .hoverSound()
                            .clickable { Feedback.tap(); onOpenDebate() },
                        contentAlignment = Alignment.Center
                    ) {
                        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) { DebateIcon(18) }
                    }
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    if (config.nvidiaApiKey.isBlank()) t("qa_offline") else t("qa_nim"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(12.dp))

            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (messages.isEmpty() && busy) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            BreathingOrb(size = 84.dp)
                            Spacer(Modifier.height(18.dp))
                            Text(t("qa_thinking"), style = SubtitleStyle, color = MoonSilver)
                        }
                    }
                } else if (messages.isEmpty()) {
                    EmptyChat(onPick = { question = it; runAsk(it) })
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        items(messages) { (q, a) ->
                            Column {
                                ChatBubble(question = q, answer = a)
                                val chips = knownTechCards.map { it.name }
                                    .filter { a.answer.contains(it, ignoreCase = true) }
                                    .take(6)
                                if (chips.isNotEmpty()) {
                                    Spacer(Modifier.height(8.dp))
                                    TechChipsRow(chips) { name ->
                                        selectedTech = knownTechCards.firstOrNull { it.name == name }
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

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VeilField(
                    value = question,
                    onValueChange = { question = it },
                    placeholder = { Text(t("qa_placeholder")) },
                    modifier = Modifier.weight(1f)
                )
                FilledTonalButton(
                    enabled = question.isNotBlank() && !busy,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.hoverSound(),
                    onClick = {
                        val q = question
                        question = ""
                        runAsk(q)
                    }
                ) { Text(if (busy) "..." else t("btn_send")) }
            }
            if (messages.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    GhostButton(onClick = {
                        Feedback.tap()
                        decisionPrefill = messages.last().first
                        decisionOpen = true
                    }) { Text("[ Record Decision ]") }
                }
            }
        }
        }
    }
    selectedTech?.let { TechCardCompactDialog(it, devStore) { selectedTech = null } }
    if (decisionOpen) {
        RecordDecisionDialog(devStore, decisionPrefill, onSaved = { }, onClose = { decisionOpen = false })
    }
}

@Composable
private fun EmptyChat(onPick: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BreathingOrb(size = 56.dp)
        Spacer(Modifier.height(12.dp))
        Text(t("qa_empty_title"), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        Text(t("qa_empty_hint"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        listOf(t("qa_sug_1"), t("qa_sug_2"), t("qa_sug_3")).forEach { s ->
            Text(
                "· $s",
                modifier = Modifier.padding(vertical = 3.dp).clickable { onPick(s) },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ChatBubble(question: String, answer: QaAnswer) {
    Column(Modifier.fillMaxWidth().floatIn(distanceDp = 14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Surface(
                color = Color(0xFF3A2A18).copy(alpha = 0.80f),
                shape = CardShapes.userBubble(),
                modifier = Modifier
                    .graphicsLayer { rotationZ = 0.8f }
                    .cinematicLight(intensity = 0.12f)
            ) {
                Text(
                    question,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF0A0E14)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth().padding(start = 48.dp), horizontalArrangement = Arrangement.Start) {
            Box {
                // 左侧金色竖线（从顶到底，Cinematic Gold 渐变）
                Canvas(Modifier.fillMaxHeight().width(2.dp)) {
                    drawRect(
                        Brush.verticalGradient(listOf(Aureate.copy(alpha = 0.5f), Aureate.copy(alpha = 0.1f), Color.Transparent))
                    )
                }
                Spacer(Modifier.width(0.dp))
            Surface(
                color = Color(0xFF0F1419).copy(alpha = 0.75f),
                shape = CardShapes.aiBubble(),
                modifier = Modifier
                    .padding(start = 10.dp)
                    .graphicsLayer { rotationZ = -0.6f }
                    .cinematicLight(intensity = 0.06f)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(answer.answer, style = MaterialTheme.typography.bodyMedium)
                    if (answer.citations.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            answer.citations.forEach { c ->
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        c,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    initial: AppConfig,
    currentVideoId: String? = null,
    onOpenTechMap: () -> Unit = {},
    onOpenWeekly: () -> Unit = {},
    onOpenDecisions: () -> Unit = {},
    onSave: (AppConfig) -> Unit,
    onCancel: () -> Unit
) {
    var apiKey by remember { mutableStateOf(initial.nvidiaApiKey) }
    var model by remember { mutableStateOf(initial.model) }
    var themeMode by remember { mutableStateOf(initial.themeMode) }
    var soundEnabled by remember { mutableStateOf(initial.soundEnabled) }
    var debateModelA by remember { mutableStateOf(initial.debateModelA.ifBlank { DebateModels.DEFAULT_A }) }
    var debateModelB by remember { mutableStateOf(initial.debateModelB.ifBlank { DebateModels.DEFAULT_B }) }
    var debateModelC by remember { mutableStateOf(initial.debateModelC.ifBlank { DebateModels.DEFAULT_C }) }
    var language by remember { mutableStateOf(initial.language) }
    val resultsDir = remember { ConfigStore.resultsDir(initial).absolutePath }

    // ---------- 学习引擎：Rebuild Tech Cards ----------
    var techDialog by remember { mutableStateOf(false) }
    var techBusy by remember { mutableStateOf(false) }
    var techDone by remember { mutableStateOf(0) }
    var techTotal by remember { mutableStateOf(0) }
    var techResult by remember { mutableStateOf<String?>(null) }
    var techError by remember { mutableStateOf<String?>(null) }
    var reviewOpen by remember { mutableStateOf(false) }
    var reverseOpen by remember { mutableStateOf(false) }
    var practiceOpen by remember { mutableStateOf(false) }
    var examOpen by remember { mutableStateOf(false) }
    var heatOpen by remember { mutableStateOf(false) }
    var polishOpen by remember { mutableStateOf(false) }
    var polishBusy by remember { mutableStateOf(false) }
    var polishStage by remember { mutableStateOf("") }
    var polishResult by remember { mutableStateOf<String?>(null) }
    var polishError by remember { mutableStateOf<String?>(null) }
    val techScope = rememberCoroutineScope()

    fun rebuildTechCards() {
        techResult = null
        techError = null
        techDone = 0
        techTotal = 0
        techDialog = true
        val videoId = currentVideoId
        if (videoId == null) {
            techError = "请先在首页选择一个视频"
            return
        }
        techBusy = true
        techScope.launch {
            try {
                val units = withContext(Dispatchers.IO) {
                    val timeline = ConfigStore.loadTimeline(initial, videoId)
                    ContextAnalyzer(null).analyze(timeline, videoId)
                }
                if (units.isEmpty()) throw IllegalStateException("当前视频没有可处理的 LearningUnit（时间线为空）")
                techTotal = units.size
                val llm = if (initial.nvidiaApiKey.isNotBlank()) {
                    NvidiaLlmClient(initial.nvidiaApiKey, model = initial.model)
                } else null
                val extractor = TechCardExtractor(llm)
                val store = TechStore()
                var count = 0
                units.forEachIndexed { idx, unit ->
                    val cards = withContext(Dispatchers.IO) { extractor.extract(unit, videoId) }
                    withContext(Dispatchers.IO) { cards.forEach { store.saveTechCard(it) } }
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

    // ---- 批次 A：三模型深度整理（带进度） ----
    var digestOpen by remember { mutableStateOf(false) }
    var digestBusy by remember { mutableStateOf(false) }
    var digestStage by remember { mutableStateOf("") }
    var digestResult by remember { mutableStateOf<String?>(null) }
    var digestError by remember { mutableStateOf<String?>(null) }

    fun runDeepDigest() {
        digestResult = null; digestError = null; digestStage = "准备…"; digestOpen = true
        val videoId = currentVideoId
        if (videoId == null) { digestError = "请先在首页选择一个视频"; return }
        digestBusy = true
        techScope.launch {
            try {
                val msg = withContext(Dispatchers.IO) {
                    VideoActions.deepDigest(initial, videoId) { pr -> digestStage = pr.stage }
                }
                digestResult = msg
            } catch (e: Exception) {
                digestError = "失败：${e.message ?: e.javaClass.simpleName}"
            } finally { digestBusy = false }
        }
    }

    Column(Modifier.fillMaxSize().padding(32.dp)) {
        if (reviewOpen) StudyReviewView(initial, currentVideoId) { reviewOpen = false }
        if (reverseOpen) ReverseLearningView { reverseOpen = false }
        if (practiceOpen) StandardPracticeView { practiceOpen = false }
        if (examOpen) ExamView(initial, currentVideoId) { examOpen = false }
        if (heatOpen) MasteryHeatView(initial, currentVideoId) { heatOpen = false }
        if (polishOpen) {
            if (!polishBusy && polishResult == null && polishError == null) {
                val vid = currentVideoId
                if (vid == null) { polishError = "请先选择一个视频" } else {
                    polishBusy = true; polishStage = "纠错中…"
                    techScope.launch {
                        try {
                            polishResult = withContext(Dispatchers.IO) {
                                VideoActions.polishTranscript(initial, vid) { i, t -> polishStage = "纠错 $i/$t" }
                            }
                        } catch (e: Exception) { polishError = "失败：${e.message}" } finally { polishBusy = false }
                    }
                }
            }
            Dialog(onDismissRequest = { if (!polishBusy) polishOpen = false }) {
                Surface(shape = RoundedCornerShape(18.dp), color = Veil.copy(alpha = 0.97f)) {
                    Column(Modifier.width(520.dp).padding(24.dp)) {
                        Text("转写纠错（三模型共识）", style = MaterialTheme.typography.titleMedium, color = CinematicGold)
                        Spacer(Modifier.height(10.dp))
                        if (polishBusy) { Text(polishStage, color = TextHigh); Spacer(Modifier.height(8.dp)); LinearProgressIndicator(Modifier.fillMaxWidth()) }
                        else {
                            Text(polishError ?: polishResult ?: "", style = MaterialTheme.typography.bodyMedium, color = if (polishError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                            if (polishResult != null && currentVideoId != null) {
                                Spacer(Modifier.height(8.dp))
                                GhostButton(onClick = { Feedback.tap(); runCatching { Desktop.getDesktop().open(File(ConfigStore.resultsDir(initial), "$currentVideoId/transcript_polished.md")) } }) { Text("打开纠错稿 transcript_polished.md") }
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { if (!polishBusy) FilledTonalButton(onClick = { polishOpen = false }, shape = RoundedCornerShape(12.dp)) { Text("关闭") } }
                    }
                }
            }
        }
        if (techDialog) {
            Dialog(onDismissRequest = { if (!techBusy) techDialog = false }) {
                Surface(shape = RoundedCornerShape(18.dp), color = Veil.copy(alpha = 0.97f)) {
                    Column(Modifier.width(420.dp).padding(24.dp)) {
                        Text("Rebuild Tech Cards", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))
                        if (techBusy) {
                            Text("正在处理当前视频...", style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { if (techTotal > 0) techDone.toFloat() / techTotal else 0f },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "$techDone / $techTotal LearningUnit",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                techError ?: techResult ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (techError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            if (!techBusy) {
                                FilledTonalButton(
                                    onClick = { Feedback.tap(); techDialog = false },
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text("关闭") }
                            }
                        }
                    }
                }
            }
        }
        if (digestOpen) {
            Dialog(onDismissRequest = { if (!digestBusy) digestOpen = false }) {
                Surface(shape = RoundedCornerShape(18.dp), color = Veil.copy(alpha = 0.97f)) {
                    Column(Modifier.width(520.dp).padding(24.dp)) {
                        Text("三模型深度整理", style = MaterialTheme.typography.titleMedium, color = CinematicGold)
                        Spacer(Modifier.height(10.dp))
                        if (digestBusy) {
                            Text(digestStage.ifBlank { "处理中…" }, style = MaterialTheme.typography.bodyMedium, color = TextHigh)
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(6.dp))
                            Text("模型较慢属正常（Kimi 可能 1-3 分钟/段），请耐心等待。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Text(digestError ?: digestResult ?: "", style = MaterialTheme.typography.bodyMedium, color = if (digestError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                            if (digestResult != null && currentVideoId != null) {
                                Spacer(Modifier.height(8.dp))
                                GhostButton(onClick = {
                                    Feedback.tap()
                                    runCatching { Desktop.getDesktop().open(File(ConfigStore.resultsDir(initial), "$currentVideoId/deep_digest.md")) }
                                }) { Text("打开报告 deep_digest.md") }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            if (!digestBusy) FilledTonalButton(onClick = { digestOpen = false }, shape = RoundedCornerShape(12.dp)) { Text("关闭") }
                        }
                    }
                }
            }
        }
        Text(t("settings_title"), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        Text(t("settings_api_key"), style = MaterialTheme.typography.labelLarge, letterSpacing = 2.sp)
        Spacer(Modifier.height(8.dp))
        VeilField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text(t("settings_api_key_label")) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        Text(t("settings_model"), style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        VeilField(
            value = model,
            onValueChange = { model = it },
            label = { Text(t("settings_model_label")) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))

        Text(t("settings_debate_models"), style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        VeilField(
            value = debateModelA,
            onValueChange = { debateModelA = it },
            label = { Text(t("settings_model_a")) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        VeilField(
            value = debateModelB,
            onValueChange = { debateModelB = it },
            label = { Text(t("settings_model_b")) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        VeilField(
            value = debateModelC,
            onValueChange = { debateModelC = it },
            label = { Text(t("settings_model_c")) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))

        Text(t("settings_theme"), style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeOption(t("theme_system"), "system", themeMode) { themeMode = it }
            ThemeOption(t("theme_light"), "light", themeMode) { themeMode = it }
            ThemeOption(t("theme_dark"), "dark", themeMode) { themeMode = it }
        }
        Spacer(Modifier.height(24.dp))

        Text(t("settings_results_dir"), style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(resultsDir, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            GhostButton(
                onClick = { Feedback.tap(); try { Desktop.getDesktop().open(File(resultsDir)) } catch (e: Exception) { } }
            ) { Text(t("btn_open")) }
        }
        Spacer(Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(t("settings_sound"), style = MaterialTheme.typography.titleSmall)
                Text(t("settings_sound_desc"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(24.dp))
        Text(t("settings_language"), style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(t("lang_en") to "en", t("lang_zh") to "zh").forEach { (label, v) ->
                GhostButton(onClick = { language = v; Language.current = v }) {
                    Text(if (language == v) "[ $label ]" else label)
                }
            }
        }
            Switch(checked = soundEnabled, onCheckedChange = {
                soundEnabled = it
                Feedback.enabled = it
                SoundSynth.enabled = it
                if (it) SoundSynth.warmUp()
            })
        }
        Spacer(Modifier.height(24.dp))

        // 学习引擎：重建当前视频的技术卡
        Text("学习引擎", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        FilledTonalButton(
            onClick = { Feedback.tap(); rebuildTechCards() },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.hoverSound()
        ) { Text("Rebuild Tech Cards") }
        Spacer(Modifier.height(8.dp))
        FilledTonalButton(
            onClick = { Feedback.tap(); runDeepDigest() },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.hoverSound()
        ) { Text("深度整理（三模型）") }
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            GhostButton(onClick = { Feedback.tap(); onOpenTechMap() }) { Text("Tech Map") }
            GhostButton(onClick = { Feedback.tap(); onOpenWeekly() }) { Text("Weekly Report") }
            GhostButton(onClick = { Feedback.tap(); onOpenDecisions() }) { Text("Decisions") }
            GhostButton(onClick = { Feedback.tap(); reviewOpen = true }) { Text("学业复习台") }
            GhostButton(onClick = { Feedback.tap(); reverseOpen = true }) { Text("逆向学习") }
            GhostButton(onClick = { Feedback.tap(); practiceOpen = true }) { Text("规范答题训练") }
            GhostButton(onClick = { Feedback.tap(); examOpen = true }) { Text("出卷/批改") }
            GhostButton(onClick = { Feedback.tap(); heatOpen = true }) { Text("掌握度热力") }
            GhostButton(onClick = { Feedback.tap(); polishOpen = true; polishResult = null; polishError = null }) { Text("转写纠错（三模型）") }
        }
        if (currentVideoId == null) {
            Spacer(Modifier.height(4.dp))
            Text(
                "请先在首页选择一个视频",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(32.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledTonalButton(
                onClick = {
                    Feedback.tap()
                    onSave(
                        AppConfig(
                            nvidiaApiKey = apiKey,
                            model = model,
                            resultsDir = initial.resultsDir,
                            themeMode = themeMode,
                            soundEnabled = soundEnabled,
                            debateModelA = debateModelA, debateModelB = debateModelB, debateModelC = debateModelC, language = language
                        )
                    )
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.hoverSound()
            ) { Text(t("btn_save")) }
            GhostButton(onClick = { Feedback.tap(); onCancel() }) { Text(t("btn_cancel")) }
        }
    }
}

@Composable
fun ThemeOption(label: String, value: String, current: String, onPick: (String) -> Unit) {
    if (current == value) {
        FilledTonalButton(
            onClick = { Feedback.tap(); onPick(value) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.hoverSound()
        ) { Text(label) }
    } else {
        GhostButton(onClick = { Feedback.tap(); onPick(value) }) { Text(label) }
    }
}

@Composable
fun SearchDialog(
    config: AppConfig,
    videos: List<String>,
    onClose: () -> Unit,
    onPick: (String) -> Unit
) {
    var q by remember { mutableStateOf("") }
    // ---- Developer Mode：搜索顶部召回块 ----
    val devStore = remember { TechStore() }
    val recallBlocks = remember(q) {
        if (q.isBlank()) emptyList()
        else runCatching { ContextualRecall(devStore).recallForSearch(q) }.getOrDefault(emptyList())
    }
    var selectedTech by remember { mutableStateOf<TechCard?>(null) }
    var selectedDecision by remember { mutableStateOf<Decision?>(null) }
    val results = remember(q, videos) {
        if (q.isBlank()) emptyList()
        else videos.flatMap { id ->
            ConfigStore.loadTimeline(config, id)
                .filter { it.speech.contains(q, ignoreCase = true) || it.screenText.contains(q, ignoreCase = true) }
                .map { id to it }
        }.take(30)
    }
    LaunchedEffect(Unit) { Feedback.open() }
    val dismiss = {
        Feedback.close()
        onClose()
    }
    Dialog(onDismissRequest = dismiss) {
        GlowBox(glow = MaterialTheme.colorScheme.primary, intensity = 0.85f, radius = 300.dp) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Veil.copy(alpha = 0.96f)
        ) {
            Column(Modifier.width(560.dp).padding(20.dp)) {
                VeilField(
                    value = q,
                    onValueChange = { q = it },
                    placeholder = { Text(t("search_placeholder")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                RecallBlockSection(recallBlocks) { block ->
                    when (block.type) {
                        "decision" -> selectedDecision =
                            devStore.getAllDecisions().firstOrNull { it.id == block.referenceId }
                        else -> selectedTech =
                            devStore.getAllTechCards().firstOrNull { it.id == block.referenceId }
                                ?: devStore.getAllTechCards().firstOrNull {
                                    it.name.equals(block.referenceId, ignoreCase = true)
                                }
                    }
                }
                if (recallBlocks.isNotEmpty()) Spacer(Modifier.height(12.dp))
                when {
                    q.isBlank() -> Text(
                        t("search_hint"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    results.isEmpty() -> Text(
                        t("search_no_results"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    else -> LazyColumn(Modifier.height(300.dp)) {
                        items(results) { pair ->
                            val id = pair.first
                            val item = pair.second
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { Feedback.close(); onPick(id) }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    id,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.width(120.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "[${formatTime(item.start)}] " + item.speech.ifBlank { item.screenText },
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    selectedTech?.let { TechCardCompactDialog(it, devStore) { selectedTech = null } }
    selectedDecision?.let { DecisionDetailDialog(it) { selectedDecision = null } }
}
}
