package com.learnagent.platform

/**
 * Platform factory for creating platform instances
 * 
 * Provides a factory method to create appropriate platform implementations
 */
object PlatformFactory {
    
    /**
     * Create a platform instance based on the current runtime
     */
    fun create(): Platform {
        throw UnsupportedOperationException("Platform not supported in common module")
    }
    
    /**
     * Get the current platform type
     */
    fun getCurrentPlatform(): PlatformType {
        throw UnsupportedOperationException("Platform not supported in common module")
    }
    
    /**
     * Check if running on specific platform
     */
    fun isPlatform(platform: PlatformType): Boolean {
        throw UnsupportedOperationException("Platform not supported in common module")
    }
}

/**
 * Platform types enumeration
 */
enum class PlatformType {
    ANDROID,
    IOS,
    DESKTOP,
    WEB
}