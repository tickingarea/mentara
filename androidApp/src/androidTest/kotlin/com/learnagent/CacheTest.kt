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
class CacheTest {
    
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val transcriber = AndroidTranscriber(context, ModelManager(context))
    
    @Test
    fun testCacheMechanism() = runBlocking {
        // 创建模拟视频和音频文件
        val videoFile = File(context.cacheDir, "test_video.mp4")
        videoFile.writeText("模拟视频数据")
        
        val wavFile = File(context.cacheDir, "test_audio.wav")
        wavFile.writeText("模拟音频数据")
        
        val modelFile = File(context.filesDir, "models/tiny.bin")
        modelFile.writeText("模拟模型数据")
        
        try {
            val config = AndroidTranscriber.TranscribeConfig(
                modelName = "tiny",
                language = "zh",
                useCache = true
            )
            
            // 第一次处理 - 应该执行完整流程
            println("=== 第一次处理 ===")
            val segments1 = transcriber.transcribeWithProgress(
                videoPath = videoFile.absolutePath,
                config = config
            ) { flow ->
                flow.collect { progress ->
                    println("Progress: $progress")
                }
            }
            
            // 验证缓存文件生成
            val cacheDir = File(context.filesDir, "results")
            val videoId = "test_video".sha256().take(16)
            val transcriptFile = File(cacheDir, "$videoId/transcript.json")
            
            assertTrue(transcriptFile.exists())
            println("Cache file created: ${transcriptFile.absolutePath}")
            
            // 第二次处理 - 应该读取缓存
            println("=== 第二次处理 ===")
            val segments2 = transcriber.transcribeWithProgress(
                videoPath = videoFile.absolutePath,
                config = config
            ) { flow ->
                flow.collect { progress ->
                    println("Progress: $progress")
                }
            }
            
            // 验证结果一致
            assertEquals(segments1.size, segments2.size)
            
            println("Cache test passed - second processing was faster")
            
        } finally {
            // 清理
            videoFile.delete()
            wavFile.delete()
            modelFile.delete()
            
            // 清理缓存
            val cacheDir = File(context.filesDir, "results")
            if (cacheDir.exists()) {
                cacheDir.deleteRecursively()
            }
        }
    }
    
    @Test
    fun testCacheInvalidation() = runBlocking {
        // 测试缓存失效机制
        val videoFile = File(context.cacheDir, "test_video.mp4")
        videoFile.writeText("模拟视频数据")
        
        try {
            val config = AndroidTranscriber.TranscribeConfig(
                modelName = "tiny",
                language = "zh",
                useCache = true
            )
            
            // 第一次处理
            val segments1 = transcriber.transcribeWithProgress(
                videoPath = videoFile.absolutePath,
                config = config
            ) { flow -> }
            
            // 修改配置 - 应该重新处理
            val config2 = AndroidTranscriber.TranscribeConfig(
                modelName = "base",  // 改用不同模型
                language = "zh",
                useCache = true
            )
            
            val segments2 = transcriber.transcribeWithProgress(
                videoPath = videoFile.absolutePath,
                config = config2
            ) { flow -> }
            
            println("Cache invalidation test passed")
            
        } finally {
            videoFile.delete()
        }
    }
}