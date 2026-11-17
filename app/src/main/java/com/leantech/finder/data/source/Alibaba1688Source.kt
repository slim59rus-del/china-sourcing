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
 * 1688 — оптовая площадка фабрик и поставщиков, поэтому фильтр «только фабрики» здесь
 * наиболее осмыслен: площадка сама помечает продавцов метками 实力商家 / 超级工厂 / 源头工厂,
 * они приходят в тегах карточки и разбираются в JsonCards.
 */
class Alibaba1688Source(
    private val mtop: MtopClient,
    private val settings: SettingsStore,
) : MarketplaceSource {

    override val marketplace = Marketplace.ALIBABA_1688

    override suspend fun search(query: String, request: SearchRequest): SourceResult {
        val data = JSONObject().apply {
            put("keywords", query)
            put("beginPage", 1)
            put("pageSize", request.limitPerMarketplace)
            put("sortType", sortValue(request))
            put("n", request.limitPerMarketplace)
            put("async", "true")
            put("filter", buildFilter(request))
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
                    message = error.message ?: "1688 не ответил",
                    needsLogin = true,
                    webFallbackUrl = webSearchUrl(query),
                )
            )
        }
    }

    /**
     * Строка фильтров площадки. Её синтаксис 1688 меняет, поэтому фильтр «фабрика» продублирован
     * на стороне приложения (Seller.isFactory): даже если серверная часть фильтра не сработает,
     * результат останется правильным.
     */
    private fun buildFilter(request: SearchRequest): String =
        if (request.factoryOnly) "sellerType:factory" else ""

    private fun sortValue(request: SearchRequest): String = when (request.sort) {
        SortOption.SALES_DESC -> "va_rmdarkgmv"
        SortOption.PRICE_ASC -> "price_asc"
        SortOption.PRICE_DESC -> "price_desc"
        SortOption.RELEVANCE -> "relevance"
    }

    private fun emptyProblem(query: String) = SourceProblem(
        marketplace = marketplace,
        message = "1688 вернул пустой список: чаще всего нужен вход в аккаунт поставщика.",
        needsLogin = true,
        webFallbackUrl = webSearchUrl(query),
    )

    companion object {
        const val SEARCH_API = "mtop.alibaba.pc.newsearch.searchoffers"
    }
}
