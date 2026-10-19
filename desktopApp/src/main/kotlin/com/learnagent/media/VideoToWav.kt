package com.learnagent.media

import java.io.File

/**
 * 视频 → WAV（音频提取）。
 *
 * 离线环境下没有 ffmpeg，改用 Windows 内置 MediaTranscoder（WinRT）把视频音轨
 * 转成 16kHz / 单声道 / 16-bit PCM WAV，供 [com.learnagent.asr.WindowsTranscriber] 转写。
 */
object VideoToWav {

    private val VIDEO_EXTENSIONS =
        setOf("mp4", "mkv", "mov", "avi", "wmv", "m4v", "webm", "flv", "ts", "3gp")

    fun isWindows(): Boolean =
        System.getProperty("os.name").orEmpty().lowercase().contains("windows")

    fun isVideo(file: File): Boolean = file.extension.lowercase() in VIDEO_EXTENSIONS

    fun isWav(file: File): Boolean = file.extension.equals("wav", ignoreCase = true)

    private fun powershellPath(): String {
        val root = System.getenv("SystemRoot") ?: "C:\\Windows"
        val full = File(root, "System32\\WindowsPowerShell\\v1.0\\powershell.exe")
        return if (full.isFile) full.absolutePath else "powershell.exe"
    }

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
     * 从 [video] 提取音频到 [wav]。
     * @return 是否成功（成功标准：进程退出码 0 且输出 WAV 已生成）
     */
    fun extractAudio(video: File, wav: File): Boolean {
        require(video.isFile) { "视频不存在: ${video.absolutePath}" }
        check(isWindows()) { "VideoToWav 仅支持 Windows 平台" }

        val scriptFile = File.createTempFile("learnagent_v2w", ".ps1")
        try {
            val resource = VideoToWav::class.java.getResourceAsStream("/video_to_wav.ps1")
                ?: error("classpath 缺少 video_to_wav.ps1")
            resource.use { input -> scriptFile.outputStream().use { input.copyTo(it) } }

            val process = startProcess(
                listOf(
                    powershellPath(), "-NoProfile", "-ExecutionPolicy", "Bypass",
                    "-File", scriptFile.absolutePath,
                    "-Video", video.absolutePath,
                    "-Wav", wav.absolutePath
                )
            )
            val log = process.inputStream.bufferedReader().readText()
            val exit = process.waitFor()
            return exit == 0 && wav.isFile && wav.length() > 44L
        } finally {
            scriptFile.delete()
        }
    }
}
