package com.learnagent.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.learnagent.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class ModelDownloadTest {
    
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val modelManager = ModelManager(context)
    
    @Test
    fun testModelDownload() = runBlocking {
        // 测试模型下载
        val modelInfo = ModelManager.ModelInfo(
            name = "tiny",
            size = 75L * 1024 * 1024, // 75MB
            url = "https://huggingface.co/systran/faster-whisper-tiny/resolve/main/tiny.bin"
        )
        
        println("开始下载模型: ${modelInfo.name}")
        
        val downloadPath = modelManager.downloadModel(
            modelInfo = modelInfo,
            onProgress = { downloaded, total ->
                val progress = downloaded.toFloat() / total.toFloat()
                println("下载进度: ${String.format("%.1f", progress * 100)}%")
            }
        )
        
        // 验证下载结果
        assertTrue(File(downloadPath).exists())
        println("模型下载完成: $downloadPath")
    }
    
    @Test
    fun testResumeDownload() = runBlocking {
        // 测试断点续传
        val modelInfo = ModelManager.ModelInfo(
            name = "tiny",
            size = 75L * 1024 * 1024,
            url = "https://huggingface.co/systran/faster-whisper-tiny/resolve/main/tiny.bin"
        )
        
        // 创建部分下载的文件
        val partialFile = File(context.filesDir, "models/tiny.bin")
        partialFile.writeBytes(ByteArray(1024 * 1024)) // 1MB 数据
        
        println("测试断点续传...")
        
        val downloadPath = modelManager.downloadModel(
            modelInfo = modelInfo,
            onProgress = { downloaded, total ->
                val progress = downloaded.toFloat() / total.toFloat()
                println("续传进度: ${String.format("%.1f", progress * 100)}%")
            }
        )
        
        // 验证续传结果
        assertTrue(File(downloadPath).exists())
        assertTrue(File(downloadPath).length() > 1024 * 1024)
        println("断点续传完成")
    }
    
    @Test
    fun testModelSelection() {
        // 测试模型选择
        val selectedModel = modelManager.selectOptimalModel()
        println("选择的模型: $selectedModel")
        
        assertTrue(listOf("tiny", "base", "small").contains(selectedModel))
    }
    
    @Test
    fun testModelCache() {
        // 测试模型缓存
        val modelFile = File(context.filesDir, "models/tiny.bin")
        modelFile.writeText("模拟模型数据")
        
        val cachedModel = modelManager.getCachedModel("tiny")
        assertTrue(cachedModel.exists())
        println("模型缓存正常: ${cachedModel.absolutePath}")
    }
}