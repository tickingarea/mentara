package com.learnagent.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.learnagent.core.DebateEngine
import com.learnagent.core.DebateModels
import com.learnagent.core.DebateResult
import com.learnagent.core.LlmClient
import com.learnagent.core.LocalDebater
import com.learnagent.data.TimelineItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 全局辩论宿主：辩论任务挂在应用级作用域上，切换页面/窗口不会中断；
 * 进度、阶段、耗时与结果都存在这里，回到面板即可续看。
 */
object DebateHost {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null

    var videoId: String? by mutableStateOf(null)
        private set
    var result: DebateResult? by mutableStateOf(null)
        private set
    var busy: Boolean by mutableStateOf(false)
        private set
    var phase: Int by mutableStateOf(0)
        private set
    var startedAt: Long by mutableStateOf(0L)
        private set
    var error: String? by mutableStateOf(null)
        private set

    fun cancel() {
        job?.cancel()
        job = null
        busy = false
        phase = 0
        startedAt = 0L
    }

    fun start(
        videoId: String?,
        topic: String,
        timeline: List<TimelineItem>,
        apiKey: String,
        modelA: String,
        modelB: String,
        modelC: String,
        netAvailable: Boolean = true
    ) {
        if (busy || timeline.isEmpty()) return
        this.videoId = videoId
        result = null
        error = null
        if (apiKey.isNotBlank() && !netAvailable) {
            error = "当前无网络连接，无法在线辩论。请连网后重试。"
            busy = false
            phase = 0
            return
        }
        busy = true
        phase = 1
        startedAt = System.currentTimeMillis()
        job = scope.launch {
            try {
                // NIM 连通性预检：有网但连不上 NIM（代理/防火墙）时立刻给出明确原因，不再干等超时
                if (apiKey.isNotBlank() && !nimReachable(apiKey)) {
                    error = "连不上 NVIDIA NIM（模型服务）：设备有网但到 NIM 不通（代理/防火墙/运营商拦截）。请换网络后重试；离线辩论可清空 API Key 使用。"
                    busy = false
                    return@launch
                }
                val context8 = com.learnagent.core.Evidence.render(
                    timeline.sortedByDescending { (it.speech + it.screenText).length }.take(8)
                )
                val engine = withContext(Dispatchers.IO) {
                    if (apiKey.isNotBlank()) {
                        val a: LlmClient = com.learnagent.llm.AndroidNimClient(apiKey, DebateModels.resolve(modelA, DebateModels.DEFAULT_A))
                        val b: LlmClient = com.learnagent.llm.AndroidNimClient(apiKey, DebateModels.resolve(modelB, DebateModels.DEFAULT_B))
                        val c: LlmClient = com.learnagent.llm.AndroidNimClient(apiKey, DebateModels.resolve(modelC, DebateModels.DEFAULT_C))
                        DebateEngine(a, b, c, com.learnagent.llm.AndroidNimClient(apiKey, DebateModels.FAST_FALLBACK))
                    } else {
                        DebateEngine(
                            LocalDebater("概述", timeline),
                            LocalDebater("挑战", timeline),
                            LocalDebater("证据", timeline)
                        )
                    }
                }
                val r = withContext(Dispatchers.IO) {
                    engine.debate(
                        topic, context8,
                        onPhase = { phase = it },
                        onRound = { _, partial -> result = partial }
                    )
                }
                result = r
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.message ?: "辩论失败"
            } finally {
                busy = false
                phase = 0
                startedAt = 0L
                job = null
            }
        }
    }
}
