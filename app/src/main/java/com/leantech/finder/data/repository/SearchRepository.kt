package com.leantech.finder.data.repository

import com.leantech.finder.data.prefs.SettingsStore
import com.leantech.finder.data.source.MarketplaceSource
import com.leantech.finder.data.translate.Translator
import com.leantech.finder.domain.model.Marketplace
import com.leantech.finder.domain.model.Product
import com.leantech.finder.domain.model.SearchOutcome
import com.leantech.finder.domain.model.SearchRequest
import com.leantech.finder.domain.model.SortOption
import com.leantech.finder.domain.model.SourceProblem
import com.leantech.finder.domain.seller.SellerSummarizer
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Полный проход поиска: перевод запроса -> площадки параллельно -> фильтры и сортировка ->
 * русские подписи (название товара и описание продавца).
 */
class SearchRepository(
    private val sources: List<MarketplaceSource>,
    private val translator: Translator,
    private val summarizer: SellerSummarizer,
    private val settings: SettingsStore,
) {

    suspend fun search(
        request: SearchRequest,
        onStage: (String) -> Unit = {},
    ): SearchOutcome = coroutineScope {
        val startedAt = System.currentTimeMillis()

        onStage("Перевожу запрос на китайский")
        val chineseQuery = translator.toChinese(request.query)?.takeIf { it.isNotBlank() } ?: request.query

        onStage("Ищу на выбранных площадках")
        val active = sources.filter { it.marketplace in request.marketplaces }
        val results = active.map { source ->
            async { source.marketplace to runCatching { source.search(chineseQuery, request) } }
        }.awaitAll()

        val problems = ArrayList<SourceProblem>()
        val perMarketplace = LinkedHashMap<Marketplace, List<Product>>()

        for ((marketplace, outcome) in results) {
            val value = outcome.getOrNull()
            if (value == null) {
                val source = active.first { it.marketplace == marketplace }
                problems += SourceProblem(
                    marketplace = marketplace,
                    message = outcome.exceptionOrNull()?.message ?: "Площадка недоступна",
                    needsLogin = true,
                    webFallbackUrl = source.webSearchUrl(chineseQuery),
                )
                continue
            }
            value.problem?.let { problems += it }
            perMarketplace[marketplace] = refine(value.products, request)
        }

        onStage("Готовлю описания продавцов")
        val products = decorate(perMarketplace.values.flatten(), request)

        SearchOutcome(
            translatedQuery = chineseQuery,
            products = products,
            problems = problems,
            tookMs = System.currentTimeMillis() - startedAt,
        )
    }

    /** Фильтр и сортировка внутри одной площадки. */
    private fun refine(products: List<Product>, request: SearchRequest): List<Product> {
        var list = products
        if (request.factoryOnly) list = list.filter { it.seller.isFactory }

        list = when (request.sort) {
            SortOption.SALES_DESC -> list.sortedWith(
                compareByDescending<Product> { it.salesCount ?: -1 }
                    .thenBy { it.priceCny ?: Double.MAX_VALUE }
            )
            SortOption.PRICE_ASC -> list.sortedBy { it.priceCny ?: Double.MAX_VALUE }
            SortOption.PRICE_DESC -> list.sortedByDescending { it.priceCny ?: -1.0 }
            SortOption.RELEVANCE -> list
        }
        return list.take(request.limitPerMarketplace)
    }

    /** Русское название товара и описание продавца для первых карточек. */
    private suspend fun decorate(products: List<Product>, request: SearchRequest): List<Product> {
        val head = products.take(DECORATE_LIMIT)
        val decorated = coroutineScope {
            head.map { product ->
                async {
                    var updated = product
                    if (settings.translateTitles) {
                        val ru = runCatching { translator.toRussian(product.title) }.getOrNull()
                        if (!ru.isNullOrBlank()) updated = updated.withTitleRu(ru)
                    }
                    val description = runCatching { summarizer.describe(updated) }.getOrNull()
                    if (!description.isNullOrBlank()) updated = updated.withSellerDescription(description)
                    updated
                }
            }.awaitAll()
        }

        val merged = products.mapIndexed { index, product -> decorated.getOrNull(index) ?: product }
        return if (request.sort == SortOption.SALES_DESC) {
            merged.sortedByDescending { it.salesCount ?: -1 }
        } else {
            merged
        }
    }

    private companion object {
        /** Сколько карточек получает перевод и описание: каждый перевод — сетевой вызов. */
        const val DECORATE_LIMIT = 18
    }
}
