package com.learnagent

import com.learnagent.core.AgentLoop
import com.learnagent.core.CommandWhitelist
import com.learnagent.core.ContextAnalyzer
import com.learnagent.core.ContextManager
import com.learnagent.core.DebateEngine
import com.learnagent.core.Evaluator
import com.learnagent.core.ExperienceStore
import com.learnagent.core.Indexer
import com.learnagent.core.LearningUnitQaEngine
import com.learnagent.core.LlmClient
import com.learnagent.core.LlmConfig
import com.learnagent.core.LocalQaEngine
import com.learnagent.core.ModelRouter
import com.learnagent.core.TaskType
import com.learnagent.core.ToolRegistry
import com.learnagent.core.merge
import com.learnagent.data.Experience
import com.learnagent.data.FrameOcr
import com.learnagent.data.LearningUnit
import com.learnagent.data.SemanticType
import com.learnagent.data.TimelineItem
import com.learnagent.data.TranscriptSegment
import com.learnagent.platform.currentTimeMillis
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StageATest {

    private fun timeline(): List<TimelineItem> = merge(
        listOf(
            TranscriptSegment(0.0, 10.0, "这节课我们讲 gradient descent 梯度下降", null),
            TranscriptSegment(10.0, 20.0, "它是优化算法 optimization 基础方法", null),
            TranscriptSegment(20.0, 30.0, "接下来讲 backpropagation 反向传播", null),
            TranscriptSegment(30.0, 40.0, "最后讲 CNN 图像识别", null)
        ),
        listOf(
            FrameOcr(2.0, "gradient descent 梯度下降", "f1.jpg"),
            FrameOcr(12.0, "optimization 学习率", "f2.jpg"),
            FrameOcr(22.0, "backpropagation 反向传播", "f3.jpg"),
            FrameOcr(32.0, "CNN 图像识别", "f4.jpg")
        )
    )

    private fun units(): List<LearningUnit> = timeline().mapIndexed { i, t ->
        LearningUnit(
            id = "v_$i", videoId = "v", start = t.start, end = t.end,
            speech = t.speech, screenText = t.screenText,
            semanticType = if (i == 0) SemanticType.DEFINITION else SemanticType.UNKNOWN,
            importance = 5 - i,
            oneLineSummary = t.speech.take(30)
        )
    }

    @Test
    fun testGenericQuestionFallback() = runBlocking {
        val items = timeline()
        val indexer = Indexer(); indexer.index(items)
        val engine = LocalQaEngine(allItems = { items }, search = { q, k -> indexer.search(q, k) })
        val ans = engine.ask("这个视频讲了什么？")
        assertTrue(ans.answer.contains("没有直接匹配") || ans.answer.contains("相关信息"), "泛问应返回核心内容，实际: ${ans.answer}")
        assertTrue(ans.citations.isNotEmpty())
    }

    @Test
    fun testLearningUnitQaEngine() = runBlocking {
        val engine = LearningUnitQaEngine(units = { units() })
        val generic = engine.ask("这个视频讲了什么？")
        assertTrue(generic.citations.isNotEmpty(), "泛问应有引用")
        assertTrue(generic.answer.contains("核心内容"), "泛问应以核心内容开头")
        val specific = engine.ask("gradient")
        assertTrue(specific.answer.contains("gradient", ignoreCase = true) || specific.answer.contains("核心内容"))
    }

    @Test
    fun testIndexerSemanticAndImportance() {
        val idx = Indexer(); idx.indexUnits(units())
        assertEquals(1, idx.searchBySemanticType(SemanticType.DEFINITION).size)
        val top = idx.searchByImportance(4)
        assertTrue(top.isNotEmpty())
        assertTrue(top.first().importance >= 4)
        assertTrue(idx.searchUnits("gradient").isNotEmpty())
    }

    private class FakeLlm(private val reply: String) : LlmClient {
        override suspend fun generateResponse(prompt: String, config: LlmConfig): String = reply
    }

    @Test
    fun testDebateEngineDivergence() = runBlocking {
        val engine = DebateEngine(FakeLlm("结论：梯度下降有效。"), FakeLlm("但是，这个结论有问题，存在反例。"))
        val r = engine.debate("梯度下降是否有效", "上下文")
        assertTrue(r.conclusion.isNotBlank())
        assertTrue(r.divergent, "含否定词应判定分歧")
        assertEquals(0.5, r.confidence)
    }

    @Test
    fun testContextManagerCompression() {
        val cm = ContextManager(maxTurns = 20)
        val history = (1..30).map { "turn$it" }
        val compressed = cm.compress(history)
        assertTrue(compressed.size < history.size)
        assertTrue(compressed.first().contains("摘要"))
        assertTrue(compressed.last() == "turn30")
    }

    @Test
    fun testModelRouter() {
        assertEquals(TaskType.CODE, ModelRouter.route("这段代码怎么改"))
        assertEquals(TaskType.DEBATE, ModelRouter.route("对比 A 和 B"))
        assertEquals(TaskType.SIMPLE_QA, ModelRouter.route("这是什么"))
        assertTrue(ModelRouter.modelFor(TaskType.COMPLEX_REASONING).isNotBlank())
    }

    @Test
    fun testToolRegistryTenTools() {
        val registry = ToolRegistry(
            search = { _, _ -> emptyList() },
            ask = { com.learnagent.core.QaAnswer("ok", emptyList()) }
        )
        val tools = registry.tools()
        assertEquals(10, tools.size, "应有 10 个工具")
        val names = tools.map { it.spec.name }.toSet()
        assertTrue(names.containsAll(listOf("search_video", "ask_question", "record_experience", "search_experience", "read_file", "write_file", "list_dir", "run_command", "search_code", "http_get")))
    }

    @Test
    fun testCommandWhitelistAndSandbox() = runBlocking {
        assertTrue(CommandWhitelist.isAllowed("dir ."))
        assertTrue(!CommandWhitelist.isAllowed("del /f"))
        val registry = ToolRegistry(search = { _, _ -> emptyList() }, ask = { com.learnagent.core.QaAnswer("ok", emptyList()) })
        val run = registry.tools().first { it.spec.name == "run_command" }
        assertTrue(run.execute(mapOf("cmd" to "del x")).contains("拒绝"), "非白名单命令应拒绝")
        val write = registry.tools().first { it.spec.name == "write_file" }
        assertTrue(write.execute(mapOf("path" to "C:/evil.txt", "content" to "x")).contains("拒绝"), "绝对路径应拒绝")
    }

    @Test
    fun testExperienceStoreJvm() {
        val scenarioId = "test_" + currentTimeMillis()
        val id = "exp_test_" + currentTimeMillis()
        ExperienceStore.saveExperience(
            Experience(id = id, scenarioId = scenarioId, choice = "c", result = "r", lesson = "梯度下降要调学习率", recordedAt = currentTimeMillis())
        )
        assertTrue(ExperienceStore.getAllExperiences().any { it.id == id })
        assertTrue(ExperienceStore.searchExperiences("学习率").any { it.id == id })
    }

    @Test
    fun testContextAnalyzerFallbackNoLlm() = runBlocking {
        val analyzer = ContextAnalyzer(null)
        val result = analyzer.analyze(timeline(), "v")
        assertEquals(4, result.size)
        assertTrue(result.all { it.oneLineSummary.isNotBlank() })
    }

    @Test
    fun testEvaluator() = runBlocking {
        val items = timeline()
        val idx = Indexer(); idx.index(items)
        val engine = LocalQaEngine(allItems = { items }, search = { q, k -> idx.search(q, k) })
        val report = Evaluator(engine).run(
            listOf(com.learnagent.core.TestCase("gradient", "gradient"), com.learnagent.core.TestCase("no_such_token_xyz", "不存在"))
        )
        assertEquals(2, report.totalTests)
        assertEquals(1, report.passed)
        assertTrue(report.accuracy > 0.0)
    }

    @Test
    fun testAgentLoopFinish() = runBlocking {
        val llm = FakeLlm("""{"thought":"done","action":"FINISH","answer":"最终答案"}""")
        val loop = AgentLoop(llm, ToolRegistry(search = { _, _ -> emptyList() }, ask = { com.learnagent.core.QaAnswer("ok", emptyList()) }).tools())
        val result = loop.run("目标")
        assertTrue(result.finished)
        assertEquals("最终答案", result.output)
    }
}
