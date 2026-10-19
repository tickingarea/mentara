package com.learnagent.platform

actual object PlatformConfig {
    actual val platformName: get() = "Desktop"
    actual val isMobile: get() = false
    actual val isDesktop: get() = true
}