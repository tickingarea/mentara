package com.learnagent.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.learnagent.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(AndroidJUnit4::class)
class WhisperJNITest {
    
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val whisperJni = WhisperJNI(context)
    
    @Test
    fun testInitContext() {
        val modelPath = "/path/to/model.bin"
        val contextPtr = whisperJni.initContext(modelPath)
        
        assertNotNull(contextPtr)
        println("JNI context initialized: $contextPtr")
    }
    
    @Test
    fun testTranscribeFile() = runBlocking {
        val contextPtr = whisperJni.initContext("/path/to/model.bin")
        val wavPath = "/path/to/audio.wav"
        val language = "zh"
        
        val result = whisperJni.transcribeFile(contextPtr, wavPath, language)
        
        assertNotNull(result)
        println("Transcription result: $result")
        
        // 验证 JSON 格式
        assert(result.startsWith("[") && result.endsWith("]"))
    }
    
    @Test
    fun testFreeContext() {
        val contextPtr = whisperJni.initContext("/path/to/model.bin")
        whisperJni.freeContext(contextPtr)
        
        println("JNI context freed")
    }
}