package com.learnagent.core

actual class QaEngine {
    
    actual suspend fun ask(question: String, videoId: String?): QaAnswer {
        // TODO: 实现桌面平台的问答功能
        return QaAnswer("功能尚未实现", 0.0f)
    }
}