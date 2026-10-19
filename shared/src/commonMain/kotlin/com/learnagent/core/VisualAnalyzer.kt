package com.learnagent.core

/**
 * 画面分析：有 LLM → 生成场景描述；无 LLM → 直接返回 OCR 文字。
 */
class VisualAnalyzer(private val llm: LlmClient? = null) {

    suspend fun describe(framePath: String, ocrText: String): String {
        val client = llm ?: return ocrText
        if (ocrText.isBlank() && framePath.isBlank()) return ""
        return try {
            client.generateResponse(
                "用一句中文描述这个视频画面（已有 OCR 文字可参考，不要编造）：$ocrText",
                LlmConfig("nvidia", "", "", "")
            ).trim().ifBlank { ocrText }
        } catch (e: Exception) {
            ocrText
        }
    }
}
