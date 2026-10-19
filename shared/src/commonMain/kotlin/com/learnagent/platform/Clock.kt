package com.learnagent.platform

/** 当前时间（毫秒）。避免在 commonMain 引入协程/时间库。 */
expect fun currentTimeMillis(): Long
