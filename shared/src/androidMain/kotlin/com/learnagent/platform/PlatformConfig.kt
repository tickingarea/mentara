package com.learnagent.platform

actual object PlatformConfig {
    actual val platformName: String get() = "Android"
    actual val isMobile: Boolean get() = true
    actual val isDesktop: Boolean get() = false
}
