package com.learnagent.core

import com.learnagent.data.TimelineItem

/**
 * 话题之外的"额外提醒/题外话"识别，方便跳着学习。
 * 纯启发式、本地可离线；命中标记为 「题外话·可跳」。
 */
object SideNote {

    private val markers = listOf(
        "题外话", "顺便", "插一句", "扯远", "说个题外", "打个广告", "广告",
        "关注我", "点赞", "投币", "一键三连", "公众号", "微信群", "加群",
        "下课", "休息一下", "下次课", "课后作业", "签到", "考试时间",
        "报名", "优惠", "私信", "别忘", "再见", "拜拜"
    )

    fun tags(timeline: List<TimelineItem>): List<String?> = timeline.map { item ->
        val t = item.speech + " " + item.screenText
        if (markers.any { t.contains(it) }) "题外话·可跳" else null
    }
}

/** 时间段概括工具：按 [from,to]（秒）截取片段。 */
object RangeSlice {
    fun slice(timeline: List<TimelineItem>, from: Double, to: Double): List<TimelineItem> {
        val lo = minOf(from, to)
        val hi = maxOf(from, to)
        return timeline.filter { it.start >= lo - 0.5 && it.start <= hi + 0.5 }
    }

    /** 解析时间：支持 "mm:ss" / "h:mm:ss" / 纯秒数 / "5分30" / "5分30秒" / "90s"；失败返回 null。 */
    fun parseStamp(s: String): Double? {
        var t = s.trim()
            .replace("，", ",")
            .replace(" ", "")
            .replace("秒", "")
            .replace("分", ":")
            .replace("时", ":")
        t = t.trimEnd('s', 'S', 'm', 'h', ':')
        if (t.isEmpty()) return null
        if (t.startsWith(":")) t = "0$t"
        if (t.endsWith(":")) return null
        val parts = t.split(':')
        return when (parts.size) {
            1 -> parts[0].toDoubleOrNull()
            2 -> {
                val m = parts[0].toDoubleOrNull() ?: return null
                val sec = parts[1].toDoubleOrNull() ?: return null
                m * 60 + sec
            }
            3 -> {
                val h = parts[0].toDoubleOrNull() ?: return null
                val m = parts[1].toDoubleOrNull() ?: return null
                val sec = parts[2].toDoubleOrNull() ?: return null
                h * 3600 + m * 60 + sec
            }
            else -> null
        }
    }

    fun fmt(seconds: Double): String {
        val t = seconds.toInt()
        fun two(v: Int) = if (v < 10) "0$v" else "$v"
        return two(t / 60) + ":" + two(t % 60)
    }
}
