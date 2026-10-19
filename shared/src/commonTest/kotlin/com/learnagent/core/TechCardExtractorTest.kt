package com.learnagent.core

import com.learnagent.data.LearningUnit
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 技术卡提取器：LLM 路径 + 三种降级路径。 */
class TechCardExtractorTest {

    private class FakeLlm(
        private val reply: String = "",
        private val error: Exception? = null
    ) : LlmClient {
        override suspend fun generateResponse(prompt: String, config: LlmConfig): String {
            error?.let { throw it }
            return reply
        }
    }

    private fun unit() = LearningUnit(
        id = "v1_0",
        videoId = "v1",
        start = 0.0,
        end = 10.0,
        speech = "ZooKeeper 是一个分布式协调服务，用于分布式锁和选主",
        screenText = "",
        keywords = listOf("ZooKeeper", "分布式锁")
    )

    private val llmReply = """
[
  {"name":"ZooKeeper","category":"DISTRIBUTED","whatItIs":"分布式协调服务",
   "whatItSolves":"分布式锁、选主","whenToUse":"强一致场景","whenNotToUse":"单机",
   "howItWorks":"ZAB 协议","alternatives":["etcd"],"relatedConcepts":["CAP"],"codeSnippet":""},
  {"name":"etcd","category":"DISTRIBUTED","whatItIs":"键值存储",
   "whatItSolves":"配置管理、服务发现","whenToUse":"云原生","whenNotToUse":"关系型需求",
   "howItWorks":"Raft","alternatives":["ZooKeeper"],"relatedConcepts":["Raft"],"codeSnippet":""}
]
""".trimIndent()

    @Test
    fun llmAvailableReturnsTwoCards() = runBlocking {
        val cards = TechCardExtractor(FakeLlm(llmReply)).extract(unit(), "v1")
        assertEquals(2, cards.size)
        assertTrue(cards.all { it.source == "LLM" })
        assertTrue(cards.all { it.sourceVideoId == "v1" })
    }

    @Test
    fun noLlmFallsBackToHeuristic() = runBlocking {
        val cards = TechCardExtractor(null).extract(unit(), "v1")
        assertTrue(cards.isNotEmpty(), "启发式至少产出 1 张")
        assertTrue(cards.all { it.source == "本地启发式" })
    }

    @Test
    fun invalidJsonFallsBackToHeuristic() = runBlocking {
        val cards = TechCardExtractor(FakeLlm("这不是 JSON")).extract(unit(), "v1")
        assertTrue(cards.isNotEmpty())
        assertTrue(cards.all { it.source == "本地启发式" })
    }

    @Test
    fun llmTimeoutFallsBackToHeuristic() = runBlocking {
        val cards = TechCardExtractor(FakeLlm(error = LlmTimeoutException())).extract(unit(), "v1")
        assertTrue(cards.isNotEmpty())
        assertTrue(cards.all { it.source == "本地启发式" })
    }
}
