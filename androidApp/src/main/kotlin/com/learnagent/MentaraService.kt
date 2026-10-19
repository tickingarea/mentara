package com.learnagent

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.learnagent.ui.DebateHost
import com.learnagent.ui.ModelHost
import com.learnagent.ui.TaskHost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Mentara 后台保活服务（前台服务 + 部分唤醒锁）：
 *  - 任何一个后台宿主（TaskHost 解析/纠错/AI、DebateHost 辩论、ModelHost 模型下载）忙时自动启动；
 *  - 挂常驻通知 + 持有 PARTIAL_WAKE_LOCK → 熄屏后任务继续跑；
 *  - 全部空闲后自动停止（服务内每 5 秒自检），不常驻耗电。
 */
class MentaraService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var wakeLock: PowerManager.WakeLock? = null
    private var pollJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 任何异常都不允许炸掉整个 App（服务是全局保活通道，必须永不崩）
        runCatching {
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(NOTI_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(NOTI_ID, buildNotification())
            }
        }.onFailure {
            runCatching { stopSelf() }
            return START_NOT_STICKY
        }
        running = true
        // 唤醒锁失败就退化为"仅前台服务保活"，绝不崩
        runCatching {
            if (wakeLock?.isHeld != true) {
                wakeLock = (getSystemService(POWER_SERVICE) as PowerManager)
                    .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Mentara:study")
                    .also { it.acquire(12 * 60 * 60 * 1000L) } // 12 小时上限，防泄漏
            }
        }.onFailure { wakeLock = null }
        if (pollJob?.isActive != true) {
            pollJob = scope.launch {
                while (isActive) {
                    if (!(TaskHost.busy || DebateHost.busy || ModelHost.busy)) {
                        stopSelf()
                        break
                    }
                    delay(5000)
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        running = false
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
        scope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val channelId = "mentara_tasks"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        runCatching {
            nm.createNotificationChannel(
                NotificationChannel(channelId, "学习任务", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, channelId)
            .setContentTitle("Mentara 后台学习中")
            .setContentText("解析 / 转写 / 辩论 / 下载 熄屏也会继续")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val NOTI_ID = 1001

        /** 保活服务状态（设置页可见，用于诊断"熄屏停止"问题）。 */
        var running by mutableStateOf(false)
            private set
        var lastStartError by mutableStateOf<String?>(null)
            private set

        /** 任一后台宿主忙时确保服务在跑（由 MentaraApp 每秒调用；停止由服务自检完成） */
        fun sync(context: Context) {
            if (!(TaskHost.busy || DebateHost.busy || ModelHost.busy)) return
            val intent = Intent(context, MentaraService::class.java)
            runCatching {
                if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent)
                else context.startService(intent)
                lastStartError = null
            }.onFailure { lastStartError = it.message ?: it.javaClass.simpleName }
        }
    }
}
