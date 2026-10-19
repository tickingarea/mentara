package com.learnagent.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 桌面语言状态：默认 English；设置页切换即时生效（全部文案经 [t] 读取）。
 */
object Language {
    var current by mutableStateOf("en")
}

/** 取文案：读 Language 状态（变更触发重组）+ 填充 {1} {2} 占位。 */
fun t(key: String, arg1: String? = null, arg2: String? = null): String {
    val lang = Language.current
    var s = Strings.get(key, lang)
    if (arg1 != null) s = s.replace("{1}", arg1)
    if (arg2 != null) s = s.replace("{2}", arg2)
    return s
}
