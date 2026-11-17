package com.leantech.finder.data.source

import com.leantech.finder.data.net.MtopClient
import com.leantech.finder.data.net.MtopException
import com.leantech.finder.data.prefs.SettingsStore
import com.leantech.finder.domain.model.Marketplace
import com.leantech.finder.domain.model.SearchRequest
import com.leantech.finder.domain.model.SortOption
import com.leantech.finder.domain.model.SourceProblem
import org.json.JSONObject

/**
 * Taobao: поиск через mtop (h5api.m.taobao.com).
 * Чаще всего нужен вход в аккаунт: без сессии площадка отдаёт пустой список.
 */
class TaobaoSource(
    private val mtop: MtopClient,
    private val settings: SettingsStore,
) : MarketplaceSource {

    override val marketplace = Marketplace.TAOBAO

    override suspend fun search(query: String, request: SearchRequest): SourceResult {
        val data = JSONObject().apply {
            put("q", query)
            put("s", 0)
            put("n", request.limitPerMarketplace)
            put("page", 1)
            put("sort", sortValue(request))
            put("style", "list")
            put("source", "h5")
            put("isNewSearch", "1")
        }

        return try {
            val response = mtop.call(
                host = marketplace.apiHost,
                api = SEARCH_API,
                version = "1.0",
                data = data,
                proxyBaseUrl = settings.proxyBaseUrl,
            )
            val products = JsonCards.extract(response, marketplace, query)
            if (products.isEmpty()) {
                SourceResult(problem = emptyProblem(query))
            } else {
                SourceResult(products)
            }
        } catch (error: MtopException) {
            SourceResult(
                problem = SourceProblem(
                    marketplace = marketplace,
                    message = error.message ?: "Taobao не ответил",
                    needsLogin = true,
                    webFallbackUrl = webSearchUrl(query),
                )
            )
        }
    }

    /** Taobao: sort=sale-desc — по продажам, price-asc | price-desc — по цене. */
    private fun sortValue(request: SearchRequest): String = when (request.sort) {
        SortOption.SALES_DESC -> "sale-desc"
        SortOption.PRICE_ASC -> "price-asc"
        SortOption.PRICE_DESC -> "price-desc"
        SortOption.RELEVANCE -> "default"
    }

    private fun emptyProblem(query: String) = SourceProblem(
        marketplace = marketplace,
        message = "Taobao вернул пустой список: почти всегда нужен вход в аккаунт.",
        needsLogin = true,
        webFallbackUrl = webSearchUrl(query),
    )

    companion object {
        const val SEARCH_API = "mtop.taobao.wsearch.appsearch"
    }
}
