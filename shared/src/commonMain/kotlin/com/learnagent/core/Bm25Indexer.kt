package com.learnagent.core

import com.learnagent.data.LearningUnit
import kotlin.math.ln

/** 同义词表（22 组，中英互扩）。 */
val TECH_SYNONYMS: Map<String, List<String>> = linkedMapOf(
    "分布式锁" to listOf("distributed lock", "redlock", "zookeeper lock"),
    "缓存" to listOf("cache", "redis", "memcached"),
    "消息队列" to listOf("message queue", "kafka", "rabbitmq", "rocketmq"),
    "负载均衡" to listOf("load balancing", "nginx", "lvs"),
    "数据库索引" to listOf("db index", "b+ tree", "innodb"),
    "并发" to listOf("concurrency", "parallel", "thread"),
    "容器" to listOf("container", "docker", "kubernetes", "k8s"),
    "微服务" to listOf("microservice", "service mesh"),
    "限流" to listOf("rate limit", "throttle"),
    "熔断" to listOf("circuit breaker", "hystrix"),
    "一致性" to listOf("consistency", "raft", "paxos", "cap"),
    "数据库事务" to listOf("transaction", "acid", "isolation"),
    "分库分表" to listOf("sharding", "partition"),
    "反向代理" to listOf("reverse proxy", "nginx"),
    "垃圾回收" to listOf("gc", "garbage collection"),
    "线程池" to listOf("thread pool", "executor"),
    "设计模式" to listOf("design pattern", "singleton", "factory"),
    "接口" to listOf("api", "interface"),
    "部署" to listOf("deploy", "deployment", "release"),
    "监控" to listOf("monitoring", "prometheus", "grafana"),
    "加密" to listOf("encryption", "aes", "rsa"),
    "机器学习" to listOf("machine learning", "ml", "deep learning")
)

/** 索引文档：tokens 为分词结果；avgLen 为可选全局平均长度（0 = 由 index() 计算）。 */
data class IndexedDoc(
    val id: String,
    val tokens: List<String>,
    val avgLen: Float = 0f
)

data class ScoredDoc(val docId: String, val score: Float)

/**
 * BM25-lite 检索：
 * score(q,d) = Σ IDF(t) × (tf·(k1+1)) / (tf + k1·(1 − b + b·|d|/avgdl))
 * IDF(t) = ln((N − df + 0.5) / (df + 0.5) + 1)
 * 默认 k1=1.2，b=0.75。
 */
class Bm25Indexer(
    private val k1: Float = 1.2f,
    private val b: Float = 0.75f
) {

    private var docs: List<IndexedDoc> = emptyList()
    private var df: Map<String, Int> = emptyMap()
    private var avgdl: Float = 0f

    fun index(docs: List<IndexedDoc>) {
        this.docs = docs
        val dfMap = mutableMapOf<String, Int>()
        docs.forEach { doc -> doc.tokens.toSet().forEach { t -> dfMap[t] = (dfMap[t] ?: 0) + 1 } }
        df = dfMap
        val providedAvg = docs.firstOrNull { it.avgLen > 0f }?.avgLen ?: 0f
        avgdl = if (providedAvg > 0f) providedAvg
        else if (docs.isEmpty()) 0f
        else docs.map { it.tokens.size }.average().toFloat()
    }

    fun search(query: String, topK: Int = 10): List<ScoredDoc> {
        val tokens = expandQuery(tokenize(query))
        if (tokens.isEmpty()) return emptyList()
        return docs
            .map { ScoredDoc(it.id, scoreTokens(tokens, it.id)) }
            .filter { it.score > 0f }
            .sortedByDescending { it.score }
            .take(topK)
    }

    /** 指定 query tokens 对某文档的 BM25 分数（供精确断言使用）。 */
    fun scoreTokens(queryTokens: List<String>, docId: String): Float {
        val doc = docs.firstOrNull { it.id == docId } ?: return 0f
        if (docs.isEmpty() || avgdl <= 0f || doc.tokens.isEmpty()) return 0f
        val tfMap = doc.tokens.groupingBy { it }.eachCount()
        val n = docs.size.toFloat()
        val dlen = doc.tokens.size.toFloat()
        var score = 0f
        queryTokens.distinct().forEach { t ->
            val tf = (tfMap[t] ?: 0).toFloat()
            if (tf > 0f) {
                val dft = (df[t] ?: 0).toFloat()
                val idf = ln((n - dft + 0.5f) / (dft + 0.5f) + 1f)
                score += idf * (tf * (k1 + 1f)) / (tf + k1 * (1f - b + b * dlen / avgdl))
            }
        }
        return score
    }

    /** 分词：小写化 → 非字母数字切分 → CJK 长词补 2-gram。 */
    fun tokenize(text: String): List<String> {
        val raw = text.lowercase().split(Regex("[^\\p{L}\\p{N}+#]+")).filter { it.isNotBlank() }
        val out = LinkedHashSet<String>()
        raw.forEach { token ->
            out.add(token)
            if (token.length > 2 && token.any { it.code > 0x2E80 }) {
                for (i in 0..token.length - 2) out.add(token.substring(i, i + 2))
            }
        }
        return out.toList()
    }

    /** 同义词双向扩展。 */
    fun expandQuery(tokens: List<String>): List<String> {
        val out = LinkedHashSet(tokens)
        TECH_SYNONYMS.forEach { (key, synonyms) ->
            val synonymTokens = synonyms.flatMap { tokenize(it) }.toSet()
            val hit = tokens.any { t -> t == key || tokenize(key).contains(t) || synonymTokens.contains(t) }
            if (hit) {
                out.add(key)
                out.addAll(tokenize(key))
                synonyms.forEach { out.add(it); out.addAll(tokenize(it)) }
            }
        }
        return out.toList()
    }
}

/** 时间窗口融合：命中片段附带前后各 1 段（相邻时间戳的 LearningUnit）。 */
data class RetrievalWithContext(
    val hit: ScoredDoc,
    val before: LearningUnit?,
    val after: LearningUnit?
)

class TimeWindowRetriever(
    private val units: List<LearningUnit>,
    private val idOf: (LearningUnit) -> String = { it.id }
) {

    fun fuse(hits: List<ScoredDoc>): List<RetrievalWithContext> = hits.map { hit ->
        val index = units.indexOfFirst { idOf(it) == hit.docId }
        RetrievalWithContext(
            hit = hit,
            before = if (index > 0) units[index - 1] else null,
            after = if (index in 0 until units.size - 1) units[index + 1] else null
        )
    }
}
