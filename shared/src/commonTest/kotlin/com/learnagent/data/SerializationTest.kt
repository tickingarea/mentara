package com.learnagent.data

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/** 学习引擎数据模型序列化往返测试。 */
class SerializationTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun techCardRoundTrip() {
        val card = TechCard(
            id = "v1_0_0_zk",
            name = "ZooKeeper",
            category = TechCategory.DISTRIBUTED,
            whatItIs = "分布式协调服务",
            whatItSolves = "分布式锁、选主、配置管理",
            whenToUse = "需要强一致性",
            whenNotToUse = "简单单机场景",
            howItWorks = "ZAB 协议 + ZNode 树",
            alternatives = listOf("etcd", "Consul"),
            relatedConcepts = listOf("CAP", "Raft"),
            codeSnippet = "zk.start()",
            sourceVideoId = "v1",
            sourceTimestamp = 12.5,
            source = "LLM"
        )
        val decoded = json.decodeFromString(TechCard.serializer(), json.encodeToString(TechCard.serializer(), card))
        assertEquals(card, decoded)
    }

    @Test
    fun techUsageRoundTrip() {
        val usage = TechUsage(
            techName = "ZooKeeper",
            firstSeenAt = 1000L,
            lastSeenAt = 2000L,
            seenCount = 3,
            usedCount = 1,
            decisionCount = 2,
            status = TechStatus.USED
        )
        val decoded = json.decodeFromString(TechUsage.serializer(), json.encodeToString(TechUsage.serializer(), usage))
        assertEquals(usage, decoded)
    }

    @Test
    fun decisionRoundTrip() {
        val decision = Decision(
            id = "d1",
            problem = "选型分布式锁",
            options = listOf("Redis", "ZooKeeper"),
            chosen = "ZooKeeper",
            reason = "强一致性",
            constraints = listOf("团队熟悉度"),
            result = "运行稳定",
            resultGood = true,
            createdAt = 123456L,
            followUpAt = 654321L,
            followUpDone = false
        )
        val decoded = json.decodeFromString(Decision.serializer(), json.encodeToString(Decision.serializer(), decision))
        assertEquals(decision, decoded)
    }

    @Test
    fun codeSnippetRoundTrip() {
        val snippet = CodeSnippet(
            id = "v1_snippet_0_0",
            language = "python",
            purpose = "代码片段（python）",
            code = "def add(a, b):\n    return a + b",
            comment = "首行：def add(a, b):",
            sourceVideoId = "v1",
            sourceTimestamp = 3.0,
            tags = listOf("函数")
        )
        val decoded = json.decodeFromString(CodeSnippet.serializer(), json.encodeToString(CodeSnippet.serializer(), snippet))
        assertEquals(snippet, decoded)
    }

    @Test
    fun knowledgeMasteryRoundTrip() {
        val mastery = KnowledgeMastery(
            pointId = "v1_kp_0_0",
            status = MasteryStatus.FAMILIAR,
            recallAccuracy = 0.85f,
            reviewCount = 4,
            correctCount = 3,
            lastReviewAt = 111L,
            nextReviewAt = 222L,
            easeFactor = 2.6f,
            interval = 6,
            totalStudyMinutes = 30
        )
        val decoded = json.decodeFromString(KnowledgeMastery.serializer(), json.encodeToString(KnowledgeMastery.serializer(), mastery))
        assertEquals(mastery, decoded)
    }

    @Test
    fun knowledgePointRoundTrip() {
        val point = KnowledgePoint(
            id = "v1_kp_0_0",
            name = "ZooKeeper",
            definition = "分布式协调服务",
            sourceVideoId = "v1",
            sourceTimestamp = 0.0,
            examLevel = ExamLevel.MUST_KNOW,
            masteryStatus = MasteryStatus.NOT_LEARNED,
            relatedIds = listOf("v1_kp_0_1")
        )
        val decoded = json.decodeFromString(KnowledgePoint.serializer(), json.encodeToString(KnowledgePoint.serializer(), point))
        assertEquals(point, decoded)
    }
}
