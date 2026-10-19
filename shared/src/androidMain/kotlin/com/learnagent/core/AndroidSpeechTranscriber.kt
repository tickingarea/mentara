package com.learnagent.core

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.learnagent.data.TranscriptSegment
import java.io.File

/**
 * Android 骞冲彴鐨勮闊宠瘑鍒櫒銆? *
 * 浣跨敤绯荤粺鍐呯疆鐨?[SpeechRecognizer]锛堟棤闇€涓嬭浇妯″瀷锛屾敮鎸佺绾夸紭鍏堬級銆? * - API 31+ 鍙敤 [RecognizerIntent.EXTRA_AUDIO_SOURCE] 璇嗗埆 WAV 鏂囦欢锛? * - 浣庣増鏈洖閫€鍒伴害鍏嬮杈撳叆銆? *
 * 绯荤粺璇嗗埆鍣ㄤ笉杩斿洖閫愯瘝鏃堕棿鎴筹紝鍥犳姣忔浠ユ暣鍙ュ舰寮忚繑鍥炪€? * 閲囩敤鍥炶皟寮?API锛岄伩鍏嶄负 shared 妯″潡寮曞叆鍗忕▼渚濊禆銆? */
class AndroidSpeechTranscriber(private val context: Context) {

    companion object {
        /** 最近一次识别失败原因（空=无错误），供上层显示。 */
        var lastError: String = ""

        fun messageOf(error: Int): String = when (error) {
            1 -> "网络超时"
            2 -> "网络错误（需联网或装离线语音包）"
            3 -> "音频录制错误"
            4 -> "无匹配结果"
            5 -> "识别器客户端错误"
            6 -> "语音超时，未检测到说话"
            7 -> "无匹配结果（可能没有说话/音质差）"
            8 -> "识别器忙"
            9 -> "缺少麦克风权限（RECORD_AUDIO）"
            else -> "识别失败(code=$error)"
        }
    }

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    /**
     * 璇嗗埆涓€涓?WAV 闊抽鏂囦欢锛?6kHz 鍗曞０閬撴晥鏋滄渶浣筹級銆?     *
     * 闇€鍦ㄥ叿澶?RECORD_AUDIO 鏉冮檺鏃惰皟鐢紱API 31+ 涓旀彁渚涙枃浠舵椂浼樺厛绂荤嚎璇嗗埆璇ユ枃浠躲€?     * 缁撴灉閫氳繃 [onResult] 鍥炶皟杩斿洖锛堥敊璇垨鏃犵粨鏋滄椂杩斿洖绌哄垪琛級銆?     */
    @Suppress("MissingPermission")
    fun transcribe(wavPath: String, onResult: (List<TranscriptSegment>) -> Unit) {
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val file = File(wavPath)
                if (file.isFile) {
                    val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, pfd)
                }
            }
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val texts = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    .orEmpty()
                val primary = texts.firstOrNull().orEmpty()
                val segments = if (primary.isBlank()) {
                    lastError = "识别器返回空结果（可能无网或未装离线语音包）"
                    emptyList()
                } else {
                    lastError = ""
                    listOf(TranscriptSegment(start = 0.0, end = 0.0, text = primary, speaker = null))
                }
                runCatching { recognizer.destroy() }
                onResult(segments)
            }

            override fun onError(error: Int) {
                lastError = messageOf(error)
                runCatching { recognizer.destroy() }
                onResult(emptyList())
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        recognizer.startListening(intent)
    }
}
