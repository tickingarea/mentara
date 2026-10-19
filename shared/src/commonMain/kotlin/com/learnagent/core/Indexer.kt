package com.learnagent.core

import com.learnagent.data.TimelineItem
import com.learnagent.data.LearningUnit
import com.learnagent.data.SemanticType

/**
 * 时间线索引器
 * 
 * 用于快速检索和查询时间线中的内容
 * 当前实现基于关键词匹配，后续会被向量检索替换
 */
class Indexer {
    
    private var indexedItems: List<TimelineItem> = emptyList()
    private var units: List<LearningUnit> = emptyList()
    
    /**
     * 索引时间线条目
     */
    fun index(items: List<TimelineItem>) {
        this.indexedItems = items
    }
    
    /**
     * 搜索关键词
     * 
     * @param query 搜索关键词
     * @param topK 返回最相关的 topK 条结果
     * @return 按相关性排序的 TimelineItem 列表
     */
    fun search(query: String, topK: Int = 5): List<TimelineItem> {
        if (query.isBlank()) {
            return emptyList()
        }
        
        if (indexedItems.isEmpty()) {
            return emptyList()
        }
        
        // 分词
        val keywords = tokenize(query)
        if (keywords.isEmpty()) {
            return emptyList()
        }
        
        // 计算每个条目的相关性分数
        val scoredItems = indexedItems.map { item ->
            val score = calculateScore(item, keywords)
            item to score
        }
        
        // 按分数降序排序，分数相同时按开始时间降序排序（最新的在前）
        val sortedItems = scoredItems
            .filter { it.second > 0 }
            .sortedByDescending { item -> item.second.toDouble() + item.first.start / 1000.0 }
            .take(topK)
            .map { it.first }
        
        return sortedItems
    }
    
    /**
     * 清空索引
     */
    fun clear() {
        this.indexedItems = emptyList()
        this.units = emptyList()
    }

    // ---------- LearningUnit 扩展（阶段 A） ----------

    /** 索引学习单元。 */
    fun indexUnits(items: List<LearningUnit>) {
        this.units = items
    }

    /** 按语义类型检索。 */
    fun searchBySemanticType(type: SemanticType, topK: Int = 5): List<LearningUnit> =
        units.filter { it.semanticType == type }.take(topK)

    /** 按重要度检索（>= minImportance，降序）。 */
    fun searchByImportance(minImportance: Int, topK: Int = 5): List<LearningUnit> =
        units.filter { it.importance >= minImportance }
            .sortedByDescending { it.importance }
            .take(topK)

    /** 在学习单元中做关键词检索（覆盖 speech/screenText/oneLineSummary/keywords）。 */
    fun searchUnits(query: String, topK: Int = 5): List<LearningUnit> {
        if (query.isBlank() || units.isEmpty()) return emptyList()
        val keywords = tokenize(query)
        if (keywords.isEmpty()) return emptyList()
        return units
            .map { unit ->
                val content = (unit.speech + " " + unit.screenText + " " + unit.oneLineSummary + " " +
                    unit.keywords.joinToString(" ")).lowercase()
                unit to keywords.count { content.contains(it) }
            }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(topK)
            .map { it.first }
    }
    
    /**
     * 分词
     * 按空格和标点符号切分
     */
    private fun tokenize(text: String): List<String> {
        val punctuation = charArrayOf(' ', '.', ',', '，', '。', '！', '？', '；', ':', '、', '"', '\'', '`')
        var cleaned = text.lowercase()
        
        // 替换标点符号为空格
        for (punct in punctuation) {
            cleaned = cleaned.replace(punct, ' ')
        }
        
        // 分割并过滤空字符串
        return cleaned
            .split("\\s+".toRegex())
            .filter { it.isNotBlank() }
    }
    
    /**
     * 计算相关性分数
     * 
     * @param item 时间线条目
     * @param keywords 关键词列表
     * @return 命中关键词数量
     */
    private fun calculateScore(item: TimelineItem, keywords: List<String>): Int {
        val content = "${item.speech} ${item.screenText}".lowercase()
        var score = 0
        
        for (keyword in keywords) {
            if (content.contains(keyword)) {
                score++
            }
        }
        
        return score
    }
}