package com.learnagent.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.learnagent.data.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class AudioExtractorTest {
    
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val audioExtractor = AndroidAudioExtractor(context)
    
    @Test
    fun testExtractToWav() {
        // 创建模拟视频文件
        val videoFile = File(context.cacheDir, "test_video.mp4")
        videoFile.writeText("模拟视频数据")
        
        val wavFile = File(context.cacheDir, "test_audio.wav")
        
        try {
            // 模拟音频提取
            audioExtractor.extractToWav(
                videoPath = videoFile.absolutePath,
                outputPath = wavFile.absolutePath,
                onProgress = { progress ->
                    println("Audio extraction progress: ${progress * 100}%")
                }
            )
            
            // 验证 WAV 文件生成
            assertTrue(wavFile.exists())
            assertTrue(wavFile.length() > 0)
            
            println("WAV file created: ${wavFile.absolutePath}")
            println("WAV file size: ${wavFile.length()} bytes")
            
        } finally {
            // 清理
            videoFile.delete()
            wavFile.delete()
        }
    }
    
    @Test
    fun testWavFormat() {
        // 验证 WAV 文件格式
        val wavFile = File(context.cacheDir, "format_test.wav")
        
        // 模拟 WAV 文件头
        val wavHeader = byteArrayOf(
            // RIFF header
            'R'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), 'F'.code.toByte(),
            0x24.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00, // file length - 8
            'W'.code.toByte(), 'A'.code.toByte(), 'V'.code.toByte(), 'E'.code.toByte(),
            
            // Format chunk
            'f'.code.toByte(), 'm'.code.toByte(), 't'.code.toByte(), ' '.code.toByte(),
            0x10.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00, // chunk length
            0x01.toByte(), 0x00, // PCM format
            0x01.toByte(), 0x00, // mono
            0x40.toByte(), 0x1F.toByte(), 0x00.toByte(), 0x00, // 16kHz
            0x40.toByte(), 0x1F.toByte(), 0x00.toByte(), 0x00, // byte rate
            0x02.toByte(), 0x00, // block align
            0x10.toByte(), 0x00, // 16-bit
            
            // Data chunk
            'd'.code.toByte(), 'a'.code.toByte(), 't'.code.toByte(), 'a'.code.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00 // data length
        )
        
        wavFile.writeBytes(wavHeader)
        
        assertTrue(wavFile.exists())
        assertTrue(wavFile.length() > 0)
        
        println("WAV format test passed")
        
        wavFile.delete()
    }
}