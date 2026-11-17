package com.leantech.finder.data.source

import com.leantech.finder.domain.model.Marketplace
import com.leantech.finder.domain.model.Product
import com.leantech.finder.domain.model.SearchRequest
import com.leantech.finder.domain.model.SourceProblem
import java.net.URLEncoder

/** Результат одной площадки: товары и (возможно) причина, по которой данных нет. */
data class SourceResult(
    val products: List<Product> = emptyList(),
    val problem: SourceProblem? = null,
)

/** Источник поиска (Taobao / 1688 / 闲鱼). */
interface MarketplaceSource {
    val marketplace: Marketplace

    /** [query] уже переведён на китайский. */
    suspend fun search(query: String, request: SearchRequest): SourceResult

    /** Веб-страница поиска — запасной путь, если API не отдал данные. */
    fun webSearchUrl(query: String): String =
        marketplace.searchUrlTemplate.format(URLEncoder.encode(query, "UTF-8"))
}
