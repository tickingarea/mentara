package com.learnagent.mcp

import com.learnagent.core.Indexer
import com.learnagent.core.LocalQaEngine
import com.learnagent.core.ExperienceStore
import com.learnagent.core.ProblemOrientedSearch
import com.learnagent.core.ProblemParser
import com.learnagent.core.TechDecisionService
import com.learnagent.core.merge
import com.learnagent.data.CodeSnippet
import com.learnagent.data.Decision
import com.learnagent.data.Experience
import com.learnagent.data.FrameOcr
import com.learnagent.data.TimelineItem
import com.learnagent.data.TranscriptSegment
import com.learnagent.platform.TechStore
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintStream

/**
 * 最小 MCP server（stdio，JSON-RPC 2.0，无第三方依赖）。
 *
 * 暴露两个工具：
 *  - search_video(query) -> 相关时间线片段
 *  - ask_question(question) -> 本地问答答案 + 时间戳引用
 *
 * 数据源为内置示例时间线（后续可替换为真实 results 目录）。
 */
private val json = Json { ignoreUnknownKeys = true; isLenient = true }
private val indexer = Indexer()
private val techStore = TechStore()

private fun sampleTimeline(): List<TimelineItem> = merge(
    listOf(
        TranscriptSegment(0.0, 10.0, "这节课我们讲梯度下降", null),
        TranscriptSegment(10.0, 20.0, "它是优化算法里的基础方法", null),
        TranscriptSegment(20.0, 30.0, "接下来讲反向传播", null),
        TranscriptSegment(30.0, 40.0, "最后讲卷积神经网络处理图像", null)
    ),
    listOf(
        FrameOcr(2.0, "梯度下降 = 沿负梯度更新参数", "frame_001.jpg"),
        FrameOcr(12.0, "学习率控制步长", "frame_002.jpg"),
        FrameOcr(22.0, "反向传播计算梯度", "frame_003.jpg"),
        FrameOcr(32.0, "CNN 用于图像识别", "frame_004.jpg")
    )
)

private fun formatStamp(seconds: Double): String {
    val t = seconds.toInt()
    fun two(v: Int) = if (v < 10) "0$v" else "$v"
    return two(t / 60) + ":" + two(t % 60)
}

private fun formatItem(item: TimelineItem): String {
    val parts = listOf(item.speech, item.screenText).filter { it.isNotBlank() }
    return "[${formatStamp(item.start)}-${formatStamp(item.end)}] " + parts.joinToString(" | ")
}

private fun resultText(id: kotlinx.serialization.json.JsonElement?, text: String): String {
    val obj = buildJsonObject {
        put("jsonrpc", "2.0")
        if (id != null) put("id", id)
        putJsonObject("result") {
            put("content", buildJsonArray {
                add(buildJsonObject {
                    put("type", "text")
                    put("text", text)
                })
            })
        }
    }
    return obj.toString()
}

private fun errorResponse(id: kotlinx.serialization.json.JsonElement?, code: Int, message: String): String =
    buildJsonObject {
        put("jsonrpc", "2.0")
        if (id != null) put("id", id)
        putJsonObject("error") {
            put("code", code)
            put("message", message)
        }
    }.toString()

private fun rawResult(id: kotlinx.serialization.json.JsonElement?, resultJson: String): String =
    buildJsonObject {
        put("jsonrpc", "2.0")
        if (id != null) put("id", id)
        put("result", json.parseToJsonElement(resultJson))
    }.toString()

fun main() {
    indexer.index(sampleTimeline())
    val qaEngine = LocalQaEngine { q, k -> indexer.search(q, k) }
    val out = PrintStream(System.out, true, "UTF-8")
    val reader: BufferedReader = BufferedReader(InputStreamReader(System.`in`, Charsets.UTF_8))

    while (true) {
        val line = reader.readLine() ?: break
        if (line.isBlank()) continue
        val msg = try {
            json.parseToJsonElement(line).jsonObject
        } catch (e: Exception) {
            continue
        }
        val id = msg["id"]
        val method = msg["method"]?.jsonPrimitive?.contentOrNull

        when (method) {
            "initialize" -> out.println(rawResult(id, """{"protocolVersion":"2024-11-05","capabilities":{"tools":{}},"serverInfo":{"name":"mentara","version":"1.0.0"}}"""))
            "notifications/initialized" -> {}
            "tools/list" -> out.println(
                rawResult(
                    id,
                    """{"tools":[
{"name":"search_video","description":"按关键词检索视频时间线","inputSchema":{"type":"object","properties":{"query":{"type":"string"}},"required":["query"]}},
{"name":"ask_question","description":"基于视频内容回答问题并给出时间戳","inputSchema":{"type":"object","properties":{"question":{"type":"string"}},"required":["question"]}},
{"name":"record_experience","description":"记录一次场景经验","inputSchema":{"type":"object","properties":{"scenarioId":{"type":"string"},"choice":{"type":"string"},"result":{"type":"string"},"lesson":{"type":"string"}},"required":["lesson"]}},
{"name":"search_experience","description":"检索经验库","inputSchema":{"type":"object","properties":{"query":{"type":"string"}},"required":["query"]}},
{"name":"search_technology","description":"问题导向的技术方案检索（本地 TechCard/Decision/CodeSnippet，LLM 不可用时降级）","inputSchema":{"type":"object","properties":{"problem":{"type":"string"}},"required":["problem"]}},
{"name":"get_decision_history","description":"查询历史决策记录","inputSchema":{"type":"object","properties":{"problem":{"type":"string"},"topN":{"type":"integer"}},"required":["problem"]}},
{"name":"get_code_snippet","description":"按用途查询代码片段","inputSchema":{"type":"object","properties":{"purpose":{"type":"string"},"language":{"type":"string"}},"required":["purpose"]}},
{"name":"record_decision","description":"记录一个技术决策（同步更新使用痕迹）","inputSchema":{"type":"object","properties":{"problem":{"type":"string"},"chosen":{"type":"string"},"reason":{"type":"string"},"options":{"type":"string"},"constraints":{"type":"string"}},"required":["problem","chosen","reason"]}}
]}"""
                )
            )
            "tools/call" -> {
                val params = msg["params"]?.jsonObject
                val name = params?.get("name")?.jsonPrimitive?.contentOrNull
                val args = params?.get("arguments")?.jsonObject
                fun arg(k: String) = args?.get(k)?.jsonPrimitive?.contentOrNull.orEmpty()
                val answer = when (name) {
                    "search_video" -> {
                        val q = arg("query")
                        val hits = indexer.search(q, 5)
                        if (hits.isEmpty()) "未检索到与 '$q' 相关的片段" else hits.joinToString("\n") { formatItem(it) }
                    }
                    "ask_question" -> {
                        val q = arg("question")
                        runBlocking { qaEngine.ask(q) }.let {
                            it.answer + if (it.citations.isEmpty()) "" else "\n引用: " + it.citations.joinToString()
                        }
                    }
                    "record_experience" -> {
                        val id = "exp_" + System.currentTimeMillis().toString(16)
                        ExperienceStore.saveExperience(
                            Experience(
                                id = id,
                                scenarioId = arg("scenarioId"),
                                choice = arg("choice"),
                                result = arg("result"),
                                lesson = arg("lesson"),
                                recordedAt = System.currentTimeMillis()
                            )
                        )
                        "已记录经验 $id"
                    }
                    "search_experience" -> {
                        val list = ExperienceStore.searchExperiences(arg("query"))
                        if (list.isEmpty()) "没有匹配的经验"
                        else list.joinToString("\n") { "- " + it.lesson.ifBlank { it.choice } }
                    }
                    "search_technology" -> {
                        val p = arg("problem")
                        runBlocking { ProblemOrientedSearch(null, techStore).search(p) }
                            .let { json.encodeToString(com.learnagent.core.ProblemSolution.serializer(), it) }
                    }
                    "get_decision_history" -> {
                        val p = arg("problem")
                        val topN = args?.get("topN")?.jsonPrimitive?.contentOrNull?.toIntOrNull()?.coerceIn(1, 20) ?: 5
                        val keywords = ProblemParser(null).parseWithDictionary(p).keywords
                        val hits = techStore.getAllDecisions().filter { d ->
                            keywords.any { k -> d.problem.contains(k, true) || d.chosen.contains(k, true) || d.reason.contains(k, true) }
                        }.take(topN)
                        json.encodeToString(ListSerializer(Decision.serializer()), hits)
                    }
                    "get_code_snippet" -> {
                        val purpose = arg("purpose")
                        val language = arg("language")
                        val hits = techStore.getAllCodeSnippets().filter { s ->
                            (language.isBlank() || s.language.equals(language, true)) &&
                                (purpose.isBlank() || s.purpose.contains(purpose, true) ||
                                    s.code.contains(purpose, true) || s.tags.any { it.contains(purpose, true) })
                        }.take(10)
                        json.encodeToString(ListSerializer(CodeSnippet.serializer()), hits)
                    }
                    "record_decision" -> {
                        fun splitList(v: String) = v.split(',', '，', ';', '；').map { it.trim() }.filter { it.isNotBlank() }
                        val d = TechDecisionService.record(
                            techStore, arg("problem"), arg("chosen"), arg("reason"),
                            options = splitList(arg("options")), constraints = splitList(arg("constraints"))
                        )
                        "已记录决策 ${d.id}"
                    }
                    else -> "未知工具: $name"
                }
                out.println(resultText(id, answer))
            }
            "ping" -> out.println(rawResult(id, "\"pong\""))
            else -> if (id != null) out.println(errorResponse(id, -32601, "Method not found: $method"))
        }
    }
}
