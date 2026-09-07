package com.notifyrelay.app.data

data class AppSettings(
    val botToken: String = "",
    val chatId: String = "",
    val forwardingEnabled: Boolean = false,
    val ignoreOngoing: Boolean = true,
    val ignoreGroupSummaries: Boolean = true,
    val keywordInclude: String = "",
    val keywordExclude: String = "",
    val allowedPackages: Set<String> = emptySet(),
    val hideSystemApps: Boolean = true,
) {
    val telegramConfigured: Boolean
        get() = botToken.isNotBlank() && chatId.isNotBlank()

    val canForward: Boolean
        get() = forwardingEnabled && telegramConfigured && allowedPackages.isNotEmpty()
}

data class ForwardedItem(
    val atMillis: Long,
    val appLabel: String,
    val title: String,
    val text: String,
    val success: Boolean,
    val error: String? = null,
)
