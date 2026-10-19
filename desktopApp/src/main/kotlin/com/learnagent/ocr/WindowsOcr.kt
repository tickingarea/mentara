package com.learnagent.ocr

import java.io.File

/**
 * 基于 Windows 内置 OCR 引擎（Windows.Media.Ocr）的本地识别实现。
 *
 * 通过调用系统 PowerShell 运行随包附带 `ocr.ps1`，把图片识别为文本。
 * 全程离线，无需 tesseract / MLKit。
 */
object WindowsOcr {

    fun isWindows(): Boolean =
        System.getProperty("os.name").orEmpty().lowercase().contains("windows")

    private fun powershellPath(): String {
        val systemRoot = System.getenv("SystemRoot") ?: "C:\\Windows"
        val full = File(systemRoot, "System32\\WindowsPowerShell\\v1.0\\powershell.exe")
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
     * 识别图片中的文字。
     *
     * @param image 待识别的图片文件（PNG/JPG）
     * @return 识别出的文本（可能为空字符串）
     */
    fun ocr(image: File): String {
        require(image.isFile) { "图片不存在: ${image.absolutePath}" }
        check(isWindows()) { "WindowsOcr 仅支持 Windows 平台" }

        val scriptFile = File.createTempFile("learnagent_ocr", ".ps1")
        val outFile = File.createTempFile("learnagent_ocr_out", ".txt")
        try {
            val script = WindowsOcr::class.java.getResourceAsStream("/ocr.ps1")
                ?: error("classpath 缺少 ocr.ps1")
            script.use { input -> scriptFile.outputStream().use { input.copyTo(it) } }

            val process = startProcess(
                listOf(
                    powershellPath(), "-NoProfile", "-ExecutionPolicy", "Bypass",
                    "-File", scriptFile.absolutePath,
                    "-Path", image.absolutePath,
                    "-Out", outFile.absolutePath
                )
            )

            val log = process.inputStream.bufferedReader().readText()
            val exit = process.waitFor()
            if (exit != 0) error("OCR 进程失败 exit=$exit\n$log")

            return outFile.readText(Charsets.UTF_8).trim()
        } finally {
            scriptFile.delete()
            outFile.delete()
        }
    }
}
