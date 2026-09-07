package com.notifyrelay.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.notifyrelay.app.listener.RelayNotificationListener

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        RelayNotificationListener.refreshForeground()
    }
}
