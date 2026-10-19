package com.learnagent

import com.learnagent.media.VideoToWav
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 验证离线视频→WAV 提取（Windows MediaTranscoder）。
 * 测试用视频来自 src/test/resources/sample_av.mp4（含音轨）。
 */
class VideoToWavTest {

    @Test
    fun testExtractAudioFromMp4() {
        if (!VideoToWav.isWindows()) return
        val resource = javaClass.getResourceAsStream("/sample_av.mp4") ?: return
        val mp4 = File.createTempFile("learnagent_sample", ".mp4")
        val wav = File.createTempFile("learnagent_sample", ".wav")
        try {
            resource.use { input -> mp4.outputStream().use { input.copyTo(it) } }
            assertTrue(VideoToWav.isVideo(mp4), "应识别为视频")
            val ok = try {
                VideoToWav.extractAudio(mp4, wav)
            } catch (e: java.io.IOException) {
                println("SKIP_ENV: 系统拒绝 Java 创建 PowerShell 进程，跳过视频抽音频测试: ${e.message}")
                return
            }
            assertTrue(ok, "应成功提取音频")

            val bytes = wav.readBytes()
            assertEquals("RIFF", String(bytes.copyOfRange(0, 4)), "应为 RIFF WAV")
            assertEquals("WAVE", String(bytes.copyOfRange(8, 12)), "应为 WAVE")

            // 解析 fmt 块，断言 1ch / 16000 / 16bit
            var pos = 12
            var channels = -1
            var sampleRate = -1
            var bits = -1
            while (pos <= bytes.size - 8) {
                val id = String(bytes.copyOfRange(pos, pos + 4))
                val size = leInt(bytes, pos + 4)
                if (id == "fmt ") {
                    channels = leShort(bytes, pos + 10)
                    sampleRate = leInt(bytes, pos + 12)
                    bits = leShort(bytes, pos + 22)
                }
                if (id == "data") break
                pos += 8 + size + (size % 2)
            }
            assertEquals(1, channels, "应为单声道")
            assertEquals(16000, sampleRate, "应为 16kHz")
            assertEquals(16, bits, "应为 16-bit")
            println("VIDEO_TO_WAV bytes=${wav.length()} ch=$channels sr=$sampleRate bits=$bits")
        } finally {
            mp4.delete()
            wav.delete()
        }
    }

    private fun leShort(b: ByteArray, o: Int): Int =
        (b[o].toInt() and 0xFF) or ((b[o + 1].toInt() and 0xFF) shl 8)

    private fun leInt(b: ByteArray, o: Int): Int =
        (b[o].toInt() and 0xFF) or ((b[o + 1].toInt() and 0xFF) shl 8) or
            ((b[o + 2].toInt() and 0xFF) shl 16) or ((b[o + 3].toInt() and 0xFF) shl 24)
}
