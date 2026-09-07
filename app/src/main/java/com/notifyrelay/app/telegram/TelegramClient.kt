package com.notifyrelay.app.telegram

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class TelegramClient(
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build(),
) {
    suspend fun sendMessage(
        token: String,
        chatId: String,
        text: String,
        parseMode: String? = null,
    ): Result<Unit> {
        return post(token, "sendMessage") {
            put("chat_id", chatId)
            put("text", text)
            put("disable_web_page_preview", true)
            if (!parseMode.isNullOrBlank()) put("parse_mode", parseMode)
        }.map { }
    }

    suspend fun detectChatId(token: String): Result<String> {
        return get(token, "getUpdates").mapCatching { json ->
            extractChatId(json.optJSONArray("result"))
                ?: error("No chat found. Open Telegram on your iPhone, start your bot, send it any message, then tap Detect again.")
        }
    }

    private suspend fun get(token: String, method: String): Result<JSONObject> {
        val request = Request.Builder()
            .url("https://api.telegram.org/bot${token.trim()}/$method")
            .get()
            .build()
        return execute(request)
    }

    private suspend fun post(
        token: String,
        method: String,
        body: JSONObject.() -> Unit,
    ): Result<JSONObject> {
        val json = JSONObject().apply(body).toString()
        val request = Request.Builder()
            .url("https://api.telegram.org/bot${token.trim()}/$method")
            .post(json.toRequestBody(JSON_MEDIA))
            .build()
        return execute(request)
    }

    private suspend fun execute(request: Request): Result<JSONObject> = withContext(Dispatchers.IO) {
        runCatching {
            http.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                val json = JSONObject(raw.ifBlank { "{}" })
                if (!response.isSuccessful || !json.optBoolean("ok", false)) {
                    val description = json.optString("description").ifBlank { response.message }
                    error(description.ifBlank { "Telegram request failed (${response.code})" })
                }
                json
            }
        }
    }

    companion object {
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

        fun extractChatId(updates: JSONArray?): String? {
            if (updates == null) return null
            for (i in updates.length() - 1 downTo 0) {
                val update = updates.optJSONObject(i) ?: continue
                val chat = update.optJSONObject("message")?.optJSONObject("chat")
                    ?: update.optJSONObject("channel_post")?.optJSONObject("chat")
                    ?: update.optJSONObject("my_chat_member")?.optJSONObject("chat")
                if (chat != null && chat.has("id")) {
                    return chat.get("id").toString()
                }
            }
            return null
        }

        fun formatNotification(appLabel: String, title: String, text: String): String {
            val body = text.trim()
            return buildString {
                append("<b>🔔 App</b>: ")
                append(escapeHtml(appLabel.ifBlank { "Notification" }))
                append("\n<b>Title</b>: ")
                append(escapeHtml(title))
                if (body.isNotBlank() && body != title) {
                    append("\n<b>Content</b>:")
                    append("\n")
                    append(escapeHtml(body))
                }
            }.trim()
        }

        private fun escapeHtml(value: String): String {
            return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
        }
    }
}
