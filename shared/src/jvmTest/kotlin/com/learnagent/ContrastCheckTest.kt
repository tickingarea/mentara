package com.learnagent

import com.learnagent.design.ContrastCheck
import com.learnagent.design.DesignTokens
import kotlin.test.Test
import kotlin.test.assertTrue

/** 对比度审计：TextHigh/Mid on Veil ≥ 4.5，TextMid on Glimmer ≥ 4.5，TextLow on Void ≥ 2.0 */
class ContrastCheckTest {

    private fun check(fg: Long, bg: Long, min: Double, label: String) {
        val ratio = ContrastCheck.contrast(fg, bg)
        assertTrue(ratio >= min, "$label contrast $ratio < $min (fg=#${fg.toString(16)}, bg=#${bg.toString(16)})")
    }

    @Test
    fun textHighOnVeil() =
        check(DesignTokens.ColorTextHigh, DesignTokens.ColorVeil, 4.5, "TextHigh on Veil")

    @Test
    fun textHighOnGlimmer() =
        check(DesignTokens.ColorTextHigh, DesignTokens.ColorGlimmer, 4.5, "TextHigh on Glimmer")

    @Test
    fun textMidOnVeil() =
        check(DesignTokens.ColorTextMid, DesignTokens.ColorVeil, 4.5, "TextMid on Veil")

    @Test
    fun textMidOnGlimmer() =
        check(DesignTokens.ColorTextMid, DesignTokens.ColorGlimmer, 4.5, "TextMid on Glimmer")

    @Test
    fun textLowOnVoid() =
        check(DesignTokens.ColorTextLow, DesignTokens.ColorVoid, 2.0, "TextLow on Void")

    @Test
    fun textMidOnVoid() =
        check(DesignTokens.ColorTextMid, DesignTokens.ColorVoid, 4.5, "TextMid on Void")
}
