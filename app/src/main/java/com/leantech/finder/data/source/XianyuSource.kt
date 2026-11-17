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
 * 闲鱼 (Idle Fish / Goofish) — площадка частных объявлений и заводских излишков.
 * Признак «фабрика» здесь слабый: фильтр работает по словам в объявлении и профиле продавца,
 * а результаты с признаками перекупщика помечаются в описании продавца.
 */
class XianyuSource(
    private val mtop: MtopClient,
    private val settings: SettingsStore,
) : MarketplaceSource {

    override val marketplace = Marketplace.XIANYU

    override suspend fun search(query: String, request: SearchRequest): SourceResult {
        val data = JSONObject().apply {
            put("keyword", query)
            put("pageNumber", 1)
            put("rowsPerPage", request.limitPerMarketplace)
            put("sortField", sortField(request))
            put("sortValue", sortValue(request))
            put("searchReqFromPage", "xySearch")
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
                    message = error.message ?: "闲鱼 не ответил",
                    needsLogin = true,
                    webFallbackUrl = webSearchUrl(query),
                )
            )
        }
    }

    private fun sortField(request: SearchRequest): String =
        if (request.sort == SortOption.RELEVANCE) "default" else "price"

    private fun sortValue(request: SearchRequest): String = when (request.sort) {
        SortOption.PRICE_ASC -> "asc"
        else -> "desc"
    }

    private fun emptyProblem(query: String) = SourceProblem(
        marketplace = marketplace,
        message = "闲鱼 вернул пустой список: обычно нужен вход в аккаунт.",
        needsLogin = true,
        webFallbackUrl = webSearchUrl(query),
    )

    companion object {
        const val SEARCH_API = "mtop.taobao.idlemtopsearch.search"
    }
}
