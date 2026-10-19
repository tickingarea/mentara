package com.learnagent.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.learnagent.ambience.AmbienceCore

/**
 * Android 人为因素 Compose 状态层：纯逻辑在共享 AmbienceCore（与桌面同源）。
 */
object AmbienceState {

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
}
