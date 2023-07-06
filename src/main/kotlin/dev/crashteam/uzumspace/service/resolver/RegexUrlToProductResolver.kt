package dev.crashteam.uzumspace.service.resolver

import dev.crashteam.uzumspace.client.uzum.UzumWebClient
import dev.crashteam.uzumspace.repository.postgre.UzumShopItemRepository
import org.springframework.stereotype.Component

@Component
class RegexUrlToProductResolver(
    private val uzumShopItemRepository: UzumShopItemRepository,
    private val kazanExpressWebClient: UzumWebClient
) : UrlToProductResolver {

    override fun resolve(url: String): ResolvedKeProduct? {
        val lastSplitOfUrl = url.split("-").last()
        if (lastSplitOfUrl.contains("skuid")) {
            val findAll = "[0-9]+".toRegex().findAll(lastSplitOfUrl)
            val productId = findAll.first().value
            val skuId = findAll.last().value
            return ResolvedKeProduct(productId, skuId)
        } else {
            val productId = "[0-9]+".toRegex().find(lastSplitOfUrl)?.value
            if (productId != null) {
                val kazanExpressShopItemEntities = uzumShopItemRepository.findByProductId(productId.toLong())
                return if (kazanExpressShopItemEntities.isNotEmpty()) {
                    ResolvedKeProduct(productId, kazanExpressShopItemEntities.first().skuId.toString())
                } else {
                    val productResponse = kazanExpressWebClient.getProductInfo(productId)
                    productResponse?.payload?.data?.skuList?.first()?.let {
                        ResolvedKeProduct(productId, it.id.toString())
                    }
                }
            }
        }
        return null
    }
}
