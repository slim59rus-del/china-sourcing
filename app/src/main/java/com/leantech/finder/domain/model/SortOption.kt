package com.leantech.finder.domain.model

/** Сортировка результатов. */
enum class SortOption(val label: String) {
    SALES_DESC("По продажам (сначала больше)"),
    PRICE_ASC("Сначала дешёвые"),
    PRICE_DESC("Сначала дорогие"),
    RELEVANCE("По релевантности площадки"),
    ;
}
