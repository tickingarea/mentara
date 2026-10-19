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

/**
 * 博查（Bocha）联网搜索：国内直连、无需翻墙，专家模式优先用它（Tavily 作海外备用）。
 * 文档：https://open.bochaai.com
 */
class AndroidBochaSearcher(private val apiKey: String) : WebSearch {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun search(query: String): List<WebResult> = withContext(Dispatchers.IO) {
        val payload = buildJsonObject {
            put("query", query)
            put("summary", true)
            put("count", 5)
        }.toString()
        val req = Request.Builder()
            .url("https://api.bochaai.com/v1/web-search")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("Bocha HTTP ${resp.code}")
            val root = Json.parseToJsonElement(resp.body?.string().orEmpty()).jsonObject
            val pages = root["data"]?.jsonObject
                ?.get("webPages")?.jsonObject
                ?.get("value")?.jsonArray
                ?: return@use emptyList()
            pages.mapNotNull { e ->
                runCatching {
                    val o = e.jsonObject
                    WebResult(
                        title = o["name"]?.jsonPrimitive?.contentOrNull ?: "",
                        url = o["url"]?.jsonPrimitive?.contentOrNull ?: "",
                        content = o["summary"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                            ?: o["snippet"]?.jsonPrimitive?.contentOrNull ?: ""
                    )
                }.getOrNull()
            }
        }
    }
}
