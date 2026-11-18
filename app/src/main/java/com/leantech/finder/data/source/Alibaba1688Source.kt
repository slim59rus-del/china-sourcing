package com.leantech.finder.data.source

import android.content.Context
import com.leantech.finder.data.net.CookieStore
import com.leantech.finder.data.net.MtopClient
import com.leantech.finder.data.net.MtopException
import com.leantech.finder.data.net.WebViewSearchFetcher
import com.leantech.finder.data.prefs.SettingsStore
import com.leantech.finder.domain.model.Marketplace
import com.leantech.finder.domain.model.Product
import com.leantech.finder.domain.model.SearchRequest
import com.leantech.finder.domain.model.SortOption
import com.leantech.finder.domain.model.SourceProblem
import org.json.JSONObject
import java.net.URLEncoder

class Alibaba1688Source(
    private val mtop: MtopClient,
    private val settings: SettingsStore,
    private val cookies: CookieStore? = null,
    private val context: Context? = null,
) : MarketplaceSource {

    override val marketplace = Marketplace.ALIBABA_1688

    override suspend fun search(query: String, request: SearchRequest): SourceResult {
        var lastError: String? = null

        // 1. Попытка через MTOP API
        for (apiName in MOBILE_SEARCH_APIS) {
            try {
                val response = mtop.call(
                    host = marketplace.apiHost,
                    api = apiName,
                    version = "1.0",
                    data = buildMobilePayload(query, request),
                    proxyBaseUrl = settings.proxyBaseUrl,
                )
                val products = JsonCards.extract(response, marketplace, query)
                if (products.isNotEmpty()) {
                    return SourceResult(products.take(request.limitPerMarketplace))
                }
                val ret = extractRetCode(response)
                if (!ret.isNullOrBlank() && !ret.contains("API_NOT_FOUNDED")) {
                    lastError = ret
                }
            } catch (e: MtopException) {
                lastError = e.message
            }
        }

        // 2. Поиск через скрытый WebView с авторизованными куками (обход RGV587 / x5sec)
        val ctx = context
        if (ctx != null) {
            val webUrl = buildMobileSearchUrl(query, request)
            val webJson = WebViewSearchFetcher.fetchJsonOrDom(ctx, webUrl)
            if (webJson != null) {
                val webProducts = JsonCards.extract(webJson, marketplace, query)
                if (webProducts.isNotEmpty()) {
                    cookies?.backupKnownDomains()
                    return SourceResult(webProducts.take(request.limitPerMarketplace))
                }
            }
        }

        return SourceResult(
            problem = SourceProblem(
                marketplace = marketplace,
                message = if (!lastError.isNullOrBlank()) {
                    "1688 вернул пустой список ($lastError)"
                } else {
                    "1688 вернул пустой список: попробуйте обновить вход или открыть поиск на сайте."
                },
                needsLogin = true,
                webFallbackUrl = webSearchUrl(query),
            )
        )
    }

    private fun buildMobileSearchUrl(query: String, request: SearchRequest): String {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val sortParam = when (request.sort) {
            SortOption.SALES_DESC -> "&sortType=va_rmdarkgmv&descendOrder=true"
            SortOption.PRICE_ASC -> "&sortType=price&descendOrder=false"
            SortOption.PRICE_DESC -> "&sortType=price&descendOrder=true"
            SortOption.RELEVANCE -> ""
        }
        return "https://1688.com" + encoded + sortParam
    }

    private fun buildMobilePayload(query: String, request: SearchRequest): JSONObject {
        val (sortField, descend) = sortParams(request)
        return JSONObject().apply {
            put("keywords", query)
            put("q", query)
            put("beginPage", 1)
            put("page", 1)
            put("pageSize", request.limitPerMarketplace)
            put("sortType", sortField)
            put("descendOrder", descend)
            put("charset", "utf-8")
        }
    }

    private fun sortParams(request: SearchRequest): Pair<String, String> = when (request.sort) {
        SortOption.SALES_DESC -> "va_rmdarkgmv" to "true"
        SortOption.PRICE_ASC -> "price" to "false"
        SortOption.PRICE_DESC -> "price" to "true"
        SortOption.RELEVANCE -> "default" to "true"
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

    companion object {
        val MOBILE_SEARCH_APIS = listOf(
            "mtop.1688.mobile.search.offer.search",
            "mtop.alibaba.wireless.search.offer.search",
        )
    }
}
