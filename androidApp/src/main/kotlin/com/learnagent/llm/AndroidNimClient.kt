package com.learnagent.llm

import com.learnagent.core.LlmClient
import com.learnagent.core.LlmConfig
import com.learnagent.core.StreamingLlmClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * NVIDIA NIM 客户端（Android，用项目已有的 OkHttp，不引入新依赖）。
 * 支持：system 提示词、temperature / max_tokens、流式（SSE）+ reasoning_content。
 */
class AndroidNimClient(
    private val apiKey: String,
    private val model: String,
    private val baseUrl: String = "https://integrate.api.nvidia.com/v1"
) : LlmClient, StreamingLlmClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .build()

    private fun payload(config: LlmConfig, prompt: String, stream: Boolean, maxTokensOverride: Int? = null): String =
        buildJsonObject {
            put("model", model)
            putJsonArray("messages") {
                if (config.system.isNotBlank()) {
                    addJsonObject {
                        put("role", "system")
                        put("content", config.system)
                    }
                }
                addJsonObject {
                    put("role", "user")
                    put("content", prompt)
                }
            }
            put("temperature", config.temperature)
            put("max_tokens", maxTokensOverride ?: config.maxTokens)
            put("stream", stream)
        }.toString()

    private fun request(prompt: String, config: LlmConfig, stream: Boolean, maxTokensOverride: Int? = null): Request =
        Request.Builder()
            .url("$baseUrl/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .header("Accept", if (stream) "text/event-stream" else "application/json")
            .post(payload(config, prompt, stream, maxTokensOverride).toRequestBody("application/json".toMediaType()))
            .build()

    override suspend fun generateResponse(prompt: String, config: LlmConfig): String =
        withContext(Dispatchers.IO) {
            val t0 = System.currentTimeMillis()
            android.util.Log.d("MENTARA", "NIM 请求开始：model=$model prompt=${prompt.length} 字符（读超时 180s）")
            client.newCall(request(prompt, config, stream = false)).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("NIM HTTP ${response.code}")
                }
                val body = response.body?.string().orEmpty()
                val root = Json.parseToJsonElement(body).jsonObject
                val message = root["choices"]?.jsonArray?.firstOrNull()
                    ?.jsonObject?.get("message")?.jsonObject
                val content = message?.get("content")?.jsonPrimitive?.contentOrNull
                val reasoning = message?.get("reasoning_content")?.jsonPrimitive?.contentOrNull
                val result = (content?.takeIf { it.isNotBlank() } ?: reasoning ?: "").trim()
                android.util.Log.d("MENTARA", "NIM 请求结束：${System.currentTimeMillis() - t0}ms，返回 ${result.length} 字符")
                result
            }
        }

    override suspend fun streamResponse(
        prompt: String,
        config: LlmConfig,
        onToken: (String) -> Unit,
        onReasoning: (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val sb = StringBuilder()
        client.newCall(request(prompt, config, stream = true)).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("NIM HTTP ${response.code}")
            val reader = response.body?.charStream()?.buffered() ?: return@use
            var line = reader.readLine()
            while (line != null) {
                val l = line.trim()
                if (l.startsWith("data:")) {
                    val data = l.removePrefix("data:").trim()
                    if (data.isNotEmpty() && data != "[DONE]") {
                        runCatching {
                            val delta = Json.parseToJsonElement(data).jsonObject["choices"]
                                ?.jsonArray?.firstOrNull()?.jsonObject?.get("delta")?.jsonObject
                            val c = delta?.get("content")?.jsonPrimitive?.contentOrNull
                            val r = delta?.get("reasoning_content")?.jsonPrimitive?.contentOrNull
                            if (!r.isNullOrBlank()) onReasoning(r)
                            if (!c.isNullOrBlank()) {
                                sb.append(c)
                                onToken(c)
                            }
                        }
                    }
                }
                line = reader.readLine()
            }
        }
        sb.toString()
    }
}
