package com.leantech.finder.data.source

import android.content.Context
import com.leantech.finder.data.net.CookieStore
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
 * 1688 — оптовая площадка фабрик и поставщиков, поэтому фильтр «только фабрики» здесь
 * наиболее осмыслен: площадка сама помечает продавцов метками 实力商家 / 超级工厂 / 源头工厂,
 * они приходят в тегах карточки и разбираются в JsonCards.
 */
class Alibaba1688Source(
    private val mtop: MtopClient,
    private val settings: SettingsStore,
    private val cookies: CookieStore? = null,
    private val context: Context? = null,
) : MarketplaceSource {

    override val marketplace = Marketplace.ALIBABA_1688

    override suspend fun search(query: String, request: SearchRequest): SourceResult {
        cookies?.restoreAllFromBackup()

        // 1. Основной путь без прокси: скрытый WebView с авторизованными куками 1688
        if (context != null && settings.proxyBaseUrl.isBlank()) {
            val webJson = WebViewSearchFetcher.fetchJsonOrDom(
                context = context,
                searchUrl = buildMobileSearchUrl(query, request),
            )
            if (webJson != null) {
                val webProducts = JsonCards.extract(webJson, marketplace, query)
                    .take(request.limitPerMarketplace)
                if (webProducts.isNotEmpty()) {
                    cookies?.backupKnownDomains()
                    return SourceResult(webProducts)
                }
            }
        }

        // 2. Резервный путь (или работа через свой прокси-сервер): MTOP API
        val data = JSONObject().apply {
            put("keywords", query)
            put("q", query)
            put("charset", "utf-8")
            put("beginPage", 1)
            put("page", 1)
            put("pageSize", request.limitPerMarketplace)
            put("n", request.limitPerMarketplace)
            put("sortType", sortValue(request))
            put("descendOrder", if (request.sort == SortOption.PRICE_ASC) "false" else "true")
            put("async", "true")
            put("filter", "")
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
                    message = error.message ?: "1688 не ответил",
                    needsLogin = true,
                    webFallbackUrl = webSearchUrl(query),
                )
            )
        }
    }

    private fun buildMobileSearchUrl(query: String, request: SearchRequest): String {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val sortSuffix = when (request.sort) {
            SortOption.SALES_DESC -> "&sortType=va_rmdarkgmv&descendOrder=true"
            SortOption.PRICE_ASC -> "&sortType=price&descendOrder=false"
            SortOption.PRICE_DESC -> "&sortType=price&descendOrder=true"
            SortOption.RELEVANCE -> ""
        }
        return "https://1688.com" + encoded + sortSuffix
    }

    private fun sortValue(request: SearchRequest): String = when (request.sort) {
        SortOption.SALES_DESC -> "va_rmdarkgmv"
        SortOption.PRICE_ASC -> "price"
        SortOption.PRICE_DESC -> "price"
        SortOption.RELEVANCE -> "default"
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
            "1688 вернул пустой список ($retInfo)"
        } else {
            "1688 вернул пустой список: попробуйте обновить вход или открыть поиск на сайте."
        }
        return SourceProblem(
            marketplace = marketplace,
            message = detail,
            needsLogin = true,
            webFallbackUrl = webSearchUrl(query),
        )
    }

    companion object {
        const val SEARCH_API = "mtop.1688.mobile.search.offer.search"
    }
}
