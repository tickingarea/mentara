package com.learnagent.llm

import com.learnagent.core.LlmClient
import com.learnagent.core.LlmConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * NVIDIA NIM（build.nvidia.com）的大模型客户端，BYOK。
 *
 * 调用 `{baseUrl}/chat/completions`，默认模型为 `meta/llama-3.1-8b-instruct`。
 */
class NvidiaLlmClient(
    private val apiKey: String,
    private val baseUrl: String = "https://integrate.api.nvidia.com/v1",
    private val model: String = "deepseek-ai/deepseek-v4.1-flash"
) : LlmClient {

    private val http: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(20))
        .build()

    override suspend fun generateResponse(prompt: String, config: LlmConfig): String {
        val payload = buildJsonObject {
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
            put("max_tokens", config.maxTokens)
            put("stream", false)
        }.toString()

        val request = HttpRequest.newBuilder(URI.create("$baseUrl/chat/completions"))
            .timeout(Duration.ofSeconds(120))
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build()

        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw IllegalStateException("NVIDIA NIM 返回 HTTP ${response.statusCode()}: ${response.body().take(300)}")
        }

        val root = Json.parseToJsonElement(response.body()).jsonObject
        val message = root["choices"]?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("message")
            ?.jsonObject
        val content = message?.get("content")?.jsonPrimitive?.contentOrNull
        val reasoning = message?.get("reasoning_content")?.jsonPrimitive?.contentOrNull
        return (content?.takeIf { it.isNotBlank() } ?: reasoning ?: "").trim()
    }
}
