package com.learnagent.design

import kotlin.math.pow

/**
 * 对比度审计（三端共用纯函数）：WCAG 相对亮度对比度。
 * 前景色与背景色的 ARGB Long → 比率。
 */
object ContrastCheck {

    /** 相对亮度（WCAG 公式）。 */
    fun relativeLuminance(argb: Long): Double {
        val r = ((argb shr 16) and 0xFF) / 255.0
        val g = ((argb shr 8) and 0xFF) / 255.0
        val b = (argb and 0xFF) / 255.0
        fun linear(c: Double): Double = if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        return 0.2126 * linear(r) + 0.7152 * linear(g) + 0.0722 * linear(b)
    }

    /** WCAG 对比度（1~21）。 */
    fun contrast(fg: Long, bg: Long): Double {
        val l1 = relativeLuminance(fg)
        val l2 = relativeLuminance(bg)
        val hi = maxOf(l1, l2)
        val lo = minOf(l1, l2)
        return (hi + 0.05) / (lo + 0.05)
    }
}
