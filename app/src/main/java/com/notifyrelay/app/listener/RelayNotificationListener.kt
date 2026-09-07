package com.notifyrelay.app.listener

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.notifyrelay.app.MainActivity
import com.notifyrelay.app.NotifyRelayApp
import com.notifyrelay.app.R
import com.notifyrelay.app.data.ForwardedItem
import com.notifyrelay.app.telegram.TelegramClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RelayNotificationListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val telegram = TelegramClient()
    private val recent = LinkedHashMap<String, Long>()

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        updateForeground()
    }

    override fun onListenerDisconnected() {
        instance = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onListenerDisconnected()
    }

    override fun onDestroy() {
        instance = null
        scope.cancel()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn ?: return
        if (notification.packageName == packageName) return

        val settings = NotifyRelayApp.instance.settings.read()
        if (!settings.canForward) return
        if (notification.packageName !in settings.allowedPackages) return
        if (settings.ignoreOngoing && isOngoing(notification)) return
        if (settings.ignoreGroupSummaries && isGroupSummary(notification.notification)) return

        val extras = notification.notification.extras
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = firstNonBlank(
            extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString(),
            extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
            extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString(),
        )
        if (title.isBlank() && text.isBlank()) return

        val haystack = "$title\n$text"
        val include = settings.keywordInclude
        if (include.isNotBlank() && !haystack.contains(include, ignoreCase = true)) return
        val exclude = settings.keywordExclude
        if (exclude.isNotBlank() && haystack.contains(exclude, ignoreCase = true)) return

        val dedupeKey = "${notification.packageName}|${notification.id}|$title|$text"
        if (isDuplicate(dedupeKey)) return

        val appLabel = runCatching {
            val info = packageManager.getApplicationInfo(notification.packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        }.getOrDefault(notification.packageName)

        val message = TelegramClient.formatNotification(appLabel, title, text)
        scope.launch {
            var lastError: String? = null
            var success = false
            for (attempt in 0 until 2) {
                val result = telegram.sendMessage(
                    settings.botToken,
                    settings.chatId,
                    message,
                    parseMode = "HTML",
                )
                if (result.isSuccess) {
                    success = true
                    break
                }
                lastError = result.exceptionOrNull()?.message
                if (attempt == 0) delay(2_000)
            }
            NotifyRelayApp.instance.settings.addLog(
                ForwardedItem(
                    atMillis = System.currentTimeMillis(),
                    appLabel = appLabel,
                    title = title.ifBlank { appLabel },
                    text = text,
                    success = success,
                    error = lastError,
                ),
            )
        }
    }

    fun updateForeground() {
        val settings = NotifyRelayApp.instance.settings.read()
        if (settings.canForward) {
            startForeground(STATUS_NOTIFICATION_ID, statusNotification())
        } else {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }

    private fun statusNotification(): android.app.Notification {
        val launch = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, NotifyRelayApp.STATUS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_relay)
            .setContentTitle(getString(R.string.status_notification_title))
            .setContentText(getString(R.string.status_notification_text))
            .setContentIntent(launch)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun isDuplicate(key: String): Boolean {
        val now = SystemClock.elapsedRealtime()
        val iterator = recent.entries.iterator()
        while (iterator.hasNext()) {
            if (now - iterator.next().value > DEDUPE_WINDOW_MS) {
                iterator.remove()
            }
        }
        if (recent.containsKey(key)) return true
        recent[key] = now
        if (recent.size > 40) {
            val oldest = recent.keys.first()
            recent.remove(oldest)
        }
        return false
    }

    companion object {
        private const val STATUS_NOTIFICATION_ID = 1001
        private const val DEDUPE_WINDOW_MS = 8_000L

        @Volatile
        var instance: RelayNotificationListener? = null
            private set

        fun refreshForeground() {
            instance?.updateForeground()
        }

        private fun isOngoing(sbn: StatusBarNotification): Boolean {
            if (sbn.isOngoing) return true
            val flags = sbn.notification.flags
            return flags and Notification.FLAG_ONGOING_EVENT != 0 ||
                flags and Notification.FLAG_FOREGROUND_SERVICE != 0
        }

        private fun isGroupSummary(notification: Notification): Boolean {
            return notification.flags and Notification.FLAG_GROUP_SUMMARY != 0
        }

        private fun firstNonBlank(vararg values: String?): String {
            return values.firstOrNull { !it.isNullOrBlank() }?.trim().orEmpty()
        }
    }
}
