package com.learnagent.media

import com.learnagent.core.Atmosphere
import com.learnagent.data.FrameOcr
import com.learnagent.ocr.WindowsOcr
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 桌面端视频抽帧 + OCR。
 *
 * 在无 ffmpeg 的环境下，用 Windows 内置 `StorageFile.GetThumbnailAsync` 抽取代表帧，
 * 再交给 [WindowsOcr]（Windows.Media.Ocr）做文字识别，产出 [FrameOcr] 列表。
 *
 * 说明：受平台互操作限制，目前每个视频抽取 1 张代表帧。
 */
object VideoFrameExtractor {

    @Serializable
    private data class FrameRef(val timestamp: Double, val imagePath: String)

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun isWindows(): Boolean =
        System.getProperty("os.name").orEmpty().lowercase().contains("windows")

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

    private fun extractResource(resource: String, dest: File) {
        val stream = VideoFrameExtractor::class.java.getResourceAsStream(resource)
            ?: error("classpath 缺少 $resource")
        stream.use { input -> dest.outputStream().use { input.copyTo(it) } }
    }

    private fun runScript(scriptFile: File, args: List<String>) {
        val process = startProcess(
            listOf(
                powershellPath(), "-NoProfile", "-ExecutionPolicy", "Bypass",
                "-File", scriptFile.absolutePath
            ) + args
        )
        process.inputStream.bufferedReader().readText()
        val exit = process.waitFor()
        if (exit != 0) error("脚本执行失败 exit=$exit")
    }

    /**
     * 抽取视频代表帧并逐帧 OCR。
     *
     * @return [FrameOcr] 列表（timestamp/text/imagePath）
     */
    fun extractAndOcr(video: File, outDir: File): List<FrameOcr> {
        require(video.isFile) { "视频不存在: ${video.absolutePath}" }
        check(isWindows()) { "VideoFrameExtractor 仅支持 Windows 平台" }
        outDir.mkdirs()

        // 释放脚本 + 自研 Media Foundation 抽帧器（多帧优先），执行后清理
        val tmpDir = File.createTempFile("learnagent_", "").apply { delete(); mkdirs() }
        try {
            val ps1 = File(tmpDir, "extract_frames.ps1")
            val cs = File(tmpDir, "mfframegrab.cs")
            extractResource("/extract_frames.ps1", ps1)
            extractResource("/mfframegrab.cs", cs)
            runScript(
                ps1,
                listOf(
                    "-Video", video.absolutePath, "-OutDir", outDir.absolutePath,
                    "-Width", "1280", "-Count", "12", "-Fps", "20", "-CsPath", cs.absolutePath
                )
            )
        } finally {
            tmpDir.deleteRecursively()
        }

        val framesJson = File(outDir, "frames.json")
        if (!framesJson.isFile) return emptyList()
        val refs = json.decodeFromString(ListSerializer(FrameRef.serializer()), framesJson.readText())

        return refs.map { ref ->
            val img = File(ref.imagePath)
            var text = try {
                WindowsOcr.ocr(img)
            } catch (e: Exception) {
                ""
            }
            // 镜像源容错：OCR 结果过少时尝试水平镜像，取识别字符更多的一侧
            if (text.count { it.isLetterOrDigit() } < 3) {
                val flipped = File(img.parentFile, img.nameWithoutExtension + "_flip.png")
                try {
                    flipHorizontal(img, flipped)
                    val t2 = WindowsOcr.ocr(flipped)
                    if (t2.count { it.isLetterOrDigit() } > text.count { it.isLetterOrDigit() }) text = t2
                } catch (e: Exception) {
                    // 忽略
                }
            }
            val mood = runCatching { moodOf(img) }.getOrDefault("")
            FrameOcr(timestamp = ref.timestamp, text = text, imagePath = img.absolutePath, mood = mood)
        }
    }

    /** 生成连续帧（密集 20fps、宽 640 JPEG）到 outDir/dense；返回帧数。 */
    fun extractDense(video: File, outDir: File, fps: Int = 20, width: Int = 640): Int {
        require(video.isFile) { "视频不存在：${video.absolutePath}" }
        check(isWindows()) { "VideoFrameExtractor 仅支持 Windows 平台" }
        val denseDir = File(outDir, "dense").apply { mkdirs() }
        val tmpDir = File.createTempFile("learnagent_", "").apply { delete(); mkdirs() }
        return try {
            val ps1 = File(tmpDir, "extract_frames.ps1")
            val cs = File(tmpDir, "mfframegrab.cs")
            extractResource("/extract_frames.ps1", ps1)
            extractResource("/mfframegrab.cs", cs)
            runScript(
                ps1,
                listOf(
                    "-Video", video.absolutePath, "-OutDir", denseDir.absolutePath,
                    "-Mode", "dense", "-Fps", fps.toString(), "-DenseWidth", width.toString(),
                    "-CsPath", cs.absolutePath
                )
            )
            val fj = File(denseDir, "frames.json")
            if (!fj.isFile) 0 else json.decodeFromString(ListSerializer(FrameRef.serializer()), fj.readText()).size
        } finally {
            tmpDir.deleteRecursively()
        }
    }

    /** 画面氛围（量化）：采样像素算 亮度/饱和/色温/对比。 */
    private fun moodOf(file: File): String {
        val img = javax.imageio.ImageIO.read(file) ?: return ""
        var r = 0L; var g = 0L; var b = 0L; var n = 0L; var minL = 255f; var maxL = 0f
        val sx = (img.width / 24).coerceAtLeast(1); val sy = (img.height / 24).coerceAtLeast(1)
        var y = 0
        while (y < img.height) {
            var x = 0
            while (x < img.width) {
                val c = img.getRGB(x, y)
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

    /** 水平镜像（用于纠正镜像源视频的 OCR）。 */
    private fun flipHorizontal(src: File, dst: File) {
        val img = javax.imageio.ImageIO.read(src) ?: return
        val out = java.awt.image.BufferedImage(img.width, img.height, java.awt.image.BufferedImage.TYPE_INT_RGB)
        val g = out.createGraphics()
        g.drawImage(img, img.width, 0, -img.width, img.height, null)
        g.dispose()
        javax.imageio.ImageIO.write(out, "png", dst)
    }
}
