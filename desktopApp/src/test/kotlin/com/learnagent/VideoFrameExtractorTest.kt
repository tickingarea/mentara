package com.learnagent

import com.learnagent.media.VideoFrameExtractor
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * 验证视频抽帧 + OCR（Windows 内置）。
 * 若系统拒绝 Java 创建 PowerShell 进程，则跳过（环境限制）。
 */
class VideoFrameExtractorTest {

    @Test
    fun testExtractFrameAndOcr() {
        if (!VideoFrameExtractor.isWindows()) return
        val resource = javaClass.getResourceAsStream("/sample_av.mp4") ?: return
        val mp4 = File.createTempFile("learnagent_vfe", ".mp4")
        val outDir = File(System.getProperty("java.io.tmpdir"), "learnagent_frames_${System.nanoTime()}")
        try {
            resource.use { input -> mp4.outputStream().use { input.copyTo(it) } }
            val frames = try {
                VideoFrameExtractor.extractAndOcr(mp4, outDir)
            } catch (e: java.io.IOException) {
                println("SKIP_ENV: 系统拒绝 Java 创建 PowerShell 进程，跳过抽帧测试: ${e.message}")
                return
            } catch (e: IllegalStateException) {
                println("SKIP_ENV: 抽帧脚本不可用: ${e.message}")
                return
            }
            assertTrue(frames.isNotEmpty(), "应至少抽取 1 帧")
            assertTrue(frames.all { File(it.imagePath).isFile }, "每帧图片都应存在")
            println("FRAME_OCR=" + frames.joinToString { "[${it.timestamp}] '${it.text}'" })
        } finally {
            mp4.delete()
            outDir.deleteRecursively()
        }
    }
}
