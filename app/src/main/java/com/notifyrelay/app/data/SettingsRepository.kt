package com.notifyrelay.app.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun read(): AppSettings {
        val allowed = prefs.getString(KEY_ALLOWED, "")
            .orEmpty()
            .split('\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
        return AppSettings(
            botToken = prefs.getString(KEY_BOT_TOKEN, "").orEmpty(),
            chatId = prefs.getString(KEY_CHAT_ID, "").orEmpty(),
            forwardingEnabled = prefs.getBoolean(KEY_FORWARDING, false),
            ignoreOngoing = prefs.getBoolean(KEY_IGNORE_ONGOING, true),
            ignoreGroupSummaries = prefs.getBoolean(KEY_IGNORE_SUMMARIES, true),
            keywordInclude = prefs.getString(KEY_KEYWORD_INCLUDE, "").orEmpty(),
            keywordExclude = prefs.getString(KEY_KEYWORD_EXCLUDE, "").orEmpty(),
            allowedPackages = allowed,
            hideSystemApps = prefs.getBoolean(KEY_HIDE_SYSTEM, true),
        )
    }

    fun update(transform: (AppSettings) -> AppSettings): AppSettings {
        val next = transform(read())
        prefs.edit()
            .putString(KEY_BOT_TOKEN, next.botToken.trim())
            .putString(KEY_CHAT_ID, next.chatId.trim())
            .putBoolean(KEY_FORWARDING, next.forwardingEnabled)
            .putBoolean(KEY_IGNORE_ONGOING, next.ignoreOngoing)
            .putBoolean(KEY_IGNORE_SUMMARIES, next.ignoreGroupSummaries)
            .putString(KEY_KEYWORD_INCLUDE, next.keywordInclude.trim())
            .putString(KEY_KEYWORD_EXCLUDE, next.keywordExclude.trim())
            .putString(KEY_ALLOWED, next.allowedPackages.sorted().joinToString("\n"))
            .putBoolean(KEY_HIDE_SYSTEM, next.hideSystemApps)
            .apply()
        return next
    }

    fun readLogs(): List<ForwardedItem> {
        val raw = prefs.getString(KEY_LOGS, "[]").orEmpty()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(
                        ForwardedItem(
                            atMillis = obj.optLong("at"),
                            appLabel = obj.optString("app"),
                            title = obj.optString("title"),
                            text = obj.optString("text"),
                            success = obj.optBoolean("ok", true),
                            error = obj.optString("error").takeIf { it.isNotBlank() },
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun addLog(item: ForwardedItem) {
        val next = (listOf(item) + readLogs()).take(MAX_LOGS)
        val array = JSONArray()
        next.forEach { entry ->
            array.put(
                JSONObject()
                    .put("at", entry.atMillis)
                    .put("app", entry.appLabel)
                    .put("title", entry.title)
                    .put("text", entry.text)
                    .put("ok", entry.success)
                    .put("error", entry.error.orEmpty()),
            )
        }
        prefs.edit().putString(KEY_LOGS, array.toString()).apply()
    }

    fun clearLogs() {
        prefs.edit().putString(KEY_LOGS, "[]").apply()
    }

    companion object {
        private const val PREFS_NAME = "notify_relay"
        private const val KEY_BOT_TOKEN = "bot_token"
        private const val KEY_CHAT_ID = "chat_id"
        private const val KEY_FORWARDING = "forwarding_enabled"
        private const val KEY_IGNORE_ONGOING = "ignore_ongoing"
        private const val KEY_IGNORE_SUMMARIES = "ignore_summaries"
        private const val KEY_KEYWORD_INCLUDE = "keyword_include"
        private const val KEY_KEYWORD_EXCLUDE = "keyword_exclude"
        private const val KEY_ALLOWED = "allowed_packages"
        private const val KEY_HIDE_SYSTEM = "hide_system_apps"
        private const val KEY_LOGS = "forward_logs"
        private const val MAX_LOGS = 30
    }
}
