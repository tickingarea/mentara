package com.learnagent.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.learnagent.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class TranscriberTest {
    
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val transcriber = AndroidTranscriber(context, ModelManager(context))
    
    @Test
    fun testTranscribeWithProgress() = runBlocking {
        // 创建模拟音频文件
        val wavFile = File(context.cacheDir, "test_audio.wav")
        wavFile.writeText("模拟音频数据")
        
        // 模拟模型文件
        val modelFile = File(context.filesDir, "models/tiny.bin")
        modelFile.writeText("模拟模型数据")
        
        try {
            val config = AndroidTranscriber.TranscribeConfig(
                modelName = "tiny",
                language = "zh",
                useCache = false
            )
            
            val segments = transcriber.transcribeWithProgress(
                videoPath = wavFile.absolutePath,
                config = config
            ) { flow ->
                flow.collect { progress ->
                    println("Transcription progress: $progress")
                }
            }
            
            // 验证转写结果
            assertTrue(segments.isNotEmpty())
            
            // 验证 JSON 格式
            segments.forEach { segment ->
                assertTrue(segment.start >= 0.0)
                assertTrue(segment.end > segment.start)
                assertTrue(segment.text.isNotEmpty())
            }
            
            // 验证时间戳递增
            for (i in 1 until segments.size) {
                assertTrue(segments[i].start >= segments[i-1].end)
            }
            
            println("Transcription completed with ${segments.size} segments")
            segments.forEach { segment ->
                println("Segment ${segment.start}-${segment.end}: ${segment.text}")
            }
            
        } finally {
            // 清理
            wavFile.delete()
            modelFile.delete()
        }
    }
    
    @Test
    fun testChineseTranscription() = runBlocking {
        // 测试中文转写结果
        val wavFile = File(context.cacheDir, "chinese_audio.wav")
        wavFile.writeText("中文音频数据")
        
        val modelFile = File(context.filesDir, "models/tiny.bin")
        modelFile.writeText("模拟模型数据")
        
        try {
            val config = AndroidTranscriber.TranscribeConfig(
                modelName = "tiny",
                language = "zh",
                useCache = false
            )
            
            val segments = transcriber.transcribeWithProgress(
                videoPath = wavFile.absolutePath,
                config = config
            ) { flow ->
                // 忽略进度流
            }
            
            // 验证中文文本
            segments.forEach { segment ->
                assertTrue(segment.text.matches(Regex(".*[\\u4e00-\\u9fa5].*")))
                println("Chinese text: ${segment.text}")
            }
            
        } finally {
            wavFile.delete()
            modelFile.delete()
        }
    }
}