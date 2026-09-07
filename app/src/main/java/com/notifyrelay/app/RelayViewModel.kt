package com.notifyrelay.app

import android.app.Application
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.notifyrelay.app.data.AppSettings
import com.notifyrelay.app.data.ForwardedItem
import com.notifyrelay.app.listener.RelayNotificationListener
import com.notifyrelay.app.telegram.TelegramClient
import com.notifyrelay.app.util.isNotificationAccessGranted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?,
    val isSystem: Boolean,
)

class RelayViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = NotifyRelayApp.instance.settings
    private val telegram = TelegramClient()

    private val _settings = MutableStateFlow(repo.read())
    val settings: StateFlow<AppSettings> = _settings

    private val _logs = MutableStateFlow(repo.readLogs())
    val logs: StateFlow<List<ForwardedItem>> = _logs

    private val _apps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val apps: StateFlow<List<InstalledApp>> = _apps

    private val _appsLoading = MutableStateFlow(false)
    val appsLoading: StateFlow<Boolean> = _appsLoading

    private val _hasNotificationAccess = MutableStateFlow(false)
    val hasNotificationAccess: StateFlow<Boolean> = _hasNotificationAccess

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    init {
        refreshSystemState()
        loadInstalledApps()
    }

    fun refreshSystemState() {
        _hasNotificationAccess.value = isNotificationAccessGranted(getApplication())
        _settings.value = repo.read()
        _logs.value = repo.readLogs()
        RelayNotificationListener.refreshForeground()
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        _settings.value = repo.update(transform)
        RelayNotificationListener.refreshForeground()
    }

    fun setForwarding(enabled: Boolean) {
        val current = _settings.value
        if (enabled && !current.telegramConfigured) {
            _statusMessage.value = "Add your Telegram bot token and chat ID first."
            return
        }
        if (enabled && current.allowedPackages.isEmpty()) {
            _statusMessage.value = "Pick at least one app to forward."
            return
        }
        if (enabled && !_hasNotificationAccess.value) {
            _statusMessage.value = "Notification access is still off."
            return
        }
        updateSettings { it.copy(forwardingEnabled = enabled) }
    }

    fun toggleApp(packageName: String) {
        updateSettings { current ->
            val next = current.allowedPackages.toMutableSet()
            if (!next.add(packageName)) next.remove(packageName)
            current.copy(allowedPackages = next)
        }
    }

    fun clearLogs() {
        repo.clearLogs()
        _logs.value = emptyList()
    }

    fun consumeStatusMessage() {
        _statusMessage.value = null
    }

    fun sendTestMessage() {
        val current = _settings.value
        if (!current.telegramConfigured) {
            _statusMessage.value = "Add your Telegram bot token and chat ID first."
            return
        }
        runTelegram("Sending test message…") {
            telegram.sendMessage(
                current.botToken,
                current.chatId,
                "NotifyRelay test\nIf you see this on your iPhone, forwarding works.",
            ).getOrThrow()
            "Test message sent. Check Telegram on your iPhone."
        }
    }

    fun detectChatId() {
        val token = _settings.value.botToken
        if (token.isBlank()) {
            _statusMessage.value = "Paste the bot token first."
            return
        }
        runTelegram("Looking for your Telegram chat…") {
            val chatId = telegram.detectChatId(token).getOrThrow()
            updateSettings { it.copy(chatId = chatId) }
            "Found chat ID $chatId"
        }
    }

    private fun runTelegram(working: String, block: suspend () -> String) {
        viewModelScope.launch {
            _busy.value = true
            _statusMessage.value = working
            val message = runCatching { block() }.fold(
                onSuccess = { it },
                onFailure = { error -> error.message ?: "Telegram request failed" },
            )
            _statusMessage.value = message
            _busy.value = false
        }
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            _appsLoading.value = true
            _apps.value = withContext(Dispatchers.IO) { queryInstalledApps() }
            _appsLoading.value = false
        }
    }

    private fun queryInstalledApps(): List<InstalledApp> {
        val pm = getApplication<Application>().packageManager
        return pm.getInstalledApplications(0)
            .map { info ->
                InstalledApp(
                    packageName = info.packageName,
                    label = pm.getApplicationLabel(info).toString(),
                    icon = runCatching { pm.getApplicationIcon(info).toImageBitmap() }.getOrNull(),
                    isSystem = info.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    private fun Drawable.toImageBitmap(): ImageBitmap {
        val bitmap = if (this is BitmapDrawable && bitmap != null) {
            bitmap
        } else {
            val width = intrinsicWidth.coerceAtLeast(96)
            val height = intrinsicHeight.coerceAtLeast(96)
            val created = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(created)
            setBounds(0, 0, canvas.width, canvas.height)
            draw(canvas)
            created
        }
        return bitmap.asImageBitmap()
    }
}
