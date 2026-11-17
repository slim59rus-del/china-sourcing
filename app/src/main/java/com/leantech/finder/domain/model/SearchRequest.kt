package com.leantech.finder.domain.model

/** Что и где искать. */
data class SearchRequest(
    val query: String,
    val marketplaces: Set<Marketplace>,
    val factoryOnly: Boolean,
    val sort: SortOption,
    val limitPerMarketplace: Int = 30,
)
