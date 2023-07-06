package dev.crashteam.uzumspace.service.resolver

interface UrlToProductResolver {
    fun resolve(url: String): ResolvedKeProduct?
}

data class ResolvedKeProduct(
    val productId: String,
    val skuId: String
)
