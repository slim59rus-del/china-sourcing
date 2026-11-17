package com.leantech.finder.data.translate

/** Сначала онлайн-перевод, при неудаче — офлайн-словарь. */
class TranslatorChain(
    private vararg val translators: Translator,
) : Translator {

    override suspend fun toChinese(text: String): String? {
        for (translator in translators) {
            val result = runCatching { translator.toChinese(text) }.getOrNull()
            if (!result.isNullOrBlank()) return result
        }
        return null
    }

    override suspend fun toRussian(text: String): String? {
        for (translator in translators) {
            val result = runCatching { translator.toRussian(text) }.getOrNull()
            if (!result.isNullOrBlank()) return result
        }
        return null
    }
}
