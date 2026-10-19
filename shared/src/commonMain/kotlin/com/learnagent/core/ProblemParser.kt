package com.learnagent.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

/** 结构化问题：核心关键词 3-8 个 + 领域 + 约束。 */
@Serializable
data class ParsedProblem(
    val keywords: List<String> = emptyList(),
    val domain: String = "其他",
    val constraints: List<String> = emptyList()
)

/**
 * 问题解析（双路）：
 * - 主路：LLM 输出 JSON（keywords/domain/constraints），解析失败或不足 → 降级
 * - 降级路：本地词典（≈180 技术词 + 分类映射）+ 正则约束词
 * 所有降级结果不标注来源字段（由上层按 llm==null 判定），但保证可用。
 */
class ProblemParser(private val llm: LlmClient?) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun parse(problem: String): ParsedProblem {
        if (problem.isBlank()) return ParsedProblem()
        if (llm == null) return parseWithDictionary(problem)
        return try {
            val parsed = parseWithLlm(problem)
            if (parsed.keywords.size >= 2) parsed else parseWithDictionary(problem)
        } catch (e: Exception) {
            parseWithDictionary(problem)
        }
    }

    private suspend fun parseWithLlm(problem: String): ParsedProblem {
        val client = llm ?: return parseWithDictionary(problem)
        val prompt = """
解析下面的技术问题，只输出 JSON：
{"keywords":["3-8 个核心技术关键词"],"domain":"分布式|并发|数据库|网络|安全|性能|架构|算法|框架|工具|其他","constraints":["高并发","低延迟","强一致"等]}

问题：$problem
""".trimIndent()
        val raw = client.generateResponse(prompt, LlmConfig("nvidia", "", "", ""))
        return parseJson(raw)
    }

    private fun parseJson(raw: String): ParsedProblem {
        val s = raw.indexOf('{')
        val e = raw.lastIndexOf('}')
        if (s < 0 || e <= s) throw IllegalArgumentException("无 JSON")
        val o = json.parseToJsonElement(raw.substring(s, e + 1)).jsonObject
        fun strList(key: String): List<String> =
            (o[key] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull } ?: emptyList()
        val keywords = strList("keywords").map { it.trim() }.filter { it.isNotBlank() }.take(8)
        val domain = (o["domain"] as? JsonPrimitive)?.contentOrNull?.trim().orEmpty().ifBlank { "其他" }
        val constraints = strList("constraints").map { it.trim() }.filter { it.isNotBlank() }.take(5)
        return ParsedProblem(keywords, domain, constraints)
    }

    /** 本地词典 + 正则。 */
    fun parseWithDictionary(problem: String): ParsedProblem {
        val keywords = LinkedHashSet<String>()
        var domain = "其他"

        // 词典命中（长词优先，避免"锁"先命中把"分布式锁"拆散）
        val sorted = DICTIONARY.sortedByDescending { it.canonical.length }
        val matched = mutableListOf<DictEntry>()
        for (entry in sorted) {
            val hit = entry.canonical.length >= 2 && problem.contains(entry.canonical, ignoreCase = true) ||
                entry.aliases.any { it.isNotBlank() && problem.contains(it, ignoreCase = true) }
            if (hit && matched.none { it.canonical == entry.canonical }) matched.add(entry)
        }
        matched.forEach { keywords.add(it.canonical) }
        if (matched.isNotEmpty()) domain = matched.first().domain

        // 约束词（验收要求：约束词也进入 keywords）
        val constraints = CONSTRAINTS.filter { problem.contains(it) }
        constraints.forEach { keywords.add(it) }

        // 英文/数字专有词
        Regex("[A-Za-z][A-Za-z0-9+#._-]{1,}").findAll(problem).forEach { m ->
            if (keywords.size < 8) keywords.add(m.value)
        }
        // 中文兜底：短句 + 滑窗（无标点长句也能切词）
        if (keywords.size < 3) {
            val segments = problem.split(Regex("[，。,.!?！？；;：:\\s]+")).map { it.trim() }.filter { it.isNotEmpty() }
            segments.forEach { seg ->
                if (keywords.size >= 8) return@forEach
                if (seg.length in 2..6) keywords.add(seg)
                if (seg.length > 4) {
                    for (n in listOf(2, 3)) {
                        var i = 0
                        while (i + n <= seg.length && keywords.size < 8) {
                            val piece = seg.substring(i, i + n)
                            if (piece.any { it.code > 0x2E80 }) keywords.add(piece)
                            i += n
                        }
                    }
                }
            }
        }
        return ParsedProblem(keywords.take(8).toList(), domain, constraints)
    }

    private data class DictEntry(val canonical: String, val domain: String, val aliases: List<String> = emptyList())

    private companion object {
        val CONSTRAINTS = listOf(
            "高并发", "低延迟", "强一致", "最终一致", "高可用", "高吞吐", "可扩展",
            "海量数据", "实时", "低内存", "低成本", "安全", "容错", "跨平台", "离线"
        )

        val DICTIONARY: List<DictEntry> = listOf(
            // 分布式
            DictEntry("分布式系统", "分布式", listOf("分布式")),
            DictEntry("分布式锁", "分布式"),
            DictEntry("一致性协议", "分布式", listOf("一致性", "共识")),
            DictEntry("选主", "分布式", listOf("leader 选举", "主节点")),
            DictEntry("Raft", "分布式"),
            DictEntry("Paxos", "分布式"),
            DictEntry("ZooKeeper", "分布式"),
            DictEntry("etcd", "分布式"),
            DictEntry("Consul", "分布式"),
            DictEntry("服务发现", "分布式"),
            DictEntry("注册中心", "分布式"),
            DictEntry("API 网关", "分布式", listOf("网关")),
            DictEntry("微服务", "分布式"),
            DictEntry("消息队列", "分布式", listOf("MQ")),
            DictEntry("Kafka", "分布式"),
            DictEntry("RabbitMQ", "分布式"),
            DictEntry("RocketMQ", "分布式"),
            DictEntry("分库分表", "分布式"),
            DictEntry("幂等", "分布式"),
            DictEntry("CAP", "分布式"),
            DictEntry("BASE", "分布式"),
            DictEntry("两阶段提交", "分布式", listOf("2PC")),
            DictEntry("TCC", "分布式"),
            DictEntry("熔断", "分布式"),
            DictEntry("限流", "分布式"),
            DictEntry("降级", "分布式"),
            DictEntry("负载均衡", "分布式"),
            // 并发
            DictEntry("并发编程", "并发", listOf("并发")),
            DictEntry("线程池", "并发"),
            DictEntry("线程", "并发"),
            DictEntry("死锁", "并发"),
            DictEntry("协程", "并发"),
            DictEntry("并行", "并发"),
            DictEntry("原子操作", "并发"),
            DictEntry("CAS", "并发"),
            DictEntry("内存屏障", "并发"),
            DictEntry("可见性", "并发"),
            DictEntry("线程安全", "并发"),
            DictEntry("生产者消费者", "并发"),
            DictEntry("信号量", "并发"),
            DictEntry("互斥锁", "并发"),
            DictEntry("读写锁", "并发"),
            DictEntry("无锁", "并发"),
            DictEntry("Actor", "并发"),
            // 数据库
            DictEntry("数据库", "数据库"),
            DictEntry("MySQL", "数据库"),
            DictEntry("PostgreSQL", "数据库"),
            DictEntry("Redis", "数据库"),
            DictEntry("MongoDB", "数据库"),
            DictEntry("索引", "数据库"),
            DictEntry("B+树", "数据库"),
            DictEntry("事务", "数据库"),
            DictEntry("ACID", "数据库"),
            DictEntry("隔离级别", "数据库"),
            DictEntry("主从复制", "数据库"),
            DictEntry("读写分离", "数据库"),
            DictEntry("慢查询", "数据库"),
            DictEntry("执行计划", "数据库"),
            DictEntry("连接池", "数据库"),
            DictEntry("NoSQL", "数据库"),
            DictEntry("时序数据库", "数据库"),
            DictEntry("向量数据库", "数据库"),
            DictEntry("数据仓库", "数据库"),
            DictEntry("OLAP", "数据库"),
            DictEntry("OLTP", "数据库"),
            // 网络
            DictEntry("TCP", "网络"),
            DictEntry("UDP", "网络"),
            DictEntry("HTTP", "网络"),
            DictEntry("HTTPS", "网络"),
            DictEntry("HTTP/2", "网络", listOf("HTTP2")),
            DictEntry("HTTP/3", "网络", listOf("HTTP3")),
            DictEntry("QUIC", "网络"),
            DictEntry("DNS", "网络"),
            DictEntry("CDN", "网络"),
            DictEntry("三次握手", "网络"),
            DictEntry("四次挥手", "网络"),
            DictEntry("拥塞控制", "网络"),
            DictEntry("粘包", "网络"),
            DictEntry("长连接", "网络"),
            DictEntry("WebSocket", "网络"),
            DictEntry("gRPC", "网络"),
            DictEntry("RPC", "网络"),
            DictEntry("NAT", "网络"),
            DictEntry("反向代理", "网络"),
            DictEntry("抓包", "网络"),
            // 安全
            DictEntry("加密", "安全"),
            DictEntry("对称加密", "安全"),
            DictEntry("非对称加密", "安全"),
            DictEntry("RSA", "安全"),
            DictEntry("AES", "安全"),
            DictEntry("哈希", "安全"),
            DictEntry("数字签名", "安全", listOf("签名")),
            DictEntry("JWT", "安全"),
            DictEntry("OAuth", "安全"),
            DictEntry("SSO", "安全"),
            DictEntry("XSS", "安全"),
            DictEntry("CSRF", "安全"),
            DictEntry("SQL注入", "安全"),
            DictEntry("权限控制", "安全", listOf("权限")),
            DictEntry("认证", "安全"),
            DictEntry("授权", "安全"),
            DictEntry("证书", "安全"),
            DictEntry("TLS", "安全"),
            DictEntry("中间人攻击", "安全"),
            DictEntry("脱敏", "安全"),
            // 性能
            DictEntry("性能优化", "性能", listOf("优化")),
            DictEntry("缓存", "性能"),
            DictEntry("本地缓存", "性能"),
            DictEntry("分布式缓存", "性能"),
            DictEntry("预加载", "性能"),
            DictEntry("懒加载", "性能"),
            DictEntry("批处理", "性能"),
            DictEntry("异步", "性能"),
            DictEntry("背压", "性能"),
            DictEntry("内存泄漏", "性能"),
            DictEntry("GC", "性能"),
            DictEntry("JVM调优", "性能"),
            DictEntry("火焰图", "性能"),
            DictEntry("压测", "性能"),
            DictEntry("QPS", "性能"),
            DictEntry("延迟", "性能"),
            DictEntry("吞吐量", "性能"),
            // 架构
            DictEntry("架构设计", "架构", listOf("架构")),
            DictEntry("设计模式", "架构"),
            DictEntry("单例", "架构"),
            DictEntry("工厂模式", "架构"),
            DictEntry("观察者模式", "架构"),
            DictEntry("策略模式", "架构"),
            DictEntry("装饰器模式", "架构"),
            DictEntry("MVC", "架构"),
            DictEntry("MVVM", "架构"),
            DictEntry("分层架构", "架构"),
            DictEntry("六边形架构", "架构"),
            DictEntry("领域驱动", "架构", listOf("DDD")),
            DictEntry("事件驱动", "架构"),
            DictEntry("CQRS", "架构"),
            DictEntry("单体", "架构"),
            DictEntry("Serverless", "架构"),
            DictEntry("云原生", "架构"),
            DictEntry("容器化", "架构", listOf("容器")),
            DictEntry("Docker", "架构"),
            DictEntry("Kubernetes", "架构", listOf("K8s", "k8s")),
            DictEntry("CI/CD", "架构"),
            DictEntry("DevOps", "架构"),
            // 算法
            DictEntry("算法", "算法"),
            DictEntry("排序", "算法"),
            DictEntry("二分查找", "算法"),
            DictEntry("动态规划", "算法"),
            DictEntry("贪心", "算法"),
            DictEntry("回溯", "算法"),
            DictEntry("图算法", "算法"),
            DictEntry("BFS", "算法"),
            DictEntry("DFS", "算法"),
            DictEntry("最短路径", "算法"),
            DictEntry("Dijkstra", "算法"),
            DictEntry("哈希表", "算法"),
            DictEntry("二叉树", "算法"),
            DictEntry("红黑树", "算法"),
            DictEntry("堆", "算法"),
            DictEntry("时间复杂度", "算法", listOf("复杂度")),
            DictEntry("空间复杂度", "算法"),
            DictEntry("递归", "算法"),
            DictEntry("分治", "算法"),
            DictEntry("滑动窗口", "算法"),
            DictEntry("双指针", "算法"),
            DictEntry("位运算", "算法"),
            // 框架
            DictEntry("Spring Boot", "框架", listOf("SpringBoot")),
            DictEntry("Spring", "框架"),
            DictEntry("React", "框架"),
            DictEntry("Vue", "框架"),
            DictEntry("Angular", "框架"),
            DictEntry("Ktor", "框架"),
            DictEntry("Django", "框架"),
            DictEntry("Flask", "框架"),
            DictEntry("MyBatis", "框架"),
            DictEntry("Hibernate", "框架"),
            DictEntry("Netty", "框架"),
            DictEntry("Electron", "框架"),
            DictEntry("Flutter", "框架"),
            DictEntry("Compose", "框架"),
            // 工具
            DictEntry("Git", "工具"),
            DictEntry("Maven", "工具"),
            DictEntry("Gradle", "工具"),
            DictEntry("Jenkins", "工具"),
            DictEntry("Nginx", "工具"),
            DictEntry("Postman", "工具"),
            DictEntry("JUnit", "工具"),
            DictEntry("JMeter", "工具"),
            DictEntry("Prometheus", "工具"),
            DictEntry("Grafana", "工具"),
            DictEntry("ELK", "工具"),
            DictEntry("Linux", "工具"),
            DictEntry("Shell", "工具"),
            DictEntry("Vim", "工具"),
            DictEntry("VS Code", "工具"),
            // 其他（AI / 语言）
            DictEntry("机器学习", "其他"),
            DictEntry("深度学习", "其他"),
            DictEntry("神经网络", "其他"),
            DictEntry("卷积", "其他"),
            DictEntry("池化", "其他"),
            DictEntry("残差连接", "其他"),
            DictEntry("注意力机制", "其他"),
            DictEntry("Transformer", "其他"),
            DictEntry("大模型", "其他", listOf("LLM")),
            DictEntry("RAG", "其他"),
            DictEntry("提示词", "其他"),
            DictEntry("微调", "其他"),
            DictEntry("扩散模型", "其他"),
            DictEntry("U-Net", "其他"),
            DictEntry("视频生成", "其他"),
            DictEntry("PyTorch", "其他"),
            DictEntry("TensorFlow", "其他"),
            DictEntry("强化学习", "其他"),
            DictEntry("梯度下降", "其他"),
            DictEntry("反向传播", "其他"),
            DictEntry("过拟合", "其他"),
            DictEntry("正则化", "其他"),
            DictEntry("特征工程", "其他"),
            DictEntry("数据清洗", "其他"),
            DictEntry("Python", "其他"),
            DictEntry("Java", "其他"),
            DictEntry("Kotlin", "其他"),
            DictEntry("C++", "其他"),
            DictEntry("Golang", "其他", listOf("Go 语言")),
            DictEntry("Rust", "其他")
        )
    }
}
