package com.learnagent.core

import com.learnagent.data.TimelineItem

/**
 * 重点知识标注：纯启发式（无需联网即可用）。
 * 依据三类信号给每个片段打"必学"标签：
 *  1) 强调词（重点/必考/记住/易错…）
 *  2) PPT 标题式画面文字（短、无句读）
 *  3) 重复出现（同一句在片中多次讲到 → 讲师在强调）
 */
object KeyKnowledge {

    private val emphasis = listOf(
        "重点", "划重点", "必考", "考点", "核心", "关键", "总结", "易错",
        "一定要", "别忘了", "考试", "难点", "记住", "注意", "易混", "必学"
    )

    /** 返回与 [timeline] 等长的标签列表；null 表示非重点。 */
    fun tags(timeline: List<TimelineItem>): List<String?> {
        val norm = timeline.map { normalize(it.speech.ifBlank { it.screenText }) }
        val freq = HashMap<String, Int>()
        norm.forEach { n -> if (n.length >= 6) freq[n] = (freq[n] ?: 0) + 1 }
        return timeline.mapIndexed { i, item ->
            val speech = item.speech
            val screen = item.screenText
            when {
                emphasis.any { speech.contains(it) } -> "强调"
                isHeading(screen) -> "标题"
                norm[i].length >= 6 && (freq[norm[i]] ?: 0) >= 2 -> "重复"
                else -> null
            }
        }
    }

    fun isKey(timeline: List<TimelineItem>, index: Int): Boolean =
        tags(timeline).getOrNull(index) != null

    /** 短、无句读 → 视为画面标题（PPT 小标题）。 */
    fun isHeading(screen: String): Boolean {
        val s = screen.trim()
        if (s.length !in 2..16) return false
        if (s.endsWith("。") || s.endsWith("，") || s.endsWith("？") || s.endsWith("！") ||
            s.endsWith(".") || s.endsWith(",") || s.endsWith("?") || s.endsWith("!")
        ) return false
        if (s.contains("，") || s.contains("。")) return false
        return true
    }

    private fun normalize(s: String): String =
        s.replace(Regex("[\\s\\u3000，。、！？；：,.!?;:]"), "")
}
