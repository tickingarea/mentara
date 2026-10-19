package com.learnagent.llm

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
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
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * 视觉描述（VLM）：用 NIM 的视觉模型看关键帧，生成"画面里能看到什么"的一句客观描述。
 * 图像会先缩到 ≤512px 再 base64 上传，控制流量与时延。
 */
class VisionCaptioner(
    private val apiKey: String,
    private val model: String = "meta/llama-3.2-11b-vision-instruct"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /** 看图改字：让视觉模型逐字核对原图中的真实文字，返回正确文本（供 OCR 校对）。 */
    suspend fun correctOcr(imageFile: File, ocrText: String): String = withContext(Dispatchers.IO) {
        if (!imageFile.isFile) return@withContext ""
        val bmp = BitmapFactory.decodeFile(imageFile.absolutePath) ?: return@withContext ""
        val scale = (768f / bmp.width).coerceAtMost(1f)
        val scaled = if (scale < 1f) Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true) else bmp
        val bos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, bos)
        if (scaled !== bmp) scaled.recycle()
        bmp.recycle()
        val b64 = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP)
        val payload = buildJsonObject {
            put("model", model)
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "user")
                    putJsonArray("content") {
                        addJsonObject {
                            put("type", "text")
                            put("text", "这是教学视频的截图。下面是从图中自动识别的文字（可能有错别字）：\n「" + ocrText.take(400) + "」\n请逐字核对图片中真实显示的文字，输出正确的文字。只输出文字本身，不要解释、不要引号；若图中没有文字、或文字是乱码/播放器残留（如 TRACE、进度条字样），输出：无")
                        }
                        addJsonObject {
                            put("type", "image_url")
                            put("image_url", buildJsonObject { put("url", "data:image/jpeg;base64,$b64") })
                        }
                    }
                }
            }
            put("temperature", 0.1)
            put("max_tokens", 500)
            put("stream", false)
        }.toString()
        val req = Request.Builder()
            .url("https://integrate.api.nvidia.com/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("VLM HTTP ${resp.code}")
            val root = Json.parseToJsonElement(resp.body?.string().orEmpty()).jsonObject
            val out = root["choices"]?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("message")?.jsonObject?.get("content")
                ?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            if (out == "无" || out == "（无）" || out.isEmpty()) "" else out
        }
    }
    suspend fun caption(imageFile: File): String = withContext(Dispatchers.IO) {
        if (!imageFile.isFile) return@withContext ""
        val bmp = BitmapFactory.decodeFile(imageFile.absolutePath) ?: return@withContext ""
        val scale = (512f / bmp.width).coerceAtMost(1f)
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true)
        } else bmp
        val bos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 70, bos)
        if (scaled !== bmp) scaled.recycle()
        bmp.recycle()
        val b64 = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP)

        val payload = buildJsonObject {
            put("model", model)
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "user")
                    putJsonArray("content") {
                        addJsonObject {
                            put("type", "text")
                            put("text", "用一句话（30字以内）客观描述这个教学视频画面里能看到什么（人物/板书/图表/演示）。只描述可见内容，不要推测。")
                        }
                        addJsonObject {
                            put("type", "image_url")
                            put("image_url", buildJsonObject { put("url", "data:image/jpeg;base64,$b64") })
                        }
                    }
                }
            }
            put("temperature", 0.2)
            put("max_tokens", 160)
            put("stream", false)
        }.toString()

        val req = Request.Builder()
            .url("https://integrate.api.nvidia.com/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("VLM HTTP ${resp.code}")
            val root = Json.parseToJsonElement(resp.body?.string().orEmpty()).jsonObject
            root["choices"]?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("message")?.jsonObject?.get("content")
                ?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        }
    }
}
