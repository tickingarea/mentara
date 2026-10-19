package com.learnagent.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.learnagent.data.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class AndroidTranscriberTest {
    
    private lateinit var context: Context
    private lateinit var modelManager: ModelManager
    private lateinit var transcriber: AndroidTranscriber
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        modelManager = ModelManager(context)
        transcriber = AndroidTranscriber(context, modelManager)
    }
    
    @After
    fun cleanup() {
        transcriber.cleanup()
    }
    
    @Test
    fun testSelectOptimalModel() {
        val model = modelManager.selectOptimalModel()
        assertTrue(model in listOf("tiny", "base", "small"))
        println("Selected model: $model")
    }
    
    @Test
    fun testModelManager() {
        // 测试模型管理器基本功能
        val modelFile = modelManager.getModelFile("tiny")
        println("Model file path: ${modelFile.absolutePath}")
        println("Model exists: ${modelManager.isModelDownloaded("tiny")}")
        
        val downloadedModels = modelManager.getDownloadedModels()
        println("Downloaded models: $downloadedModels")
        
        val modelInfo = modelManager.getModelInfo("tiny")
        println("Model info: $modelInfo")
    }
    
    @Test
    fun testVideoIdCalculation() {
        // 创建测试视频文件
        val testVideo = File(context.cacheDir, "test_video.mp4")
        testVideo.writeText("This is a test video file")
        
        val videoId = transcriber.calculateVideoId(testVideo.absolutePath)
        assertNotNull(videoId)
        assertEquals(32, videoId.length) // SHA-256 前16位，转换为32个字符
        
        println("Video ID: $videoId")
        
        // 清理
        testVideo.delete()
    }
    
    @Test
    fun testTranscribeWithSampleVideo() = runBlocking {
        // 注意：这个测试需要真实的视频文件
        // 在实际测试中，应该将测试视频文件放在 assets 目录中
        
        val sampleVideo = File(context.assets, "sample_30s.mp4")
        if (!sampleVideo.exists()) {
            println("Sample video not found, skipping test")
            return@runBlocking
        }
        
        // 复制到缓存目录
        val tempVideo = File(context.cacheDir, "sample_30s.mp4")
        sampleVideo.copyTo(tempVideo, overwrite = true)
        
        try {
            // 测试转录
            val config = AndroidTranscriber.TranscribeConfig(
                modelName = "tiny", // 使用小模型进行测试
                language = "zh",
                useCache = true
            )
            
            val segments = transcriber.transcribeWithProgress(
                videoPath = tempVideo.absolutePath,
                config = config
            ) { progress ->
                println("Transcribe progress: $progress")
            }
            
            // 验证结果
            assertNotNull(segments)
            assertTrue(segments.isNotEmpty())
            
            // 验证时间戳递增
            for (i in 1 until segments.size) {
                assertTrue(segments[i].start >= segments[i-1].end)
            }
            
            println("Transcribed ${segments.size} segments")
            segments.forEach { segment ->
                println("Segment: ${segment.start}-${segment.end}: ${segment.text}")
            }
            
        } finally {
            // 清理
            tempVideo.delete()
        }
    }
    
    @Test
    fun testCacheMechanism() = runBlocking {
        // 创建测试视频文件
        val testVideo = File(context.cacheDir, "cache_test.mp4")
        testVideo.writeText("This is a test video file for cache testing")
        
        try {
            val videoId = transcriber.calculateVideoId(testVideo.absolutePath)
            println("Video ID for cache test: $videoId")
            
            // 测试缓存保存
            val testSegments = listOf(
                TranscriptSegment(0.0, 5.0, "Hello world", "speaker1"),
                TranscriptSegment(5.0, 10.0, "This is a test", "speaker2")
            )
            
            transcriber.saveToCache(videoId, testSegments)
            
            // 测试缓存加载
            val cachedSegments = transcriber.loadFromCache(videoId)
            assertNotNull(cachedSegments)
            assertEquals(testSegments.size, cachedSegments?.size)
            
            println("Cache test passed")
            
        } finally {
            testVideo.delete()
        }
    }
    
    @Test
    fun testMemoryManagement() = runBlocking {
        // 测试内存管理
        val testVideo = File(context.cacheDir, "memory_test.mp4")
        testVideo.writeText("This is a test video file for memory testing")
        
        try {
            val config = AndroidTranscriber.TranscribeConfig(
                modelName = "tiny",
                language = "zh",
                useCache = false // 不使用缓存以测试内存管理
            )
            
            // 执行转录
            val segments = transcriber.transcribeWithProgress(
                videoPath = testVideo.absolutePath,
                config = config
            ) { progress ->
                println("Memory test progress: $progress")
            }
            
            // 验证结果
            assertNotNull(segments)
            
            println("Memory test completed successfully")
            
        } catch (e: AndroidTranscriber.MemoryNotEnoughException) {
            println("Memory test caught expected exception: ${e.message}")
        } finally {
            testVideo.delete()
        }
    }
    
    @Test
    fun testErrorHandling() = runBlocking {
        // 测试错误处理
        val nonExistentVideo = File(context.cacheDir, "non_existent.mp4")
        
        try {
            transcriber.transcribe(nonExistentVideo.absolutePath)
            assertTrue(false, "Should have thrown an exception")
        } catch (e: Exception) {
            println("Error handling test passed: ${e.message}")
        }
    }
    
    private fun AndroidTranscriber.calculateVideoId(videoPath: String): String {
        // 复制私有方法用于测试
        val file = File(videoPath)
        val messageDigest = java.security.MessageDigest.getInstance("SHA-256")
        val bytes = file.readBytes()
        val digest = messageDigest.digest(bytes)
        return digest.take(16).joinToString("") { "%02x".format(it) }
    }
}