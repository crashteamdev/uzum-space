package dev.crashteam.uzumspace.price

import dev.crashteam.uzumspace.price.model.CalculationResult
import dev.crashteam.uzumspace.price.model.CalculatorOptions
import dev.crashteam.uzumspace.repository.postgre.UzumAccountShopItemCompetitorRepository
import dev.crashteam.uzumspace.repository.postgre.entity.UzumAccountShopItemCompetitorEntity
import dev.crashteam.uzumspace.service.UzumShopItemService
import dev.crashteam.uzumspace.service.model.ShopItemCompetitor
import java.math.BigDecimal
import java.util.*

class EqualPriceChangeCalculatorStrategy(
    private val uzumAccountShopItemCompetitorRepository: UzumAccountShopItemCompetitorRepository,
    private val uzumShopItemService: UzumShopItemService
) : PriceChangeCalculatorStrategy {
    override fun calculatePrice(
        keAccountShopItemId: UUID,
        sellPriceMinor: BigDecimal,
        options: CalculatorOptions?
    ): CalculationResult? {
        val shopItemCompetitors: List<UzumAccountShopItemCompetitorEntity> =
            uzumAccountShopItemCompetitorRepository.findShopItemCompetitors(keAccountShopItemId)
        val minimalPriceCompetitor: ShopItemCompetitor = shopItemCompetitors.mapNotNull {
            val shopItemEntity = uzumShopItemService.findShopItem(
                it.productId,
                it.skuId
            ) ?: return@mapNotNull null
            ShopItemCompetitor(shopItemEntity, it)
        }.filter {
            it.shopItemEntity.availableAmount > 0
        }.minByOrNull {
            uzumShopItemService.getRecentPrice(it.shopItemEntity)!!
        } ?: return null
        val competitorPrice: BigDecimal = uzumShopItemService.getRecentPrice(minimalPriceCompetitor.shopItemEntity)!!
        val competitorPriceMinor = competitorPrice.movePointRight(2)

        if (options?.minimumThreshold != null && competitorPriceMinor < BigDecimal.valueOf(options.minimumThreshold)) {
            return CalculationResult(
                newPriceMinor = BigDecimal.valueOf(options.minimumThreshold),
                competitorId = minimalPriceCompetitor.competitorEntity.id
            )
        } else if (options?.maximumThreshold != null && competitorPriceMinor > BigDecimal.valueOf(options.maximumThreshold)) {
            return CalculationResult(
                newPriceMinor =  BigDecimal.valueOf(options.maximumThreshold),
                competitorId = minimalPriceCompetitor.competitorEntity.id
            )
        }

        if (competitorPriceMinor > sellPriceMinor || competitorPriceMinor < sellPriceMinor) {
            return CalculationResult(
                newPriceMinor = competitorPriceMinor,
                competitorId = minimalPriceCompetitor.competitorEntity.id
            )
        }
        return null;
    }
}