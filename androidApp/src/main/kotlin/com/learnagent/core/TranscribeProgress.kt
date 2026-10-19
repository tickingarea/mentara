package com.learnagent.core

import com.learnagent.data.VideoMeta

/**
 * Transcription progress states
 * 
 * Represents different states during the transcription process
 */
sealed class TranscribeProgress {
    
    /**
     * Transcription is idle
     */
    object Idle : TranscribeProgress()
    
    /**
     * Transcription has started
     */
    object Started : TranscribeProgress()
    
    /**
     * Extracting audio from video
     */
    object ExtractingAudio : TranscribeProgress()
    
    /**
     * Processing audio with whisper model
     */
    object Processing : TranscribeProgress()
    
    /**
     * Transcription completed successfully
     */
    data class Completed(val result: TranscriptionResult) : TranscribeProgress()
    
    /**
     * Transcription was cancelled
     */
    object Cancelled : TranscribeProgress()
    
    /**
     * Transcription failed with error
     */
    data class Error(val message: String) : TranscribeProgress()
    
    /**
     * Downloading model
     */
    data class DownloadingModel(val progress: Float, val modelName: String) : TranscribeProgress()
    
    /**
     * Model download completed
     */
    data class ModelDownloaded(val modelName: String) : TranscribeProgress()
}

/**
 * Transcription result data class
 */
data class TranscriptionResult(
    val text: String,
    val confidence: Float,
    val language: String,
    val duration: Float,
    val segments: List<TranscriptionSegment>
)

/**
 * Transcription segment data class
 */
data class TranscriptionSegment(
    val start: Float,
    val end: Float,
    val text: String,
    val confidence: Float,
    val words: List<Word>
)

/**
 * Word data class
 */
data class Word(
    val word: String,
    val start: Float,
    val end: Float,
    val confidence: Float
)

/**
 * Transcription options
 */
data class TranscriptionOptions(
    val model: String = "base.en",
    val language: String? = null,
    val temperature: Float = 0.0f,
    val bestOf: Int = 5,
    val beamSize: Int = 5,
    val patience: Float = 1.0f,
    val lengthPenalty: Float = 1.0f,
    val suppressBlank: Boolean = true,
    val wordTimestamps: Boolean = true,
    val prependPunctuations: String = "\"'“¿([{-",
    val appendPunctuations: String = "\"'.。,，!！?？:：;；)}]-～",
    val maxTokens: Int? = null
)