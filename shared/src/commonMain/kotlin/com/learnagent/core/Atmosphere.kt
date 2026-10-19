package com.learnagent.core

/** 画面氛围量化结果（值均为 0..1 或 -1..1，可复算，不依赖模型）。 */
data class FrameMood(
    val brightness: Float,   // 0..1
    val saturation: Float,   // 0..1
    val warmth: Float,       // -1(冷) .. +1(暖)
    val contrast: Float,     // 0..1
    val labels: List<String>
)

/**
 * 画面氛围判定（确定性规则，非生成式，避免幻觉）：
 * 由平台层从像素算出 brightness/saturation/warmth/contrast，这里只做阈值分类。
 */
object Atmosphere {
    fun classify(brightness: Float, saturation: Float, warmth: Float, contrast: Float): FrameMood {
        val labels = ArrayList<String>(4)
        labels += when {
            brightness < 0.28f -> "昏暗"
            brightness > 0.68f -> "明亮"
            else -> "中等亮度"
        }
        labels += when {
            saturation < 0.18f -> "低饱和"
            saturation > 0.45f -> "鲜艳"
            else -> "自然色"
        }
        labels += when {
            warmth > 0.12f -> "暖调"
            warmth < -0.12f -> "冷调"
            else -> "中性色温"
        }
        labels += when {
            contrast < 0.16f -> "柔和"
            contrast > 0.33f -> "高对比"
            else -> "均衡对比"
        }
        return FrameMood(brightness, saturation, warmth, contrast, labels)
    }

    fun label(mood: FrameMood): String = mood.labels.joinToString("·")

    /** 由 RGB(0..255) 像素均值区域计算指标。 */
    fun fromAverages(rAvg: Float, gAvg: Float, bAvg: Float, minLuma: Float, maxLuma: Float): FrameMood {
        val r = rAvg / 255f; val g = gAvg / 255f; val b = bAvg / 255f
        val brightness = (0.299f * r + 0.587f * g + 0.114f * b).coerceIn(0f, 1f)
        val maxc = maxOf(r, g, b); val minc = minOf(r, g, b)
        val saturation = if (maxc <= 0f) 0f else ((maxc - minc) / maxc).coerceIn(0f, 1f)
        val warmth = (r - b).coerceIn(-1f, 1f)
        val contrast = ((maxLuma - minLuma) / 255f).coerceIn(0f, 1f)
        return classify(brightness, saturation, warmth, contrast)
    }
}
