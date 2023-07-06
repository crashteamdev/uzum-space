package dev.crashteam.uzumspace.price

import dev.crashteam.uzumspace.price.model.CalculationResult
import dev.crashteam.uzumspace.price.model.CalculatorOptions
import dev.crashteam.uzumspace.repository.postgre.UzumAccountShopItemCompetitorRepository
import dev.crashteam.uzumspace.service.UzumShopItemService
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.*

@Component
class QuantityDependentPriceChangeCalculatorStrategy(
    private val uzumAccountShopItemCompetitorRepository: UzumAccountShopItemCompetitorRepository,
    private val uzumShopItemService: UzumShopItemService
) : PriceChangeCalculatorStrategy {
    override fun calculatePrice(
        keAccountShopItemId: UUID,
        sellPriceMinor: BigDecimal,
        options: CalculatorOptions?
    ): CalculationResult? {
        TODO("Not yet implemented")
    }
}