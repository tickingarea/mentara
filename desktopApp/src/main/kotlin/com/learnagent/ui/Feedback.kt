package com.learnagent.ui

import com.learnagent.audio.PcmSynth
import com.learnagent.audio.SoundSynth
import java.awt.Window
import javax.swing.SwingUtilities

/**
 * 桌面端触感/音效反馈（SoundSynth 合成音 + 窗口微震，无额外依赖）。
 * 通过 [enabled] 支持设置项「音效反馈」开关；[hapticsEnabled] 由同一开关控制。
 */
object Feedback {

    var enabled: Boolean = true

    var hapticsEnabled: Boolean = true

    @Volatile
    var window: Window? = null

    @Volatile
    private var lastHoverAt = 0L

    private fun sound(s: PcmSynth.Sound, volumeScale: Float = 1f) {
        if (!enabled) return
        runCatching { SoundSynth.play(s, volumeScale) }
    }

    /** 鼠标滑过可点击元素（节流 120ms，避免连响）。 */
    fun hover() {
        val now = System.currentTimeMillis()
        if (now - lastHoverAt < 120) return
        lastHoverAt = now
        sound(PcmSynth.Sound.HOVER)
    }

    /** 轻提示 / 点击按钮、卡片。 */
    fun tap() {
        Ambience.noteInteraction()
        sound(PcmSynth.Sound.TAP)
    }

    /** 成功：导入、记录经验、保存。 */
    fun success() {
        sound(PcmSynth.Sound.SUCCESS)
        haptic(shakePx = 2, cycles = 2)
    }

    /** 失败：处理错误。 */
    fun error() {
        sound(PcmSynth.Sound.ERROR)
        haptic(shakePx = 3, cycles = 3)
    }

    /** 打开对话框。 */
    fun open() {
        sound(PcmSynth.Sound.OPEN)
    }

    /** 关闭对话框。 */
    fun close() {
        sound(PcmSynth.Sound.CLOSE)
    }

    /** Splash 主题音。 */
    fun splashTone(volumeScale: Float = 1f) {
        sound(PcmSynth.Sound.SPLASH_TONE, volumeScale)
    }

    /** Splash 幕 2：三声叮。 */
    fun chime(volumeScale: Float = 1f) {
        sound(PcmSynth.Sound.CHIME, volumeScale)
    }

    /** Splash 幕 3：低频嗡声。 */
    fun drone(volumeScale: Float = 1f) {
        sound(PcmSynth.Sound.DRONE, volumeScale)
    }

    /** Splash 幕 4：温柔呼声。 */
    fun whoosh(volumeScale: Float = 1f) {
        sound(PcmSynth.Sound.WHOOSH, volumeScale)
    }

    /** 久坐休息提醒（柔和）。 */
    fun rest() {
        sound(PcmSynth.Sound.REST)
    }

    /**
     * 触感反馈：窗口微震 1–3 像素（30–70ms 内往返后复位）。
     * 仅用于「成功」与「错误」。
     */
    private fun haptic(shakePx: Int, cycles: Int) {
        if (!hapticsEnabled) return
        val w = window ?: return
        val thread = Thread {
            val origin = intArrayOf(0, 0)
            runCatching {
                SwingUtilities.invokeAndWait {
                    origin[0] = w.x
                    origin[1] = w.y
                }
            }.onFailure { return@Thread }
            val offsets = ArrayList<Int>(cycles * 3 + 1)
            repeat(cycles) {
                offsets.add(shakePx)
                offsets.add(-shakePx)
            }
            offsets.add(0)
            for (d in offsets) {
                SwingUtilities.invokeLater { runCatching { w.setLocation(origin[0] + d, origin[1]) } }
                Thread.sleep(14)
            }
            SwingUtilities.invokeLater { runCatching { w.setLocation(origin[0], origin[1]) } }
        }
        thread.isDaemon = true
        thread.start()
    }
}
