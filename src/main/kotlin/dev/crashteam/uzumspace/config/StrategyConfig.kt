package dev.crashteam.uzumspace.config

import dev.crashteam.repricer.repository.postgre.CloseToMinimalStrategyOptionRepository
import dev.crashteam.repricer.repository.postgre.EqualPriceStrategyOptionRepository
import dev.crashteam.repricer.repository.postgre.QuantityDependentStrategyOptionRepository
import dev.crashteam.repricer.repository.postgre.StrategyOptionRepository
import dev.crashteam.uzumspace.db.model.enums.StrategyType
import dev.crashteam.uzumspace.price.CloseToMinimalPriceChangeCalculatorStrategy
import dev.crashteam.uzumspace.price.EqualPriceChangeCalculatorStrategy
import dev.crashteam.uzumspace.price.PriceChangeCalculatorStrategy
import dev.crashteam.uzumspace.price.QuantityDependentPriceChangeCalculatorStrategy
import dev.crashteam.uzumspace.repository.postgre.UzumAccountShopItemCompetitorRepository
import dev.crashteam.uzumspace.service.UzumShopItemService
import org.jooq.DSLContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class StrategyConfig {

    @Bean
    fun strategies(dsl: DSLContext): Map<StrategyType, StrategyOptionRepository> {
        return mapOf(
            StrategyType.close_to_minimal to CloseToMinimalStrategyOptionRepository(dsl),
            StrategyType.quantity_dependent to QuantityDependentStrategyOptionRepository(dsl),
            StrategyType.equal_price to EqualPriceStrategyOptionRepository(dsl)
        )
    }

    @Bean
    fun calculators(
        competitorRepository: UzumAccountShopItemCompetitorRepository,
        uzumShopItemService: UzumShopItemService,
    ): Map<StrategyType, PriceChangeCalculatorStrategy> {
        return mapOf(
            StrategyType.close_to_minimal to CloseToMinimalPriceChangeCalculatorStrategy(
                competitorRepository,
                uzumShopItemService
            ),
            StrategyType.quantity_dependent to QuantityDependentPriceChangeCalculatorStrategy(
                competitorRepository,
                uzumShopItemService
            ),
            StrategyType.equal_price to EqualPriceChangeCalculatorStrategy(
                competitorRepository,
                uzumShopItemService
            )
        )
    }
}