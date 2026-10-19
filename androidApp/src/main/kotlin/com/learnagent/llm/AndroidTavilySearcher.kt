package com.learnagent.llm

import com.learnagent.core.WebResult
import com.learnagent.core.WebSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/** Tavily 联网搜索（专家模式用）。 */
class AndroidTavilySearcher(private val apiKey: String) : WebSearch {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun search(query: String): List<WebResult> = withContext(Dispatchers.IO) {
        val body = buildJsonObject {
            put("api_key", apiKey)
            put("query", query)
            put("max_results", 5)
            put("search_depth", "advanced")
        }.toString()
        val req = Request.Builder()
            .url("https://api.tavily.com/search")
            .header("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("Tavily HTTP ${resp.code}")
            val root = Json.parseToJsonElement(resp.body?.string().orEmpty()).jsonObject
            root["results"]?.jsonArray?.map { e ->
                val o = e.jsonObject
                WebResult(
                    title = o["title"]?.jsonPrimitive?.contentOrNull ?: "",
                    url = o["url"]?.jsonPrimitive?.contentOrNull ?: "",
                    content = o["content"]?.jsonPrimitive?.contentOrNull ?: ""
                )
            }.orEmpty()
        }
    }
}
