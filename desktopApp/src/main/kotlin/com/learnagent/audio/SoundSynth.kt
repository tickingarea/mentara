package com.learnagent.audio

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine

/**
 * 桌面播放层：合成已下沉到共享 PcmSynth（三端同音色），
 * 本对象只保留 SourceDataLine 声线池（预热 / 异步 / 可叠加 / 空闲自动关闭）。
 */
object SoundSynth {

    @Volatile
    var enabled: Boolean = true

    private val format = AudioFormat(PcmSynth.SAMPLE_RATE.toFloat(), 16, 1, true, false)
    private val cache = HashMap<PcmSynth.Sound, ByteArray>()

    private class Voice {
        val queue = java.util.concurrent.LinkedBlockingQueue<ByteArray>(8)
    }

    private val voices: List<Voice> = List(3) { Voice() }
    private val voicesStarted = java.util.concurrent.atomic.AtomicBoolean(false)

    /** 预热播放线程（在后台线程调用，不阻塞 UI）。 */
    fun warmUp() {
        if (!enabled) return
        if (!voicesStarted.compareAndSet(false, true)) return
        voices.forEach { voice ->
            val thread = Thread {
                while (true) {
                    var chunk = runCatching { voice.queue.take() }.getOrNull() ?: return@Thread
                    runCatching {
                        var line: SourceDataLine? = null
                        try {
                            line = AudioSystem.getSourceDataLine(format)
                            line.open(format)
                            line.start()
                            while (chunk != null) {
                                line.write(chunk, 0, chunk.size)
                                chunk = voice.queue.poll(2400, java.util.concurrent.TimeUnit.MILLISECONDS)
                            }
                            line.drain()
                        } finally {
                            line?.runCatching {
                                stop()
                                close()
                            }
                        }
                    }
                }
            }
            thread.isDaemon = true
            thread.name = "mentara-voice"
            thread.start()
        }
    }

    /** 播放（异步，可叠加）。[volumeScale] 用于 Splash 中的渐变音量。 */
    fun play(sound: PcmSynth.Sound, volumeScale: Float = 1f) {
        if (!enabled) return
        val pcm = synchronized(cache) {
            cache.getOrPut(sound) { PcmSynth.render(sound) }
        }
        start(if (volumeScale == 1f) pcm else PcmSynth.scale(pcm, volumeScale))
    }

    /** 预生成 PCM 到内存（SoundPool 语义）+ 预热声线。 */
    fun preload(vararg sounds: PcmSynth.Sound) {
        synchronized(cache) {
            sounds.forEach { cache.getOrPut(it) { PcmSynth.render(it) } }
        }
        warmUp()
    }

    private fun start(pcm: ByteArray) {
        if (!voicesStarted.get()) warmUp()
        val voice = voices.minByOrNull { it.queue.size } ?: return
        voice.queue.offer(pcm)
    }
}
