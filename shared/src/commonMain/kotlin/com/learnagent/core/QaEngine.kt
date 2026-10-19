package com.learnagent.core

import kotlinx.serialization.Serializable
import com.learnagent.data.TimelineItem
import com.learnagent.data.LearningUnit

@Serializable
data class QaAnswer(
    val answer: String,
    val citations: List<String>
)

@Serializable
data class LlmConfig(
    val provider: String,
    val apiKey: String,
    val baseUrl: String,
    val model: String,
    /** 系统提示词（可为空）。 */
    val system: String = "",
    val temperature: Double = 0.6,
    val maxTokens: Int = 4096
)

/** 回答模式：严谨（只用视频、可溯源）/ 专家（允许通识+联网，质量优先）。 */
enum class AnswerMode { STRICT, EXPERT }

/** 联网检索结果。 */
data class WebResult(val title: String, val url: String, val content: String)

/** 联网搜索接口（注入实现，如 Tavily）。 */
fun interface WebSearch {
    suspend fun search(query: String): List<WebResult>
}

interface QaEngine {
    suspend fun ask(question: String, videoId: String? = null): QaAnswer
}

interface LlmClient {
    suspend fun generateResponse(prompt: String, config: LlmConfig): String
}

/** 支持流式输出（SSE）的客户端；同时可回传推理过程（reasoning_content）。 */
interface StreamingLlmClient {
    suspend fun streamResponse(
        prompt: String,
        config: LlmConfig,
        onToken: (String) -> Unit,
        onReasoning: (String) -> Unit
    ): String
}

class KeywordQaEngine(
    private val search: suspend (String, Int) -> List<TimelineItem>,
    private val llmClient: LlmClient? = null
) : QaEngine {
    
    override suspend fun ask(question: String, videoId: String?): QaAnswer {
        val searchResults = search(question, 10)
        
        if (searchResults.isEmpty()) {
            return QaAnswer(
                answer = "视频中未提及",
                citations = emptyList()
            )
        }
        
        if (llmClient == null) {
            return QaAnswer(
                answer = "未配置API Key",
                citations = searchResults.map { it.start.toString() }
            )
        }
        
        val context = searchResults.joinToString("\n") { 
            "时间点: , 内容: " 
        }
        
        val prompt = """
            问题: 
        
            相关视频内容:
            
        
            请基于以上视频内容回答问题，引用相关的时间点。
        """.trim()
        
        val answer = llmClient.generateResponse(prompt, LlmConfig(
            provider = "",
            apiKey = "",
            baseUrl = "",
            model = ""
        ))
        
        return QaAnswer(
            answer = answer,
            citations = searchResults.map { it.start.toString() }
        )
    }
}

/**
 * 纯本地的问答引擎（不依赖外部 LLM）。
 *
 * 用检索到的 [TimelineItem] 直接拼接答案，并返回时间戳引用。
 * 在未配置大模型时提供可用的问答能力。
 */
class LocalQaEngine(
    private val allItems: (() -> List<TimelineItem>)? = null,
    private val search: suspend (String, Int) -> List<TimelineItem>
) : QaEngine {

    override suspend fun ask(question: String, videoId: String?): QaAnswer {
        var prefix = ""
        var results = search(question, 5)

        if (results.isEmpty() && isGenericQuestion(question)) {
            val all = allItems?.invoke().orEmpty()
            if (all.isNotEmpty()) {
                prefix = "没有直接匹配，但以下是视频的核心内容：\n"
                results = all.take(5)
            }
        }

        if (results.isEmpty()) {
            return QaAnswer(answer = "视频中未提及相关内容。", citations = emptyList())
        }

        val answer = buildString {
            if (prefix.isNotEmpty()) append(prefix)
            append("根据视频内容，找到 ")
            append(results.size)
            append(" 条相关信息：\n")
            for (item in results) {
                val parts = listOf(item.speech, item.screenText).filter { it.isNotBlank() }
                val text = if (parts.isEmpty()) "（无内容）" else parts.joinToString(" | ")
                append("- [").append(formatStamp(item.start)).append("] ").append(text).append('\n')
            }
        }.trim()

        return QaAnswer(
            answer = answer,
            citations = results.map { formatStamp(it.start) }
        )
    }

    private fun formatStamp(seconds: Double): String {
        val total = seconds.toInt()
        return two(total / 60) + ":" + two(total % 60)
    }

    private fun two(value: Int): String = if (value < 10) "0$value" else value.toString()
}

/** 泛问检测：短句 + 无 ASCII 字母数字 + 命中"什么/讲了/内容/介绍/总结/重点/回顾"。 */
fun isGenericQuestion(q: String): Boolean {
    if (q.length >= 15) return false
    if (q.any { it.code < 128 && it.isLetterOrDigit() }) return false
    val markers = listOf("什么", "讲了", "内容", "介绍", "总结", "重点", "回顾", "聊")
    return markers.any { q.contains(it) }
}

private fun stampOf(seconds: Double): String {
    val t = seconds.toInt()
    fun two(v: Int) = if (v < 10) "0$v" else "$v"
    return two(t / 60) + ":" + two(t % 60)
}

/**
 * 基于 [LearningUnit] 的问答引擎：
 * - 泛问 → 返回 importance>=4 的 top5
 * - 检索为空 → 降级返回 importance 最高的 top5
 * - 有 LLM → 用 LearningUnit 升华内容生成回答；失败/无 LLM → 本地拼接
 */
class LearningUnitQaEngine(
    private val units: () -> List<LearningUnit>,
    private val llmClient: LlmClient? = null
) : QaEngine {

    override suspend fun ask(question: String, videoId: String?): QaAnswer {
        val all = units()
        if (all.isEmpty()) return QaAnswer("视频中未提及相关内容。", emptyList())

        val generic = isGenericQuestion(question)
        val keywords = question.lowercase()
            .split(Regex("[\\s,，。、！？；：]+"))
            .filter { it.isNotBlank() }

        val hits: List<LearningUnit> = when {
            generic -> all.filter { it.importance >= 4 }.sortedByDescending { it.importance }.take(5)
            else -> {
                val matched = all.filter { u ->
                    val content = (u.speech + " " + u.screenText + " " + u.oneLineSummary).lowercase()
                    keywords.any { content.contains(it) }
                }
                if (matched.isNotEmpty()) matched.take(5)
                else all.sortedByDescending { it.importance }.take(5)
            }
        }

        val fallbackPrefix = when {
            generic -> "以下是视频的核心内容：\n"
            hits.none { u -> keywords.any { (u.speech + u.screenText).lowercase().contains(it) } } ->
                "没有直接匹配，但以下是视频的核心内容：\n"
            else -> ""
        }

        fun local(): String = buildString {
            append(fallbackPrefix)
            hits.forEach { u ->
                val text = u.oneLineSummary.ifBlank { u.speech.ifBlank { u.screenText } }
                append("- [").append(stampOf(u.start)).append("] ").append(text).append('\n')
            }
        }.trim()

        val answer = if (llmClient != null) {
            val context = hits.joinToString("\n") { "[${stampOf(it.start)}] ${it.oneLineSummary.ifBlank { it.speech }}" }
            val prompt = "基于以下视频内容回答问题（只使用给定内容）：\n$context\n\n问题：$question"
            try {
                llmClient.generateResponse(prompt, LlmConfig("nvidia", "", "", ""))
            } catch (e: Exception) {
                local()
            }
        } else {
            local()
        }

        return QaAnswer(answer = answer, citations = hits.map { stampOf(it.start) })
    }
}

/**
 * 基于远程大模型（如 NVIDIA NIM）的问答引擎。
 *
 * 先用检索结果构造带时间戳的上下文，再交给 [LlmClient]（BYOK）生成回答，
 * 并把命中片段的时间戳作为引用返回。
 */
class RemoteQaEngine(
    private val search: suspend (String, Int) -> List<TimelineItem>,
    private val llmClient: LlmClient,
    private val topK: Int = 8,
    private val fallbackAll: (suspend () -> List<TimelineItem>)? = null,
    private val mode: () -> AnswerMode = { AnswerMode.EXPERT },
    private val webSearch: WebSearch? = null,
    private val extraContext: () -> String = { "" }
) : QaEngine {

    override suspend fun ask(question: String, videoId: String?): QaAnswer =
        askStream(question, videoId, {}, {})

    /** 流式问答：onToken 逐字回调；onReasoning 回传思考过程。 */
    suspend fun askStream(
        question: String,
        videoId: String?,
        onToken: (String) -> Unit,
        onReasoning: (String) -> Unit
    ): QaAnswer {
        var results = search(question, topK)
        if (results.size < 2 && fallbackAll != null) {
            // 全片均匀采样（语音段优先）：旧版 take(60) 在 20fps 下只等于"开头 3 秒"
            val all = fallbackAll()
            val speech = all.filter { it.speech.isNotBlank() }
            val picked = if (speech.size >= 2) Evidence.spread(speech, 60) else Evidence.spread(all, 60)
            if (picked.size > results.size) results = picked
        }
        val expert = mode() == AnswerMode.EXPERT
        // 不再拒答：没有命中也照常回答（视频内容 + 通识自由发挥）
        val context = results.joinToString("\n") { item ->
            buildString {
                append("[").append(formatStamp(item.start)).append("]")
                val sp = item.speech.trim()
                val sc = item.screenText.trim()
                if (sp.isNotEmpty()) append(" 语音：").append(sp)
                if (sc.isNotEmpty()) append(" ｜画面：").append(sc)
                if (sp.isEmpty() && sc.isEmpty()) append("（无内容）")
            }
        }

        // 专家模式：联网检索（若已接）
        val ws = webSearch
        val web = if (expert && ws != null) {
            runCatching { ws.search(question) }.getOrDefault(emptyList())
        } else emptyList()

        val skill = extraContext().trim()

        val system = if (expert)
            "你是 Mentara——本应用内置的 AI 学习助手，回答中自称「Mentara」。" +
                "像传统对话 AI 一样自由、自然地回答，不要轻易拒答；同时你拥有当前视频的完整内容作为额外知识，" +
                "优先结合视频内容并标注 [mm:ss]，视频没有的部分就用你自己的知识自由展开。能深入就深入，不要惜字。"
        else
            "你是 Mentara——本应用内置的 AI 学习助手，回答中自称「Mentara」。" +
                "像传统对话 AI 一样自由地回答；涉及视频内容时以视频为准并标注 [mm:ss]，不编造视频里没有的内容；" +
                "视频未涉及的部分可以用你的通识回答，不要拒答。"

        val prompt = buildString {
            append("【当前视频内容】\n")
            if (results.isNotEmpty()) append(context).append("\n\n")
            else append("（本视频未直接涉及该问题，可作为背景参考）\n\n")
            if (skill.isNotBlank()) {
                append("【Mentara 永久知识库（该视频完整档案，未删减）】\n")
                append(skill.take(16000)).append("\n\n")
            }
            if (web.isNotEmpty()) {
                append("【联网检索结果】\n")
                web.forEachIndexed { i, w ->
                    append("(").append(i + 1).append(") ").append(w.title).append(" — ").append(w.url).append("\n")
                    append(w.content.take(500)).append("\n")
                }
                append("\n")
            }
            append("问题：").append(question).append("\n\n")
            append(
                if (expert) "要求：用中文，先给核心结论，再多角度展开分析，尽量详实、深入，不要缩减内容；涉及视频内容处标注 [mm:ss]。"
                else "要求：用中文回答；引用视频内容处标注 [mm:ss]。"
            )
        }

        val cfg = LlmConfig(
            provider = "nvidia", apiKey = "", baseUrl = "", model = "",
            system = system,
            temperature = if (expert) 0.7 else 0.2,
            maxTokens = 4096
        )

        val answer = if (llmClient is StreamingLlmClient) {
            llmClient.streamResponse(prompt, cfg, onToken, onReasoning)
        } else {
            llmClient.generateResponse(prompt, cfg)
        }.ifBlank { "（模型未返回内容：请检查网络 / API Key，或稍后重试）" }

        return QaAnswer(answer = answer, citations = results.map { formatStamp(it.start) })
    }

    private fun formatStamp(seconds: Double): String {
        val total = seconds.toInt()
        return two(total / 60) + ":" + two(total % 60)
    }

    private fun two(value: Int): String = if (value < 10) "0$value" else value.toString()
}