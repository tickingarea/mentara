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

/**
 * 通用后台任务宿主：耗时操作（解析 / 纠错 / AI 重点 / AI 老师 …）挂在应用级作用域，
 * 切换页面不会中断；进度、阶段、结果、耗时都存在这里，回到任意页面都能续看。
 */
object TaskHost {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null

    var title: String by mutableStateOf("")
        private set
    var stage: String by mutableStateOf("")
        private set
    var busy: Boolean by mutableStateOf(false)
        private set
    var progress: Float by mutableStateOf(-1f)
        private set
    var startedAt: Long by mutableStateOf(0L)
        private set
    var result: String? by mutableStateOf(null)
        private set
    var error: String? by mutableStateOf(null)
        private set

    val active: Boolean get() = busy || result != null || error != null

    fun clear() {
        if (busy) return
        result = null
        error = null
        title = ""
    }

    fun cancel() {
        job?.cancel()
        job = null
        busy = false
        startedAt = 0L
        stage = "已取消"
    }

    /**
     * 启动一个后台任务。[work] 内用 report(stage, progress) 上报（progress<0 = 不确定）。
     * 返回的字符串作为结果展示（可为长文本）。
     */
    fun run(
        title: String,
        work: suspend (report: (String, Float) -> Unit) -> String
    ) {
        if (busy) return
        this.title = title
        stage = "准备中…"
        progress = -1f
        busy = true
        result = null
        error = null
        startedAt = System.currentTimeMillis()
        job = scope.launch {
            try {
                val r = work { s, p -> stage = s; progress = p }
                result = r
                com.learnagent.voice.MentaraVoice.speak(r.take(200))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.message ?: e.javaClass.simpleName
            } finally {
                busy = false
                startedAt = 0L
                job = null
            }
        }
    }
}
