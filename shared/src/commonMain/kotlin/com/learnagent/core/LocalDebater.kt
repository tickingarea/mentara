package com.learnagent.core

import com.learnagent.data.TimelineItem

/**
 * 本地辩论启发式回退：未配置 API Key 时，
 * 用三种"视角"（概述 / 挑战 / 裁决→证据）对视频时间线做抽取式分析。
 * 所有输出明确标注"（本地启发式）"，绝不伪装成远程大模型的回答。
 */
class LocalDebater(
    private val role: String,
    private val timeline: List<TimelineItem>
) : LlmClient {

    override suspend fun generateResponse(prompt: String, config: LlmConfig): String {
        val sentences = timeline
            .flatMap { (it.speech + "。" + it.screenText).split(Regex("[。；;\n]")) }
            .map { it.trim() }
            .filter { it.length >= 8 }
        if (sentences.isEmpty()) return "（本地启发式：时间线为空，无法分析）"

        val negMarkers = listOf("不", "错", "局限", "问题", "然而", "但是", "反例", "失败", "难")
        return when (role) {
            "挑战" -> {
                val found = sentences.filter { s -> negMarkers.any { s.contains(it) } }.take(2)
                (if (found.isEmpty()) {
                    listOf("现有内容没有找到明显反例；建议补充边界条件与失败场景验证。")
                } else found).joinToString("；") + "。（本地启发式）"
            }
            "裁决" -> {
                val best = sentences.maxByOrNull { it.length }
                "采纳主结论；并在适用范围上保持谨慎。" +
                    (best?.let { "关键证据：$it。" } ?: "") + "（本地启发式）"
            }
            else -> {
                sentences.sortedByDescending { it.length }.take(2).joinToString("；") + "。（本地启发式）"
            }
        }
    }
}
