package com.learnagent.asr

import com.learnagent.data.TranscriptSegment
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 基于 Windows 内置语音识别（System.Speech）的本地转写实现。
 *
 * 通过随包附带的 `transcribe.ps1` 调用 zh-CN 识别引擎，对 WAV 音频做离线转写。
 * 无需 Python / Vosk / Whisper，也无需下载模型。
 */
object WindowsTranscriber {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun isWindows(): Boolean =
        System.getProperty("os.name").orEmpty().lowercase().contains("windows")

    private fun powershellPath(): String {
        val root = System.getenv("SystemRoot") ?: "C:\\Windows"
        val full = File(root, "System32\\WindowsPowerShell\\v1.0\\powershell.exe")
        return if (full.isFile) full.absolutePath else "powershell.exe"
    }

    /**
     * 启动 PowerShell。Windows 在短时间内密集创建进程时会偶发
     * CreateProcess error=5（拒绝访问），此处做有限重试。
     */
    private fun startProcess(args: List<String>): Process {
        var last: java.io.IOException? = null
        for (attempt in 0 until 3) {
            try {
                return ProcessBuilder(args).redirectErrorStream(true).start()
            } catch (e: java.io.IOException) {
                last = e
                Thread.sleep(300L * (attempt + 1))
            }
        }
        throw last!!
    }

    /**
     * 转写一个 WAV 音频文件（16kHz/单声道效果最佳）。
     *
     * @return 转录片段列表（含 start/end 秒与文本）
     */
    fun transcribe(wav: File, culture: String = "zh-CN"): List<TranscriptSegment> {
        require(wav.isFile) { "音频不存在: ${wav.absolutePath}" }
        check(isWindows()) { "WindowsTranscriber 仅支持 Windows 平台" }

        val scriptFile = File.createTempFile("learnagent_transcribe", ".ps1")
        val outFile = File.createTempFile("learnagent_transcribe_out", ".json")
        try {
            val script = WindowsTranscriber::class.java.getResourceAsStream("/transcribe.ps1")
                ?: error("classpath 缺少 transcribe.ps1")
            script.use { input -> scriptFile.outputStream().use { input.copyTo(it) } }

            val process = startProcess(
                listOf(
                    powershellPath(), "-NoProfile", "-ExecutionPolicy", "Bypass",
                    "-File", scriptFile.absolutePath,
                    "-Wav", wav.absolutePath,
                    "-Out", outFile.absolutePath,
                    "-Culture", culture
                )
            )

            val log = process.inputStream.bufferedReader().readText()
            val exit = process.waitFor()
            if (exit != 0) error("转写进程失败 exit=$exit\n$log")

            val text = outFile.readText(Charsets.UTF_8).trim()
            if (text.isEmpty()) return emptyList()
            return json.decodeFromString(ListSerializer(TranscriptSegment.serializer()), text)
        } finally {
            scriptFile.delete()
            outFile.delete()
        }
    }
}
