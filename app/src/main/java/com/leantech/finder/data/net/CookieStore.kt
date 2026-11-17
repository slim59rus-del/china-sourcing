package com.leantech.finder.data.net

import android.webkit.CookieManager

/**
 * Единое хранилище cookie: то же системное хранилище, куда пишет встроенный браузер приложения.
 * Поэтому после ручного входа в аккаунт запросы уходят уже авторизованными.
 */
class CookieStore(private val cookieManager: CookieManager = CookieManager.getInstance()) {

    fun cookieHeader(url: String): String = cookieManager.getCookie(url).orEmpty()

    fun read(url: String, name: String): String? = cookieHeader(url)
        .split(';')
        .asSequence()
        .map { it.trim() }
        .firstOrNull { it.startsWith("$name=") }
        ?.substringAfter('=')

    fun has(url: String, name: String): Boolean = read(url, name) != null

    fun clear() {
        cookieManager.removeAllCookies(null)
        cookieManager.flush()
    }

    fun flush() = cookieManager.flush()
}
