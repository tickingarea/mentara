package com.learnagent.core

import com.learnagent.data.TimelineItem

/**
 * 大模型语义重点判定：让 LLM 判断哪些片段是「重点必学」，并给简短理由。
 * 无 LLM / 失败 / 无法解析时，回退到 [KeyKnowledge] 的启发式结果（不丢标签、不联网也能用）。
 */
class SemanticKeyJudge(private val llm: LlmClient?) {

    suspend fun judge(timeline: List<TimelineItem>): List<String?> {
        val heuristic = KeyKnowledge.tags(timeline)
        val client = llm ?: return heuristic
        if (timeline.isEmpty()) return heuristic

        val idxs = Evidence.spreadIndices(timeline.size, 300)
        val numbered = Evidence.renderNumbered(timeline, idxs)

        val prompt = buildString {
            append("下面是一门网课的视频证据：每段含【语音(讲师原话)】与【画面(PPT/板书文字)】，")
            append("已按时间顺序从整片均匀采样，编号 [序号] 对应原始时间线位置。\n")
            append("请综合【语音+画面】两项证据，判断哪些是「重点必学」，只依据给定内容，不要编造。\n")
            append("只输出 JSON 数组，元素形如 {\"i\":序号,\"why\":\"理由(≤6字)\"}；非重点不要输出，不要任何多余文字。\n\n")
            append(numbered)
        }

        val raw = runCatching {
            client.generateResponse(prompt, LlmConfig(provider = "nvidia", apiKey = "", baseUrl = "", model = ""))
        }.getOrNull() ?: return heuristic

        val pairs = parse(raw)
        if (pairs.isEmpty()) return heuristic

        val out = heuristic.toMutableList()
        pairs.forEach { (i, why) ->
            if (i in out.indices) out[i] = why.ifBlank { "AI重点" }
        }
        return out
    }

    private fun parse(raw: String): List<Pair<Int, String>> {
        val objects = Regex("\\{[^{}]*\\}")
        val iRe = Regex("\"i\"\\s*:\\s*(\\d+)")
        val wRe = Regex("\"why\"\\s*:\\s*\"([^\"]*)\"")
        return objects.findAll(raw).mapNotNull { m ->
            val s = m.value
            val i = iRe.find(s)?.groupValues?.get(1)?.toIntOrNull() ?: return@mapNotNull null
            val why = wRe.find(s)?.groupValues?.get(1)?.trim() ?: ""
            i to why
        }.toList()
    }
}
