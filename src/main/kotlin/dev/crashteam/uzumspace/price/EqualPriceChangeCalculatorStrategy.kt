package dev.crashteam.uzumspace.price

import dev.crashteam.uzumspace.price.model.CalculationResult
import dev.crashteam.uzumspace.price.model.CalculatorOptions
import dev.crashteam.uzumspace.repository.postgre.UzumAccountShopItemCompetitorRepository
import dev.crashteam.uzumspace.repository.postgre.entity.UzumAccountShopItemCompetitorEntity
import dev.crashteam.uzumspace.service.UzumShopItemService
import dev.crashteam.uzumspace.service.model.ShopItemCompetitor
import mu.KotlinLogging
import java.math.BigDecimal
import java.util.*

private val log = KotlinLogging.logger {}

class EqualPriceChangeCalculatorStrategy(
    private val uzumAccountShopItemCompetitorRepository: UzumAccountShopItemCompetitorRepository,
    private val uzumShopItemService: UzumShopItemService
) : PriceChangeCalculatorStrategy {
    override fun calculatePrice(
        uzumAccountShopItemId: UUID,
        sellPriceMinor: BigDecimal,
        options: CalculatorOptions?
    ): CalculationResult? {
        val shopItemCompetitors: List<UzumAccountShopItemCompetitorEntity> =
            uzumAccountShopItemCompetitorRepository.findShopItemCompetitors(uzumAccountShopItemId)
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

        var newPriceMinor: BigDecimal? = null
        if ((options?.minimumThreshold != null && options.maximumThreshold != null)
            && (competitorPriceMinor >= BigDecimal.valueOf(options.minimumThreshold)
                    && competitorPriceMinor <= BigDecimal.valueOf(options.maximumThreshold))
        ) {
            newPriceMinor = competitorPriceMinor
        } else if (options?.minimumThreshold != null && competitorPriceMinor < BigDecimal.valueOf(options.minimumThreshold)) {
            newPriceMinor = BigDecimal.valueOf(options.minimumThreshold)
        } else if (options?.maximumThreshold != null && competitorPriceMinor > BigDecimal.valueOf(options.maximumThreshold)) {
            newPriceMinor = BigDecimal.valueOf(options.maximumThreshold)
        }
        if (newPriceMinor != null && newPriceMinor.compareTo(sellPriceMinor) == 0) {
            log.info { "New price $newPriceMinor equal to current price for shop item $uzumAccountShopItemId" }
            return null
        } else if (newPriceMinor != null) {
            return CalculationResult(
                newPriceMinor = newPriceMinor,
                competitorId = minimalPriceCompetitor.competitorEntity.id
            )
        }

        if ((options?.maximumThreshold == null && competitorPriceMinor > sellPriceMinor)
            || (options?.minimumThreshold == null && competitorPriceMinor < sellPriceMinor)
        ) {
            return CalculationResult(
                newPriceMinor = competitorPriceMinor,
                competitorId = minimalPriceCompetitor.competitorEntity.id
            )
        }
        return null;
    }
}