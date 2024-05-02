package dev.crashteam.uzumspace.price

import dev.crashteam.uzumspace.price.model.CalculationResult
import dev.crashteam.uzumspace.price.model.CalculatorOptions
import dev.crashteam.uzumspace.repository.postgre.UzumAccountShopItemCompetitorRepository
import dev.crashteam.uzumspace.repository.postgre.entity.UzumAccountShopItemCompetitorEntity
import dev.crashteam.uzumspace.service.AnalyticsService
import dev.crashteam.uzumspace.service.UzumShopItemService
import dev.crashteam.uzumspace.service.model.ShopItemCompetitor
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.util.CollectionUtils
import java.math.BigDecimal
import java.util.*

private val log = KotlinLogging.logger {}

@Component
class CloseToMinimalPriceChangeCalculatorStrategy(
    private val uzumAccountShopItemCompetitorRepository: UzumAccountShopItemCompetitorRepository,
    private val uzumShopItemService: UzumShopItemService,
    private val analyticsService: AnalyticsService
) : PriceChangeCalculatorStrategy {

    override fun calculatePrice(
        uzumAccountShopItemId: UUID,
        sellPriceMinor: BigDecimal,
        options: CalculatorOptions?
    ): CalculationResult? {
        val shopItemCompetitors: List<UzumAccountShopItemCompetitorEntity> =
            uzumAccountShopItemCompetitorRepository.findShopItemCompetitors(uzumAccountShopItemId)
        if (CollectionUtils.isEmpty(shopItemCompetitors)) {
            log.info { "Not found competitors for shop item with id $uzumAccountShopItemId pool items." }
            return null
        }
        val minimalPriceCompetitor: ShopItemCompetitor = shopItemCompetitors.mapNotNull {
            val shopItemEntity = uzumShopItemService.findShopItem(
                it.productId,
                it.skuId
            ) ?: return@mapNotNull null
            ShopItemCompetitor(shopItemEntity, it)
        }.filter {
            if (options?.competitorAvailableAmount != null) {
                it.shopItemEntity.availableAmount > options.competitorAvailableAmount
            } else {
                true
            }
        }.minByOrNull {
            uzumShopItemService.getRecentPrice(it.shopItemEntity)!!
        } ?: return null

        if (minimalPriceCompetitor.shopItemEntity.availableAmount.toInt() <= 0 && options?.changeNotAvailableItemPrice == false) {
            log.info { "Competitor available amount is 0, not changing price" }
            return null
        }

        if (options?.competitorSalesAmount != null) {
            val competitorSales = analyticsService.getCompetitorSales(minimalPriceCompetitor.competitorEntity.productId)
                ?: return null
            if (competitorSales <= options.competitorSalesAmount) {
                log.info { "Last sale value of competitor is $competitorSales and our barrier is ${options.competitorSalesAmount}." }
                return null
            }
        }


        log.debug { "Minimal price competitor: $minimalPriceCompetitor" }
        val competitorPrice: BigDecimal = uzumShopItemService.getRecentPrice(minimalPriceCompetitor.shopItemEntity)!!
        val competitorPriceMinor = competitorPrice.movePointRight(2)
        log.debug { "Recent competitor price: $competitorPrice. keAccountShopItemId=$uzumAccountShopItemId" }

        if (competitorPriceMinor >= sellPriceMinor) {
            log.debug {
                "Competitor price is the same or higher." +
                        " competitorPrice=${competitorPriceMinor}; sellPrice=$sellPriceMinor. keAccountShopItemId=$uzumAccountShopItemId"
            }
            // If price too much higher than our we need to rise our price
            val expectedPriceMinor =
                competitorPriceMinor - (options?.step?.toBigDecimal() ?: BigDecimal.ZERO).movePointRight(2)
            if (expectedPriceMinor > sellPriceMinor && options?.maximumThreshold != null
                && expectedPriceMinor <= BigDecimal.valueOf(options.maximumThreshold)
            ) {
                return CalculationResult(
                    newPriceMinor = expectedPriceMinor,
                    competitorId = minimalPriceCompetitor.competitorEntity.id
                )
            }
            return null // No need to change price
        } else {
            val newPrice: BigDecimal = (competitorPrice - (options?.step?.toBigDecimal() ?: BigDecimal.ZERO))
            log.debug { "Competitor price = $competitorPrice. New price = $newPrice. Current sell price = $sellPriceMinor. keAccountShopItemId=$uzumAccountShopItemId" }

            var newPriceMinor = newPrice.movePointRight(2)
            if (options?.minimumThreshold != null && newPriceMinor < BigDecimal.valueOf(options.minimumThreshold)) {
                newPriceMinor = BigDecimal.valueOf(options.minimumThreshold)
            } else if (options?.maximumThreshold != null && newPriceMinor > BigDecimal.valueOf(options.maximumThreshold)) {
                newPriceMinor = BigDecimal.valueOf(options.maximumThreshold)
            }
            log.debug { "newPriceMinor=$newPriceMinor;sellPriceMinor=$sellPriceMinor;keAccountShopItemId=$uzumAccountShopItemId" }

            if (newPriceMinor == sellPriceMinor) return null // No need to change price

            return CalculationResult(
                newPriceMinor = newPriceMinor,
                competitorId = minimalPriceCompetitor.competitorEntity.id
            )
        }
    }
}
