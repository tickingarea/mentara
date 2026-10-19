package com.learnagent.ui

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.learnagent.data.TimelineItem
import com.learnagent.ui.theme.Aureate
import com.learnagent.ui.theme.MoonSilver
import com.learnagent.ui.theme.TextHigh
import com.learnagent.ui.theme.TextMid
import com.learnagent.ui.theme.Veil
import java.io.File

private fun tlFmt(sec: Double): String {
    val t = sec.toInt().coerceAtLeast(0)
    fun two(v: Int) = if (v < 10) "0$v" else "$v"
    return two(t / 60) + ":" + two(t % 60)
}

/**
 * 安卓学习时间线：内嵌播放原视频（VideoView，带原音频）+ 逐段列表；
 * 点某段 → 视频 seek 到该时间并暂停在该帧（你要的"暂停画面跳到对应帧"）。
 */
@Composable
fun AndroidStudyTimeline(
    videoFile: File?,
    timeline: List<TimelineItem>,
    onClose: () -> Unit
) {
    var videoView by remember { mutableStateOf<VideoView?>(null) }
    var fullscreen by remember { mutableStateOf(false) }
    val keyTags = remember(timeline) { com.learnagent.core.KeyKnowledge.tags(timeline) }
    val tlCtx = androidx.compose.ui.platform.LocalContext.current
    var clickPlays by remember { mutableStateOf(tlCtx.getSharedPreferences("mentara", android.content.Context.MODE_PRIVATE).getBoolean("timelineClickPlays", true)) }
    val sideTags = remember(timeline) { com.learnagent.core.SideNote.tags(timeline) }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().padding(10.dp), color = Veil.copy(alpha = 0.98f), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxSize().padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("学习时间线", style = androidx.compose.material3.MaterialTheme.typography.titleMedium, color = Accent)
                    Spacer(Modifier.width(10.dp))
                    Text("点某段 → 视频跳到该帧并暂停", style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = TextMid)
                    Spacer(Modifier.weight(1f))
                    Text(
                        if (clickPlays) "点击后：继续播放" else "点击后：暂停记笔记",
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        color = TextMid,
                        modifier = Modifier.clickable {
                            clickPlays = !clickPlays
                            tlCtx.getSharedPreferences("mentara", android.content.Context.MODE_PRIVATE).edit().putBoolean("timelineClickPlays", clickPlays).apply()
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                    if (videoFile != null && videoFile.isFile) {
                        TextButton(onClick = { fullscreen = !fullscreen }) { Text(if (fullscreen) "退出全屏" else "全屏") }
                    }
                    TextButton(onClick = onClose) { Text("关闭") }
                }
                if (videoFile != null && videoFile.isFile) {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                val mc = MediaController(ctx)
                                mc.setAnchorView(this)
                                setMediaController(mc)
                                setVideoURI(Uri.fromFile(videoFile))
                                setOnPreparedListener { it.isLooping = false }
                                videoView = this
                            }
                        },
                        modifier = if (fullscreen) Modifier.fillMaxSize() else Modifier.fillMaxWidth().height(230.dp)
                    )
                } else {
                    Text("找不到源文件（请重新导入）", color = TextMid, modifier = Modifier.padding(8.dp))
                }
                if (!fullscreen) {
                Spacer(Modifier.height(8.dp))
                if (timeline.isEmpty()) {
                    Text("暂无内容：先点『抽帧 OCR』『转写』", color = TextMid)
                } else {
                    LazyColumn(Modifier.weight(1f)) {
                        items(timeline.size) { i ->
                            val item = timeline[i]
                            val keyTag = keyTags.getOrNull(i)
                            Column(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                    .background(if (keyTag != null) Aureate.copy(alpha = 0.14f) else MoonSilver.copy(alpha = 0.06f))
                                    .clickable {
                                        videoView?.let { vv ->
                                            runCatching {
                                                vv.seekTo((item.start * 1000).toInt())
                                                if (clickPlays) vv.start() else vv.pause()
                                            }
                                        }
                                    }
                                    .padding(8.dp)
                            ) {
                                Text("[${tlFmt(item.start)} - ${tlFmt(item.end)}]" + if (keyTag != null) "   ★必学·$keyTag" else "" + (sideTags.getOrNull(i)?.let { "   ⤼$it" } ?: ""), color = Aureate, style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                                Spacer(Modifier.height(2.dp))
                                if (item.speech.isNotBlank()) {
                                    Text("语音：${item.speech.take(140)}", color = TextHigh, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                                }
                                if (item.screenText.isNotBlank()) {
                                    Spacer(Modifier.height(2.dp))
                                    Text("画面：${item.screenText.take(80)}", color = MoonSilver, style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
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

@Composable
fun AndroidTimelinePane(videoFile: File?, timeline: List<TimelineItem>) {
    var videoView by remember { mutableStateOf<VideoView?>(null) }
    var fullscreen by remember { mutableStateOf(false) }
    val keyTags = remember(timeline) { com.learnagent.core.KeyKnowledge.tags(timeline) }
    val sideTags = remember(timeline) { com.learnagent.core.SideNote.tags(timeline) }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var clickPlays by remember { mutableStateOf(ctx.getSharedPreferences("mentara", android.content.Context.MODE_PRIVATE).getBoolean("timelineClickPlays", true)) }
    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("时间线 · 主视图", style = androidx.compose.material3.MaterialTheme.typography.labelMedium, color = Accent)
            Spacer(Modifier.weight(1f))
            if (videoFile != null && videoFile.isFile) {
                Text(
                    if (fullscreen) "退出全屏" else "全屏",
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = Accent,
                    modifier = Modifier.clickable { fullscreen = !fullscreen }
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                if (clickPlays) "点击后：继续播放" else "点击后：暂停记笔记",
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                color = TextMid,
                modifier = Modifier.clickable {
                    clickPlays = !clickPlays
                    ctx.getSharedPreferences("mentara", android.content.Context.MODE_PRIVATE).edit().putBoolean("timelineClickPlays", clickPlays).apply()
                }
            )
        }
        Spacer(Modifier.height(6.dp))
        if (videoFile != null && videoFile.isFile) {
            AndroidView(
                factory = { c ->
                    VideoView(c).apply {
                        val mc = MediaController(c)
                        mc.setAnchorView(this)
                        setMediaController(mc)
                        setVideoURI(Uri.fromFile(videoFile))
                        setOnPreparedListener { it.isLooping = false }
                        videoView = this
                    }
                },
                modifier = if (fullscreen) Modifier.fillMaxSize() else Modifier.fillMaxWidth().height(240.dp)
            )
            Spacer(Modifier.height(8.dp))
        } else {
            Text("找不到源文件（请重新导入）", color = TextMid, modifier = Modifier.padding(8.dp))
        }
        if (timeline.isEmpty()) {
            Text("暂无内容：解析后会逐条出现（解析中也会实时刷新）", color = TextMid)
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(timeline.size) { i ->
                    val item = timeline[i]
                    val keyTag = keyTags.getOrNull(i)
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            .background(if (keyTag != null) Aureate.copy(alpha = 0.14f) else MoonSilver.copy(alpha = 0.06f))
                            .clickable {
                                videoView?.let { vv ->
                                    runCatching {
                                        vv.seekTo((item.start * 1000).toInt())
                                        if (clickPlays) vv.start() else vv.pause()
                                    }
                                }
                            }
                            .padding(8.dp)
                    ) {
                        Text("[" + tlFmt(item.start) + " - " + tlFmt(item.end) + "]" + if (keyTag != null) "   ★" + keyTag else "" + (sideTags.getOrNull(i)?.let { "   ⤼" + it } ?: ""), color = Aureate, style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                        if (item.speech.isNotBlank()) {
                            Text("语音：" + item.speech.take(140), color = TextHigh, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                        }
                        if (item.screenText.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text("画面：" + item.screenText.take(80), color = MoonSilver, style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}