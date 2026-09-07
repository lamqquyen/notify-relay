package com.notifyrelay.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.notifyrelay.app.data.SettingsRepository

class NotifyRelayApp : Application() {
    lateinit var settings: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        settings = SettingsRepository(this)
        createStatusChannel()
    }

    private fun createStatusChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            STATUS_CHANNEL_ID,
            "Forwarding status",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows when NotifyRelay is forwarding notifications"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val STATUS_CHANNEL_ID = "notify_relay_status"
        lateinit var instance: NotifyRelayApp
            private set
    }
}
