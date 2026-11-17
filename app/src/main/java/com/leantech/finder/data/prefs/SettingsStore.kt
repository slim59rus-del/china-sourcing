package com.leantech.finder.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.leantech.finder.domain.model.Marketplace
import com.leantech.finder.domain.model.SortOption

/**
 * Настройки и сессия. Лежат в зашифрованном хранилище: там же пометки о входе в аккаунты.
 * Логин и пароль приложение не хранит — вход выполняется на странице площадки,
 * а приложение сохраняет только полученную сессию (cookie).
 */
class SettingsStore(context: Context) {

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "china_sourcing_prefs",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    var resultLimit: Int
        get() = prefs.getInt(KEY_LIMIT, 30)
        set(value) = prefs.edit().putInt(KEY_LIMIT, value.coerceIn(10, 100)).apply()

    var sort: SortOption
        get() = SortOption.entries.firstOrNull { it.name == prefs.getString(KEY_SORT, null) }
            ?: SortOption.SALES_DESC
        set(value) = prefs.edit().putString(KEY_SORT, value.name).apply()

    var factoryOnly: Boolean
        get() = prefs.getBoolean(KEY_FACTORY, false)
        set(value) = prefs.edit().putBoolean(KEY_FACTORY, value).apply()

    var selectedSources: Set<Marketplace>
        get() = prefs.getStringSet(KEY_SOURCES, null)
            ?.mapNotNull { key -> Marketplace.entries.firstOrNull { it.key == key } }
            ?.toSet()
            ?.takeIf { it.isNotEmpty() }
            ?: setOf(Marketplace.ALIBABA_1688)
        set(value) = prefs.edit().putStringSet(KEY_SOURCES, value.map { it.key }.toSet()).apply()

    var lastQuery: String
        get() = prefs.getString(KEY_LAST_QUERY, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_LAST_QUERY, value).apply()

    var translatorEndpoint: String
        get() = prefs.getString(KEY_TRANSLATOR, DEFAULT_TRANSLATOR).orEmpty()
        set(value) = prefs.edit().putString(KEY_TRANSLATOR, value.trim()).apply()

    /** Свой сервер для подписи и обхода блокировок: пусто — обращаемся к площадке напрямую. */
    var proxyBaseUrl: String
        get() = prefs.getString(KEY_PROXY, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_PROXY, value.trim()).apply()

    var translateTitles: Boolean
        get() = prefs.getBoolean(KEY_TRANSLATE_TITLES, true)
        set(value) = prefs.edit().putBoolean(KEY_TRANSLATE_TITLES, value).apply()

    fun markLoggedIn(marketplace: Marketplace, value: Boolean) =
        prefs.edit().putBoolean(loginKey(marketplace), value).apply()

    fun isMarkedLoggedIn(marketplace: Marketplace): Boolean =
        prefs.getBoolean(loginKey(marketplace), false)

    fun clearSession() {
        prefs.edit()
            .remove(KEY_LOGIN_TAOBAO)
            .remove(KEY_LOGIN_1688)
            .remove(KEY_LOGIN_XIANYU)
            .apply()
    }

    private fun loginKey(marketplace: Marketplace) = when (marketplace) {
        Marketplace.TAOBAO -> KEY_LOGIN_TAOBAO
        Marketplace.ALIBABA_1688 -> KEY_LOGIN_1688
        Marketplace.XIANYU -> KEY_LOGIN_XIANYU
    }

    companion object {
        const val DEFAULT_TRANSLATOR = "https://translate.googleapis.com/translate_a/single"

        private const val KEY_LIMIT = "result_limit"
        private const val KEY_SORT = "sort"
        private const val KEY_FACTORY = "factory_only"
        private const val KEY_SOURCES = "sources"
        private const val KEY_LAST_QUERY = "last_query"
        private const val KEY_TRANSLATOR = "translator_endpoint"
        private const val KEY_PROXY = "proxy_base_url"
        private const val KEY_TRANSLATE_TITLES = "translate_titles"
        private const val KEY_LOGIN_TAOBAO = "login_taobao"
        private const val KEY_LOGIN_1688 = "login_1688"
        private const val KEY_LOGIN_XIANYU = "login_xianyu"
    }
}
