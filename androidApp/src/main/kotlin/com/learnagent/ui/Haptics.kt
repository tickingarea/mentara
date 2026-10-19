package com.learnagent.ui

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator

/**
 * 触感反馈（对齐桌面"窗口微震"语义）：
 *  - 成功 = 轻震 2 次 / 错误 = 3 次，仅关键操作
 *  - Splash 三节点落地各轻震一次
 */
object Haptics {

    @Volatile
    var enabled: Boolean = true

    private fun pulse(context: Context, pattern: LongArray) {
        if (!enabled) return
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (!vibrator.hasVibrator()) return
        runCatching {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        }
    }

    /** 按压轻触。 */
    fun tap(context: Context) = pulse(context, longArrayOf(0, 18))

    /** 成功：2 次轻震。 */
    fun success(context: Context) = pulse(context, longArrayOf(0, 30, 70, 30))

    /** 错误：3 次轻震。 */
    fun error(context: Context) = pulse(context, longArrayOf(0, 40, 60, 40, 60, 40))
}
