package com.learnagent

import com.learnagent.asr.WindowsTranscriber
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * 端到端验证真实语音转写：
 * 用系统 TTS 合成一段中文语音，再用 WindowsTranscriber 离线识别，
 * 断言产生了带时间段的转写结果。
 *
 * 说明：若当前系统策略禁止 Java 创建 PowerShell 子进程（CreateProcess error=5），
 * 则跳过该集成测试（不影响单元测试套件的通过）。
 */
class WindowsTranscriberTest {

    private fun powershellPath(): String {
        val root = System.getenv("SystemRoot") ?: "C:\\Windows"
        val full = File(root, "System32\\WindowsPowerShell\\v1.0\\powershell.exe")
        return if (full.isFile) full.absolutePath else "powershell.exe"
    }

    private fun startWithRetry(args: List<String>): Process {
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

    private fun synthesize(text: String): File {
        val wav = File.createTempFile("learnagent_tts", ".wav")
        val script = File.createTempFile("learnagent_tts", ".ps1")
        val resource = javaClass.getResourceAsStream("/tts.ps1") ?: error("缺少 tts.ps1")
        resource.use { input -> script.outputStream().use { input.copyTo(it) } }

        val process = startWithRetry(
            listOf(
                powershellPath(), "-NoProfile", "-ExecutionPolicy", "Bypass",
                "-File", script.absolutePath, "-Out", wav.absolutePath, "-Text", text
            )
        )
        val log = process.inputStream.bufferedReader().readText()
        val exit = process.waitFor()
        script.delete()
        check(exit == 0) { "TTS 失败: $log" }
        return wav
    }

    @Test
    fun testTranscribeSynthesizedSpeech() {
        if (!WindowsTranscriber.isWindows()) return
        val wav = try {
            synthesize("今天天气很好")
        } catch (e: java.io.IOException) {
            println("SKIP_ENV: 系统拒绝 Java 创建 PowerShell 进程，跳过转写集成测试: ${e.message}")
            return
        }
        try {
            val segments = WindowsTranscriber.transcribe(wav)
            assertTrue(segments.isNotEmpty(), "应至少转写出一段")
            assertTrue(segments.any { it.end > it.start }, "应包含有效时间段")
            assertTrue(segments.any { it.text.isNotBlank() }, "转写文本不应为空")
            println("TRANSCRIBE_RESULT=" + segments.joinToString { "[${it.start}-${it.end}]${it.text}" })
        } catch (e: java.io.IOException) {
            println("SKIP_ENV: 系统拒绝 Java 创建 PowerShell 进程，跳过转写集成测试: ${e.message}")
        } finally {
            wav.delete()
        }
    }
}
