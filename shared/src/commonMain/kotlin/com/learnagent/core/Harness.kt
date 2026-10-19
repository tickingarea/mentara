package com.learnagent.core

import com.learnagent.data.Experience
import com.learnagent.data.TimelineItem
import com.learnagent.platform.currentTimeMillis
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// ---------------- Harness 内核（阶段 A） ----------------

data class ToolSpec(
    val name: String,
    val description: String,
    val parameters: Map<String, String>
)

data class Tool(
    val spec: ToolSpec,
    val execute: suspend (Map<String, String>) -> String
)

/** 平台执行器：文件 / 命令 / 网络，由各端注入。 */
interface ToolRunner {
    suspend fun readFile(path: String): String
    suspend fun writeFile(path: String, content: String): String
    suspend fun listDir(path: String): String
    suspend fun runCommand(command: String): String
    suspend fun httpGet(url: String): String
}

object NoopToolRunner : ToolRunner {
    override suspend fun readFile(path: String) = "（当前平台未启用文件工具）"
    override suspend fun writeFile(path: String, content: String) = "（当前平台未启用文件工具）"
    override suspend fun listDir(path: String) = "（当前平台未启用文件工具）"
    override suspend fun runCommand(command: String) = "（当前平台未启用命令工具）"
    override suspend fun httpGet(url: String) = "（当前平台未启用网络工具）"
}

/** run_command 白名单。 */
object CommandWhitelist {
    private val allowed = listOf("dir", "type", "findstr", "echo")
    fun isAllowed(cmd: String): Boolean {
        val head = cmd.trim().lowercase().split(Regex("\\s+")).firstOrNull().orEmpty()
        return allowed.contains(head)
    }
    fun allowedNames(): List<String> = allowed
}

/** 10 个工具。 */
class ToolRegistry(
    private val search: suspend (String, Int) -> List<TimelineItem>,
    private val ask: suspend (String) -> QaAnswer,
    private val runner: ToolRunner = NoopToolRunner,
    private val httpEnabled: Boolean = false,
    private val projectRoot: String = "."
) {

    fun tools(): List<Tool> = listOf(
        Tool(ToolSpec("search_video", "按关键词检索视频时间线", mapOf("query" to "string"))) { a ->
            val q = a["query"].orEmpty()
            val hits = search(q, 5)
            if (hits.isEmpty()) "未找到相关片段"
            else hits.joinToString("\n") { "[${it.start.toInt()}] ${it.speech.ifBlank { it.screenText }}" }
        },
        Tool(ToolSpec("ask_question", "基于视频内容回答问题", mapOf("question" to "string"))) { a ->
            ask(a["question"].orEmpty()).answer
        },
        Tool(
            ToolSpec(
                "record_experience",
                "记录一次场景经验",
                mapOf("scenarioId" to "string", "choice" to "string", "result" to "string", "lesson" to "string")
            )
        ) { a ->
            val id = "exp_" + currentTimeMillis().toString(16)
            ExperienceStore.saveExperience(
                Experience(
                    id = id,
                    scenarioId = a["scenarioId"].orEmpty(),
                    choice = a["choice"].orEmpty(),
                    result = a["result"].orEmpty(),
                    lesson = a["lesson"].orEmpty(),
                    recordedAt = currentTimeMillis()
                )
            )
            "已记录经验 $id"
        },
        Tool(ToolSpec("search_experience", "检索经验库", mapOf("query" to "string"))) { a ->
            val list = ExperienceStore.searchExperiences(a["query"].orEmpty())
            if (list.isEmpty()) "没有匹配的经验"
            else list.joinToString("\n") { "- ${it.lesson.ifBlank { it.choice }}" }
        },
        Tool(ToolSpec("read_file", "读取文本文件", mapOf("path" to "string"))) { a ->
            runner.readFile(a["path"].orEmpty())
        },
        Tool(ToolSpec("write_file", "写入文本文件（限项目目录内）", mapOf("path" to "string", "content" to "string"))) { a ->
            val p = a["path"].orEmpty()
            if (!isInsideProject(p)) "拒绝：路径必须在项目目录内" else runner.writeFile(p, a["content"].orEmpty())
        },
        Tool(ToolSpec("list_dir", "列出目录内容", mapOf("path" to "string"))) { a ->
            runner.listDir(a["path"].orEmpty())
        },
        Tool(ToolSpec("run_command", "执行白名单命令（dir/type/findstr/echo）", mapOf("cmd" to "string"))) { a ->
            val cmd = a["cmd"].orEmpty()
            if (!CommandWhitelist.isAllowed(cmd)) "拒绝：命令不在白名单（${CommandWhitelist.allowedNames().joinToString("/")}）"
            else runner.runCommand(cmd)
        },
        Tool(ToolSpec("search_code", "在项目中搜索代码", mapOf("pattern" to "string"))) { a ->
            val pattern = a["pattern"].orEmpty()
            runner.runCommand("findstr /s /i /m \"$pattern\" *.*")
        },
        Tool(ToolSpec("http_get", "HTTP GET（可关闭）", mapOf("url" to "string"))) { a ->
            if (!httpEnabled) "http_get 已禁用" else runner.httpGet(a["url"].orEmpty())
        }
    )

    /** 仅允许项目目录内、非绝对路径、无 .. 的路径。 */
    private fun isInsideProject(path: String): Boolean {
        if (path.isBlank()) return false
        if (path.contains("..")) return false
        if (path.startsWith("/") || path.startsWith("\\")) return false
        if (Regex("^[A-Za-z]:").containsMatchIn(path)) return false
        return true
    }
}

data class AgentStep(val thought: String, val action: String, val observation: String)
data class AgentResult(val steps: List<AgentStep>, val finished: Boolean, val output: String)

/**
 * ReAct 循环：思考 → 决策 → 执行 → 观察 → 循环（<= maxSteps）。
 * 停止：action=FINISH / 连续 2 次相同观察 / 超 maxSteps。
 * 超时由调用方用 withTimeout 包裹（commonMain 不引入协程）。
 */
class AgentLoop(
    private val llm: LlmClient,
    private val tools: List<Tool>,
    private val maxSteps: Int = 10
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun run(goal: String): AgentResult {
        val steps = mutableListOf<AgentStep>()
        var lastObservation = ""
        var repeatCount = 0
        val toolList = tools.joinToString("\n") { "- ${it.spec.name}: ${it.spec.description}" }

        for (i in 0 until maxSteps) {
            val history = steps.joinToString("\n") { "思考:${it.thought} 动作:${it.action} 观察:${it.observation}" }
            val prompt = buildString {
                append("你是 Mentara Agent。目标：").append(goal).append("\n")
                append("可用工具：\n").append(toolList).append("\n")
                append("历史：\n").append(history).append("\n")
                append("只输出 JSON：{\"thought\":\"...\",\"action\":\"工具名或FINISH\",\"actionInput\":{...},\"answer\":\"action=FINISH 时的最终答案\"}")
            }
            val raw = try {
                llm.generateResponse(prompt, LlmConfig("nvidia", "", "", ""))
            } catch (e: Exception) {
                return AgentResult(steps, false, "LLM 失败：${e.message}")
            }

            val obj = runCatching {
                val s = raw.indexOf('{')
                val e = raw.lastIndexOf('}')
                json.parseToJsonElement(raw.substring(s, e + 1)).jsonObject
            }.getOrNull() ?: return AgentResult(steps, false, raw)

            val thought = obj["thought"]?.jsonPrimitive?.content ?: ""
            val action = obj["action"]?.jsonPrimitive?.content ?: "FINISH"
            if (action == "FINISH") {
                return AgentResult(steps, true, obj["answer"]?.jsonPrimitive?.content ?: raw)
            }

            val args = obj["actionInput"]?.jsonObject?.mapValues { it.value.jsonPrimitive.content } ?: emptyMap()
            val tool = tools.firstOrNull { it.spec.name == action }
            val observation = tool?.let {
                runCatching { it.execute(args) }.getOrElse { e -> "工具错误：${e.message}" }
            } ?: "未知工具：$action"

            steps.add(AgentStep(thought, action, observation))
            if (observation == lastObservation) repeatCount++ else repeatCount = 0
            lastObservation = observation
            if (repeatCount >= 1) break
        }
        return AgentResult(steps, false, steps.lastOrNull()?.observation ?: "")
    }
}

/** 上下文管理：> maxTurns 轮时把旧内容压缩成摘要。 */
class ContextManager(
    private val maxTurns: Int = 20,
    private val maxChars: Int = 8000
) {
    fun compress(history: List<String>): List<String> {
        if (history.size <= maxTurns) return history
        val keepCount = maxTurns / 2
        val keep = history.takeLast(keepCount)
        val old = history.dropLast(keepCount)
        val summary = "（较早对话摘要）" + old.joinToString("；") { it.take(40) }
        return listOf(summary.take(maxChars)) + keep
    }
}

enum class TaskType { SIMPLE_QA, COMPLEX_REASONING, CODE, DEBATE, TOOL_CALL }

/** 多模型路由。 */
object ModelRouter {
    fun modelFor(type: TaskType): String = when (type) {
        TaskType.SIMPLE_QA -> "qwen/qwen3-8b"
        TaskType.COMPLEX_REASONING -> "deepseek-ai/deepseek-v4.1-flash"
        TaskType.CODE -> "deepseek-ai/deepseek-coder-6.7b-instruct"
        TaskType.DEBATE -> "meta/llama-3.1-8b-instruct"
        TaskType.TOOL_CALL -> "deepseek-ai/deepseek-v4.1-flash"
    }

    fun route(question: String): TaskType {
        val q = question.lowercase()
        return when {
            q.contains("代码") || q.contains("code") || q.contains("函数") -> TaskType.CODE
            q.contains("辩论") || q.contains("对比") || q.contains("debate") -> TaskType.DEBATE
            q.contains("工具") || q.contains("tool") || q.contains("执行") -> TaskType.TOOL_CALL
            q.length > 40 || q.contains("为什么") || q.contains("推导") -> TaskType.COMPLEX_REASONING
            else -> TaskType.SIMPLE_QA
        }
    }
}

data class TestCase(val question: String, val expectedAnswer: String, val expectedTimeStamp: String? = null)
data class EvalReport(
    val totalTests: Int,
    val passed: Int,
    val failed: Int,
    val accuracy: Double,
    val failures: List<String>
)

/** 自动评估：跑黄金集，输出报告。 */
class Evaluator(private val engine: QaEngine) {
    suspend fun run(cases: List<TestCase>): EvalReport {
        var passed = 0
        val failures = mutableListOf<String>()
        for (case in cases) {
            val answer = try {
                engine.ask(case.question).answer
            } catch (e: Exception) {
                ""
            }
            if (answer.contains(case.expectedAnswer)) passed++
            else failures.add("Q='${case.question}' 期望包含 '${case.expectedAnswer}'，实际 '${answer.take(60)}'")
        }
        val accuracy = if (cases.isEmpty()) 0.0 else passed.toDouble() / cases.size
        return EvalReport(cases.size, passed, cases.size - passed, accuracy, failures)
    }
}
