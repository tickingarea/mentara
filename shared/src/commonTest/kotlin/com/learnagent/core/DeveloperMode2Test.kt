package com.learnagent.core

import com.learnagent.data.TechCard
import com.learnagent.data.TechCategory
import com.learnagent.data.TechStatus
import com.learnagent.platform.TechStore
import com.learnagent.platform.currentTimeMillis
import com.learnagent.ui.TechMapLayoutEngine
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Developer Mode 第 2 批：解析 / 逆向 / 搜索 / 召回 / 痕迹 / 周报 / 技术地图。 */
class DeveloperMode2Test {

    /** 每个测试实例固定一个 stamp（getter 会导致同一测试内名字漂移）。 */
    private val stamp = currentTimeMillis()

    private class FakeLlm(private val reply: String, private val error: Exception? = null) : LlmClient {
        override suspend fun generateResponse(prompt: String, config: LlmConfig): String {
            error?.let { throw it }
            return reply
        }
    }

    // ---------- 1. ProblemParser ----------

    @Test
    fun parserDictionaryHitsKeywordsAndConstraint() {
        val parsed = ProblemParser(null).parseWithDictionary("我要解决高并发下的分布式锁")
        assertTrue(parsed.keywords.contains("分布式锁"), "应命中词典：分布式锁")
        assertTrue(parsed.keywords.contains("高并发"), "约束词应进入 keywords（验收要求）")
        assertEquals("分布式", parsed.domain)
        assertTrue(parsed.constraints.contains("高并发"))
    }

    @Test
    fun parserOfflineReturnsAtLeastThreeKeywords() = runBlocking {
        val parsed = ProblemParser(null).parse("视频生成模型怎么训练")
        assertTrue(parsed.keywords.size >= 3, "离线降级也要 ≥3 个关键词，实际=${parsed.keywords}")
    }

    @Test
    fun parserInvalidJsonFallsBack() = runBlocking {
        val parsed = ProblemParser(FakeLlm("这不是 JSON")).parse("Redis 缓存击穿怎么办")
        assertTrue(parsed.keywords.contains("Redis"), "LLM 非法 JSON 应降级词典")
        assertTrue(parsed.keywords.isNotEmpty())
    }

    // ---------- 2. ReverseLearningEngine ----------

    @Test
    fun reverseLearningBuildsTreeAndPath() = runBlocking {
        val engine = ReverseLearningEngine(null, TechStore())
        val root = engine.analyze("视频生成")
        assertEquals("视频生成", root.name)
        assertTrue(root.children.isNotEmpty(), "本地词典应有前置")
        assertTrue(root.children.any { it.name == "扩散模型" })
        val text = engine.learningPathText(root)
        assertTrue(text.contains("你的目标：视频生成"))
        assertTrue(text.contains("建议从"))
    }

    // ---------- 3. ProblemOrientedSearch ----------

    @Test
    fun problemSearchLocalHitReturnsLocalSource() = runBlocking {
        val store = TechStore()
        val name = "并发锁测试_$stamp"
        store.saveTechCard(
            TechCard(
                id = "test_$stamp", name = name, category = TechCategory.CONCURRENCY,
                whatItIs = "测试用并发锁", whatItSolves = "并发锁场景",
                whenToUse = "高并发", whenNotToUse = "单线程", howItWorks = ""
            )
        )
        val solution = ProblemOrientedSearch(null, store).search("并发锁")
        assertEquals("本地", solution.source)
        assertTrue(solution.primary.contains("并发锁测试"), "本地命中应引用测试卡，实际=${solution.primary}")
    }

    @Test
    fun problemSearchNoLocalNoLlmFallsBack() = runBlocking {
        val solution = ProblemOrientedSearch(null, TechStore()).search("如何用腓尼基字母优化量子纠缠采样")
        assertEquals("本地启发式", solution.source)
    }

    // ---------- 4. ContextualRecall ----------

    @Test
    fun recallFindsMatchingDecision() {
        val store = TechStore()
        val marker = "召回决策_$stamp"
        TechDecisionService.record(
            store = store,
            problem = "使用 $marker 选择方案",
            chosen = "方案A",
            reason = "因为 $marker 更稳",
            now = currentTimeMillis()
        )
        val blocks = ContextualRecall(store).recallForSearch(marker)
        assertTrue(blocks.any { it.type == "decision" }, "应召回匹配的决策")
    }

    // ---------- 6. TechUsageTracker ----------

    @Test
    fun usageTrackerDedupeAndUpgrade() {
        val store = TechStore()
        val name = "痕迹测试_$stamp"
        var fakeNow = 1_000_000_000_000L
        val tracker = TechUsageTracker(store) { fakeNow }

        tracker.onTechCardOpened(name)
        tracker.onTechCardOpened(name)   // 1h 内重复 → 只算 1 次
        var usage = store.getAllTechUsage().first { it.techName == name }
        assertEquals(1, usage.seenCount)

        fakeNow += 2 * 3_600_000L
        tracker.onTechCardOpened(name)   // 超过 1h → 再算 1 次
        usage = store.getAllTechUsage().first { it.techName == name }
        assertEquals(2, usage.seenCount)
        assertEquals(TechStatus.DISCOVERED, usage.status)

        repeat(3) { tracker.onTechCardUsed(name) }
        usage = store.getAllTechUsage().first { it.techName == name }
        assertEquals(3, usage.usedCount)
        assertEquals(TechStatus.USED, usage.status)

        repeat(2) { tracker.onTechCardUsed(name) }
        usage = store.getAllTechUsage().first { it.techName == name }
        assertEquals(5, usage.usedCount)
        assertEquals(TechStatus.FLUENT, usage.status)

        // 单向升级：再 seen 不会降级
        fakeNow += 2 * 3_600_000L
        tracker.onTechCardOpened(name)
        usage = store.getAllTechUsage().first { it.techName == name }
        assertEquals(TechStatus.FLUENT, usage.status)
    }

    // ---------- 8. WeeklyReport ----------

    @Test
    fun weeklyReportWeekStartsSundayAndGenerates() {
        // 1970-01-01 是周四 → 所在周从 1969-12-28（周日）开始
        assertEquals(-4L * 86_400_000L, WeeklyReportGenerator.weekStart(0L))

        val store = TechStore()
        val name = "周报测试_$stamp"
        val now = currentTimeMillis()
        val tracker = TechUsageTracker(store) { now }
        tracker.onTechCardUsed(name)
        val report = WeeklyReportGenerator(store, now).generate()
        assertNotNull(report)
        assertTrue(report.weekEnd > report.weekStart)
        assertTrue(report.suggestion.isNotBlank())
    }

    // ---------- 7. TechMap ----------

    @Test
    fun techMapBuildsNodesAndEdges() {
        val a = TechCard(
            id = "a_$stamp", name = "MapA_$stamp", category = TechCategory.DISTRIBUTED,
            whatItIs = "a", whatItSolves = "", whenToUse = "", whenNotToUse = "", howItWorks = "",
            relatedConcepts = listOf("MapB_$stamp")
        )
        val b = TechCard(
            id = "b_$stamp", name = "MapB_$stamp", category = TechCategory.NETWORK,
            whatItIs = "b", whatItSolves = "", whenToUse = "", whenNotToUse = "", howItWorks = ""
        )
        val layout = TechMapLayoutEngine.build(listOf(a, b), emptyList(), 800f, 600f)
        assertEquals(2, layout.nodes.size)
        assertEquals(1, layout.edges.size, "relatedConcepts 命中应连线")
        assertTrue(layout.nodes.all { it.x in 0f..800f && it.y in 0f..600f })
        assertEquals(0xFF4A5260, TechMapLayoutEngine.statusColor(TechStatus.DISCOVERED))
        assertEquals(0xFFB8A8D4, TechMapLayoutEngine.statusColor(TechStatus.EXPERT))
        assertNotNull(TechMapLayoutEngine.hitTest(layout.nodes, layout.nodes[0].x, layout.nodes[0].y))
    }
}
