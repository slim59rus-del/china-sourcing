package com.leantech.finder.domain.model

/**
 * Продавец, которого удалось вытащить из карточки товара.
 * [factorySignals] — признаки, по которым решено, что это фабрика/производитель.
 */
data class Seller(
    val id: String? = null,
    val name: String = "",
    val shopUrl: String? = null,
    val location: String? = null,
    val rating: String? = null,
    val yearsOnPlatform: Int? = null,
    val tags: List<String> = emptyList(),
    val isFactory: Boolean = false,
    val factorySignals: List<String> = emptyList(),
) {
    val hasData: Boolean get() = name.isNotBlank() || shopUrl != null
}
