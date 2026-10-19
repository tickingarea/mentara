package com.learnagent.core

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable

/**
 * 辩论默认模型 ID（V3：NVIDIA NIM 三模型）。
 * 设置里的 debateModelA/B/C 为空时回退到这里。
 * 端点由 NvidiaLlmClient / AndroidNimClient 固定为 https://integrate.api.nvidia.com/v1。
 */
object DebateModels {
    const val DEFAULT_A = "moonshotai/kimi-k3"
    const val DEFAULT_B = "deepseek-ai/deepseek-v4.1-flash"
    const val DEFAULT_C = "z-ai/glm-5.3-flash"

    /** 快速兜底模型（当某一席超时/失败时接替，保证辩论不整体失败）。 */
    const val FAST_FALLBACK = "z-ai/glm-5.3-flash"

    fun resolve(configured: String, fallback: String): String =
        configured.trim().ifBlank { fallback }
}

/** 三席位的角色化 system prompt（拼接到用户 prompt 前）。 */
object DebateRoles {
    const val IDENTITY = "你是 Mentara（本应用内置 AI）的辩论席位，回答中自称「Mentara」。"
    const val A = IDENTITY + "侧重长上下文推理和 agentic 思维，关注长远视角、可扩展性、演化路径。"
    const val B = IDENTITY + "侧重工程严谨性，关注边界条件、失败模式、在什么条件下会失效。"
    const val C = IDENTITY + "侧重中文语境和实际落地，关注落地成本、团队接受度、真实工程中的使用方式。"

    fun prompt(role: String, text: String): String = "$role\n\n$text"
}

/**
 * 三模型辩论结果（四轮对抗协议）。
 *
 * 旧字段全部保留（StageATest 依赖 conclusion / divergent / confidence 的旧语义）；
 * 新字段全部带默认值：critiques（3 条交叉批判）、judge（中立裁判）、agreement（三方一致性 0~1）。
 */
@Serializable
data class DebateResult(
    // ---- 旧字段（一个不删） ----
    val conclusion: String,
    val critique: String,
    val response: String,
    val confidence: Double,
    val divergent: Boolean,
    val proposerA: String = "",
    val proposerB: String = "",
    val proposerC: String = "",
    val minorityReport: String = "",
    // ---- 四轮协议新字段 ----
    /** 3 条交叉批判，格式 "【B 批 A】…"（谁批谁在文本内注明）。 */
    val critiques: List<String> = emptyList(),
    /** 中立裁判全文（最终结论 + 各提案可取之处 + 各提案问题）。 */
    val judge: String = "",
    /** 三方一致性 0~1（提案两两 bigram 相似度均值）；-1 表示未计算。 */
    val agreement: Double = -1.0
)

/**
 * DebateEngine（四轮真对抗）：
 *  第 1 轮 提案：A / B / C 各自独立给出结论（**并行**）
 *  第 2 轮 交叉批判：B 批 A → C 批 B → A 批 C（**并行**），产出 3 条批判
 *  第 3 轮 中立裁决：裁判综合 3 提案 + 3 批判（**与回应并行**）
 *  第 4 步 后处理：agreement = 提案两两相似度均值（连续 0~1）；最低者自动成为少数派
 *
 * 容错：任一模型超时/失败 → **自动回退到 [fallback]**（若提供），仍失败才显示占位文本；
 *       绝不整体抛出；协程取消原样传播（支持"取消辩论"）。
 */
class DebateEngine(
    private val llmA: LlmClient,
    private val llmB: LlmClient,
    private val llmC: LlmClient? = null,
    private val fallback: LlmClient? = null
) {

    suspend fun debate(
        topic: String,
        context: String,
        onPhase: (Int) -> Unit = {},
        onRound: (Int, DebateResult) -> Unit = { _, _ -> }
    ): DebateResult {
        val cfg = LlmConfig("nvidia", "", "", "")
        val judgeLlm = llmC ?: llmB

        // 单席调用：先主模型，超时/失败自动回退快模型；都失败才给占位文本。
        suspend fun slot(client: LlmClient, who: String, prompt: String): String {
            suspend fun run(c: LlmClient): String? = try {
                withTimeoutOrNull(LLM_CALL_TIMEOUT_MS) { c.generateResponse(prompt, cfg) }
            } catch (e: Exception) {
                if (e::class.simpleName == "CancellationException") throw e
                null
            }
            val first = run(client)
            if (!first.isNullOrBlank()) return first
            if (fallback != null) {
                val fb = run(fallback)
                if (!fb.isNullOrBlank()) return fb
            }
            return "（$who 不可用/超时，已跳过）"
        }

        // ---- 第 1 轮：提案（并行） ----
        onPhase(1)
        val (conclusion, proposerB, proposerC) = coroutineScope {
            val pa = async { slot(llmA, "模型 A", DebateRoles.prompt(DebateRoles.A, "给出结论（只基于给定内容）：$topic\n$context")) }
            val pb = async { slot(llmB, "模型 B", DebateRoles.prompt(DebateRoles.B, "给出你的独立结论（只基于给定内容）：$topic\n$context")) }
            val pc = async {
                if (llmC != null) {
                    slot(llmC, "模型 C", DebateRoles.prompt(DebateRoles.C, "给出你的独立结论（只基于给定内容）：$topic\n$context"))
                } else {
                    slot(llmB, "模型 C", DebateRoles.prompt(DebateRoles.C, "换一个完全不同的角度给出结论（只基于给定内容）：$topic\n$context"))
                }
            }
            Triple(pa.await(), pb.await(), pc.await())
        }
        val round1 = DebateResult(
            conclusion = conclusion, critique = "", response = "",
            confidence = 0.0, divergent = false,
            proposerA = conclusion, proposerB = proposerB, proposerC = proposerC
        )
        onRound(1, round1)

        // ---- 第 2 轮：交叉批判（并行） ----
        onPhase(2)
        val (critBA, critCB, critAC) = coroutineScope {
            val cba = async { slot(llmB, "模型 B", DebateRoles.prompt(DebateRoles.B, "批判以下结论，指出问题或反例：\n$conclusion")) }
            val ccb = async {
                if (llmC != null) {
                    slot(llmC, "模型 C", DebateRoles.prompt(DebateRoles.C, "批判以下结论，指出问题或反例：\n$proposerB"))
                } else {
                    slot(llmB, "模型 C", DebateRoles.prompt(DebateRoles.C, "换一个角度批判以下结论，指出问题或反例：\n$proposerB"))
                }
            }
            val cac = async { slot(llmA, "模型 A", DebateRoles.prompt(DebateRoles.A, "批判以下结论，指出问题或反例：\n$proposerC")) }
            Triple(cba.await(), ccb.await(), cac.await())
        }
        val critiques = listOf(
            "【B 批 A】$critBA",
            "【C 批 B】$critCB",
            "【A 批 C】$critAC"
        )
        val round2 = round1.copy(critique = critBA, critiques = critiques)
        onRound(2, round2)

        // ---- 第 3 轮：中立裁决（与回应并行） ----
        onPhase(3)
        val judgePrompt = buildString {
            append("你是 Mentara 的中立裁判，回答中自称「Mentara」。三份提案与三条批判如下：\n")
            append("提案A：").append(conclusion).append('\n')
            append("提案B：").append(proposerB).append('\n')
            append("提案C：").append(proposerC).append('\n')
            critiques.forEach { append(it).append('\n') }
            append("请输出：1) 最终结论 2) 各提案的可取之处 3) 各提案的问题")
        }
        val (judge, response) = coroutineScope {
            val j = async { slot(judgeLlm, "中立裁判", judgePrompt) }
            val r = async { slot(llmA, "模型 A", DebateRoles.prompt(DebateRoles.A, "回应以下批判：\n$critBA")) }
            j.await() to r.await()
        }
        val round3 = round2.copy(response = response, judge = judge)
        onRound(3, round3)

        // ---- 第 4 步：后处理（连续一致性 + 少数派） ----
        val allFailed = listOf(conclusion, proposerB, proposerC)
            .all { it.contains("不可用") || it.contains("超时") }
        val simAB = similarity(conclusion, proposerB)
        val simBC = similarity(proposerB, proposerC)
        val simAC = similarity(conclusion, proposerC)
        val agreement = if (allFailed) -1.0 else (simAB + simBC + simAC) / 3.0
        val avgA = (simAB + simAC) / 2.0
        val avgB = (simAB + simBC) / 2.0
        val avgC = (simAC + simBC) / 2.0
        val minority = when {
            avgA <= avgB && avgA <= avgC -> conclusion
            avgB <= avgC -> proposerB
            else -> proposerC
        }

        // 旧语义保持：divergent 仍由"B 批 A 的否定词/重合度"启发式决定（StageATest 依赖）
        val divergent = if (allFailed) false else detectDivergence(conclusion, critBA)
        val confidence = if (allFailed) 0.0 else if (divergent) 0.5 else 0.8

        return round3.copy(
            divergent = divergent,
            confidence = confidence,
            minorityReport = minority,
            agreement = agreement
        )
    }

    /**
     * 字符 bigram 集合相似度（对中英文都友好）：
     * 交集大小 / 较小集合大小，0~1；空文本返回 0。
     */
    private fun similarity(a: String, b: String): Double {
        fun bigrams(s: String): Set<String> {
            val t = s.filter { !it.isWhitespace() }
            if (t.isEmpty()) return emptySet()
            if (t.length == 1) return setOf(t)
            return (0 until t.length - 1).map { t.substring(it, it + 2) }.toSet()
        }
        val ga = bigrams(a)
        val gb = bigrams(b)
        if (ga.isEmpty() || gb.isEmpty()) return 0.0
        return ga.intersect(gb).size.toDouble() / minOf(ga.size, gb.size)
    }

    /** 简单启发式：批判中出现否定词或与结论关键词重叠很低 → 视为分歧（旧语义保持）。 */
    private fun detectDivergence(conclusion: String, critique: String): Boolean {
        val negations = listOf("不", "错", "有误", "问题", "反驳", "然而", "但是", "不成立")
        if (negations.any { critique.contains(it) }) return true
        val a = conclusion.lowercase().replace(Regex("\\W"), "").toSet()
        val b = critique.lowercase().replace(Regex("\\W"), "").toSet()
        if (a.isEmpty() || b.isEmpty()) return false
        val overlap = a.intersect(b).size.toDouble() / minOf(a.size, b.size)
        return overlap < 0.2
    }

    companion object {
        /** 单模型单次调用超时（V5）：45 秒；手机网络到 NIM 可能偏慢，超时自动回退快模型，仍失败才跳过。 */
        const val LLM_CALL_TIMEOUT_MS = 45_000L
    }
}
