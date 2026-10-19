package com.learnagent.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/** 是否有可用网络（用于在线问答/辩论快速失败，避免无网时长时间等待）。 */
fun isOnline(context: Context): Boolean {
    return runCatching {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }.getOrDefault(false)
}

private var nimCache: Pair<Long, Boolean>? = null

/**
 * NVIDIA NIM 连通性预检（带 30 秒缓存）。
 * 用于区分"设备有网但连不上 NIM"（被墙/代理/防火墙）——能下载模型 ≠ 能连 NIM。
 */
suspend fun nimReachable(apiKey: String, force: Boolean = false): Boolean {
    val now = System.currentTimeMillis()
    nimCache?.let { if (!force && now - it.first < 30_000) return it.second }
    val ok = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        runCatching {
            val conn = java.net.URL("https://integrate.api.nvidia.com/v1/models").openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            val code = conn.responseCode
            conn.disconnect()
            code in 200..299
        }.getOrDefault(false)
    }
    nimCache = now to ok
    return ok
}
