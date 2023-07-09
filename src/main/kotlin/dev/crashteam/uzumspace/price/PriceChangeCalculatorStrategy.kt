package dev.crashteam.uzumspace.price

import dev.crashteam.uzumspace.price.model.CalculationResult
import dev.crashteam.uzumspace.price.model.CalculatorOptions
import java.math.BigDecimal
import java.util.*

interface PriceChangeCalculatorStrategy {
    fun calculatePrice(
        uzumAccountShopItemId: UUID,
        sellPriceMinor: BigDecimal,
        options: CalculatorOptions? = null
    ): CalculationResult?
}
