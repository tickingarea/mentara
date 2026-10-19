package com.learnagent.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.learnagent.config.AppConfig
import com.learnagent.config.ConfigStore
import com.learnagent.core.DeepDigestReport
import com.learnagent.core.SpacedRepetitionEngine
import com.learnagent.data.KnowledgeMastery
import com.learnagent.data.TimelineItem
import com.learnagent.media.VideoFrameExtractor
import com.learnagent.media.VideoToWav
import com.learnagent.platform.TechStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip

@Serializable
private data class FrameRef(val timestamp: Double, val imagePath: String)

private val tlJson = Json { ignoreUnknownKeys = true; isLenient = true }

private fun tlFmt(sec: Double): String {
    val t = sec.toInt().coerceAtLeast(0)
    fun two(v: Int) = if (v < 10) "0$v" else "$v"
    return two(t / 60) + ":" + two(t % 60)
}

/**
 * 学习时间线（批次 B/C/D）：
 *  - 内嵌播放器：Java 声音播放原音频（WAV）+ 关键帧画面；▶/⏸、可拖动、点某段跳转
 *  - 逐段：三模型共识/分歧 + OCR/画面文字 + 关键帧缩略图
 *  - SM-2：每段「已掌握 / 不熟」写掌握度，显示复习次数与下次复习
 */
@Composable
fun StudyTimelineView(
    config: AppConfig,
    videoId: String,
    timeline: List<TimelineItem>,
    onClose: () -> Unit
) {
    val dir = remember(config, videoId) { File(ConfigStore.resultsDir(config), videoId) }
    val report: DeepDigestReport? = remember(dir) {
        runCatching {
            val f = File(dir, "deep_digest.json")
            if (f.isFile) tlJson.decodeFromString(DeepDigestReport.serializer(), f.readText()) else null
        }.getOrNull()
    }
    fun loadFrames(): List<FrameRef> {
        val dense = File(dir, "dense/frames.json")
        val f = if (dense.isFile) dense else File(dir, "frames.json")
        return runCatching {
            if (f.isFile) tlJson.decodeFromString(ListSerializer(FrameRef.serializer()), f.readText()) else emptyList()
        }.getOrDefault(emptyList())
    }
    var frames by remember(dir) { mutableStateOf(loadFrames()) }
    val scope = rememberCoroutineScope()
    var denseBusy by remember { mutableStateOf(false) }
    var denseMsg by remember { mutableStateOf<String?>(null) }
    val source: File? = remember(config, videoId) { ConfigStore.sourceFile(config, videoId) }

    // ---- D：掌握度（SM-2） ----
    val store = remember { TechStore() }
    val engine = remember { SpacedRepetitionEngine() }
    val masteryMap = remember { mutableStateMapOf<String, KnowledgeMastery>() }
    LaunchedEffect(Unit) { runCatching { store.getAllMastery().forEach { masteryMap[it.pointId] = it } } }
    fun mark(pid: String, q: Int) {
        val cur = masteryMap[pid] ?: KnowledgeMastery(pointId = pid)
        val up = engine.update(cur, q, System.currentTimeMillis())
        runCatching { store.saveMastery(up) }
        masteryMap[pid] = up
    }

    // ---- C：内嵌播放器（原音频 + 关键帧画面） ----
    var clip by remember { mutableStateOf<Clip?>(null) }
    var audioError by remember { mutableStateOf<String?>(null) }
    var playing by remember { mutableStateOf(false) }
    var posSec by remember { mutableStateOf(0.0) }
    var durationSec by remember { mutableStateOf(0.0) }

    LaunchedEffect(source, dir) {
        withContext(Dispatchers.IO) {
            try {
                if (source == null) { audioError = "找不到源文件"; return@withContext }
                val wav = if (source.extension.lowercase() == "wav") source
                else File(dir, "audio.wav").let { f ->
                    if (!f.isFile) { if (!VideoToWav.extractAudio(source, f)) throw IllegalStateException("音频提取失败") }
                    f
                }
                val c = AudioSystem.getClip()
                AudioSystem.getAudioInputStream(wav).use { c.open(it) }
                withContext(Dispatchers.Main) {
                    clip = c
                    durationSec = c.microsecondLength / 1e6
                }
            } catch (e: Exception) {
                audioError = e.message ?: e.javaClass.simpleName
            }
        }
    }

    fun seek(t: Double) {
        val c = clip ?: return
        c.microsecondPosition = (t * 1e6).toLong().coerceIn(0L, c.microsecondLength)
        posSec = t.coerceIn(0.0, durationSec)
    }
    fun togglePlay() {
        val c = clip ?: return
        if (playing) { c.stop(); playing = false }
        else { if (c.microsecondPosition >= c.microsecondLength - 10_000) c.microsecondPosition = 0; c.start(); playing = true }
    }
    LaunchedEffect(playing, clip) {
        while (playing) {
            clip?.let { posSec = it.microsecondPosition / 1e6 }
            delay(50)
        }
    }

    val currentFrame: FrameRef? = frames.lastOrNull { it.timestamp <= posSec } ?: frames.firstOrNull()

    Dialog(onDismissRequest = { clip?.stop(); onClose() }) {
        Surface(shape = RoundedCornerShape(18.dp), color = Veil.copy(alpha = 0.97f)) {
            Column(Modifier.width(960.dp).height(680.dp).padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("学习时间线", style = MaterialTheme.typography.titleMedium, color = CinematicGold)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (report != null) "三模型共识/分歧 · 内嵌播放 · SM-2 标记" else "（未跑深度整理：显示原始段落）",
                        style = MaterialTheme.typography.labelSmall, color = TextMid
                    )
                    Spacer(Modifier.weight(1f))
                    FilledTonalButton(onClick = { clip?.stop(); onClose() }, shape = RoundedCornerShape(12.dp)) { Text("关闭") }
                }
                Spacer(Modifier.height(10.dp))

                // 播放器条
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(onClick = { togglePlay() }, shape = RoundedCornerShape(12.dp)) {
                        Text(if (playing) "⏸ 暂停" else "▶ 播放")
                    }
                    Spacer(Modifier.width(8.dp))
                    FilledTonalButton(
                        onClick = {
                            if (denseBusy) return@FilledTonalButton
                            val src = source ?: return@FilledTonalButton
                            denseBusy = true; denseMsg = "正在生成连续帧（20fps）…"
                            scope.launch {
                                val n = withContext(Dispatchers.IO) { runCatching { VideoFrameExtractor.extractDense(src, dir) }.getOrDefault(-1) }
                                frames = loadFrames()
                                denseMsg = if (n > 0) "连续帧就绪：$n 帧" else "连续帧生成失败"
                                denseBusy = false
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) { Text(if (denseBusy) "生成中…" else "生成连续帧(20fps)") }
                    denseMsg?.let { Spacer(Modifier.width(8.dp)); Text(it, style = MaterialTheme.typography.labelSmall, color = if (it.contains("失败")) Coral else TextMid) }
                    Spacer(Modifier.width(10.dp))
                    Text("${tlFmt(posSec)} / ${tlFmt(durationSec)}", style = MaterialTheme.typography.labelMedium, color = TextHigh)
                    Spacer(Modifier.width(12.dp))
                    Slider(
                        value = posSec.toFloat(),
                        onValueChange = { seek(it.toDouble()) },
                        valueRange = 0f..(durationSec.coerceAtLeast(1.0).toFloat()),
                        modifier = Modifier.weight(1f)
                    )
                    if (audioError != null) { Spacer(Modifier.width(8.dp)); Text("音频不可用：$audioError", style = MaterialTheme.typography.labelSmall, color = Coral) }
                }
                // 当前画面
                Box(
                    Modifier.fillMaxWidth().height(230.dp).clip(RoundedCornerShape(10.dp)).background(Veil),
                    contentAlignment = Alignment.Center
                ) {
                    val bmp: ImageBitmap? = currentFrame?.let { fr ->
                        remember(fr.imagePath) {
                            runCatching { File(fr.imagePath).inputStream().use { loadImageBitmap(it) } }.getOrNull()
                        }
                    }
                    if (bmp != null) Image(bmp, null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().height(230.dp))
                    else Text("暂无关键帧（先执行『抽帧 OCR』）", color = TextMid)
                }
                Spacer(Modifier.height(12.dp))

                val segments: List<Triple<Double, Double, String>> =
                    if (report != null) report.segments.map { Triple(it.start, it.end, "${videoId}_${it.start.toInt()}") }
                    else timeline.map { Triple(it.start, it.end, "${videoId}_${it.start.toInt()}") }

                if (segments.isEmpty()) {
                    Text("暂无内容：先执行『抽帧 OCR』与『深度整理』", color = TextMid)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(segments) { (start, end, pid) ->
                            val digest = report?.segments?.firstOrNull { it.start == start && it.end == end }
                            val item = timeline.firstOrNull { kotlin.math.abs(it.start - start) < 0.05 }
                            val frame = frames.firstOrNull { it.timestamp in start..end }
                                ?: frames.minByOrNull { kotlin.math.abs(it.timestamp - start) }
                            val mastery = masteryMap[pid]
                            Column(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                    .background(Glimmer.copy(alpha = 0.30f)).padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.Top) {
                                    if (frame != null) {
                                        val bmp: ImageBitmap? = remember(frame.imagePath) {
                                            runCatching { File(frame.imagePath).inputStream().use { loadImageBitmap(it) } }.getOrNull()
                                        }
                                        if (bmp != null) Image(bmp, null, contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(150.dp, 84.dp).clip(RoundedCornerShape(8.dp)))
                                        else Box(Modifier.size(150.dp, 84.dp).clip(RoundedCornerShape(8.dp)).background(Veil))
                                        Spacer(Modifier.width(12.dp))
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("[${tlFmt(start)} - ${tlFmt(end)}]", style = MaterialTheme.typography.labelMedium, color = Aureate, fontWeight = FontWeight.SemiBold)
                                            Spacer(Modifier.width(10.dp))
                                            FilledTonalButton(onClick = { seek(start); if (!playing) togglePlay() }, shape = RoundedCornerShape(10.dp)) { Text("▶ 本段") }
                                            Spacer(Modifier.width(6.dp))
                                            FilledTonalButton(onClick = { mark(pid, 5) }, shape = RoundedCornerShape(10.dp)) { Text("已掌握") }
                                            Spacer(Modifier.width(6.dp))
                                            FilledTonalButton(onClick = { mark(pid, 1) }, shape = RoundedCornerShape(10.dp)) { Text("不熟") }
                                            mastery?.let {
                                                Spacer(Modifier.width(8.dp))
                                                Text("SM-2：${it.status.name} · 复习 ${it.reviewCount} 次" + if (it.nextReviewAt > 0) " · 下次 ${tlFmt(it.nextReviewAt / 1000.0 % 86400)}" else "",
                                                    style = MaterialTheme.typography.labelSmall, color = TextMid)
                                            }
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        val cons = digest?.consensus.orEmpty()
                                        if (cons.isNotEmpty()) cons.forEach { c -> Text("· $c", style = MaterialTheme.typography.bodyMedium, color = TextHigh) }
                                        else item?.let { it.speech.ifBlank { it.screenText } }?.takeIf { it.isNotBlank() }?.let { Text("· ${it.take(200)}", style = MaterialTheme.typography.bodyMedium, color = TextHigh) }
                                        digest?.divergences?.take(3)?.forEach { d ->
                                            Spacer(Modifier.height(4.dp))
                                            Text("分歧 · ${d.topic}：" + d.views.joinToString("；"), style = MaterialTheme.typography.bodySmall, color = Coral, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                        }
                                        item?.screenText?.takeIf { it.isNotBlank() }?.let {
                                            Spacer(Modifier.height(4.dp))
                                            Text("画面文字：${it.take(160)}", style = MaterialTheme.typography.bodySmall, color = MoonSilver)
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
}
