package com.learnagent.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 模型下载宿主（独立于 TaskHost）：
 *  - 下载 Whisper 模型不再占用全局单任务槽 → 不会被"解析/画面描述"等长任务卡死；
 *  - 应用级作用域 → 切换页面不中断；
 *  - 自带进度/阶段/结果状态，设置页实时显示。
 */
object ModelHost {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null

    var busy: Boolean by mutableStateOf(false)
        private set
    var stage: String by mutableStateOf("")
        private set
    var progress: Float by mutableStateOf(-1f)
        private set
    var message: String? by mutableStateOf(null)
        private set

    fun cancel() {
        job?.cancel()
        job = null
        busy = false
        stage = "已取消（已下载部分不会浪费，重新下载会校验后替换）"
    }

    /** 下载 SenseVoice 模型（228MB，hf-mirror）。 */
    fun downloadSenseVoice(filesDir: File) {
        if (busy) return
        busy = true
        message = null
        progress = -1f
        stage = "连接镜像…"
        job = scope.launch {
            try {
                val ok = withContext(Dispatchers.IO) {
                    com.learnagent.asr.SenseVoiceEngine.download(filesDir) { name, d, t ->
                        if (t > 0) {
                            progress = (d.toFloat() / t).coerceIn(0f, 1f)
                            stage = "$name " + (d * 100 / t) + "%"
                        } else {
                            stage = "$name ${d / 1048576}MB"
                        }
                    }
                }
                message = if (ok) "SenseVoice 模型下载完成（中文最准，离线可用）" else "下载失败：检查网络后重试"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message = "下载失败：${e.message ?: e.javaClass.simpleName}"
            } finally {
                busy = false
                job = null
            }
        }
    }

}
