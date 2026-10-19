package com.learnagent.data

import com.learnagent.core.Indexer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IndexerTest {

    private fun sampleItems() = listOf(
        TimelineItem(start = 0.0, end = 10.0, speech = "梯度下降是优化算法", screenText = "梯度下降", framePath = ""),
        TimelineItem(start = 10.0, end = 20.0, speech = "反向传播计算梯度", screenText = "反向传播", framePath = ""),
        TimelineItem(start = 20.0, end = 30.0, speech = "卷积神经网络处理图像", screenText = "CNN", framePath = "")
    )

    @Test
    fun testIndexAndSearch() {
        val idx = Indexer()
        idx.index(sampleItems())
        val results = idx.search("梯度", topK = 5)
        assertTrue(results.isNotEmpty())
        assertEquals(10.0, results[0].start)
    }

    @Test
    fun testSearchNotFound() {
        val idx = Indexer()
        idx.index(sampleItems())
        val results = idx.search("量子力学", topK = 5)
        assertEquals(0, results.size)
    }

    @Test
    fun testSearchMultiKeyword() {
        val idx = Indexer()
        idx.index(sampleItems())
        val results = idx.search("梯度 反向传播", topK = 5)
        assertTrue(results.size >= 1)
    }

    @Test
    fun testClear() {
        val idx = Indexer()
        idx.index(sampleItems())
        idx.clear()
        val results = idx.search("梯度", topK = 5)
        assertEquals(0, results.size)
    }
}