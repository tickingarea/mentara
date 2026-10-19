package com.learnagent.core

import com.learnagent.data.LearningUnit
import com.learnagent.data.TechCard
import com.learnagent.data.TechCategory
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

/** LLM 超时（由 LlmClient 实现抛出）；提取器捕获后降级本地启发式，不崩溃。 */
class LlmTimeoutException(message: String = "LLM timeout") : Exception(message)

/**
 * 技术卡提取器。
 * 有 LLM → 调 LLM 解析 JSON；无 LLM / 非法 JSON / 超时 / 任何异常 → 本地启发式降级。
 * 单次最多 3 张卡。
 */
class TechCardExtractor(private val llm: LlmClient?) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun extract(unit: LearningUnit, videoId: String): List<TechCard> {
        if (llm == null) return extractWithHeuristic(unit, videoId)
        return try {
            val cards = extractWithLlm(unit, videoId)
            if (cards.isEmpty()) extractWithHeuristic(unit, videoId) else cards
        } catch (e: Exception) {
            extractWithHeuristic(unit, videoId)
        }
    }

    private suspend fun extractWithLlm(unit: LearningUnit, videoId: String): List<TechCard> {
        val client = llm ?: return extractWithHeuristic(unit, videoId)
        val prompt = """
从以下技术内容中，提取技术卡。

内容：${unit.speech}
画面：${unit.screenText}

对每个技术点，填写 JSON：
{
  "name": "ZooKeeper",
  "category": "DISTRIBUTED",
  "whatItIs": "分布式协调服务",
  "whatItSolves": "分布式锁、选主、配置管理",
  "whenToUse": "需要强一致性、QPS > 5000",
  "whenNotToUse": "不需要强一致性的简单场景",
  "howItWorks": "基于 ZAB 协议，通过 ZNode 树和 Watch 实现",
  "alternatives": ["etcd", "Consul"],
  "relatedConcepts": ["CAP", "Raft", "Paxos"],
  "codeSnippet": ""
}

规则：
- whatItIs 不超过 15 字
- whatItSolves 列 2-3 个核心场景
- howItWorks 只讲大概，不要展开
- 一次最多提取 3 个技术点
- 只输出 JSON 数组（0-3 个对象），不要解释
""".trimIndent()
        val raw = client.generateResponse(prompt, LlmConfig("nvidia", "", "", ""))
        return parseCards(raw, unit, videoId)
    }

    /** 解析 LLM 返回；无法解析时抛异常，由 [extract] 降级。 */
    private fun parseCards(raw: String, unit: LearningUnit, videoId: String): List<TechCard> {
        val arrStart = raw.indexOf('[')
        val arrEnd = raw.lastIndexOf(']')
        if (arrStart >= 0 && arrEnd > arrStart) {
            val arr = json.parseToJsonElement(raw.substring(arrStart, arrEnd + 1)).jsonArray
            return arr.mapIndexedNotNull { i, el ->
                (el as? JsonObject)?.toTechCard(unit, videoId, i, source = "LLM")
            }.take(3)
        }
        // 容错：LLM 只返回单个对象
        val objStart = raw.indexOf('{')
        val objEnd = raw.lastIndexOf('}')
        if (objStart >= 0 && objEnd > objStart) {
            val obj = json.parseToJsonElement(raw.substring(objStart, objEnd + 1)).jsonObject
            return listOfNotNull(obj.toTechCard(unit, videoId, 0, source = "LLM"))
        }
        throw IllegalArgumentException("LLM 未返回有效 JSON")
    }

    private fun JsonObject.toTechCard(unit: LearningUnit, videoId: String, index: Int, source: String): TechCard? {
        fun str(key: String, default: String = ""): String =
            (this[key] as? JsonPrimitive)?.contentOrNull ?: default

        fun list(key: String): List<String> =
            (this[key] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull } ?: emptyList()

        val name = str("name").trim()
        if (name.isBlank()) return null
        return TechCard(
            id = cardId(videoId, unit, name, index),
            name = name,
            category = runCatching { TechCategory.valueOf(str("category", "OTHER").uppercase()) }
                .getOrDefault(TechCategory.OTHER),
            whatItIs = str("whatItIs").take(30),
            whatItSolves = str("whatItSolves"),
            whenToUse = str("whenToUse"),
            whenNotToUse = str("whenNotToUse"),
            howItWorks = str("howItWorks"),
            alternatives = list("alternatives"),
            relatedConcepts = list("relatedConcepts"),
            codeSnippet = str("codeSnippet"),
            sourceVideoId = videoId,
            sourceTimestamp = unit.start,
            source = source
        )
    }

    // ---------- 本地启发式降级 ----------

    private fun extractWithHeuristic(unit: LearningUnit, videoId: String): List<TechCard> {
        val text = (unit.speech + " " + unit.screenText + " " + unit.keywords.joinToString(" ")).trim()
        if (text.isBlank()) return emptyList()

        val names = LinkedHashSet<String>()
        PROPER_NOUN.findAll(text).forEach { m -> names.add(m.value) }
        KEYWORD_TECH.forEach { (kw, canonical) -> if (text.contains(kw)) names.add(canonical) }
        if (names.isEmpty()) names.add(text.take(12))

        return names.take(3).mapIndexed { i, name ->
            TechCard(
                id = cardId(videoId, unit, name, i),
                name = name,
                category = classify(text),
                whatItIs = text.take(15),
                whatItSolves = unit.oneLineSummary.ifBlank { text.take(30) },
                whenToUse = unit.whenToUse,
                whenNotToUse = "",
                howItWorks = unit.intuition,
                relatedConcepts = unit.keywords.take(5),
                sourceVideoId = videoId,
                sourceTimestamp = unit.start,
                source = "本地启发式"
            )
        }
    }

    private fun classify(text: String): TechCategory {
        val t = text.lowercase()
        return when {
            listOf("锁", "线程", "并发", "死锁", "async", "coroutine").any { t.contains(it) } -> TechCategory.CONCURRENCY
            listOf("分布式", "选主", "一致性", "raft", "paxos", "zookeeper", "etcd").any { t.contains(it) } -> TechCategory.DISTRIBUTED
            listOf("数据库", "索引", "事务", "sql", "mysql", "redis").any { t.contains(it) } -> TechCategory.DATABASE
            listOf("网络", "tcp", "http", "udp", "socket", "rpc").any { t.contains(it) } -> TechCategory.NETWORK
            listOf("安全", "加密", "认证", "token", "https").any { t.contains(it) } -> TechCategory.SECURITY
            listOf("性能", "优化", "缓存", "qps", "延迟").any { t.contains(it) } -> TechCategory.PERFORMANCE
            listOf("架构", "设计模式", "分层", "领域驱动").any { t.contains(it) } -> TechCategory.ARCHITECTURE
            listOf("算法", "排序", "复杂度", "动态规划").any { t.contains(it) } -> TechCategory.ALGORITHM
            listOf("框架", "spring", "react", "vue", "ktor").any { t.contains(it) } -> TechCategory.FRAMEWORK
            listOf("工具", "git", "docker", "gradle", "maven").any { t.contains(it) } -> TechCategory.TOOL
            else -> TechCategory.OTHER
        }
    }

    private fun cardId(videoId: String, unit: LearningUnit, name: String, index: Int): String =
        "${videoId}_${unit.start.toInt()}_${index}_${name.lowercase().hashCode()}"

    private companion object {
        /** 英文/大写开头专有名词，如 ZooKeeper / JVM / TCP。 */
        val PROPER_NOUN = Regex("\\b[A-Z][A-Za-z0-9+#._-]{1,}\\b")

        /** 中文技术关键词 → 规范技术名。 */
        val KEYWORD_TECH = linkedMapOf(
            "分布式" to "分布式系统",
            "一致性" to "一致性协议",
            "选主" to "选主",
            "分布式锁" to "分布式锁",
            "微服务" to "微服务",
            "缓存" to "缓存",
            "消息队列" to "消息队列",
            "数据库" to "数据库",
            "索引" to "索引",
            "事务" to "事务",
            "容器" to "容器化",
            "网关" to "API 网关"
        )
    }
}
