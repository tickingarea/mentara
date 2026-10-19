package com.learnagent.core

/** 文本质量过滤：剔除 OCR 乱码/残影与 ASR 幻觉（在送入大模型前拦截）。 */
object TextQuality {

    /** OCR 乱码判定：有效字符过少、纯符号、重复字符连发、英文乱码（如 TRACE、t.tt記TTT）。 */
    fun isGarbageOcr(s: String): Boolean {
        val t = s.trim()
        if (t.length < 3) return true
        val cjk = t.count { it.code in 0x4E00..0x9FFF }
        if (cjk > 0) {
            if (cjk < 3) return true // 少于 3 个中文字符
            return Regex("(.)\\1{3,}").containsMatchIn(t) // TTTT/OOOO 连发
        }
        // 无中文：至少 2 个 ≥3 字母的单词，且字母占比 ≥60%，否则视为乱码
        val words = Regex("[A-Za-z]{3,}").findAll(t).count()
        if (words < 2) return true
        val letters = t.count { it.isLetter() }
        if (letters < t.length * 6 / 10) return true
        return Regex("(.)\\1{3,}").containsMatchIn(t)
    }

    /** ASR 幻觉判定：如 "my when when when"、整句高度重复、超短噪声。 */
    fun isHallucinatedAsr(s: String): Boolean {
        val t = s.trim()
        if (t.length < 2) return true
        val words = t.split(Regex("[\\s,，。！？;；]+")).filter { it.isNotBlank() }
        if (words.size >= 3 && words.toSet().size <= 2) return true // my when when when
        if (Regex("(\\S+)(\\s+\\1\\b){2,}").containsMatchIn(t)) return true // 同词连发≥3
        if (Regex("(.)\\1{5,}").containsMatchIn(t)) return true // 同字符连发≥6
        val u = t.replace(Regex("\\s"), "")
        if (u.length >= 8 && u.toSet().size <= 3) return true // 高度重复
        return false
    }
}
