package com.learnagent

import com.learnagent.core.Indexer
import com.learnagent.core.LlmClient
import com.learnagent.core.LlmConfig
import com.learnagent.core.RemoteQaEngine
import com.learnagent.core.merge
import com.learnagent.data.FrameOcr
import com.learnagent.data.TranscriptSegment
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 验证远程大模型问答引擎：用假 LlmClient 断言它正确构造带时间戳的上下文并返回引用。
 * 不访问网络。
 */
class RemoteQaEngineTest {

    private class FakeLlm(private val reply: String) : LlmClient {
        var lastPrompt: String = ""
        override suspend fun generateResponse(prompt: String, config: LlmConfig): String {
            lastPrompt = prompt
            return reply
        }
    }

    private fun timeline() = merge(
        listOf(
            TranscriptSegment(start = 0.0, end = 10.0, text = "梯度下降是优化算法", speaker = null),
            TranscriptSegment(start = 20.0, end = 30.0, text = "反向传播计算梯度", speaker = null)
        ),
        listOf(FrameOcr(timestamp = 2.0, text = "梯度下降 = 沿负梯度更新参数", imagePath = "f1.jpg"))
    )

    @Test
    fun testRemoteQaBuildsContextAndCitations() = runBlocking {
        val indexer = Indexer()
        indexer.index(timeline())
        val llm = FakeLlm("梯度下降在 00:00 处提到。")
        val engine = RemoteQaEngine({ q, k -> indexer.search(q, k) }, llm)

        val answer = engine.ask("梯度下降")
        assertTrue(answer.answer.contains("梯度下降"), "应返回模型回答")
        assertTrue(answer.citations.isNotEmpty(), "应有时间戳引用")
        assertTrue(llm.lastPrompt.contains("00:00"), "prompt 应包含时间戳上下文")
        assertTrue(llm.lastPrompt.contains("梯度下降"), "prompt 应包含检索内容")
        assertTrue(llm.lastPrompt.contains("问题：梯度下降"), "prompt 应包含用户问题")
    }

    @Test
    fun testRemoteQaNoMatch() = runBlocking {
        val indexer = Indexer()
        indexer.index(emptyList())
        val engine = RemoteQaEngine({ q, k -> indexer.search(q, k) }, FakeLlm("x"))
        val answer = engine.ask("量子力学")
        assertEquals(emptyList(), answer.citations)
        // 新行为：不再拒答——检索无命中也照常回答（模型自由发挥 + 通识）
        assertEquals("x", answer.answer)
    }
}
