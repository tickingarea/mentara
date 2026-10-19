package com.learnagent.core

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Android 音频提取器（V4 修复版）
 *
 * 旧版三个致命 bug（bad_wav / 0 段 / 变速变调）：
 *  1) WAV 头用 DataOutputStream（大端）写，而 WAV 必须小端 → Whisper/系统识别器都读不了；
 *  2) 解码器输入缓冲区填错（填到了自己的临时 buffer，queueInputBuffer 的却是解码器下标）；
 *  3) 声称重采样实际只是原样拷贝（44.1kHz 立体声被当 16k 单声道用）。
 * 本版：小端 WAV 头 + 真·线性插值重采样到 16k 单声道 + 正确的 EOS 排空 + finally 释放。
 */
class AndroidAudioExtractor(private val context: Context) {

    companion object {
        private const val SAMPLE_RATE = 16000
        private const val CHANNELS = 1
        private const val BIT_DEPTH = 16
    }

    class AudioFormatException(message: String) : Exception(message)

    private fun le16(v: Int): ByteArray =
        byteArrayOf((v and 0xFF).toByte(), ((v shr 8) and 0xFF).toByte())

    private fun le32(v: Long): ByteArray = byteArrayOf(
        (v and 0xFF).toByte(), ((v shr 8) and 0xFF).toByte(),
        ((v shr 16) and 0xFF).toByte(), ((v shr 24) and 0xFF).toByte()
    )

    private fun MediaFormat.intOr(key: String, def: Int): Int =
        runCatching { getInteger(key) }.getOrDefault(def)

    suspend fun extractToWav(
        videoPath: String,
        outputPath: String,
        onProgress: (Float) -> Unit
    ) = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        var dos: DataOutputStream? = null
        try {
            extractor.setDataSource(videoPath)

            var audioTrackIndex = -1
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                if (f.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                    audioTrackIndex = i
                    break
                }
            }
            if (audioTrackIndex == -1) {
                throw AudioFormatException("视频里没有音轨（No audio track）")
            }
            extractor.selectTrack(audioTrackIndex)

            val fmt = extractor.getTrackFormat(audioTrackIndex)
            val mime = fmt.getString(MediaFormat.KEY_MIME)
                ?: throw AudioFormatException("音频格式不支持")
            var srcRate = fmt.intOr(MediaFormat.KEY_SAMPLE_RATE, SAMPLE_RATE)
            var srcChannels = fmt.intOr(MediaFormat.KEY_CHANNEL_COUNT, 1)
            val durationUs = runCatching { fmt.getLong(MediaFormat.KEY_DURATION) }.getOrDefault(0L)
            val expectedBytes = if (durationUs > 0) (durationUs / 1_000_000.0 * SAMPLE_RATE * 2).toLong() else 0L

            decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(fmt, null, null, 0)
            decoder.start()

            val wavFile = File(outputPath)
            wavFile.parentFile?.mkdirs()
            dos = DataOutputStream(BufferedOutputStream(FileOutputStream(wavFile), 1 shl 16))

            // 小端 WAV 头（先写 0，收尾再回填大小）
            fun writeHeader(dataSize: Long) {
                dos!!.let {
                    it.write("RIFF".toByteArray()); it.write(le32(36 + dataSize)); it.write("WAVE".toByteArray())
                    it.write("fmt ".toByteArray()); it.write(le32(16))
                    it.write(le16(1))                      // PCM
                    it.write(le16(CHANNELS))               // 单声道
                    it.write(le32(SAMPLE_RATE.toLong()))
                    it.write(le32((SAMPLE_RATE * CHANNELS * BIT_DEPTH / 8).toLong()))
                    it.write(le16(CHANNELS * BIT_DEPTH / 8))
                    it.write(le16(BIT_DEPTH))
                    it.write("data".toByteArray()); it.write(le32(dataSize))
                }
            }
            writeHeader(0)

            var totalBytes = 0L
            var sawInputEOS = false
            var sawOutputEOS = false
            val info = MediaCodec.BufferInfo()

            while (!sawOutputEOS) {
                if (!sawInputEOS) {
                    val inIdx = decoder.dequeueInputBuffer(10_000)
                    if (inIdx >= 0) {
                        val inBuf = decoder.getInputBuffer(inIdx)
                        if (inBuf != null) {
                            inBuf.clear()
                            val sampleSize = extractor.readSampleData(inBuf, 0)
                            if (sampleSize < 0) {
                                decoder.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                sawInputEOS = true
                            } else {
                                decoder.queueInputBuffer(inIdx, 0, sampleSize, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        } else {
                            sawInputEOS = true
                        }
                    }
                }
                val outIdx = decoder.dequeueOutputBuffer(info, 10_000)
                if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val of = decoder.outputFormat
                    srcRate = of.intOr(MediaFormat.KEY_SAMPLE_RATE, srcRate).coerceAtLeast(8000)
                    srcChannels = of.intOr(MediaFormat.KEY_CHANNEL_COUNT, srcChannels).coerceIn(1, 8)
                } else if (outIdx >= 0) {
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) sawOutputEOS = true
                    decoder.getOutputBuffer(outIdx)?.let { buf ->
                        buf.position(info.offset)
                        buf.limit(info.offset + info.size)
                        val pcm = resampleTo16kMono(buf, info.size, srcRate, srcChannels)
                        if (pcm.isNotEmpty()) {
                            dos!!.write(pcm)
                            totalBytes += pcm.size
                            if (expectedBytes > 0) onProgress((totalBytes.toFloat() / expectedBytes).coerceIn(0f, 1f))
                        }
                    }
                    decoder.releaseOutputBuffer(outIdx, false)
                }
            }

            if (totalBytes == 0L) throw AudioFormatException("音轨解码为空（可能是无声视频或解码器不支持该音轨）")

            // 回填真实大小（小端）
            dos.flush()
            dos.close()
            RandomAccessFile(wavFile, "rw").use { raf ->
                raf.seek(4); raf.write(le32(36 + totalBytes))
                raf.seek(40); raf.write(le32(totalBytes))
            }
        } catch (e: Exception) {
            runCatching { dos?.close() }
            throw e
        } finally {
            runCatching { decoder?.stop() }
            runCatching { decoder?.release() }
            runCatching { extractor.release() }
        }
    }

    /**
     * 真·重采样：任意 采样率/声道 的 16-bit PCM → 16kHz 单声道（小端）。
     * 多声道先混单声道，再线性插值到 16k。
     */
    private fun resampleTo16kMono(buf: ByteBuffer, size: Int, srcRate: Int, srcChannels: Int): ByteArray {
        val ch = srcChannels.coerceIn(1, 8)
        val frames = size / (2 * ch)
        if (frames <= 0) return ByteArray(0)
        buf.order(ByteOrder.LITTLE_ENDIAN)

        val mono = FloatArray(frames)
        for (f in 0 until frames) {
            var acc = 0f
            for (c in 0 until ch) acc += buf.short.toFloat()
            mono[f] = acc / ch / 32768f
        }

        val outCount = if (srcRate == SAMPLE_RATE) frames
        else ((frames.toLong() * SAMPLE_RATE) / srcRate.toLong()).toInt().coerceAtLeast(1)
        val out = FloatArray(outCount)
        if (srcRate == SAMPLE_RATE) {
            System.arraycopy(mono, 0, out, 0, frames)
        } else {
            val ratio = frames.toDouble() / outCount
            for (i in 0 until outCount) {
                val pos = i * ratio
                val i0 = pos.toInt().coerceAtMost(frames - 1)
                val i1 = (i0 + 1).coerceAtMost(frames - 1)
                val frac = (pos - i0).toFloat()
                out[i] = mono[i0] * (1f - frac) + mono[i1] * frac
            }
        }

        val bytes = ByteArray(outCount * 2)
        for (i in out.indices) {
            var s = (out[i] * 32767f).toInt()
            if (s > 32767) s = 32767
            if (s < -32768) s = -32768
            bytes[i * 2] = (s and 0xFF).toByte()
            bytes[i * 2 + 1] = ((s shr 8) and 0xFF).toByte()
        }
        return bytes
    }
}
