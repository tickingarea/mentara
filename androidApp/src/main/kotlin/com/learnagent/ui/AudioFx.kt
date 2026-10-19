package com.learnagent.ui

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.learnagent.audio.PcmSynth
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Android 音效播放层：合成在共享 PcmSynth（与桌面同音色），
 * 这里用 AudioTrack 声线池（3 条常驻、异步、可叠加、空闲 2.4s 自动释放）。
 * 触屏无 hover → HOVER 音效在 Android 上不触发。
 */
@Suppress("DEPRECATION")
object AudioFx {

    @Volatile
    var enabled: Boolean = true

    private val cache = HashMap<PcmSynth.Sound, ByteArray>()

    private class Voice {
        val queue = LinkedBlockingQueue<ByteArray>(8)
    }

    private val voices = List(3) { Voice() }
    private val started = AtomicBoolean(false)

    /** 预热声线池（后台线程，不阻塞 UI）。 */
    fun warmUp(context: android.content.Context? = null) {
        if (!enabled) return
        if (!started.compareAndSet(false, true)) return
        voices.forEach { voice ->
            Thread {
                while (true) {
                    var chunk = runCatching { voice.queue.take() }.getOrNull() ?: return@Thread
                    runCatching {
                        var track: AudioTrack? = null
                        try {
                            val minBuf = AudioTrack.getMinBufferSize(
                                PcmSynth.SAMPLE_RATE,
                                AudioFormat.CHANNEL_OUT_MONO,
                                AudioFormat.ENCODING_PCM_16BIT
                            )
                            track = AudioTrack(
                                AudioManager.STREAM_MUSIC,
                                PcmSynth.SAMPLE_RATE,
                                AudioFormat.CHANNEL_OUT_MONO,
                                AudioFormat.ENCODING_PCM_16BIT,
                                maxOf(minBuf, 16384),
                                AudioTrack.MODE_STREAM
                            )
                            track.play()
                            while (chunk != null) {
                                track.write(chunk, 0, chunk.size)
                                chunk = voice.queue.poll(2400, TimeUnit.MILLISECONDS)
                            }
                        } finally {
                            track?.runCatching {
                                stop()
                                release()
                            }
                        }
                    }
                }
            }.apply {
                isDaemon = true
                name = "mentara-voice"
                start()
            }
        }
    }

    fun play(sound: PcmSynth.Sound, volumeScale: Float = 1f) {
        if (!enabled) return
        warmUp()
        val pcm = synchronized(cache) {
            cache.getOrPut(sound) { PcmSynth.render(sound) }
        }
        val data = if (volumeScale == 1f) pcm else PcmSynth.scale(pcm, volumeScale)
        val voice = voices.minByOrNull { it.queue.size } ?: return
        voice.queue.offer(data)
    }

    // 桌面 Feedback 语义对齐的快捷方法
    fun tap() = play(PcmSynth.Sound.TAP)
    fun success() = play(PcmSynth.Sound.SUCCESS)
    fun error() = play(PcmSynth.Sound.ERROR)
    fun open() = play(PcmSynth.Sound.OPEN)
    fun close() = play(PcmSynth.Sound.CLOSE)
    fun splashTone(v: Float = 1f) = play(PcmSynth.Sound.SPLASH_TONE, v)
    fun chime(v: Float = 1f) = play(PcmSynth.Sound.CHIME, v)
    fun drone(v: Float = 1f) = play(PcmSynth.Sound.DRONE, v)
    fun whoosh(v: Float = 1f) = play(PcmSynth.Sound.WHOOSH, v)
    fun rest() = play(PcmSynth.Sound.REST)
}
