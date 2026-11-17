package com.leantech.finder.domain.model

data class SourceProblem(
    val marketplace: Marketplace,
    val message: String,
    val needsLogin: Boolean = false,
    val webFallbackUrl: String? = null,
)

data class SearchOutcome(
    val translatedQuery: String,
    val products: List<Product>,
    val problems: List<SourceProblem>,
    val tookMs: Long,
) {
    val hasProblems: Boolean get() = problems.isNotEmpty()
}
