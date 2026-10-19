package com.learnagent.platform

/**
 * Platform abstraction interface
 * 
 * Defines the common interface for different platform implementations
 */
interface Platform {
    
    /**
     * Get the platform name
     */
    val name: String
    
    /**
     * Get the platform version
     */
    val version: String
    
    /**
     * Check if the platform supports the required features
     */
    fun supportsFeature(feature: String): Boolean
    
    /**
     * Get platform-specific information
     */
    fun getInfo(): Map<String, Any>
}