package com.learnagent.sync

import com.learnagent.data.VideoMeta

/**
 * Synchronization protocol interface
 * 
 * Defines the contract for data synchronization between devices
 */
interface SyncProtocol {
    
    /**
     * Initialize the sync protocol
     */
    suspend fun initialize()
    
    /**
     * Register a video for synchronization
     */
    suspend fun registerVideo(video: VideoMeta): SyncResult
    
    /**
     * Sync video data with remote
     */
    suspend fun syncVideo(videoId: String): SyncResult
    
    /**
     * Get sync status for a specific video
     */
    suspend fun getSyncStatus(videoId: String): SyncStatus
    
    /**
     * Get all videos that need synchronization
     */
    suspend fun getPendingSyncVideos(): List<VideoMeta>
    
    /**
     * Cancel ongoing sync operation
     */
    suspend fun cancelSync(videoId: String): Boolean
    
    /**
     * Clean up sync data
     */
    suspend fun cleanup(): Boolean
}