package com.learnagent.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * 程序化 PCM 音效合成（三端共享，44100Hz / 16-bit signed / 单声道 / 小端）。
 *
 * 由桌面 SoundSynth 下沉而来，纯函数、无平台依赖：
 * 技术：Karplus-Strong 拨弦、FM 钟声、ADSR / 指数包络、20–50ms 延迟混响模拟。
 * 播放层由各端实现（桌面 SourceDataLine / Android AudioTrack）。
 */
object PcmSynth {

    const val SAMPLE_RATE = 44100

    enum class Sound {
        HOVER, TAP, SUCCESS, ERROR, OPEN, CLOSE, SPLASH_TONE,
        /** 环境音：默认关闭，永不自动播放，仅保留供用户手动开启的路径。 */
        AMBIENT,
        CHIME, DRONE, WHOOSH, REST
    }

    private val cache = HashMap<Sound, ByteArray>()

    /** 渲染（带内存缓存）。 */
    fun render(sound: Sound): ByteArray = synchronized(cache) {
        cache.getOrPut(sound) { encode(raw(sound)) }
    }

    /** 音量缩放（Splash 渐变音量用）。 */
    fun scale(pcm: ByteArray, factor: Float): ByteArray {
        val out = ByteArray(pcm.size)
        for (i in pcm.indices step 2) {
            val v = ((pcm[i + 1].toInt() shl 8) or (pcm[i].toInt() and 0xFF)).toShort().toInt()
            val s = (v * factor).toInt().coerceIn(-32768, 32767)
            out[i] = (s and 0xFF).toByte()
            out[i + 1] = ((s shr 8) and 0xFF).toByte()
        }
        return out
    }

    // ============================================================
    //  合成核心（与桌面音色完全一致）
    // ============================================================

    private fun raw(sound: Sound): FloatArray = when (sound) {
        Sound.HOVER -> withReverb(
            sine(880.0, 0.025, 0.15, attack = 0.002, decayTau = 0.010), 0.10f
        )
        Sound.TAP -> withReverb(
            mix(
                pluck(523.25, 0.08, 0.30),
                sine(659.25, 0.08, 0.26, attack = 0.003, decayTau = 0.030)
            ), 0.25f
        )
        Sound.SUCCESS -> withReverb(
            arpeggio(doubleArrayOf(523.25, 659.25, 783.99), step = 0.08, note = 0.20, volume = 0.25), 0.28f
        )
        Sound.ERROR -> withReverb(
            mix(
                square(220.0, 0.20, 0.20, attack = 0.004, decayTau = 0.085),
                sine(110.0, 0.20, 0.20, attack = 0.004, decayTau = 0.100)
            ), 0.15f
        )
        Sound.OPEN -> withReverb(sweep(300.0, 800.0, 0.15, 0.20), 0.18f)
        Sound.CLOSE -> withReverb(sweep(800.0, 300.0, 0.12, 0.15), 0.15f)
        Sound.SPLASH_TONE -> splashTone()
        Sound.AMBIENT -> withReverb(
            mix(
                sine(60.0, 2.0, 0.03, attack = 0.20, decayTau = 4.0),
                sine(90.0, 2.0, 0.03, attack = 0.20, decayTau = 4.0)
            ), 0.08f
        )
        Sound.CHIME -> withReverb(fmBell(880.0, 0.9, 0.18), 0.35f)
        Sound.DRONE -> withReverb(
            mix(
                sine(110.0, 1.6, 0.08, attack = 0.50, decayTau = 2.5),
                sine(165.0, 1.6, 0.05, attack = 0.50, decayTau = 2.2)
            ), 0.25f
        )
        Sound.WHOOSH -> withReverb(noiseSweep(0.6, 0.10), 0.20f)
        Sound.REST -> withReverb(
            arpeggio(doubleArrayOf(392.0, 523.25), step = 0.14, note = 0.42, volume = 0.12), 0.30f
        )
    }

    /** 指数衰减 + 线性起音包络。 */
    private fun env(t: Double, volume: Double, attack: Double, decayTau: Double): Double {
        val a = if (attack <= 0.0) 1.0 else (t / attack).coerceAtMost(1.0)
        return volume * a * exp(-t / decayTau)
    }

    private fun sine(
        freq: Double,
        seconds: Double,
        volume: Double,
        attack: Double = 0.004,
        decayTau: Double = seconds * 0.40
    ): FloatArray {
        val n = (SAMPLE_RATE * seconds).toInt()
        return FloatArray(n) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            (sin(2.0 * PI * freq * t) * env(t, volume, attack, decayTau)).toFloat()
        }
    }

    private fun square(
        freq: Double,
        seconds: Double,
        volume: Double,
        attack: Double = 0.005,
        decayTau: Double = seconds * 0.40
    ): FloatArray {
        val n = (SAMPLE_RATE * seconds).toInt()
        return FloatArray(n) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            val s = if (sin(2.0 * PI * freq * t) >= 0.0) 1.0 else -1.0
            (s * env(t, volume, attack, decayTau)).toFloat()
        }
    }

    /** 频率扫掠（相位积分保证连续）。 */
    private fun sweep(f0: Double, f1: Double, seconds: Double, volume: Double): FloatArray {
        val n = (SAMPLE_RATE * seconds).toInt()
        return FloatArray(n) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            val phase = 2.0 * PI * (f0 * t + (f1 - f0) * t * t / (2.0 * seconds))
            (sin(phase) * env(t, volume, 0.010, seconds * 0.60)).toFloat()
        }
    }

    /** Karplus-Strong 拨弦：噪声激励 + 延迟线平均滤波。 */
    private fun pluck(freq: Double, seconds: Double, volume: Double, decay: Double = 0.996): FloatArray {
        val n = (SAMPLE_RATE * seconds).toInt()
        val period = (SAMPLE_RATE / freq).toInt().coerceAtLeast(2)
        val rnd = Random(freq.toInt())
        val buf = FloatArray(period) { rnd.nextFloat() * 2f - 1f }
        val out = FloatArray(n)
        for (i in 0 until n) {
            val idx = i % period
            val cur = buf[idx]
            val next = buf[(idx + 1) % period]
            out[i] = cur
            buf[idx] = ((cur + next) * 0.5f * decay).toFloat()
        }
        for (i in out.indices) {
            val t = i.toDouble() / SAMPLE_RATE
            out[i] = (out[i] * env(t, volume, 0.002, seconds * 0.35)).toFloat()
        }
        return out
    }

    /** FM 钟声：载波 + 指数衰减调制指数。 */
    private fun fmBell(freq: Double, seconds: Double, volume: Double): FloatArray {
        val n = (SAMPLE_RATE * seconds).toInt()
        val modRatio = 1.414
        return FloatArray(n) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            val index = 5.0 * exp(-t / 0.18)
            val s = sin(2.0 * PI * freq * t + index * sin(2.0 * PI * freq * modRatio * t))
            (s * env(t, volume, 0.002, seconds * 0.42)).toFloat()
        }
    }

    /** 带通噪声音扫：模拟气息（"呼"）。 */
    private fun noiseSweep(seconds: Double, volume: Double): FloatArray {
        val n = (SAMPLE_RATE * seconds).toInt()
        val out = FloatArray(n)
        val rnd = Random(7)
        var lp = 0f
        var bp = 0f
        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE
            val k = t / seconds
            val cutoff = (0.02 + 0.18 * sin(PI * k)).toFloat()
            val white = rnd.nextFloat() * 2f - 1f
            lp += cutoff * (white - lp)
            bp += cutoff * (lp - bp)
            out[i] = (bp * env(t, volume, 0.08, seconds * 0.45)).toFloat()
        }
        return out
    }

    /** C5→E5→G5 琶音（Karplus-Strong 音符叠加）。 */
    private fun arpeggio(freqs: DoubleArray, step: Double, note: Double, volume: Double): FloatArray {
        val total = (freqs.size - 1) * step + note
        val n = (SAMPLE_RATE * total).toInt()
        val out = FloatArray(n)
        freqs.forEachIndexed { idx, f ->
            val start = (idx * step * SAMPLE_RATE).toInt()
            val noteArr = pluck(f, note, volume)
            for (i in noteArr.indices) {
                val j = start + i
                if (j < n) out[j] += noteArr[i]
            }
        }
        for (i in out.indices) out[i] = out[i].coerceIn(-1f, 1f)
        return out
    }

    /** Splash 主题音：220Hz 基频 + 440/660/880 泛音，2 秒缓慢衰减。 */
    private fun splashTone(): FloatArray {
        val seconds = 2.0
        val n = (SAMPLE_RATE * seconds).toInt()
        val partials = doubleArrayOf(220.0, 440.0, 660.0, 880.0)
        val gains = doubleArrayOf(1.0, 0.5, 0.28, 0.16)
        val out = FloatArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE
            var s = 0.0
            for (p in partials.indices) {
                s += gains[p] * sin(2.0 * PI * partials[p] * t + 0.4 * sin(2.0 * PI * 0.6 * t))
            }
            val attack = (t / 0.18).coerceAtMost(1.0)
            out[i] = (s / 1.94 * 0.25 * attack * exp(-t / 0.95)).toFloat()
        }
        return withReverb(out, 0.30f)
    }

    /** 混响模拟：23ms / 41ms 两路延迟叠加。 */
    private fun withReverb(src: FloatArray, mix: Float): FloatArray {
        val out = src.copyOf()
        val d1 = (SAMPLE_RATE * 0.023).toInt()
        val d2 = (SAMPLE_RATE * 0.041).toInt()
        for (i in d2 until out.size) {
            out[i] += src[i - d1] * mix + src[i - d2] * (mix * 0.55f)
        }
        for (i in out.indices) out[i] = out[i].coerceIn(-1f, 1f)
        return out
    }

    private fun mix(vararg parts: FloatArray): FloatArray {
        val n = parts.maxOfOrNull { it.size } ?: 0
        val out = FloatArray(n)
        parts.forEach { p -> for (i in p.indices) out[i] += p[i] }
        for (i in out.indices) out[i] = (out[i] * 0.85f).coerceIn(-1f, 1f)
        return out
    }

    private fun encode(samples: FloatArray): ByteArray {
        val bytes = ByteArray(samples.size * 2)
        for (i in samples.indices) {
            val v = (samples[i].coerceIn(-1f, 1f) * 32767f).toInt()
            bytes[i * 2] = (v and 0xFF).toByte()
            bytes[i * 2 + 1] = ((v shr 8) and 0xFF).toByte()
        }
        return bytes
    }
}
