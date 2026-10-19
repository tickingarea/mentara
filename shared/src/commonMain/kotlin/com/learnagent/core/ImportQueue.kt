package com.learnagent.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** 批量导入状态。 */
data class ImportProgress(
    val currentIndex: Int,
    val total: Int,
    val currentFile: String,
    val state: ImportState,
    val doneCount: Int = 0,
    val failedCount: Int = 0
)

enum class ImportState { PENDING, DOWNLOADING, PROCESSING, DONE, FAILED, CANCELLED }

/**
 * 共享批量导入队列（桌面拖拽/文件夹、Android SAF 多选共用）。
 *
 * - [enqueue] 追加路径并自动开始串行处理
 * - [cancelAll] 取消剩余任务（已完成的保留）
 * - [progress] 状态流（PENDING/PROCESSING/DONE/FAILED/CANCELLED）
 *
 * processor 由平台注入（桌面 ConfigStore.importVideo / Android filesDir 复制）。
 */
class ImportQueue(
    private val processor: suspend (String) -> Unit = {}
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _progress = MutableStateFlow(ImportProgress(0, 0, "", ImportState.PENDING))
    val progress: Flow<ImportProgress> = _progress

    private val pending = ArrayDeque<String>()
    private var job: Job? = null
    private var doneCount = 0
    private var failedCount = 0
    private var total = 0

    @Synchronized
    fun enqueue(paths: List<String>) {
        val clean = paths.filter { it.isNotBlank() }
        if (clean.isEmpty()) return
        clean.forEach { pending.addLast(it) }
        total += clean.size
        _progress.value = ImportProgress(
            currentIndex = doneCount + failedCount,
            total = total,
            currentFile = pending.firstOrNull().orEmpty(),
            state = ImportState.PENDING,
            doneCount = doneCount,
            failedCount = failedCount
        )
        start()
    }

    @Synchronized
    fun cancelAll() {
        job?.cancel()
        job = null
        pending.clear()
        _progress.value = ImportProgress(
            currentIndex = doneCount + failedCount,
            total = total,
            currentFile = "",
            state = ImportState.CANCELLED,
            doneCount = doneCount,
            failedCount = failedCount
        )
    }

    @Synchronized
    private fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            while (true) {
                val path = synchronized(this@ImportQueue) {
                    if (pending.isEmpty()) null else pending.removeFirst()
                } ?: break
                val index = doneCount + failedCount
                _progress.value = ImportProgress(
                    index, total, path, ImportState.PROCESSING, doneCount, failedCount
                )
                try {
                    processor(path)
                    synchronized(this@ImportQueue) { doneCount++ }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    synchronized(this@ImportQueue) { failedCount++ }
                }
            }
            _progress.value = ImportProgress(
                doneCount + failedCount, total, "", ImportState.DONE, doneCount, failedCount
            )
        }
    }
}
