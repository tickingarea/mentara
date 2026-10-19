package com.learnagent.platform

/**
 * Platform implementation base class
 * 
 * Provides common functionality for platform implementations
 */
abstract class PlatformImpl : Platform {
    
    override fun supportsFeature(feature: String): Boolean {
        return getSupportedFeatures().contains(feature)
    }
    
    /**
     * Get list of supported features for this platform
     */
    protected abstract fun getSupportedFeatures(): Set<String>
    
    /**
     * Validate platform capabilities
     */
    fun validateCapabilities(): Boolean {
        return getRequiredFeatures().all { supportsFeature(it) }
    }
    
    /**
     * Get required features for the application
     */
    protected open fun getRequiredFeatures(): Set<String> {
        return setOf("audio_processing", "file_io", "network")
    }
}