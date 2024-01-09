package dev.crashteam.uzumspace.converter

import dev.crashteam.openapi.space.model.UzumAccountShopItemStrategy
import dev.crashteam.uzumspace.repository.postgre.entity.strategy.UzumAccountShopItemStrategyEntity
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class UzumAccountShopItemStrategyConverter: DataConverter<UzumAccountShopItemStrategyEntity, UzumAccountShopItemStrategy> {
    override fun convert(source: UzumAccountShopItemStrategyEntity): UzumAccountShopItemStrategy {
        return UzumAccountShopItemStrategy().apply {
            id = source.id
            minimumThreshold = (source.minimumThreshold?.toBigDecimal() ?: BigDecimal.ZERO).movePointLeft(2).toDouble()
            maximumThreshold = (source.maximumThreshold?.toBigDecimal() ?: BigDecimal.ZERO).movePointLeft(2).toDouble()
            step = source.step
            strategyType = source.strategyType
            discount = source.discount?.toBigDecimal()
            accountShopItemId = source.accountShopItemId
        }
    }
}