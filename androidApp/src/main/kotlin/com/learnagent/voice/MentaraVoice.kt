package com.learnagent.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/**
 * Mentara 的"少女音"语音播报：自动挑系统 TTS 里最"少女感"的中文女声朗读回答。
 * 诚实说明：音色由手机自带 TTS 引擎决定——想要"清纯萝莉/游戏爱丽丝"那种音色，
 * 需在系统设置→TTS→下载对应语音包（华为/小米/讯飞都有少女音包）；App 会自动优先选中带
 * 少女/女声/小萝莉标记的中文语音。
 */
object MentaraVoice {

    private var tts: TextToSpeech? = null

    var enabled: Boolean by mutableStateOf(false)
        private set
    var status: String by mutableStateOf("")
        private set
    var voiceName: String by mutableStateOf("")
    var speaking: Boolean by mutableStateOf(false)
        private set

    fun init(context: Context) {
        if (tts != null) return
        val prefs = context.getSharedPreferences("mentara", Context.MODE_PRIVATE)
        enabled = prefs.getBoolean("voiceSpeak", true)
        tts = TextToSpeech(context.applicationContext) { st ->
            if (st == TextToSpeech.SUCCESS) {
                pickVoice()
                runCatching {
                    tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) { speaking = true }
                        override fun onDone(utteranceId: String?) { speaking = false }
                        override fun onError(utteranceId: String?) { speaking = false }
                    })
                }
                status = if (voiceName.isNotBlank()) "已就绪 · $voiceName" else "已就绪（系统默认中文音）"
            } else {
                status = "TTS 初始化失败（系统无 TTS 引擎？）"
            }
        }
    }

    /** 自动挑"少女感"中文女声：少女/小萝莉/晓晓/女声/XiaoYan 类优先。 */
    private fun pickVoice() {
        val t = tts ?: return
        runCatching {
            val markers = listOf("少女", "萝莉", "女声", "晓晓", "小燕", "xiaoyan", "xiaoxiao", "female", "young", "child", "alice")
            val zh = t.voices.orEmpty().filter { it.locale.language.equals("zh", true) }
            val picked = zh.firstOrNull { v -> markers.any { v.name.lowercase().contains(it) } }
            t.language = Locale.SIMPLIFIED_CHINESE
            if (picked != null) {
                t.voice = picked
                voiceName = picked.name
            } else {
                voiceName = zh.firstOrNull()?.name ?: ""
            }
        }
    }

    /** 朗读一段文字（自动去掉时间戳/markdown 噪音，最长 500 字）。 */
    fun speak(text: String) {
        if (!enabled) return
        val t = tts ?: return
        val clean = text
            .replace(Regex("\\[[0-9]+:[0-9]+\\]"), " ")
            .replace(Regex("[#*`>_·|]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(500)
        if (clean.isBlank()) return
        speaking = true
        runCatching { t.speak(clean, TextToSpeech.QUEUE_ADD, null, "mentara") }
    }

    /** 立即停止播报（供"停止播报"按钮/点击打断调用）。 */
    fun stop() {
        speaking = false
        runCatching { tts?.stop() }
    }

    fun setEnabled(context: Context, value: Boolean) {
        enabled = value
        context.getSharedPreferences("mentara", Context.MODE_PRIVATE)
            .edit().putBoolean("voiceSpeak", value).apply()
        if (!value) runCatching { tts?.stop() }
    }
}
