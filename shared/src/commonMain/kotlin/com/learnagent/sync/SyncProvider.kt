package com.learnagent.sync

import com.learnagent.data.VideoMeta

data class SyncResult(
    val success: Boolean,
    val message: String = "",
    val syncedCount: Int = 0
)

data class SyncConfig(
    val enabled: Boolean = false,
    val provider: String = "",
    val endpoint: String = ""
)

data class SyncStats(
    val totalVideos: Int,
    val syncedVideos: Int,
    val failedVideos: Int,
    val lastSyncTime: Long,
    val totalSyncTime: Long,
    val averageSyncTime: Long
)

interface SyncProvider {
    suspend fun push()
    suspend fun pull()
    fun status(): SyncStatus
    suspend fun syncVideo(video: VideoMeta): SyncResult
    suspend fun getPendingSyncVideos(): List<VideoMeta>
    suspend fun cancelSync(videoId: String): Boolean
    suspend fun configure(config: SyncConfig): Boolean
    suspend fun getSyncStats(): SyncStats
}