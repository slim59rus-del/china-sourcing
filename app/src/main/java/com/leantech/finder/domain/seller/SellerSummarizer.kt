package com.leantech.finder.domain.seller

import com.leantech.finder.data.translate.Translator
import com.leantech.finder.domain.model.Marketplace
import com.leantech.finder.domain.model.Product
import com.leantech.finder.domain.model.Seller

/**
 * Короткое описание продавца по-русски: название магазина, регион, срок работы, рейтинг, метки
 * площадки и вывод о том, фабрика это или перекупщик. Только факты из карточки — если данных нет,
 * так и написано.
 */
class SellerSummarizer(
    private val translator: Translator,
    private val translateFields: Boolean = true,
) {

    suspend fun describe(product: Product): String {
        val seller = product.seller
        val lines = mutableListOf<String>()

        lines += seller.name.takeIf { it.isNotBlank() }
            ?.let { "Продавец: $it" }
            ?: "Продавец: имя в карточке не указано"

        seller.location?.takeIf { it.isNotBlank() }?.let { raw ->
            val translated = if (translateFields) runCatching { translator.toRussian(raw) }.getOrNull() else null
            lines += if (translated.isNullOrBlank()) "Регион: $raw" else "Регион: $translated ($raw)"
        }

        seller.yearsOnPlatform?.let { years ->
            if (years in 1..60) lines += "Работает на площадке: около $years г."
        }
        seller.rating?.takeIf { it.isNotBlank() }?.let { lines += "Рейтинг площадки: $it" }

        product.salesText?.takeIf { it.isNotBlank() }?.let { sales ->
            lines += if (product.marketplace == Marketplace.XIANYU) {
                "Спрос: $sales (в 闲鱼 это счётчик «хотят купить», продаж у объявления нет)"
            } else {
                "Продажи: $sales"
            }
        }

        lines += when {
            seller.isFactory -> "Тип: фабрика/производитель. Признаки: " +
                seller.factorySignals.joinToString(", ")
            else -> "Тип: прямых признаков фабрики в карточке нет — похоже на магазин или посредника"
        }

        if (seller.tags.isNotEmpty()) {
            val tags = seller.tags.take(6).map { tag ->
                val ru = if (translateFields) runCatching { translator.toRussian(tag) }.getOrNull() else null
                if (ru.isNullOrBlank()) tag else ru
            }
            lines += "Метки площадки: " + tags.joinToString(", ")
        }

        seller.shopUrl?.let { lines += "Страница продавца: $it" }
        product.priceCny?.let { price -> lines += "Цена: $price ¥" }

        return lines.joinToString("\n")
    }

    /** Описание по продавцу, когда карточки товара под рукой нет. */
    suspend fun describeSeller(seller: Seller, marketplace: Marketplace = Marketplace.ALIBABA_1688): String =
        describe(
            Product(
                id = seller.id.orEmpty(),
                marketplace = marketplace,
                title = "",
                itemUrl = "",
                seller = seller,
            )
        )
}
