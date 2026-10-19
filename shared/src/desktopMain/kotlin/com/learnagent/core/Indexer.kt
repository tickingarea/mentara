package com.learnagent.core

import com.learnagent.data.TimelineItem

actual class Indexer {
    
    actual suspend fun index(items: List<TimelineItem>) {
        // TODO: 实现桌面平台的索引建立
    }
    
    actual suspend fun search(query: String, topK: Int): List<TimelineItem> {
        // TODO: 实现桌面平台的搜索功能
        return emptyList()
    }
}