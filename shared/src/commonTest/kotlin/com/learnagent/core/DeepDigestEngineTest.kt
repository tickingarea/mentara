package com.learnagent.core

import com.learnagent.data.LearningUnit
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeepDigestEngineTest {

    private fun unit() = LearningUnit(
        id = "u1", videoId = "v1", start = 10.0, end = 20.0,
        speech = "梯度下降沿负梯度更新参数，学习率控制步长",
        screenText = "学习率 = 0.001"
    )

    private class Fake(private val behavior: (String) -> String) : LlmClient {
        override suspend fun generateResponse(prompt: String, config: LlmConfig): String = behavior(prompt)
    }

    private class Thrower : LlmClient {
        override suspend fun generateResponse(prompt: String, config: LlmConfig): String =
            throw IllegalStateException("boom")
    }

    private fun smartFake() = Fake { p ->
        when {
            p.contains("共同观点") ->
                "【共同观点】\n- 必学：梯度下降沿负梯度更新参数\n【分歧点】\n- 主题：学习率取值；A：固定 1e-3；B：自适应；C：经验值"
            p.contains("你的初稿") -> "融合稿：梯度下降 + 学习率控制步长"
            else -> "初稿：梯度下降要点、学习率 0.001"
        }
    }

    @Test
    fun digestProducesConsensusAndDivergence() = runBlocking {
        val engine = DeepDigestEngine(
            listOf(
                Triple("Kimi K3", smartFake(), null),
                Triple("DeepSeek V4.1", smartFake(), null),
                Triple("GLM 5.3", smartFake(), null)
            ),
            maxRounds = 1
        )
        val report = engine.digest("v1", listOf(unit()))
        assertEquals(1, report.segments.size)
        assertEquals(3, report.models.size)
        val seg = report.segments[0]
        assertTrue(seg.consensus.any { it.contains("梯度下降") }, "应解析出共同观点：${seg.consensus}")
        assertTrue(seg.divergences.isNotEmpty(), "应解析出分歧点")
        assertEquals("学习率取值", seg.divergences[0].topic)
        assertTrue(seg.divergences[0].views.size >= 2)
        assertFalse(seg.degraded)
        assertTrue(seg.perModel.containsKey("Kimi K3"))
    }

    @Test
    fun fallbackSeatRescuesWhenPrimaryFails() = runBlocking {
        val engine = DeepDigestEngine(
            listOf(
                Triple("Kimi K3", Thrower(), smartFake()),   // 主模型失败 → 备选接力
                Triple("DeepSeek V4.1", smartFake(), null),
                Triple("GLM 5.3", smartFake(), null)
            ),
            maxRounds = 1,
            callTimeoutMs = 5_000
        )
        val report = engine.digest("v1", listOf(unit()))
        val seg = report.segments[0]
        assertFalse(seg.degraded, "有备选接力不应降级")
        assertTrue(seg.perModel.containsKey("Kimi K3"), "备选接力后仍应有 A 席稿件")
    }

    @Test
    fun allModelsFailMarksDegraded() = runBlocking {
        val engine = DeepDigestEngine(
            listOf(
                Triple("A", Thrower(), null),
                Triple("B", Thrower(), null),
                Triple("C", Thrower(), null)
            ),
            maxRounds = 1,
            retries = 0,
            callTimeoutMs = 3_000
        )
        val report = engine.digest("v1", listOf(unit()))
        assertTrue(report.segments[0].degraded, "全部失败应标降级，不能崩")
    }
}
