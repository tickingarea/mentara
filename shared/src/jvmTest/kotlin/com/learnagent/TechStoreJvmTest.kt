package com.learnagent

import com.learnagent.data.CodeSnippet
import com.learnagent.data.Decision
import com.learnagent.data.KnowledgeMastery
import com.learnagent.data.TechCard
import com.learnagent.data.TechCategory
import com.learnagent.data.TechStatus
import com.learnagent.data.TechUsage
import com.learnagent.platform.TechStore
import kotlin.test.Test
import kotlin.test.assertTrue

/** jvm actual TechStore 落盘往返（~/.mentara/...）。 */
class TechStoreJvmTest {

    private val stamp = System.currentTimeMillis()

    @Test
    fun techCardSaveAndRead() {
        val store = TechStore()
        val id = "test_card_$stamp"
        store.saveTechCard(
            TechCard(
                id = id,
                name = "TestZK",
                category = TechCategory.DISTRIBUTED,
                whatItIs = "测试",
                whatItSolves = "",
                whenToUse = "",
                whenNotToUse = "",
                howItWorks = "",
                source = "测试"
            )
        )
        assertTrue(store.getAllTechCards().any { it.id == id && it.name == "TestZK" })
    }

    @Test
    fun allEntitiesSaveAndRead() {
        val store = TechStore()
        store.saveTechUsage(
            TechUsage(techName = "TestTech_$stamp", firstSeenAt = 1L, lastSeenAt = 2L, status = TechStatus.DISCOVERED)
        )
        store.saveDecision(
            Decision(id = "test_decision_$stamp", problem = "p", chosen = "c", reason = "r", createdAt = 1L)
        )
        store.saveCodeSnippet(
            CodeSnippet(id = "test_snippet_$stamp", language = "kotlin", purpose = "p", code = "val x = 1")
        )
        store.saveMastery(KnowledgeMastery(pointId = "test_point_$stamp"))

        assertTrue(store.getAllTechUsage().any { it.techName == "TestTech_$stamp" })
        assertTrue(store.getAllDecisions().any { it.id == "test_decision_$stamp" })
        assertTrue(store.getAllCodeSnippets().any { it.id == "test_snippet_$stamp" })
        assertTrue(store.getAllMastery().any { it.pointId == "test_point_$stamp" })
    }
}
