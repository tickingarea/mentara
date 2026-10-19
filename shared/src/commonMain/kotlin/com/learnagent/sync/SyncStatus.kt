package com.learnagent.sync

/**
 * Synchronization status enumeration
 * 
 * Represents the current state of synchronization operations
 */
enum class SyncStatus {
    /**
     * Synchronization is idle, not running
     */
    IDLE,
    
    /**
     * Synchronization is in progress
     */
    SYNCING,
    
    /**
     * Synchronization completed successfully
     */
    SUCCESS,
    
    /**
     * Synchronization failed with an error
     */
    ERROR,
    
    /**
     * Synchronization detected conflicts that need resolution
     */
    CONFLICT,
    
    /**
     * Synchronization is queued and waiting to start
     */
    QUEUED,
    
    /**
     * Synchronization was cancelled by user
     */
    CANCELLED,
    
    /**
     * Synchronization is waiting for network connectivity
     */
    WAITING_FOR_NETWORK,
    
    /**
     * Synchronization is waiting for storage space
     */
    WAITING_FOR_STORAGE
}

/**
 * Extended sync status with additional information
 */
data class SyncStatusInfo(
    val status: SyncStatus,
    val progress: Float = 0f,
    val message: String = "",
    val lastSyncTime: Long = 0L,
    val retryCount: Int = 0,
    val estimatedTimeRemaining: Long = 0L
)