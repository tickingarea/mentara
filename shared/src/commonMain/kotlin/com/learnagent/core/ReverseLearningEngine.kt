package com.learnagent.core

import com.learnagent.platform.TechStore
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

/** 逆向学习树节点：level 0 = 目标，1/2/3 = 前置层级。 */
data class KnowledgeNode(
    val name: String,
    val level: Int,
    val mastered: Boolean,
    val children: List<KnowledgeNode> = emptyList()
)

/**
 * 逆向学习引擎（Petersson 方法）：
 * 从目标反推前置知识，最多 3 层；已掌握的不再展开。
 * LLM 拆解失败/离线 → 本地前置词典降级。
 */
class ReverseLearningEngine(
    private val llm: LlmClient?,
    private val techStore: TechStore
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun analyze(goal: String): KnowledgeNode =
        KnowledgeNode(goal, 0, isMastered(goal), prerequisites(goal, 1, mutableSetOf(goal)))

    /** 文本学习路径（与设计示例同构）。 */
    fun learningPathText(root: KnowledgeNode): String {
        val sb = StringBuilder()
        sb.append("你的目标：").append(root.name).append("\n\n需要学：\n")
        val start = deepestUnmasteredLeaf(root)
        val children = root.children
        children.forEachIndexed { i, c -> render(sb, c, "  ", i == children.lastIndex, start) }
        sb.append("\n")
        sb.append(if (start != null) "建议从\"${start.name}\"开始。" else "目标相关前置已全部掌握，可直接实践。")
        return sb.toString()
    }

    private fun render(sb: StringBuilder, node: KnowledgeNode, indent: String, last: Boolean, start: KnowledgeNode?) {
        sb.append(indent).append(if (last) "└─ " else "├─ ").append(node.name)
            .append(if (node.mastered) "（已掌握）" else "（未掌握）")
        if (start != null && node.name == start.name && node.level == start.level) sb.append(" ← 先学这个")
        sb.append("\n")
        val children = node.children
        children.forEachIndexed { i, c -> render(sb, c, indent + "│   ", i == children.lastIndex, start) }
    }

    private fun deepestUnmasteredLeaf(root: KnowledgeNode): KnowledgeNode? {
        val leaves = mutableListOf<KnowledgeNode>()
        fun walk(n: KnowledgeNode) {
            if (n.children.isEmpty()) {
                if (!n.mastered) leaves.add(n)
            } else n.children.forEach { walk(it) }
        }
        walk(root)
        return leaves.maxByOrNull { it.level }
    }

    private suspend fun prerequisites(topic: String, level: Int, seen: MutableSet<String>): List<KnowledgeNode> {
        if (level > 3) return emptyList()
        val names = if (llm != null) {
            try {
                llmPrerequisites(topic)
            } catch (e: Exception) {
                localPrerequisites(topic)
            }
        } else localPrerequisites(topic)
        return names.map { it.trim() }.filter { it.isNotBlank() && seen.add(it) }.take(5).map { name ->
            val mastered = isMastered(name)
            val children = if (!mastered) prerequisites(name, level + 1, seen) else emptyList()
            KnowledgeNode(name, level, mastered, children)
        }
    }

    private suspend fun llmPrerequisites(topic: String): List<String> {
        val client = llm ?: return localPrerequisites(topic)
        val prompt = """
列出掌握「$topic」所需的直接前置知识（最多 5 个，不要展开二级）。
只输出 JSON：{"prerequisites":["前置1","前置2"]}
若是基础概念无前置，输出 {"prerequisites":[]}
""".trimIndent()
        val raw = client.generateResponse(prompt, LlmConfig("nvidia", "", "", ""))
        if (raw.contains("无前置") || raw.contains("基础概念")) return emptyList()
        val s = raw.indexOf('{')
        val e = raw.lastIndexOf('}')
        if (s < 0 || e <= s) throw IllegalArgumentException("无 JSON")
        val o = json.parseToJsonElement(raw.substring(s, e + 1)).jsonObject
        return (o["prerequisites"] as? JsonArray)
            ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
            ?.filter { it.isNotBlank() }
            ?: emptyList()
    }

    /** TechUsage：usedCount ≥1 或状态 ≥ FAMILIAR 视为已掌握。 */
    private fun isMastered(name: String): Boolean =
        techStore.getAllTechUsage().any {
            it.techName.equals(name, ignoreCase = true) &&
                (it.usedCount >= 1 || it.status.ordinal >= com.learnagent.data.TechStatus.FAMILIAR.ordinal)
        }

    private fun localPrerequisites(topic: String): List<String> {
        val key = PREREQUISITES.keys.firstOrNull { topic.contains(it, ignoreCase = true) }
        return key?.let { PREREQUISITES[it].orEmpty() } ?: emptyList()
    }

    private companion object {
        val PREREQUISITES = linkedMapOf(
            "视频生成" to listOf("扩散模型", "深度学习", "PyTorch"),
            "扩散模型" to listOf("深度学习", "概率论", "U-Net"),
            "U-Net" to listOf("卷积神经网络", "残差连接"),
            "深度学习" to listOf("机器学习", "线性代数", "微积分"),
            "机器学习" to listOf("线性代数", "概率论", "Python"),
            "Transformer" to listOf("注意力机制", "深度学习"),
            "注意力机制" to listOf("深度学习", "向量运算"),
            "大模型" to listOf("Transformer", "分布式训练"),
            "RAG" to listOf("大模型", "向量数据库"),
            "分布式锁" to listOf("并发编程", "分布式系统"),
            "分布式系统" to listOf("网络基础", "一致性协议"),
            "一致性协议" to listOf("分布式系统"),
            "微服务" to listOf("分布式系统", "容器化"),
            "Kubernetes" to listOf("容器化", "网络基础")
        )
    }
}
