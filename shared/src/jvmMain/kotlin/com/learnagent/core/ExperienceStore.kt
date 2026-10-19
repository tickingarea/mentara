package com.learnagent.core

import com.learnagent.data.Experience
import com.learnagent.data.Scenario
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

/** 桌面实现：~/.mentara/experience/。 */
actual object ExperienceStore {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; isLenient = true }

    private val dir: File
        get() = File(System.getProperty("user.home"), ".mentara/experience").apply { mkdirs() }

    private val scenariosFile: File get() = File(dir, "scenarios.json")
    private val experiencesFile: File get() = File(dir, "experiences.json")

    private fun readScenarios(): MutableList<Scenario> = try {
        if (scenariosFile.isFile) {
            json.decodeFromString(ListSerializer(Scenario.serializer()), scenariosFile.readText()).toMutableList()
        } else mutableListOf()
    } catch (e: Exception) {
        mutableListOf()
    }

    private fun readExperiences(): MutableList<Experience> = try {
        if (experiencesFile.isFile) {
            json.decodeFromString(ListSerializer(Experience.serializer()), experiencesFile.readText()).toMutableList()
        } else mutableListOf()
    } catch (e: Exception) {
        mutableListOf()
    }

    actual fun saveScenario(scenario: Scenario) {
        val list = readScenarios()
        list.removeAll { it.id == scenario.id }
        list.add(scenario)
        scenariosFile.writeText(json.encodeToString(ListSerializer(Scenario.serializer()), list))
    }

    actual fun saveExperience(experience: Experience) {
        val list = readExperiences()
        list.removeAll { it.id == experience.id }
        list.add(experience)
        experiencesFile.writeText(json.encodeToString(ListSerializer(Experience.serializer()), list))
    }

    actual fun getAllScenarios(): List<Scenario> = readScenarios()

    actual fun getAllExperiences(): List<Experience> = readExperiences()

    actual fun searchExperiences(query: String): List<Experience> {
        if (query.isBlank()) return emptyList()
        val q = query.lowercase()
        return readExperiences().filter {
            it.choice.lowercase().contains(q) || it.result.lowercase().contains(q) || it.lesson.lowercase().contains(q)
        }
    }

    actual fun getPendingFollowUps(nowMillis: Long): List<Experience> =
        readExperiences().filter { !it.followUpDone && it.followUpAt in 1..nowMillis }
}
