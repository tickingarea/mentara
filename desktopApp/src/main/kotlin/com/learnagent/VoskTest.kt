package com.learnagent

import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.io.FileInputStream

fun main() {
    LibVosk.setLogLevel(LogLevel.WARNINGS)

    val modelPath = "C:\\Users\\Administrator\\Documents\\Default Project\\learnagent\\vosk-model\\vosk-model-small-cn-0.22"
    val audioPath = "C:\\Users\\Administrator\\Documents\\Default Project\\learnagent\\test_audio.wav"

    if (!File(modelPath).exists()) {
        println("模型不存在: $modelPath")
        return
    }
    if (!File(audioPath).exists()) {
        println("音频不存在: $audioPath")
        return
    }

    println("加载模型...")
    val model = Model(modelPath)
    val recognizer = Recognizer(model, 16000.0f)

    println("开始识别...")
    val fis = FileInputStream(audioPath)
    val buffer = ByteArray(4096)
    var bytesRead: Int
    var allText = ""

    while (fis.read(buffer).also { bytesRead = it } >= 0) {
        if (recognizer.acceptWaveForm(buffer, bytesRead)) {
            val result = recognizer.result
            println("中间结果: $result")
            allText += result
        } else {
            val partial = recognizer.partialResult
            if (partial.length > 20) println("partial: $partial")
        }
    }

    val finalResult = recognizer.finalResult
    println("最终结果: $finalResult")
    allText += finalResult

    println("=== 完整识别文本 ===")
    println(allText)

    fis.close()
    recognizer.close()
    model.close()
}