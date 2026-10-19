package com.learnagent.asr

import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineSenseVoiceModelConfig
import com.learnagent.data.TranscriptSegment
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * SenseVoice-Small（阿里开源，中文 CER ≈5% 量级）本地推理引擎，基于 sherpa-onnx。
 * 模型内置在 APK（assets/sensevoice），首启强制安装到私有目录；资源缺失时联网兜底（hf-mirror）。
 */
object SenseVoiceEngine {

    private const val MODEL_FILE = "model.int8.onnx"
    private const val TOKENS_FILE = "tokens.txt"
    private const val HF = "https://hf-mirror.com/csukuangfj/sherpa-onnx-sense-voice-zh-en-ja-ko-yue-2024-07-17/resolve/main/"

    fun modelDir(filesDir: File): File = File(filesDir, "models/sensevoice").apply { mkdirs() }

    fun ready(filesDir: File): Boolean {
        val d = modelDir(filesDir)
        val m = File(d, MODEL_FILE)
        val t = File(d, TOKENS_FILE)
        return m.isFile && m.length() > 200L * 1024 * 1024 && t.isFile && t.length() > 50_000
    }

    /**
     * 首启安装：优先从 APK 内置资源拷贝；**逐文件独立校验**，某个文件失败自动联网兜底（hf-mirror）。
     * 已就位的文件直接跳过（中断后可秒级续装）。
     */
    fun installFromAssets(context: android.content.Context, filesDir: File, onProgress: (Long, Long) -> Unit = { _, _ -> }): Boolean {
        if (ready(filesDir)) return true
        val d = modelDir(filesDir)
        runCatching { File(d, MODEL_FILE + ".part").delete(); File(d, TOKENS_FILE + ".part").delete() } // 残留清理
        var done = 0L
        val total = 239_600_000L
        for (name in listOf(MODEL_FILE, TOKENS_FILE)) {
            val dest = File(d, name)
            val needMin = if (name == MODEL_FILE) 200L * 1024 * 1024 else 50_000L
            if (dest.isFile && dest.length() > needMin) {
                done += dest.length()
                continue
            }
            // ① APK 内置资源
            var ok = runCatching {
                val tmp = File(d, name + ".part")
                context.assets.open("sensevoice/$name").use { ins ->
                    tmp.outputStream().use { out ->
                        val buf = ByteArray(1 shl 16)
                        while (true) {
                            val r = ins.read(buf)
                            if (r < 0) break
                            out.write(buf, 0, r)
                            done += r
                            onProgress(done, total)
                        }
                    }
                }
                if (dest.exists()) dest.delete()
                tmp.renameTo(dest)
            }.getOrDefault(false)
            // ② 兜底：联网下载（资源缺失/拷贝异常时）
            if (!ok || !dest.isFile || dest.length() <= needMin) {
                ok = runCatching {
                    val tmp = File(d, name + ".part")
                    val conn = (URL(HF + name).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 15000
                        readTimeout = 120000
                        instanceFollowRedirects = true
                    }
                    if (conn.responseCode !in 200..299) return@runCatching false
                    conn.inputStream.use { ins ->
                        tmp.outputStream().use { out ->
                            val buf = ByteArray(1 shl 16)
                            while (true) {
                                val r = ins.read(buf)
                                if (r < 0) break
                                out.write(buf, 0, r)
                                done += r
                                onProgress(done, total)
                            }
                        }
                    }
                    if (dest.exists()) dest.delete()
                    tmp.renameTo(dest)
                }.getOrDefault(false)
            }
            if (!ok || !dest.isFile || dest.length() <= needMin) return false
        }
        return ready(filesDir)
    }

    /** 下载模型（模型 + tokens 两个文件）；返回是否成功。 */
    fun download(filesDir: File, onProgress: (String, Long, Long) -> Unit = { _, _, _ -> }): Boolean {
        val d = modelDir(filesDir)
        val ok1 = downloadOne(HF + MODEL_FILE, File(d, MODEL_FILE), onProgress)
        val ok2 = ok1 && downloadOne(HF + TOKENS_FILE, File(d, TOKENS_FILE), onProgress)
        if (ok2 && !ready(filesDir)) {
            runCatching { File(d, MODEL_FILE).delete() }
            return false
        }
        return ok2
    }

    private fun downloadOne(url: String, dest: File, onProgress: (String, Long, Long) -> Unit): Boolean {
        val tmp = File(dest.parentFile, dest.name + ".part")
        return try {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 60000
                instanceFollowRedirects = true
            }
            if (conn.responseCode !in 200..299) return false
            val total = conn.contentLengthLong
            conn.inputStream.use { ins ->
                tmp.outputStream().use { out ->
                    val buf = ByteArray(1 shl 16)
                    var done = 0L
                    while (true) {
                        val r = ins.read(buf)
                        if (r < 0) break
                        out.write(buf, 0, r)
                        done += r
                        onProgress(dest.name, done, if (total > 0) total else -1)
                    }
                }
            }
            if (dest.exists()) dest.delete()
            tmp.renameTo(dest)
        } catch (e: Exception) {
            runCatching { tmp.delete() }
            false
        }
    }

    /** 转写一个 16k 单声道 16bit WAV。返回整段一条（时间戳由上层分块偏移负责）。 */
    fun transcribe(filesDir: File, wav: File): List<TranscriptSegment> {
        val d = modelDir(filesDir)
        val model = File(d, MODEL_FILE)
        val tokens = File(d, TOKENS_FILE)
        if (!model.isFile || !tokens.isFile) throw IllegalStateException("缺少 SenseVoice 模型（请先完成安装）")

        val parsed = readWavFloat(wav)
        val samples = parsed.first
        val rate = parsed.second
        if (samples.isEmpty()) throw IllegalStateException("音频为空（SenseVoice）")

        val config = OfflineRecognizerConfig(
            featConfig = FeatureConfig(sampleRate = 16000, featureDim = 80),
            modelConfig = OfflineModelConfig(
                senseVoice = OfflineSenseVoiceModelConfig(
                    model = model.absolutePath,
                    language = "zh",
                    useInverseTextNormalization = true
                ),
                tokens = tokens.absolutePath,
                numThreads = 4,
                provider = "cpu"
            )
        )
        val recognizer = OfflineRecognizer(null, config)
        try {
            val stream = recognizer.createStream()
            stream.acceptWaveform(samples, rate)
            recognizer.decode(stream)
            val text = recognizer.getResult(stream).text.trim()
            return if (text.isBlank()) emptyList() else listOf(TranscriptSegment(start = 0.0, end = 0.0, text = text, speaker = null))
        } finally {
            runCatching { recognizer.release() }
        }
    }

    /** 自研 WAV 解析：16bit PCM，多声道混单声道 → FloatArray。 */
    private fun readWavFloat(wav: File): Pair<FloatArray, Int> {
        val bytes = wav.readBytes()
        if (bytes.size < 44) return FloatArray(0) to 16000
        var pos = 12
        var channels = 1; var sampleRate = 16000; var bits = 16; var dataOff = -1; var dataLen = 0
        while (pos <= bytes.size - 8) {
            val id = String(bytes, pos, 4, Charsets.US_ASCII)
            val size = (bytes[pos + 4].toInt() and 0xFF) or ((bytes[pos + 5].toInt() and 0xFF) shl 8) or
                ((bytes[pos + 6].toInt() and 0xFF) shl 16) or ((bytes[pos + 7].toInt() and 0xFF) shl 24)
            when (id) {
                "fmt " -> {
                    channels = (bytes[pos + 10].toInt() and 0xFF) or ((bytes[pos + 11].toInt() and 0xFF) shl 8)
                    sampleRate = (bytes[pos + 12].toInt() and 0xFF) or ((bytes[pos + 13].toInt() and 0xFF) shl 8) or
                        ((bytes[pos + 14].toInt() and 0xFF) shl 16) or ((bytes[pos + 15].toInt() and 0xFF) shl 24)
                    bits = (bytes[pos + 22].toInt() and 0xFF) or ((bytes[pos + 23].toInt() and 0xFF) shl 8)
                }
                "data" -> { dataOff = pos + 8; dataLen = size; break }
            }
            pos += 8 + size + (size % 2)
        }
        if (dataOff < 0 || bits != 16) return FloatArray(0) to sampleRate
        if (channels < 1) channels = 1
        val avail = bytes.size - dataOff
        if (dataLen <= 0 || dataLen > avail) dataLen = avail
        val frames = dataLen / (2 * channels)
        val out = FloatArray(frames)
        for (f in 0 until frames) {
            var acc = 0f
            for (c in 0 until channels) {
                val i = dataOff + (f * channels + c) * 2
                val s = ((bytes[i].toInt() and 0xFF) or (bytes[i + 1].toInt() shl 8)).toShort()
                acc += s.toFloat()
            }
            out[f] = acc / channels / 32768f
        }
        return out to sampleRate
    }
}
