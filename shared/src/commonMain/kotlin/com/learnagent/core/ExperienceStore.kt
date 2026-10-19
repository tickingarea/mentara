package com.learnagent.core

import com.learnagent.data.Experience
import com.learnagent.data.Scenario

/**
 * 经验存储（expect）：场景与经验的新增/检索/待复习。
 * jvm → ~/.mentara/experience/ ；android → context.filesDir/experience/。
 */
expect object ExperienceStore {
    fun saveScenario(scenario: Scenario)
    fun saveExperience(experience: Experience)
    fun getAllScenarios(): List<Scenario>
    fun getAllExperiences(): List<Experience>
    fun searchExperiences(query: String): List<Experience>
    fun getPendingFollowUps(nowMillis: Long): List<Experience>
}
