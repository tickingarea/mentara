package com.learnagent.core

import com.learnagent.data.CodeSnippet
import com.learnagent.data.Decision
import com.learnagent.data.TechCard
import com.learnagent.platform.TechStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

@Serializable
data class Alternative(val name: String, val applicableWhen: String)

/** 问题导向的解决方案（不是"知识"，是"可执行的方案"）。 */
@Serializable
data class ProblemSolution(
    val problem: String,
    val primary: String,
    val alternatives: List<Alternative> = emptyList(),
    val codeSnippet: String? = null,
    val pitfalls: List<String> = emptyList(),
    val whenNotToUse: String? = null,
    val relatedDecisions: List<Decision> = emptyList(),
    val source: String
)

/**
 * 问题导向搜索：
 * 1. ProblemParser 解析
 * 2. 本地检索 TechCard / Decision / CodeSnippet
 * 3. 命中 ≥1 → 直接返回（source=本地）
 * 4. 不够 → LLM 补全（source=LLM）
 * 5. LLM 不可用/失败 → 返回本地结果并标注"本地启发式"
 */
class ProblemOrientedSearch(
    private val llm: LlmClient?,
    private val techStore: TechStore
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun search(problem: String): ProblemSolution {
        val parsed = ProblemParser(llm).parse(problem)
        val cards = techStore.getAllTechCards().filter { cardMatches(it, parsed) }
        val decisions = techStore.getAllDecisions().filter { textMatches(it.problem, parsed) }
        val snippets = techStore.getAllCodeSnippets().filter { snippetMatches(it, parsed) }

        val enough = cards.isNotEmpty() || decisions.isNotEmpty() || snippets.isNotEmpty()
        if (enough) return localSolution(problem, cards, decisions, snippets, "本地")
        if (llm != null) {
            try {
                return llmSolution(problem, parsed, decisions)
            } catch (e: Exception) {
                // 降级
            }
        }
        return localSolution(problem, cards, decisions, snippets, "本地启发式")
    }

    private fun localSolution(
        problem: String,
        cards: List<TechCard>,
        decisions: List<Decision>,
        snippets: List<CodeSnippet>,
        source: String
    ): ProblemSolution {
        val topCard = cards.firstOrNull()
        val topDecision = decisions.firstOrNull()
        val topSnippet = snippets.firstOrNull()
        val primary = when {
            topCard != null -> "首选：${topCard.name} —— ${topCard.whatItSolves.ifBlank { topCard.whatItIs }}" +
                if (topCard.whenToUse.isNotBlank()) "（适用：${topCard.whenToUse}）" else ""
            topDecision != null -> "参考你之前的决策：${topDecision.chosen}（理由：${topDecision.reason}）" +
                if (topDecision.result.isNotBlank()) "｜结果：${topDecision.result}" else ""
            topSnippet != null -> "可直接复用代码片段：${topSnippet.purpose.ifBlank { topSnippet.language }}"
            else -> "本地库暂无匹配。建议先用关键词「${ProblemParser(null).parseWithDictionary(problem).keywords.take(3).joinToString(" ")}」在视频中检索；配置 LLM 后可生成完整方案。"
        }
        val alternatives = cards.drop(1).take(3).map { Alternative(it.name, it.whenToUse.ifBlank { it.whatItSolves }) }
        return ProblemSolution(
            problem = problem,
            primary = primary,
            alternatives = alternatives,
            codeSnippet = topCard?.codeSnippet?.takeIf { it.isNotBlank() } ?: topSnippet?.code,
            pitfalls = cards.map { it.whenNotToUse }.filter { it.isNotBlank() }.take(3),
            whenNotToUse = topCard?.whenNotToUse?.takeIf { it.isNotBlank() },
            relatedDecisions = decisions.take(5),
            source = source
        )
    }

    private suspend fun llmSolution(
        problem: String,
        parsed: ParsedProblem,
        decisions: List<Decision>
    ): ProblemSolution {
        val client = llm ?: throw IllegalStateException("no llm")
        val prompt = """
你是资深工程师。针对问题给出可执行方案，只输出 JSON：
{"primary":"首选方案+理由","alternatives":[{"name":"备选","applicableWhen":"何时用"}],
 "codeSnippet":"可复制代码或空串","pitfalls":["坑1","坑2"],"whenNotToUse":"什么时候不该用"}

问题：$problem
关键词：${parsed.keywords.joinToString()}
领域：${parsed.domain}
约束：${parsed.constraints.joinToString()}
""".trimIndent()
        val raw = client.generateResponse(prompt, LlmConfig("nvidia", "", "", ""))
        val s = raw.indexOf('{')
        val e = raw.lastIndexOf('}')
        if (s < 0 || e <= s) throw IllegalArgumentException("无 JSON")
        val o = json.parseToJsonElement(raw.substring(s, e + 1)).jsonObject
        fun str(key: String) = (o[key] as? JsonPrimitive)?.contentOrNull.orEmpty()
        val alts = (o["alternatives"] as? JsonArray)?.mapNotNull { el ->
            val obj = el as? kotlinx.serialization.json.JsonObject ?: return@mapNotNull null
            val name = (obj["name"] as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
            Alternative(name, (obj["applicableWhen"] as? JsonPrimitive)?.contentOrNull.orEmpty())
        } ?: emptyList()
        val pitfalls = (o["pitfalls"] as? JsonArray)
            ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull } ?: emptyList()
        return ProblemSolution(
            problem = problem,
            primary = str("primary"),
            alternatives = alts,
            codeSnippet = str("codeSnippet").takeIf { it.isNotBlank() },
            pitfalls = pitfalls,
            whenNotToUse = str("whenNotToUse").takeIf { it.isNotBlank() },
            relatedDecisions = decisions.take(5),
            source = "LLM"
        )
    }

    private fun cardMatches(card: TechCard, parsed: ParsedProblem): Boolean {
        val text = listOf(card.name, card.whatItIs, card.whatItSolves, card.relatedConcepts.joinToString(" ")).joinToString(" ")
        return textMatches(text, parsed)
    }

    private fun snippetMatches(snippet: CodeSnippet, parsed: ParsedProblem): Boolean {
        val text = listOf(snippet.purpose, snippet.language, snippet.tags.joinToString(" "), snippet.code).joinToString(" ")
        return textMatches(text, parsed)
    }

    private fun textMatches(text: String, parsed: ParsedProblem): Boolean {
        if (text.isBlank()) return false
        val terms = parsed.keywords.filter { it.isNotBlank() }
        if (terms.isEmpty()) return false
        return terms.any { text.contains(it, ignoreCase = true) }
    }
}
