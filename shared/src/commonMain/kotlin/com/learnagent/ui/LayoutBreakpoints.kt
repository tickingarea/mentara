package com.learnagent.ui

/** 自适应断点（dp）：手机 / 紧凑平板 / 全尺寸平板。 */
object LayoutBreakpoints {
    const val PHONE_MAX = 600
    const val TABLET_COMPACT_MAX = 900

    fun forWidth(dp: Int): LayoutMode = when {
        dp < PHONE_MAX -> LayoutMode.PHONE
        dp < TABLET_COMPACT_MAX -> LayoutMode.TABLET_COMPACT
        else -> LayoutMode.TABLET_FULL
    }
}

enum class LayoutMode { PHONE, TABLET_COMPACT, TABLET_FULL }
