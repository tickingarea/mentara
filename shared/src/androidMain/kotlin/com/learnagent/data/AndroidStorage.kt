package com.learnagent.data

import android.content.Context
import java.io.File

/**
 * Android 平台的存储实现。
 *
 * 实现 commonMain 的 [Storage] 接口，使用应用外部私有目录存放 results。
 */
class AndroidStorage(private val context: Context) : Storage {

    override fun getResultsDir(): String {
        val externalDir = context.getExternalFilesDir(null) ?: context.filesDir
        return File(externalDir, ResultsDir.RESULTS_ROOT).absolutePath
    }

    override fun writeJson(path: String, content: String) {
        val file = File(path)
        file.parentFile?.mkdirs()
        file.writeText(content)
    }

    override fun readJson(path: String): String? {
        val file = File(path)
        return if (file.exists()) file.readText() else null
    }

    override fun listVideoIds(): List<String> {
        val dir = File(getResultsDir())
        if (!dir.exists()) return emptyList()
        return dir.listFiles { f -> f.isDirectory }?.map { it.name } ?: emptyList()
    }

    override fun ensureVideoDir(videoId: String): String {
        val dir = File(getResultsDir(), videoId)
        dir.mkdirs()
        return dir.absolutePath
    }

    override fun deleteVideoDir(videoId: String) {
        File(getResultsDir(), videoId).deleteRecursively()
    }

    override fun fileExists(path: String): Boolean = File(path).exists()

    override fun deleteFile(path: String) {
        val f = File(path)
        if (f.exists()) f.delete()
    }

    companion object {
        fun fromContext(context: Context): AndroidStorage =
            AndroidStorage(context.applicationContext)
    }
}
