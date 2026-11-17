package com.leantech.finder.domain.model

/** Найденный товар: ссылка, цена, продажи, продавец и русские подписи. */
data class Product(
    val id: String,
    val marketplace: Marketplace,
    val title: String,
    val itemUrl: String,
    val titleRu: String? = null,
    val priceCny: Double? = null,
    val priceText: String = "",
    val salesCount: Int? = null,
    val salesText: String? = null,
    val imageUrl: String? = null,
    val seller: Seller = Seller(),
    /** Краткое описание продавца по-русски: заполняется при подготовке результата. */
    val sellerDescription: String? = null,
) {
    fun withTitleRu(text: String): Product = copy(titleRu = text)

    fun withSellerDescription(text: String): Product = copy(sellerDescription = text)

    val displayTitle: String get() = titleRu?.takeIf { it.isNotBlank() } ?: title
}
