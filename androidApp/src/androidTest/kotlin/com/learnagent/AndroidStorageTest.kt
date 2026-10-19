package com.learnagent

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.learnagent.data.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class AndroidStorageTest {
    
    private lateinit var storage: Storage
    private lateinit var context: Context
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        storage = Storage.fromContext(context)
        
        // 清理测试数据
        cleanupTestData()
    }
    
    @After
    fun cleanup() {
        cleanupTestData()
    }
    
    private fun cleanupTestData() {
        val resultsDir = File(storage.getResultsDir())
        if (resultsDir.exists()) {
            resultsDir.listFiles()?.forEach { file ->
                if (file.name == "test-video") {
                    storage.deleteVideoDir("test-video")
                }
            }
        }
    }
    
    @Test
    fun testGetResultsDir() {
        val resultsDir = storage.getResultsDir()
        assertNotNull(resultsDir)
        assertTrue(resultsDir.endsWith("results"))
        println("Results directory: $resultsDir")
    }
    
    @Test
    fun testEnsureVideoDir() {
        val videoId = "test-video"
        val videoDir = storage.ensureVideoDir(videoId)
        
        assertNotNull(videoDir)
        assertTrue(videoDir.endsWith("test-video"))
        assertTrue(File(videoDir).exists())
        assertTrue(File(videoDir).isDirectory)
        
        println("Video directory: $videoDir")
    }
    
    @Test
    fun testWriteAndReadJson() {
        val videoId = "test-video"
        val videoDir = storage.ensureVideoDir(videoId)
        
        val videoMeta = VideoMeta(
            videoId = videoId,
            deviceId = "test-device-123",
            lastModified = System.currentTimeMillis(),
            schemaVersion = 1,
            title = "Test Video Title",
            duration = 180.0
        )
        
        val metaPath = ResultsDir.getMetaPath(videoId)
        val json = JsonConfig.json.encodeToString(videoMeta)
        
        // 写入JSON文件
        storage.writeJson(metaPath, json)
        
        // 读取JSON文件
        val readJson = storage.readJson(metaPath)
        assertNotNull(readJson)
        
        // 验证文件内容
        val deserialized = JsonConfig.json.decodeFromString<VideoMeta>(readJson)
        assertEquals(videoId, deserialized.videoId)
        assertEquals("test-device-123", deserialized.deviceId)
        assertEquals("Test Video Title", deserialized.title)
        assertEquals(180.0, deserialized.duration)
        
        println("Written and read JSON successfully")
        println("Video ID: ${deserialized.videoId}")
        println("Device ID: ${deserialized.deviceId}")
        println("Title: ${deserialized.title}")
        println("Duration: ${deserialized.duration}")
        println("Last Modified: ${deserialized.lastModified}")
    }
    
    @Test
    fun testListVideoIds() {
        // 确保目录存在
        storage.ensureVideoDir("video1")
        storage.ensureVideoDir("video2")
        
        val videoIds = storage.listVideoIds()
        assertTrue(videoIds.contains("video1"))
        assertTrue(videoIds.contains("video2"))
        
        println("Found video IDs: $videoIds")
    }
    
    @Test
    fun testFileExists() {
        val videoId = "test-video"
        storage.ensureVideoDir(videoId)
        
        val metaPath = ResultsDir.getMetaPath(videoId)
        storage.writeJson(metaPath, "{}")
        
        assertTrue(storage.fileExists(metaPath))
        assertFalse(storage.fileExists("/nonexistent/path.json"))
    }
    
    @Test
    fun testDeleteFile() {
        val videoId = "test-video"
        storage.ensureVideoDir(videoId)
        
        val metaPath = ResultsDir.getMetaPath(videoId)
        storage.writeJson(metaPath, "{}")
        
        assertTrue(storage.fileExists(metaPath))
        storage.deleteFile(metaPath)
        assertFalse(storage.fileExists(metaPath))
    }
    
    @Test
    fun testDeleteVideoDir() {
        val videoId = "test-video"
        storage.ensureVideoDir(videoId)
        
        val videoDir = File(storage.getResultsDir(), videoId)
        assertTrue(videoDir.exists())
        
        storage.deleteVideoDir(videoId)
        assertFalse(videoDir.exists())
    }
    
    @Test
    fun testCompleteWorkflow() {
        val videoId = "complete-test-video"
        
        // 1. 确保视频目录存在
        val videoDir = storage.ensureVideoDir(videoId)
        println("Created video directory: $videoDir")
        
        // 2. 创建多个数据文件
        val videoMeta = VideoMeta(
            videoId = videoId,
            deviceId = "complete-test-device",
            lastModified = System.currentTimeMillis(),
            schemaVersion = 1,
            title = "Complete Test Video",
            duration = 300.0
        )
        
        val transcriptSegments = listOf(
            TranscriptSegment(0.0, 10.0, "Hello everyone", "speaker1"),
            TranscriptSegment(10.0, 20.0, "Welcome to the test", "speaker2")
        )
        
        val frames = listOf(
            FrameOcr(5.0, "Screen content 1", "/frame1.jpg"),
            FrameOcr(15.0, "Screen content 2", "/frame2.jpg")
        )
        
        // 3. 写入所有文件
        storage.writeJson(ResultsDir.getMetaPath(videoId), JsonConfig.json.encodeToString(videoMeta))
        storage.writeJson(ResultsDir.getTranscriptPath(videoId), JsonConfig.json.encodeToString(transcriptSegments))
        storage.writeJson(ResultsDir.getFramesPath(videoId), JsonConfig.json.encodeToString(frames))
        
        // 4. 验证文件存在
        assertTrue(storage.fileExists(ResultsDir.getMetaPath(videoId)))
        assertTrue(storage.fileExists(ResultsDir.getTranscriptPath(videoId)))
        assertTrue(storage.fileExists(ResultsDir.getFramesPath(videoId)))
        
        // 5. 读取并验证数据
        val readMeta = JsonConfig.json.decodeFromString<VideoMeta>(storage.readJson(ResultsDir.getMetaPath(videoId))!!)
        assertEquals(videoId, readMeta.videoId)
        assertEquals("complete-test-device", readMeta.deviceId)
        assertEquals("Complete Test Video", readMeta.title)
        
        val readTranscript = JsonConfig.json.decodeFromString<List<TranscriptSegment>>(storage.readJson(ResultsDir.getTranscriptPath(videoId))!!)
        assertEquals(2, readTranscript.size)
        assertEquals("Hello everyone", readTranscript[0].text)
        assertEquals("Welcome to the test", readTranscript[1].text)
        
        val readFrames = JsonConfig.json.decodeFromString<List<FrameOcr>>(storage.readJson(ResultsDir.getFramesPath(videoId))!!)
        assertEquals(2, readFrames.size)
        assertEquals("Screen content 1", readFrames[0].text)
        assertEquals("Screen content 2", readFrames[1].text)
        
        // 6. 清理
        storage.deleteVideoDir(videoId)
        assertFalse(File(videoDir).exists())
        
        println("Complete workflow test passed!")
    }
    
    private fun assertFalse(condition: Boolean) {
        kotlin.test.assertFalse(condition)
    }
}