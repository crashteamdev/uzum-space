package dev.crashteam.uzumspace.converter

import dev.crashteam.openapi.space.model.CloseToMinimalStrategy
import dev.crashteam.openapi.space.model.EqualPriceStrategy
import dev.crashteam.openapi.space.model.UzumAccountShopItemStrategy
import dev.crashteam.openapi.space.model.QuantityDependentStrategy
import dev.crashteam.openapi.space.model.Strategy
import dev.crashteam.uzumspace.db.model.enums.StrategyType
import dev.crashteam.uzumspace.repository.postgre.entity.strategy.UzumAccountShopItemStrategyEntity
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class UzumAccountShopItemStrategyConverter: DataConverter<UzumAccountShopItemStrategyEntity, UzumAccountShopItemStrategy> {
    override fun convert(source: UzumAccountShopItemStrategyEntity): UzumAccountShopItemStrategy {
        return UzumAccountShopItemStrategy().apply {
            id = source.id
            strategy = getStrategy(source)
        }
    }

    private fun getStrategy(source: UzumAccountShopItemStrategyEntity): Strategy {
        val minimumThreshold = (source.minimumThreshold?.toBigDecimal() ?: BigDecimal.ZERO).movePointLeft(2)
        val maximumThreshold = (source.maximumThreshold?.toBigDecimal() ?: BigDecimal.ZERO).movePointLeft(2)
        return when(source.strategyType) {
            StrategyType.close_to_minimal.name -> CloseToMinimalStrategy(source.step, source.strategyType,
                minimumThreshold.toDouble(), maximumThreshold.toDouble())
            StrategyType.quantity_dependent.name -> QuantityDependentStrategy(source.step, source.strategyType,
                minimumThreshold.toDouble(), maximumThreshold.toDouble())
            StrategyType.equal_price.name -> EqualPriceStrategy(source.strategyType, minimumThreshold.toDouble(),
                maximumThreshold.toDouble())
            else -> throw IllegalArgumentException("No such strategy - ${source.strategyType}")
        }
    }
}