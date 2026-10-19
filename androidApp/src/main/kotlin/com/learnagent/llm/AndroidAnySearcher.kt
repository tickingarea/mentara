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
 * AnySearch 联网搜索（中国团队；个人注册每日 1000 次免费；不填 Key 也可匿名访问，限流较低）。
 * 失败/无结果时自动回落到免费必应（AndroidBingSearcher），保证"总有免费联网"。
 */
class AndroidAnySearcher(private val apiKey: String) : WebSearch {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val bing by lazy { AndroidBingSearcher() }

    override suspend fun search(query: String): List<WebResult> = withContext(Dispatchers.IO) {
        val primary = runCatching { anysearch(query) }.getOrDefault(emptyList())
        if (primary.isNotEmpty()) primary
        else runCatching { bing.search(query) }.getOrDefault(emptyList())
    }

    private fun anysearch(query: String): List<WebResult> {
        val payload = buildJsonObject {
            put("query", query)
            put("max_results", 5)
            put("language", "zh")
        }.toString()
        val rb = Request.Builder()
            .url("https://api.anysearch.com/v1/search")
            .header("Content-Type", "application/json")
            .header("X-Anysearch-Client", "Mentara/1.0 (Android)")
        if (apiKey.isNotBlank()) rb.header("Authorization", "Bearer $apiKey")
        val req = rb.post(payload.toRequestBody("application/json".toMediaType())).build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("AnySearch HTTP ${resp.code}")
            val root = Json.parseToJsonElement(resp.body?.string().orEmpty()).jsonObject
            val results = root["data"]?.jsonObject?.get("results")?.jsonArray ?: return emptyList()
            return results.mapNotNull { e ->
                runCatching {
                    val o = e.jsonObject
                    WebResult(
                        title = o["title"]?.jsonPrimitive?.contentOrNull ?: "",
                        url = o["url"]?.jsonPrimitive?.contentOrNull ?: "",
                        content = o["content"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                            ?: o["snippet"]?.jsonPrimitive?.contentOrNull ?: ""
                    )
                }.getOrNull()
            }
        }
    }

    companion object {
        private val regClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
        }

        /**
         * 一键注册（仅需真实邮箱，无需验证码）——官方设计的 Agent 注册流程。
         * 成功返回 api_key（as_sk_...）；失败 message 透传官方错误（如 email_already_registered / Rate limited）。
         */
        suspend fun register(email: String): Result<String> = withContext(Dispatchers.IO) {
            runCatching {
                val payload = buildJsonObject { put("email", email) }.toString()
                val req = Request.Builder()
                    .url("https://api.anysearch.com/v1/auth/email/register")
                    .header("Content-Type", "application/json")
                    .header("X-Anysearch-Client", "Mentara/1.0 (Android)")
                    .post(payload.toRequestBody("application/json".toMediaType()))
                    .build()
                regClient.newCall(req).execute().use { resp ->
                    val bodyStr = resp.body?.string().orEmpty()
                    val root = runCatching { Json.parseToJsonElement(bodyStr).jsonObject }.getOrNull()
                    val data = root?.get("data")?.jsonObject
                    val code = root?.get("code")?.jsonPrimitive?.contentOrNull
                    val msg = root?.get("message")?.jsonPrimitive?.contentOrNull ?: ("HTTP ${resp.code}")
                    val retry = data?.get("retry_after_seconds")?.jsonPrimitive?.contentOrNull
                    if (!resp.isSuccessful && root == null) throw IllegalStateException("HTTP ${resp.code}")
                    if (code != "0") {
                        val extra = if (retry != null) "（约 $retry 秒后重试）" else ""
                        throw IllegalStateException(msg + extra)
                    }
                    val key = data?.get("api_key")?.jsonObject?.get("key")?.jsonPrimitive?.contentOrNull
                        ?: throw IllegalStateException("未返回 Key，请到 anysearch.com/console/api-keys 手动获取")
                    key
                }
            }
        }
    }
}
