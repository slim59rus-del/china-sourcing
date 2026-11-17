package com.leantech.finder.data.translate

/** Перевод запроса и подписей. */
interface Translator {
    /** Русский (или любой другой) запрос -> китайский. */
    suspend fun toChinese(text: String): String?

    /** Китайский текст -> русский. */
    suspend fun toRussian(text: String): String?
}
