package com.learnagent.app

import com.learnagent.asr.WindowsTranscriber
import com.learnagent.config.AppConfig
import com.learnagent.config.ConfigStore
import com.learnagent.core.ContextAnalyzer
import com.learnagent.core.PolishedTranscript
import com.learnagent.core.TranscriptPolisher
import com.learnagent.data.TranscriptSegment
import com.learnagent.core.DebateModels
import com.learnagent.core.DeepDigestEngine
import com.learnagent.core.DeepDigestReport
import com.learnagent.core.DigestProgress
import com.learnagent.llm.NvidiaLlmClient
import com.learnagent.media.VideoFrameExtractor
import com.learnagent.media.VideoToWav
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 对已导入视频执行真实处理，并把结果写入 results/<videoId>/。
 * 结果文件与 Android/桌面共享同一数据格式（transcript.json / frames.json）。
 */
object VideoActions {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; isLenient = true }

    private fun videoDir(config: AppConfig, videoId: String): File =
        File(ConfigStore.resultsDir(config), videoId).apply { mkdirs() }

    /** 语音转写：视频 → WAV → 文本 → transcript.json。 */
    fun transcribe(config: AppConfig, videoId: String): String {
        val source = ConfigStore.sourceFile(config, videoId) ?: return "找不到源文件（请重新导入）"
        val wav = if (VideoToWav.isWav(source)) {
            source
        } else {
            val tmp = File.createTempFile("learnagent_audio", ".wav")
            if (!VideoToWav.extractAudio(source, tmp)) {
                tmp.delete()
                return "音频提取失败（无音轨或格式不支持）"
            }
            tmp
        }
        return try {
            val segments = WindowsTranscriber.transcribe(wav)
            File(videoDir(config, videoId), "transcript.json").writeText(json.encodeToString(segments))
            "转写完成：${segments.size} 段"
        } finally {
            if (wav != source) wav.delete()
        }
    }

    /** 抽帧 + OCR：视频 → 代表帧 → 文字 → frames.json。 */
    fun ocrFrames(config: AppConfig, videoId: String): String {
        val source = ConfigStore.sourceFile(config, videoId) ?: return "找不到源文件（请重新导入）"
        val frames = VideoFrameExtractor.extractAndOcr(source, videoDir(config, videoId))
        return "抽帧 OCR 完成：${frames.size} 帧"
    }

    /**
     * 三模型深度整理（批次 A）：
     * 逐段三模型独立整理 → 交叉校对 N 轮 → 共识/分歧；产出 deep_digest.json + deep_digest.md。
     */
    suspend fun deepDigest(
        config: AppConfig,
        videoId: String,
        onProgress: (DigestProgress) -> Unit = {}
    ): String {
        val key = config.nvidiaApiKey.trim()
        if (key.isBlank()) return "未配置 NVIDIA API Key（设置页填写）"
        val timeline = ConfigStore.loadTimeline(config, videoId)
        if (timeline.isEmpty()) return "当前视频没有内容（先转写/抽帧）"
        onProgress(DigestProgress(0, timeline.size, "分析学习单元…"))
        val units = ContextAnalyzer(null).analyze(timeline, videoId)

        fun client(model: String) = NvidiaLlmClient(key, model = model)
        val engine = DeepDigestEngine(
            seats = listOf(
                // A 席：Kimi K3，超时/失败自动换 K2.6
                Triple("Kimi K3", client(DebateModels.DEFAULT_A), client("moonshotai/kimi-k2.6")),
                Triple("DeepSeek V4.1", client(DebateModels.DEFAULT_B), null),
                Triple("GLM 5.3", client(DebateModels.DEFAULT_C), null)
            ),
            callTimeoutMs = 180_000,
            retries = 2,
            maxRounds = 3
        )
        val report: DeepDigestReport = engine.digest(videoId, units, onProgress)
        val dir = videoDir(config, videoId)
        File(dir, "deep_digest.json").writeText(json.encodeToString(DeepDigestReport.serializer(), report))
        File(dir, "deep_digest.md").writeText(renderMarkdown(report))
        return "深度整理完成：${report.segments.size} 段，共识 ${report.consensus.size} 条，分歧 ${report.divergences.size} 条"
    }

    /** 转写纠错（三模型共识，带出处）。 */
    suspend fun polishTranscript(config: AppConfig, videoId: String, onProgress: (Int, Int) -> Unit = { _, _ -> }): String {
        val key = config.nvidiaApiKey.trim()
        if (key.isBlank()) return "未配置 NVIDIA API Key"
        val f = File(videoDir(config, videoId), "transcript.json")
        if (!f.isFile) return "没有转写文件（请先转写）"
        val segs = json.decodeFromString(ListSerializer(TranscriptSegment.serializer()), f.readText())
        fun c(m: String) = NvidiaLlmClient(key, model = m)
        val polisher = TranscriptPolisher(
            listOf(
                "GLM 5.3" to c(DebateModels.DEFAULT_C),
                "DeepSeek V4.1" to c(DebateModels.DEFAULT_B),
                "Kimi K3" to c(DebateModels.DEFAULT_A)
            ),
            timeoutMs = 120_000, retries = 1
        )
        val r = polisher.polish(videoId, segs, onProgress)
        File(videoDir(config, videoId), "transcript_polished.json")
            .writeText(json.encodeToString(PolishedTranscript.serializer(), r))
        File(videoDir(config, videoId), "transcript_polished.md").writeText(polisher.markdown(r))
        return "纠错完成：${r.segments.size} 句 · 高置信 ${r.segments.count { it.confidence >= 1f }}"
    }

    private fun renderMarkdown(r: DeepDigestReport): String {
        fun ts(sec: Double): String {
            val t = sec.toInt(); fun two(v: Int) = if (v < 10) "0$v" else "$v"
            return two(t / 60) + ":" + two(t % 60)
        }
        val sb = StringBuilder()
        sb.append("# 深度整理：").append(r.videoId).append("\n\n")
        sb.append("模型：").append(r.models.joinToString(" / ")).append("；交叉校对 ").append(r.rounds).append(" 轮\n\n")
        sb.append("## 全局共识（必学知识点）\n")
        r.consensus.forEach { sb.append("- ").append(it).append('\n') }
        sb.append("\n## 全局分歧\n")
        r.divergences.forEach { d ->
            sb.append("- 主题：").append(d.topic).append('\n')
            d.views.forEach { v -> sb.append("  - ").append(v).append('\n') }
        }
        sb.append("\n## 分段明细\n")
        r.segments.forEach { s ->
            sb.append("\n### [").append(ts(s.start)).append(" - ").append(ts(s.end)).append("]\n")
            if (s.degraded) sb.append("（三模型均不可用，已降级）\n")
            sb.append("**共识**\n"); s.consensus.forEach { sb.append("- ").append(it).append('\n') }
            if (s.divergences.isNotEmpty()) {
                sb.append("**分歧**\n")
                s.divergences.forEach { d ->
                    sb.append("- ").append(d.topic).append("：").append(d.views.joinToString("；")).append('\n')
                }
            }
            s.perModel.forEach { (m, txt) ->
                sb.append("\n**").append(m).append(" 定稿（原文）**\n\n").append(txt).append('\n')
            }
        }
        return sb.toString()
    }
}
