package com.leantech.finder.data.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class MtopException(message: String, val retCodes: List<String> = emptyList()) : Exception(message)

/**
 * Клиент h5api (mtop): сам подписывает запрос и повторяет его один раз, если сервер вернул
 * просроченный токен (в этом случае площадка успевает выдать новый _m_h5_tk).
 *
 * Два режима:
 *  - напрямую (proxyBaseUrl пустой) — приложение говорит с площадкой от своего имени;
 *  - через свой сервер (proxyBaseUrl заполнен) — подпись делает сервер, приложение только
 *    передаёт api и data. Это обычный путь для рабочей сборки.
 */
class MtopClient(
    private val cookies: CookieStore,
    private val http: OkHttpClient = defaultClient(),
    private val appKey: String = "12574478",
) {

    suspend fun call(
        host: String,
        api: String,
        version: String,
        data: JSONObject,
        proxyBaseUrl: String = "",
        extraQuery: Map<String, String> = emptyMap(),
    ): JSONObject = withContext(Dispatchers.IO) {
        execute(host, api, version, data, proxyBaseUrl, extraQuery, retry = true)
    }

    private suspend fun execute(
        host: String,
        api: String,
        version: String,
        data: JSONObject,
        proxyBaseUrl: String,
        extraQuery: Map<String, String>,
        retry: Boolean,
    ): JSONObject {
        val payload = data.toString()
        val url = if (proxyBaseUrl.isNotBlank()) {
            proxyUrl(proxyBaseUrl, host, api, version, payload)
        } else {
            directUrl(host, api, version, payload, extraQuery)
        }

        val body = get(url)
        val json = runCatching { JSONObject(body) }.getOrElse {
            throw MtopException("Ответ площадки не является JSON: ${body.take(180)}")
        }
        val ret = json.optJSONArray("ret")?.let { array ->
            (0 until array.length()).map { array.optString(it) }
        }.orEmpty()

        val tokenGone = ret.any { code ->
            code.contains("令牌") || code.contains("TOKEN") || code.contains("FAIL_SYS_ILLEGAL_ACCESS")
        }
        if (tokenGone && retry) {
            cookies.flush()
            return execute(host, api, version, data, proxyBaseUrl, extraQuery, retry = false)
        }
        if (tokenGone) {
            throw MtopException(loginHint(host), ret)
        }
        if (body.contains("\"SUCCESS::")) return json

        // Часть ответов приходит с ret=SUCCESS, но пустым содержимым — отдаём как есть,
        // разбор сделает извлекатель карточек.
        return json
    }

    private fun loginHint(host: String): String =
        if (host.contains("1688")) "1688 не отдал результаты: войдите в аккаунт 1688 в приложении"
        else if (host.contains("goofish")) "闲鱼 не отдал результаты: войдите в аккаунт 闲鱼 в приложении"
        else "Taobao не отдал результаты: войдите в аккаунт Taobao в приложении"

    private fun directUrl(
        host: String,
        api: String,
        version: String,
        payload: String,
        extraQuery: Map<String, String>,
    ): String {
        val timestamp = System.currentTimeMillis().toString()
        val token = cookies.read(host, "_m_h5_tk")?.substringBefore('_').orEmpty()
        val sign = Sign.mtop(token, timestamp, appKey, payload)

        val params = LinkedHashMap<String, String>()
        params["jsv"] = "2.7.2"
        params["appKey"] = appKey
        params["t"] = timestamp
        params["sign"] = sign
        params["api"] = api
        params["v"] = version
        params["type"] = "json"
        params["dataType"] = "json"
        params["H5Request"] = "true"
        params["data"] = payload
        params.putAll(extraQuery)

        return "$host/h5/$api/$version/?" + params.entries.joinToString("&") { (key, value) ->
            "$key=${enc(value)}"
        }
    }

    private fun proxyUrl(
        proxyBaseUrl: String,
        host: String,
        api: String,
        version: String,
        payload: String,
    ): String {
        val separator = if (proxyBaseUrl.contains('?')) '&' else '?'
        return proxyBaseUrl.trimEnd('/') + separator +
            "api=" + enc(api) + "&v=" + enc(version) + "&host=" + enc(host) + "&data=" + enc(payload)
    }

    private fun get(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", MOBILE_UA)
            .header("Accept", "application/json, text/plain, */*")
            .header("Referer", refererFor(url))
            .apply {
                val cookie = cookies.cookieHeader(url)
                if (cookie.isNotBlank()) header("Cookie", cookie)
            }
            .build()

        http.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw MtopException("HTTP ${response.code} от ${request.url.host}")
            }
            return text
        }
    }

    private fun refererFor(url: String): String = when {
        url.contains("1688") -> "https://m.1688.com/"
        url.contains("goofish") -> "https://www.goofish.com/"
        else -> "https://m.taobao.com/"
    }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")

    companion object {
        const val MOBILE_UA =
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/124.0.0.0 Mobile Safari/537.36"

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .callTimeout(35, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }
}
