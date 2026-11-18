package com.leantech.finder.data.source

import android.content.Context
import com.leantech.finder.data.net.MtopClient
import com.leantech.finder.data.net.MtopException
import com.leantech.finder.data.net.WebViewSearchFetcher
import com.leantech.finder.data.prefs.SettingsStore
import com.leantech.finder.domain.model.Marketplace
import com.leantech.finder.domain.model.SearchRequest
import com.leantech.finder.domain.model.SortOption
import com.leantech.finder.domain.model.SourceProblem
import org.json.JSONObject
import java.net.URLEncoder

/**
 * 闲鱼 (Idle Fish / Goofish) — площадка частных объявлений и заводских излишков.
 * Признак «фабрика» здесь слабый: фильтр работает по словам в объявлении и профиле продавца,
 * а результаты с признаками перекупщика помечаются в описании продавца.
 */
class XianyuSource(
    private val mtop: MtopClient,
    private val settings: SettingsStore,
    private val context: Context? = null,
) : MarketplaceSource {

    override val marketplace = Marketplace.XIANYU

    override suspend fun search(query: String, request: SearchRequest): SourceResult {
        // 1. Основной путь без прокси: скрытый WebView с авторизованными куками Goofish (обходит RGV587_ERROR)
        if (context != null && settings.proxyBaseUrl.isBlank()) {
            val webJson = WebViewSearchFetcher.fetchJsonOrDom(
                context = context,
                searchUrl = buildGoofishSearchUrl(query),
            )
            if (webJson != null) {
                val webProducts = JsonCards.extract(webJson, marketplace, query)
                    .take(request.limitPerMarketplace)
                if (webProducts.isNotEmpty()) {
                    return SourceResult(webProducts)
                }
            }
        }

        // 2. Резервный путь: MTOP API
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
                .take(request.limitPerMarketplace)
            if (products.isEmpty()) {
                val retInfo = extractRetCode(response)
                SourceResult(problem = emptyProblem(query, retInfo))
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

    private fun buildGoofishSearchUrl(query: String): String {
        val encoded = URLEncoder.encode(query, "UTF-8")
        return "https://goofish.com" + encoded
    }

    private fun sortField(request: SearchRequest): String =
        if (request.sort == SortOption.RELEVANCE) "default" else "price"

    private fun sortValue(request: SearchRequest): String = when (request.sort) {
        SortOption.PRICE_ASC -> "asc"
        else -> "desc"
    }

    private fun extractRetCode(response: JSONObject): String? {
        val retArray = response.optJSONArray("ret") ?: return null
        val messages = buildList {
            for (i in 0 until retArray.length()) {
                val item = retArray.optString(i)
                if (item.isNotBlank() && !item.startsWith("SUCCESS")) {
                    add(item)
                }
            }
        }
        return messages.takeIf { it.isNotEmpty() }?.joinToString("; ")
    }

    private fun emptyProblem(query: String, retInfo: String? = null): SourceProblem {
        val detail = if (!retInfo.isNullOrBlank()) {
            "闲鱼 вернул пустой список ($retInfo)"
        } else {
            "闲鱼 вернул пустой список: обычно нужен вход в аккаунт."
        }
        return SourceProblem(
            marketplace = marketplace,
            message = detail,
            needsLogin = true,
            webFallbackUrl = webSearchUrl(query),
        )
    }

    companion object {
        const val SEARCH_API = "mtop.taobao.idlemtopsearch.search"
    }
}
