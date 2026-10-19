package com.learnagent

import com.learnagent.core.DebateEngine
import com.learnagent.core.LlmClient
import com.learnagent.core.LlmConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

/**
 * 四轮对抗协议测试：交叉批判 3 条、中立裁判、连续一致性、onRound 流式回调。
 * （旧语义由 StageATest.testDebateEngineDivergence 覆盖，此处只验证新协议。）
 */
class DebateEngineTest {

    private class FakeLlm(private val reply: String) : LlmClient {
        override suspend fun generateResponse(prompt: String, config: LlmConfig): String = reply
    }

    @Test
    fun testCrossCritiqueProtocol() = runBlocking {
        val engine = DebateEngine(
            FakeLlm("提案甲：梯度下降通过迭代最小化损失。"),
            FakeLlm("提案乙：梯度下降依赖学习率，选择不当会发散。"),
            FakeLlm("提案丙：在小数据集上梯度下降表现稳定。")
        )
        val rounds = mutableListOf<Int>()
        val result = engine.debate(
            topic = "梯度下降是否有效",
            context = "上下文内容",
            onRound = { round, partial ->
                rounds.add(round)
                if (round == 1) {
                    assertTrue(partial.proposerA.isNotBlank(), "第 1 轮应包含提案 A")
                    assertTrue(partial.proposerC.isNotBlank(), "第 1 轮应包含提案 C")
                    assertTrue(partial.critiques.isEmpty(), "第 1 轮不应有批判")
                }
                if (round == 2) {
                    assertEquals(3, partial.critiques.size, "第 2 轮应有 3 条交叉批判")
                }
            }
        )

        assertEquals(listOf(1, 2, 3), rounds, "onRound 应按 1→2→3 触发")
        assertEquals(3, result.critiques.size, "交叉批判必须是 3 条")
        assertTrue(result.critiques.all { it.contains("批") }, "每条批判须注明谁批谁")
        assertTrue(result.judge.isNotBlank(), "中立裁决不能为空")
        assertTrue(result.agreement in 0.0..1.0, "agreement 应为 0~1 连续值，实际=${result.agreement}")
        assertTrue(result.proposerA.isNotBlank() && result.proposerB.isNotBlank() && result.proposerC.isNotBlank())
        assertTrue(result.minorityReport.isNotBlank(), "少数派报告应自动产生")
    }

    @Test
    fun testAgreementIdenticalProposals() = runBlocking {
        val same = "完全相同的提案内容用于验证一致性上限"
        val engine = DebateEngine(FakeLlm(same), FakeLlm(same), FakeLlm(same))
        val result = engine.debate("议题", "上下文")
        assertTrue(result.agreement > 0.99, "完全相同提案的 agreement 应接近 1，实际=${result.agreement}")
    }

    @Test
    fun testDegradedModelNeverFailsWhole() = runBlocking {
        // 模型 C 每次都抛错 → 降级文案，其余继续
        val broken = object : LlmClient {
            override suspend fun generateResponse(prompt: String, config: LlmConfig): String =
                throw IllegalStateException("连接超时")
        }
        val engine = DebateEngine(
            FakeLlm("提案A"),
            FakeLlm("提案B"),
            broken
        )
        val result = engine.debate("议题", "上下文")
        assertTrue(result.proposerC.contains("不可用"), "失败的模型应有降级文案")
        assertEquals(3, result.critiques.size, "任一模型失败不影响整体")
        assertTrue(result.agreement >= 0.0, "agreement 仍应可计算")
    }
}
