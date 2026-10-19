package com.learnagent.ui

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/**
 * 3D 轨道运动词汇（三端共享，Splash 三圆环与两端的背景轨道/游走光灵共用，视觉同源）。
 * 纯 Kotlin 数学，无 Compose / 平台依赖（shared 的 android 目标限制）。
 *
 * 设计目标：灵活、聪慧、不可测——
 *  - 多层正弦叠加伪噪声（无外部依赖）
 *  - 每个节点相位 / 频率 / 振幅 / 倾角全不同 → 永不同步
 *  - 时间本身带快慢起伏（easedTime）→ 永不匀速
 *  - 轨道叠加噪声扰动 → 永非正圆
 */
object OrbitMotion {

    /** Splash 轨道场边长（dp）。 */
    const val SPLASH_FIELD = 380

    /** Splash 场内归一化 → 像素缩放（x / y）。 */
    const val SPLASH_SCALE_X = 0.18f
    const val SPLASH_SCALE_Y = 0.16f

    /** 一维分层正弦伪噪声，输出约 [-1,1]，平滑但非严格周期。 */
    fun noise(x: Float): Float =
        sin(x * 1.7f) * 0.50f +
            sin(x * 2.31f + 1.7f) * 0.32f +
            sin(x * 0.73f + 3.1f) * 0.18f

    /** 非匀速时间：自带快慢节律（呼吸式时间流）。 */
    fun easedTime(t: Float): Float = t + 0.9f * sin(t * 0.23f)

    data class Orbit(
        val phase: Float,
        val freqX: Float,
        val freqY: Float,
        val freqZ: Float,
        val ax: Float,
        val ay: Float,
        val az: Float,
        val tilt: Float
    )

    /** 三条互不相干的轨道参数（相位 2.1 错开；频率与振幅逐节点递变）。 */
    val ORBITS: List<Orbit> = List(3) { i ->
        Orbit(
            phase = i * 2.1f,
            freqX = 0.37f + i * 0.09f,
            freqY = 0.53f + i * 0.11f,
            freqZ = 0.71f + i * 0.07f,
            ax = 1.00f + i * 0.25f,
            ay = 0.68f + i * 0.15f,
            az = 1.50f + i * 0.31f,
            tilt = ((15.0 + i * 10.0) * PI / 180.0).toFloat()
        )
    }

    /**
     * 不规则 3D 轨道位置（归一化输出，相对轨道中心）。
     * out[0]=x, out[1]=y, out[2]=z（z 为伪深度）。
     */
    fun position(i: Int, t: Float, out: FloatArray) {
        val o = ORBITS[i]
        val et = easedTime(t)
        var x = o.ax * sin(et * o.freqX + o.phase) * cos(o.tilt)
        var y = o.ay * cos(et * o.freqY + o.phase * 0.7f)
        var z = o.az * sin(et * o.freqZ + o.phase * 1.3f)
        x += noise(et * 0.5f + i) * 0.15f
        y += noise(et * 0.7f + i * 2f) * 0.15f
        z += noise(et * 0.9f + i * 3f) * 0.10f
        out[0] = x
        out[1] = y
        out[2] = z
    }

    /**
     * Splash 编舞位置：在 [position] 基础上叠加——
     *  - pop（入场进度）：从中心与深处"漂来"（0-2s 慢入）
     *  - 5-7s 高潮：三环短暂靠近又分开（converge 脉冲）
     */
    fun splashPosition(i: Int, t: Float, pop: Float, out: FloatArray) {
        position(i, t, out)
        val dx = t - 6f
        val converge = exp(-dx * dx / 1.7f)
        val amp = (1f - 0.45f * converge) * pop
        out[0] = out[0] * amp
        out[1] = out[1] * amp
        out[2] = out[2] * amp - (1f - pop) * 1.4f
    }
}
