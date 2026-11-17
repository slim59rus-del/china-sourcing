package com.leantech.finder.data.translate

import com.leantech.finder.data.net.MtopClient
import com.leantech.finder.data.prefs.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Онлайн-перевод через настраиваемый адрес (по умолчанию публичный переводчик Google).
 * Адрес меняется в настройках приложения: если он недоступен из вашей сети, укажите свой
 * (собственный прокси или сервис с тем же форматом ответа).
 */
class WebTranslator(
    private val settings: SettingsStore,
    private val http: OkHttpClient = defaultClient(),
) : Translator {

    override suspend fun toChinese(text: String): String? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        if (isMostlyChinese(trimmed)) return trimmed
        return translate(trimmed, source = "ru", target = "zh-CN")
    }

    override suspend fun toRussian(text: String): String? =
        translate(text.trim(), source = "zh-CN", target = "ru")

    private suspend fun translate(text: String, source: String, target: String): String? =
        withContext(Dispatchers.IO) {
            if (text.isEmpty()) return@withContext null
            val endpoint = settings.translatorEndpoint.ifBlank { SettingsStore.DEFAULT_TRANSLATOR }
            val separator = if (endpoint.contains('?')) '&' else '?'
            val url = endpoint.trimEnd('/') + separator +
                "client=gtx&sl=" + source + "&tl=" + target + "&dt=t&q=" +
                URLEncoder.encode(text, "UTF-8")

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", MtopClient.MOBILE_UA)
                .build()

            runCatching {
                http.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val body = response.body?.string().orEmpty()
                    parse(body)
                }
            }.getOrNull()
        }

    /** Формат ответа: [[["перевод","оригинал",...], ...], ...] */
    private fun parse(body: String): String? {
        val root = runCatching { JSONArray(body) }.getOrNull() ?: return null
        val segments = root.optJSONArray(0) ?: return null
        val builder = StringBuilder()
        for (index in 0 until segments.length()) {
            val segment = segments.optJSONArray(index) ?: continue
            val piece = segment.optString(0)
            if (piece.isNotBlank()) builder.append(piece)
        }
        return builder.toString().trim().ifBlank { null }
    }

    /** Доля китайских иероглифов: если запрос уже на китайском, переводить не нужно. */
    fun isMostlyChinese(text: String): Boolean {
        val han = text.count { it.code in 0x4E00..0x9FFF }
        return han > 0 && han >= text.count { !it.isWhitespace() } / 2
    }

    companion object {
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}
