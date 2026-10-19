package com.learnagent.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * 云端语音转写（OpenAI 兼容 POST /audio/transcriptions，multipart 上传）。
 * 兼容：SiliconFlow（FunAudioLLM/SenseVoiceSmall）、OpenAI whisper-1、任何同协议服务。
 * 手机端只负责传音频文件，不再本地加载模型。
 */
class CloudAsrClient(
    private val apiKey: String,
    baseUrl: String = "https://api.siliconflow.cn/v1",
    private val model: String = "TeleAI/TeleSpeechASR"
) {
    private val base = baseUrl.trim().trimEnd('/')

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    /** text = 整段文本；segments = (start秒, end秒, 文本)（服务返回 verbose_json 时才有）。 */
    data class Result(val text: String, val segments: List<Triple<Double, Double, String>>)

    suspend fun transcribeFile(wav: File): Result = withContext(Dispatchers.IO) {
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("model", model)
            .addFormDataPart("language", "zh")
            .addFormDataPart("response_format", "json")
            .addFormDataPart("file", wav.name, wav.asRequestBody("audio/wav".toMediaType()))
            .build()
        val req = Request.Builder()
            .url("$base/audio/transcriptions")
            .header("Authorization", "Bearer $apiKey")
            .post(body)
            .build()
        client.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw IllegalStateException("ASR HTTP ${resp.code}：${text.take(200)}")
            val root = runCatching { Json.parseToJsonElement(text).jsonObject }.getOrNull()
                ?: return@withContext Result(text.trim(), emptyList())
            val full = root["text"]?.jsonPrimitive?.contentOrNull.orEmpty().trim()
            val segs = root["segments"]?.jsonArray?.mapNotNull { e ->
                runCatching {
                    val o = e.jsonObject
                    Triple(
                        o["start"]!!.jsonPrimitive.content.toDouble(),
                        o["end"]!!.jsonPrimitive.content.toDouble(),
                        o["text"]!!.jsonPrimitive.content.trim()
                    )
                }.getOrNull()
            }.orEmpty()
            Result(full, segs)
        }
    }
}
