package com.learnagent.data

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * 桌面平台的存储实现
 * 
 * 使用本地文件系统进行数据存储
 */
actual class Storage {
    
    actual fun getResultsDir(): String {
        val homeDir = System.getProperty("user.home")
        return File(homeDir, ".learnagent/results").absolutePath
    }
    
    actual fun writeJson(path: String, content: String) {
        try {
            val file = File(path)
            file.parentFile?.mkdirs()
            
            FileOutputStream(file).use { outputStream ->
                outputStream.write(content.toByteArray())
            }
        } catch (e: IOException) {
            throw IOException("Failed to write JSON to $path", e)
        }
    }
    
    actual fun readJson(path: String): String? {
        return try {
            val file = File(path)
            if (!file.exists()) return null
            
            FileInputStream(file).use { inputStream ->
                inputStream.readBytes().toString()
            }
        } catch (e: IOException) {
            throw IOException("Failed to read JSON from $path", e)
        }
    }
    
    actual fun listVideoIds(): List<String> {
        val resultsDir = File(getResultsDir())
        if (!resultsDir.exists()) return emptyList()
        
        return resultsDir.listFiles { file ->
            file.isDirectory && file.name != "."
        }?.map { it.name } ?: emptyList()
    }
    
    actual fun ensureVideoDir(videoId: String): String {
        val videoDir = File(getResultsDir(), videoId)
        videoDir.mkdirs()
        return videoDir.absolutePath
    }
    
    actual fun deleteVideoDir(videoId: String) {
        val videoDir = File(getResultsDir(), videoId)
        deleteRecursively(videoDir)
    }
    
    actual fun fileExists(path: String): Boolean {
        return File(path).exists()
    }
    
    actual fun deleteFile(path: String) {
        val file = File(path)
        if (file.exists()) {
            file.delete()
        }
    }
    
    /**
     * 递归删除目录及其内容
     */
    private fun deleteRecursively(file: File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteRecursively(it) }
        }
        file.delete()
    }
}