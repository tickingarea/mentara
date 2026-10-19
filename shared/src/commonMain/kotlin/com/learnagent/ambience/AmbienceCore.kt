package com.learnagent.ambience

import com.learnagent.design.DesignTokens

/**
 * 人为因素纯逻辑（三端共享，无 Compose / 无平台依赖）。
 * 桌面 Ambience 与 Android AmbienceState 都是它的薄封装——行为完全一致。
 *
 *  - 时间段 accent：06-12 暖紫 / 12-18 幻紫 / 18-06 深紫
 *  - 近 60 秒操作频率 → 速度 ±20%
 *  - 空闲 30 秒 → 沉睡
 *  - 使用 >1h → 休息提醒；>3h → 夜读模式
 *  - 内容量 → 粒子数 36~80
 */
object AmbienceCore {

    private val tapTimes = ArrayDeque<Long>()
    private var startedAt = 0L
    private var lastInteractionAt = 0L
    private var lastBreakNoticeAt = 0L

    /** 会话起点（各端包装层启动时调用）。 */
    fun start(now: Long) {
        startedAt = now
        lastInteractionAt = now
        synchronized(tapTimes) { tapTimes.clear() }
    }

    fun noteInteraction(now: Long) {
        lastInteractionAt = now
        synchronized(tapTimes) {
            tapTimes.addLast(now)
            while (tapTimes.isNotEmpty() && now - tapTimes.first() > 60_000) tapTimes.removeFirst()
        }
    }

    private fun tapRate(now: Long): Int = synchronized(tapTimes) {
        tapTimes.count { now - it <= 60_000 }
    }

    /** 快 +20% / 慢 −20% / 基准 1.0。 */
    fun speedFactor(now: Long): Float {
        val rate = tapRate(now)
        return when {
            rate >= 10 -> 1.2f
            rate <= 2 -> 0.8f
            else -> 1.0f
        }
    }

    /** 空闲 30 秒 → 沉睡。 */
    fun isSleeping(now: Long): Boolean = now - lastInteractionAt > 30_000

    fun sessionMinutes(now: Long): Long = if (startedAt == 0L) 0 else (now - startedAt) / 60_000

    /** >3 小时 → 夜读模式。 */
    fun isNightMode(now: Long): Boolean = sessionMinutes(now) >= 180

    /** >1h 且距上次提醒 >10min → 触发一次休息提醒。 */
    fun shouldShowBreakNotice(now: Long): Boolean {
        if (sessionMinutes(now) >= 60 && now - lastBreakNoticeAt > 10 * 60_000) {
            lastBreakNoticeAt = now
            return true
        }
        return false
    }

    /** 粒子数：36 基础，随内容量增长，上限 80。 */
    fun particleCount(videoCount: Int): Int = (36 + videoCount * 4).coerceIn(36, 80)

    /** 时间段 accent 的 ARGB 原始值（V3 晨雾紫 / 睡莲绿 / 深海蓝，DesignTokens 同源）。 */
    fun accentRaw(hour: Int): Long = when (hour) {
        in 6..11 -> DesignTokens.ColorMorningPurple
        in 12..17 -> DesignTokens.ColorLilyGreen
        else -> DesignTokens.ColorNightPurple
    }

    // ---------- 测试 / 预览辅助（跨模块需 public） ----------
    fun rewindSession(now: Long, byMillis: Long) {
        startedAt = now - kotlin.math.abs(byMillis)
    }
}
