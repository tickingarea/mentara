package com.learnagent.core

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.learnagent.data.FrameOcr
import com.learnagent.data.TranscriptSegment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.coroutines.resume

/**
 * 安卓真实视频处理：语音转写（MediaExtractor/MediaCodec → 16k WAV → 系统识别器）+
 * 抽帧 OCR（MediaMetadataRetriever 取帧 → ML Kit 中文 OCR），写入 results/<id>/{transcript,frames}.json。
 */
object AndroidVideoActions {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val zhOptions by lazy { ChineseTextRecognizerOptions.Builder().build() }

    /** 轻量帧签名：缩到 32x32 灰度（简单像素差对比的成本底线，非全图差分）。 */
    private fun signatureOf(bmp: Bitmap): IntArray {
        val s = Bitmap.createScaledBitmap(bmp, 32, 32, true)
        val px = IntArray(32 * 32)
        s.getPixels(px, 0, 32, 0, 0, 32, 32)
        val g = IntArray(32 * 32)
        for (i in px.indices) {
            val c = px[i]
            g[i] = (((c shr 16) and 0xFF) * 299 + ((c shr 8) and 0xFF) * 587 + (c and 0xFF) * 114) / 1000
        }
        if (s !== bmp) s.recycle()
        return g
    }

    /** 相似度 = 1 - 平均灰度差/255；>= 0.90 视为画面未实质变化。 */
    private fun similarity(a: IntArray, b: IntArray): Double {
        if (a.size != b.size) return 0.0
        var diff = 0L
        for (i in a.indices) diff += kotlin.math.abs(a[i] - b[i])
        return 1.0 - (diff.toDouble() / a.size) / 255.0
    }

    /** ML Kit OCR（带 60 秒超时；超时/失败返回空串，绝不永久阻塞抽帧循环）。 */
    private suspend fun ocrText(recognizer: com.google.mlkit.vision.text.TextRecognizer, bmp: Bitmap, timeoutMs: Long = 60_000L): String {
        val ok = kotlinx.coroutines.withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { cont ->
                recognizer.process(InputImage.fromBitmap(bmp, 0))
                    .addOnSuccessListener { if (cont.isActive) cont.resume(it.text) }
                    .addOnFailureListener { if (cont.isActive) cont.resume("") }
                    .addOnCanceledListener { if (cont.isActive) cont.resume("") }
            }
        }
        if (ok == null) android.util.Log.w("MENTARA", "OCR 超时 ${timeoutMs}ms，该帧跳过")
        return ok ?: ""
    }

    private fun moodOf(bmp: Bitmap): String {
        var r = 0L; var g = 0L; var b = 0L; var n = 0L; var minL = 255f; var maxL = 0f
        val sx = (bmp.width / 24).coerceAtLeast(1); val sy = (bmp.height / 24).coerceAtLeast(1)
        var y = 0
        while (y < bmp.height) { var x = 0
            while (x < bmp.width) {
                val c = bmp.getPixel(x, y)
                val rr = (c shr 16) and 0xFF; val gg = (c shr 8) and 0xFF; val bb = c and 0xFF
                r += rr; g += gg; b += bb; n++
                val l = 0.299f * rr + 0.587f * gg + 0.114f * bb
                if (l < minL) minL = l; if (l > maxL) maxL = l
                x += sx
            }
            y += sy
        }
        if (n == 0L) return ""
        return Atmosphere.label(Atmosphere.fromAverages(r.toFloat() / n, g.toFloat() / n, b.toFloat() / n, minL, maxL))
    }

    /** 语音转写：视频 → WAV → 文本 → transcript.json。 */
    suspend fun transcribe(context: Context, sourcePath: String, outDir: File, onWhisperProgress: (Int) -> Unit = {}): List<TranscriptSegment> {
        return withContext(Dispatchers.IO) {
            val wav = File(outDir, "audio.wav")
            AndroidAudioExtractor(context).extractToWav(sourcePath, wav.absolutePath) { }
            // ⓪ SenseVoice 全程优先：选了它且模型就位 → 一路用它（不看云端、不需要网络）
            val lpAny = context.getSharedPreferences("mentara", android.content.Context.MODE_PRIVATE)
            val wantSenseAny = (lpAny.getString("localAsrEngine", "sensevoice") ?: "sensevoice") == "sensevoice"
            if (wantSenseAny && com.learnagent.asr.SenseVoiceEngine.ready(context.filesDir)) {
                val outF0 = File(outDir, "transcript.json")
                val chunks0 = splitWav(wav, 15.0)
                val existing0 = if (outF0.isFile) runCatching {
                    json.decodeFromString(ListSerializer(TranscriptSegment.serializer()), outF0.readText())
                }.getOrDefault(emptyList()) else emptyList()
                val segs0 = ArrayList<TranscriptSegment>(existing0)
                val covered0 = existing0.lastOrNull()?.end ?: 0.0
                android.util.Log.d("MENTARA", "SenseVoice 全程优先：${chunks0.size} 块")
                chunks0.forEachIndexed { ci, (file, off) ->
                    if (off + 15.0 <= covered0) { file.delete(); return@forEachIndexed }
                    segs0.removeAll { it.start >= off - 0.5 }
                    val inner0 = com.learnagent.asr.SenseVoiceEngine.transcribe(context.filesDir, file)
                    inner0.forEach { s ->
                        val txt = s.text.trim()
                        val parts = txt.split(Regex("(?<=[。！？?!；;])")).map { it.trim() }.filter { it.isNotBlank() }
                        val fp = parts.filter { !TextQuality.isHallucinatedAsr(it) }
                        val use = if (fp.isEmpty()) listOf(if (txt.isBlank()) txt else "（未识别）") else fp
                        val totalChars = use.sumOf { it.length }.coerceAtLeast(1)
                        var accT = 0.0
                        use.forEach { p ->
                            val dur = 15.0 * p.length / totalChars
                            segs0.add(TranscriptSegment(start = off + accT, end = off + accT + dur, text = p, speaker = null))
                            accT += dur
                        }
                    }
                    file.delete()
                    outF0.writeText(json.encodeToString(ListSerializer(TranscriptSegment.serializer()), segs0))
                    onWhisperProgress(((ci + 1) * 100 / chunks0.size.coerceAtLeast(1)).coerceIn(0, 100))
                }
                if (segs0.isNotEmpty()) return@withContext segs0
            }
            // ① 云端 ASR 优先（配置了 Key 就走网络，手机不再加载本地模型）
            val prefs = context.getSharedPreferences("mentara", android.content.Context.MODE_PRIVATE)
            val asrKey = prefs.getString("asrApiKey", "") ?: ""
            if (asrKey.isNotBlank()) {
                val asrBase = prefs.getString("asrBaseUrl", "https://api.siliconflow.cn/v1") ?: "https://api.siliconflow.cn/v1"
                val asrModel = prefs.getString("asrModel", "TeleAI/TeleSpeechASR") ?: "TeleAI/TeleSpeechASR"
                val outF = File(outDir, "transcript.json")
                val chunks = splitWav(wav, 60.0) // 60 秒/块：时间戳更细、单包更小、可断点续跑
                val existing = if (outF.isFile) runCatching {
                    json.decodeFromString(ListSerializer(TranscriptSegment.serializer()), outF.readText())
                }.getOrDefault(emptyList()) else emptyList()
                val segsAll = ArrayList<TranscriptSegment>(existing)
                val covered = existing.lastOrNull()?.end ?: 0.0
                val cli = com.learnagent.llm.CloudAsrClient(asrKey, asrBase, asrModel)
                android.util.Log.d("MENTARA", "云端 ASR：${chunks.size} 块（60s/块）→ $asrBase model=$asrModel")
                chunks.forEachIndexed { ci, (file, off) ->
                    if (off + 60.0 <= covered) { file.delete(); return@forEachIndexed }
                    segsAll.removeAll { it.start >= off - 0.5 }
                    val r = cli.transcribeFile(file) // 失败直接抛错，不静默
                    if (r.segments.isNotEmpty()) {
                        r.segments.forEach { (s, e, t) -> if (t.isNotBlank()) segsAll.add(TranscriptSegment(off + s, off + e, t, null)) }
                    } else if (r.text.isNotBlank()) {
                        segsAll.add(TranscriptSegment(off, off + 60.0, r.text, null))
                    }
                    file.delete()
                    outF.writeText(json.encodeToString(ListSerializer(TranscriptSegment.serializer()), segsAll)) // 每块落盘，断点续跑
                    onWhisperProgress(((ci + 1) * 100 / chunks.size).coerceIn(0, 100))
                    android.util.Log.d("MENTARA", "云端 ASR 块 ${ci + 1}/${chunks.size} 完成，累计 ${segsAll.size} 段")
                }
                if (segsAll.isNotEmpty()) return@withContext segsAll
            }
            // 兜底：系统识别器（分段）
            val chunks = splitWav(wav, 25.0)
            val segments = ArrayList<TranscriptSegment>()
            withContext(Dispatchers.Main) {
                chunks.forEach { (file, offset) ->
                    val segs = awaitTranscribe(context, file.absolutePath)
                    segs.forEach { s ->
                        segments.add(
                            TranscriptSegment(
                                start = offset + (if (s.start > 0) s.start else 0.0),
                                end = offset + (if (s.end > 0) s.end else 25.0),
                                text = s.text,
                                speaker = s.speaker
                            )
                        )
                    }
                    file.delete()
                }
            }
            if (segments.isEmpty()) {
                val reason = AndroidSpeechTranscriber.lastError.ifBlank { "语音识别无结果" }
                throw IllegalStateException("语音转写 0 段：$reason")
            }
            File(outDir, "transcript.json")
                .writeText(json.encodeToString(ListSerializer(TranscriptSegment.serializer()), segments))
            segments
        }
    }

    /**
     * 三模型共识纠错：读 transcript.json → 纠错（改错别字/去幻觉、禁止新增信息）→ 覆盖写回
     * transcript.json（供时间线读取）+ 生成 transcript_polished.md。返回纠错后句数（-1=无转写）。
     */
    suspend fun polishTranscript(
        videoId: String,
        dir: File,
        apiKey: String,
        modelA: String,
        modelB: String,
        modelC: String,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): Int = withContext(Dispatchers.IO) {
        val f = File(dir, "transcript.json")
        if (!f.isFile) return@withContext -1
        val segs = json.decodeFromString(ListSerializer(TranscriptSegment.serializer()), f.readText())
        if (segs.isEmpty()) return@withContext 0
        fun c(m: String) = com.learnagent.llm.AndroidNimClient(apiKey, m)
        val polisher = TranscriptPolisher(
            listOf("A" to c(modelA), "B" to c(modelB), "C" to c(modelC)),
            timeoutMs = 120_000, retries = 1
        )
        val r = polisher.polish(videoId, segs, onProgress)
        val corrected = r.segments.map { TranscriptSegment(start = it.start, end = it.end, text = it.final, speaker = null) }
        f.writeText(json.encodeToString(ListSerializer(TranscriptSegment.serializer()), corrected))
        File(dir, "transcript_polished.md").writeText(polisher.markdown(r))
        corrected.size
    }

    /** 把 16k 单声道 16bit PCM WAV 按秒切分成若干子 WAV；返回 (文件, 起始秒)。 */
    /**
     * 视觉模型画面描述：给 frames.json 里的关键帧生成 caption（已有描述的跳过），
     * 写回 frames.json（经 merge 并入时间线的 caption 字段 → 进入大模型证据）。
     * 返回已带描述的帧数（-1 = 没有 frames.json）。
     */
    suspend fun captionFrames(
        dir: File,
        apiKey: String,
        visionModel: String,
        maxFrames: Int = 1_000_000,
        everyNth: Int = 1,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): Int = withContext(Dispatchers.IO) {
        val f = File(dir, "frames.json")
        if (!f.isFile) return@withContext -1
        val frames = json.decodeFromString(ListSerializer(FrameOcr.serializer()), f.readText())
        val step = everyNth.coerceAtLeast(1)
        val toDo = frames.filter {
            it.caption.isBlank() && it.imagePath.isNotBlank() && File(it.imagePath).isFile
        }.filterIndexed { p, _ -> p % step == 0 }.take(maxFrames)
        if (toDo.isEmpty()) return@withContext frames.count { it.caption.isNotBlank() }

        val captioner = com.learnagent.llm.VisionCaptioner(apiKey, visionModel)
        val byTs = frames.map { it.timestamp to it }.toMap().toMutableMap()
        var done = frames.count { it.caption.isNotBlank() }
        fun persist() {
            f.writeText(json.encodeToString(ListSerializer(FrameOcr.serializer()), frames.map { byTs[it.timestamp] ?: it }))
        }
        toDo.forEachIndexed { i, fr ->
            val c = try {
                captioner.caption(File(fr.imagePath))
            } catch (e: kotlinx.coroutines.CancellationException) {
                persist() // 取消：已完成描述先写盘，下次增量续跑
                throw e
            } catch (e: Exception) {
                ""
            }
            if (c.isNotBlank()) {
                byTs[fr.timestamp] = fr.copy(caption = c.take(200))
                done++
            }
            if ((i + 1) % 10 == 0 || i == toDo.size - 1) persist()
            onProgress(i + 1, toDo.size)
        }
        done
    }

    private fun splitWav(wav: File, secondsPerChunk: Double): List<Pair<File, Double>> {
        // V8 流式切块：只读 4KB 头 + 分段拷贝；78 分钟音频不再整段进内存（旧 readBytes 约 150MB 会 OOM）
        if (wav.length() < 44) return listOf(wav to 0.0)
        var channels = 1; var sampleRate = 16000; var dataOff = -1L; var dataLen = -1L
        java.io.RandomAccessFile(wav, "r").use { raf ->
            val head = ByteArray(4096)
            val n = raf.read(head)
            var pos = 12
            while (pos <= n - 8) {
                val id = String(head, pos, 4, Charsets.US_ASCII)
                var size = 0
                for (b in 0 until 4) size = size or ((head[pos + 4 + b].toInt() and 0xFF) shl (8 * b))
                when (id) {
                    "fmt " -> {
                        channels = (head[pos + 10].toInt() and 0xFF) or ((head[pos + 11].toInt() and 0xFF) shl 8)
                        sampleRate = 0
                        for (b in 0 until 4) sampleRate = sampleRate or ((head[pos + 12 + b].toInt() and 0xFF) shl (8 * b))
                    }
                    "data" -> { dataOff = (pos + 8).toLong(); dataLen = size.toLong() }
                }
                if (dataOff >= 0) break
                pos += 8 + size + (size % 2)
            }
        }
        if (dataOff < 0 || channels < 1) return listOf(wav to 0.0)
        val avail = wav.length() - dataOff
        if (dataLen <= 0 || dataLen > avail) dataLen = avail // 容错：声明长度不靠谱时按实际文件截取
        val bytesPerSec = sampleRate * channels * 2
        if (bytesPerSec <= 0) return listOf(wav to 0.0)
        val chunkBytes = (bytesPerSec * secondsPerChunk).toLong().coerceAtLeast(1)
        val out = ArrayList<Pair<File, Double>>()
        java.io.RandomAccessFile(wav, "r").use { raf ->
            val buf = ByteArray(1 shl 16)
            var off = 0L; var idx = 0
            while (off < dataLen && idx < 100000) {
                val len = minOf(chunkBytes, dataLen - off)
                val f = File(wav.parentFile, "chunk_%03d.wav".format(idx))
                f.outputStream().use { os ->
                    val h = ByteArray(44)
                    fun put32(p: Int, v: Long) { for (b in 0 until 4) h[p + b] = ((v shr (8 * b)) and 0xFF).toByte() }
                    fun put16(p: Int, v: Int) { h[p] = (v and 0xFF).toByte(); h[p + 1] = ((v shr 8) and 0xFF).toByte() }
                    "RIFF".toByteArray().copyInto(h, 0); put32(4, 36 + len); "WAVE".toByteArray().copyInto(h, 8)
                    "fmt ".toByteArray().copyInto(h, 12); put32(16, 16); put16(20, 1)
                    put16(22, channels); put32(24, sampleRate.toLong()); put32(28, bytesPerSec.toLong())
                    put16(32, channels * 2); put16(34, 16)
                    "data".toByteArray().copyInto(h, 36); put32(40, len)
                    os.write(h)
                    raf.seek(dataOff + off)
                    var copied = 0L
                    while (copied < len) {
                        val want = minOf(buf.size.toLong(), len - copied).toInt()
                        val r = raf.read(buf, 0, want)
                        if (r < 0) break
                        os.write(buf, 0, r); copied += r
                    }
                }
                out.add(f to (off.toDouble() / bytesPerSec))
                off += len; idx++
            }
        }
        return if (out.isEmpty()) listOf(wav to 0.0) else out
    }

    private suspend fun awaitTranscribe(context: Context, wavPath: String): List<TranscriptSegment> =
        suspendCancellableCoroutine { cont ->
            AndroidSpeechTranscriber(context).transcribe(wavPath) { segs ->
                if (cont.isActive) cont.resume(segs)
            }
        }

    /**
     * 抽帧 OCR（V4 全量不省略）：
     *  - OPTION_CLOSEST 精确取帧（不吸附关键帧 → 不再出现"同一画面重复"）；
     *  - 删除了原来的 3000 帧硬上限（那是静默截断的根因）；
     *  - 增量续跑：已抽过的时间点自动跳过（取消/中断后重跑不浪费）；
     *  - 每 500 帧落盘一次 + 取消时落盘，进度不丢；
     *  - 帧图缩到 ≤1280px 再存（省磁盘，OCR 精度不受影响）。
     */
    suspend fun ocrFrames(
        context: Context,
        sourcePath: String,
        outDir: File,
        everySeconds: Double = 1.0,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }
    ): List<FrameOcr> = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        val framesFile = File(outDir, "frames.json")
        val all = ArrayList<FrameOcr>()
        try {
            retriever.setDataSource(sourcePath)
            val durMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            val total = if (durMs > 0) ((durMs / 1000.0) / everySeconds).toInt() + 1 else 30
            // 增量续跑：载入已抽过的帧
            if (framesFile.isFile) {
                runCatching {
                    all.addAll(json.decodeFromString(ListSerializer(FrameOcr.serializer()), framesFile.readText()))
                }
            }
            val doneTs = HashSet<Double>(all.size * 2)
            all.forEach { doneTs.add(Math.round(it.timestamp * 1000.0) / 1000.0) }
            onProgress(all.size, total)
            val recognizer = TextRecognition.getClient(zhOptions)
            var newCount = 0
            var skipCount = 0
            var processed = 0
            var prevSig: IntArray? = null
            var nullStreak = 0
            var t = 0.0
            try {
                while ((durMs > 0 && t * 1000 < durMs) || (durMs <= 0 && nullStreak < 5 && all.size < 200000)) {
                    ensureActive()
                    val key = Math.round(t * 1000.0) / 1000.0
                    if (!doneTs.contains(key)) {
                        val bmp = runCatching {
                            retriever.getFrameAtTime((t * 1_000_000).toLong(), MediaMetadataRetriever.OPTION_CLOSEST)
                        }.getOrNull()
                        if (bmp != null) {
                            processed++
                            val sig = signatureOf(bmp)
                            val sim = if (prevSig != null) similarity(sig, prevSig!!) else 0.0
                            if (total <= 60 || processed % 10 == 0) {
                                android.util.Log.d("MENTARA", "抽帧 $processed/$total 相似度=" + "%.2f".format(sim))
                            }
                            if (prevSig != null && sim >= 0.90) {
                                // 画面未实质变化（PPT 停留/静止镜头）：跳过 OCR，不送入识别流程
                                skipCount++
                                bmp.recycle()
                                onProgress(processed, total)
                                t += everySeconds
                                continue
                            }
                            val cropped = runCatching {
                                val cw = bmp.width; val chh = bmp.height
                                Bitmap.createBitmap(bmp, 0, chh * 6 / 100, cw, (chh - chh * 6 / 100 - chh * 10 / 100).coerceAtLeast(1))
                            }.getOrDefault(bmp)
                            val rawText = ocrText(recognizer, cropped)
                            if (cropped !== bmp) cropped.recycle()
                            val text = if (TextQuality.isGarbageOcr(rawText)) "" else rawText
                            val saveBmp = if (bmp.width > 1280) {
                                Bitmap.createScaledBitmap(bmp, 1280, (bmp.height * 1280 / bmp.width).coerceAtLeast(1), true)
                            } else bmp
                            val img = File(outDir, "frame_%05d.jpg".format(all.size + 1))
                            runCatching { img.outputStream().use { saveBmp.compress(Bitmap.CompressFormat.JPEG, 78, it) } }
                            if (saveBmp !== bmp) saveBmp.recycle()
                            val mood = runCatching { moodOf(bmp) }.getOrDefault("")
                            all.add(FrameOcr(key, text, img.absolutePath, mood = mood))
                            doneTs.add(key)
                            bmp.recycle()
                            prevSig = sig
                            newCount++
                            nullStreak = 0
                            if (newCount % 2000 == 0) persistFrames(framesFile, all)
                            if (newCount % 20 == 0) onProgress(processed, total)
                        } else {
                            nullStreak++
                        }
                    }
                    t += everySeconds
                }
                onProgress(processed, total)
                android.util.Log.d("MENTARA", "抽帧完成：处理 $processed/总 $total，OCR 留存 ${all.size} 帧，相似跳过 $skipCount")
            } catch (e: kotlinx.coroutines.CancellationException) {
                persistFrames(framesFile, all) // 取消：进度落盘，下次增量续跑
                throw e
            }
            runCatching { recognizer.close() }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            // 出错也不崩，但绝不再静默：打进 Logcat 供排查
            android.util.Log.e("MENTARA", "ocrFrames 异常中断（已取 ${all.size} 帧）", e)
        } finally {
            persistFrames(framesFile, all)
            runCatching { retriever.release() }
        }
        all
    }

    /**
     * Mentara 永久 skill：把「完全未删减」的合并时间线（语音+画面+氛围+画面描述）提炼成 skill.md，
     * 永久存档（换页/重启/换视频都不丢）；问答时作为「永久知识库」注入模型。
     * 返回合并时间线条数。
     */
    suspend fun buildSkill(dir: File): Int = withContext(Dispatchers.IO) {
        val tf = File(dir, "transcript.json")
        val ff = File(dir, "frames.json")
        if (!tf.isFile && !ff.isFile) return@withContext 0
        val transcript = if (tf.isFile) runCatching {
            json.decodeFromString(ListSerializer(TranscriptSegment.serializer()), tf.readText())
        }.getOrDefault(emptyList()) else emptyList()
        val frames = if (ff.isFile) runCatching {
            json.decodeFromString(ListSerializer(FrameOcr.serializer()), ff.readText())
        }.getOrDefault(emptyList()) else emptyList()
        val timeline = com.learnagent.core.merge(transcript, frames)
        val body = com.learnagent.core.Evidence.aiDigest(timeline, 3000, 300_000)
        val header = buildString {
            append("# Mentara 永久知识 · ").append(dir.name).append('\n')
            append("生成时间：").append(java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date())).append('\n')
            append("语音段：").append(transcript.size).append(" · 原始帧：").append(frames.size)
            append("（完全未删减，全部保留在 frames.json；时间线显示层仅折叠完全重复的连续同画面帧）\n\n")
        }
        File(dir, "skill.md").writeText(header + body)
        timeline.size
    }

    /** 画面原图校对：视觉模型逐帧"看图改字"，直接修正 frames.json 里的 OCR 错别字。返回改掉的字数。 */
    suspend fun proofreadFramesWithVision(
        dir: File,
        apiKey: String,
        visionModel: String,
        maxFrames: Int = 300,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): Int = withContext(Dispatchers.IO) {
        val f = File(dir, "frames.json")
        if (!f.isFile) return@withContext -1
        val frames = json.decodeFromString(ListSerializer(FrameOcr.serializer()), f.readText())
        val toDo = frames.filter { it.text.isNotBlank() && it.imagePath.isNotBlank() && File(it.imagePath).isFile }.take(maxFrames)
        if (toDo.isEmpty()) return@withContext 0
        val vlm = com.learnagent.llm.VisionCaptioner(apiKey, visionModel)
        val byTs = frames.map { it.timestamp to it }.toMap().toMutableMap()
        fun persist() { f.writeText(json.encodeToString(ListSerializer(FrameOcr.serializer()), frames.map { byTs[it.timestamp] ?: it })) }
        var changed = 0
        toDo.forEachIndexed { i, fr ->
            val corrected = try {
                vlm.correctOcr(File(fr.imagePath), fr.text)
            } catch (e: kotlinx.coroutines.CancellationException) { persist(); throw e }
            catch (e: Exception) { "" }
            if (corrected.isNotBlank() && corrected.trim() != fr.text.trim()) {
                byTs[fr.timestamp] = fr.copy(text = corrected.take(500))
                changed++
            }
            if ((i + 1) % 20 == 0 || i == toDo.size - 1) persist()
            onProgress(i + 1, toDo.size)
        }
        changed
    }
    /** 读取「语音(Whisper) + 画面(OCR)」合并原文（供大模型纠错后处理）。 */
    suspend fun rawEvidence(dir: File, maxChars: Int = 12000): String = withContext(Dispatchers.IO) {
        runCatching {
            val tf = File(dir, "transcript.json"); val ff = File(dir, "frames.json")
            val tr = if (tf.isFile) json.decodeFromString(ListSerializer(TranscriptSegment.serializer()), tf.readText()) else emptyList()
            val fr = if (ff.isFile) json.decodeFromString(ListSerializer(FrameOcr.serializer()), ff.readText()) else emptyList()
            com.learnagent.core.Evidence.render(com.learnagent.core.merge(tr, fr), maxChars)
        }.getOrDefault("")
    }

    private fun persistFrames(file: File, frames: List<FrameOcr>) {
        runCatching {
            file.writeText(json.encodeToString(ListSerializer(FrameOcr.serializer()), frames))
        }
    }
}
