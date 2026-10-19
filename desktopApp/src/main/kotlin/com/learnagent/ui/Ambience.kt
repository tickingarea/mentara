package com.learnagent.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.learnagent.ambience.AmbienceCore

/**
 * 桌面人为因素薄封装：纯逻辑在共享 AmbienceCore（与 Android 同源），
 * 这里只持有 Compose 状态，公共 API 与行为和旧版完全一致。
 */
object Ambience {

    var particleCount by mutableStateOf(36)
        private set
    var speedFactor by mutableStateOf(1f)
        private set
    var sleeping by mutableStateOf(false)
        private set
    var nightMode by mutableStateOf(false)
        private set
    var breakNotice by mutableStateOf(false)
        private set

    init {
        AmbienceCore.start(System.currentTimeMillis())
    }

    fun noteInteraction() {
        val now = System.currentTimeMillis()
        AmbienceCore.noteInteraction(now)
        if (sleeping) sleeping = false
    }

    fun dismissBreakNotice() {
        breakNotice = false
    }

    fun updateContentVolume(videoCount: Int) {
        particleCount = AmbienceCore.particleCount(videoCount)
    }

    /** 由主界面每秒调用一次。 */
    fun tick() {
        val now = System.currentTimeMillis()
        speedFactor = AmbienceCore.speedFactor(now)
        sleeping = AmbienceCore.isSleeping(now)
        nightMode = AmbienceCore.isNightMode(now)
        if (AmbienceCore.shouldShowBreakNotice(now)) breakNotice = true
    }

    /** 测试/预览辅助：直接设定粒子数（不影响业务）。 */
    fun previewParticles(count: Int) {
        particleCount = count.coerceIn(20, 80)
    }

    internal fun resetSessionForTest(now: Long) {
        AmbienceCore.start(now)
        speedFactor = 1f
        sleeping = false
        nightMode = false
        breakNotice = false
    }

    internal fun ageMinutesForTest(minutes: Long) {
        AmbienceCore.rewindSession(System.currentTimeMillis(), kotlin.math.abs(minutes) * 60_000)
    }
}
