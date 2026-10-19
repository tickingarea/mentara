package com.learnagent.core

import com.learnagent.data.LearningUnit
import com.learnagent.ui.LayoutBreakpoints
import com.learnagent.ui.LayoutMode
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 平台补强 A 批：BM25 精确分数 / 同义词扩展 / 时间窗融合 / 断点。 */
class Bm25IndexerTest {

    /** 手工计算 BM25 断言（k1=1.2, b=0.75）。 */
    @Test
    fun testBm25Score() {
        val docs = listOf(
            IndexedDoc("d1", listOf("分布式", "锁", "redlock")),
            IndexedDoc("d2", listOf("分布式", "缓存")),
            IndexedDoc("d3", listOf("缓存", "redis"))
        )
        val indexer = Bm25Indexer(k1 = 1.2f, b = 0.75f)
        indexer.index(docs)

        // 手工计算：
        // N=3, df("分布式")=2, avgdl=(3+2+2)/3=2.3333, |d1|=3
        // idf = ln((3-2+0.5)/(2+0.5)+1) = ln(1.6) ≈ 0.470004
        // denom = 1 + 1.2*(1-0.75+0.75*3/2.3333) = 1 + 1.2*(0.25+0.964286) = 2.457143
        // score = 0.470004 * (1*2.2)/2.457143 ≈ 0.420825
        val expected = 0.470004f * (1f * 2.2f) / 2.457143f
        val actual = indexer.scoreTokens(listOf("分布式"), "d1")
        assertTrue(abs(expected - actual) < 1e-3f, "BM25 分数应一致：expected=$expected actual=$actual")

        // 不含查询词的文档得 0
        assertEquals(0f, indexer.scoreTokens(listOf("分布式"), "d3"))
    }

    /** 搜"分布式锁"能命中含 "redlock" 的文档（同义词扩展）。 */
    @Test
    fun testSynonymExpansion() {
        val docs = listOf(
            IndexedDoc("d1", listOf("caching", "redis")),
            IndexedDoc("d2", listOf("redlock", "zookeeper")),
            IndexedDoc("d3", listOf("sorting", "algorithm"))
        )
        val indexer = Bm25Indexer()
        indexer.index(docs)
        val hits = indexer.search("分布式锁", topK = 3)
        assertTrue(hits.isNotEmpty(), "同义词扩展后应有命中")
        assertEquals("d2", hits.first().docId, "应命中含 redlock 的文档")
        assertTrue(TECH_SYNONYMS.size >= 20, "同义词表至少 20 组，实际=${TECH_SYNONYMS.size}")
    }

    /** 时间窗：命中片段的 before/after 不为空。 */
    @Test
    fun testTimeWindow() {
        val units = listOf(
            unit("u0", 0.0, "第一段"),
            unit("u1", 10.0, "分布式锁使用 redlock"),
            unit("u2", 20.0, "第三段")
        )
        val hits = listOf(ScoredDoc("u1", 1.5f))
        val fused = TimeWindowRetriever(units).fuse(hits)
        assertEquals(1, fused.size)
        assertTrue(fused.first().before != null, "before 应不为空")
        assertTrue(fused.first().after != null, "after 应不为空")
        assertEquals("u0", fused.first().before?.id)
        assertEquals("u2", fused.first().after?.id)
    }

    private fun unit(id: String, start: Double, speech: String) = LearningUnit(
        id = id, videoId = "v1", start = start, end = start + 10.0, speech = speech
    )

    // ---------- 断点 ----------

    @Test
    fun testLayoutBreakpoints() {
        assertEquals(LayoutMode.PHONE, LayoutBreakpoints.forWidth(412))
        assertEquals(LayoutMode.PHONE, LayoutBreakpoints.forWidth(599))
        assertEquals(LayoutMode.TABLET_COMPACT, LayoutBreakpoints.forWidth(600))
        assertEquals(LayoutMode.TABLET_COMPACT, LayoutBreakpoints.forWidth(899))
        assertEquals(LayoutMode.TABLET_FULL, LayoutBreakpoints.forWidth(900))
        assertEquals(LayoutMode.TABLET_FULL, LayoutBreakpoints.forWidth(1200))
    }
}
