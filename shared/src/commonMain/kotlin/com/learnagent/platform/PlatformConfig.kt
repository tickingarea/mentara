package com.learnagent.platform

/**
 * 平台相关配置
 *
 * 各平台差异通过 KMP 的 expect/actual 机制提供：
 * - jvm  : shared/src/jvmMain
 * - android: shared/src/androidMain
 */
expect object PlatformConfig {
    val platformName: String
    val isMobile: Boolean
    val isDesktop: Boolean
}
