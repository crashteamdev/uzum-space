package dev.crashteam.repricer.repository.postgre

import dev.crashteam.openapi.space.model.EqualPriceStrategy
import dev.crashteam.openapi.space.model.Strategy
import dev.crashteam.uzumspace.db.model.enums.StrategyType
import dev.crashteam.uzumspace.db.model.tables.StrategyOption
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class EqualPriceStrategyOptionRepository(private val dsl: DSLContext) :
    StrategyOptionRepository {
    override fun <T: Strategy> save(id: Long, t: T): Long {
        val strategyOption = StrategyOption.STRATEGY_OPTION
        val strategy = t as EqualPriceStrategy
        return dsl.insertInto(
            strategyOption,
            strategyOption.MAXIMUM_THRESHOLD,
            strategyOption.MINIMUM_THRESHOLD,
            strategyOption.DISCOUNT,
            strategyOption.CHANGE_NOT_AVAILABLE_ITEM_PRICE,
            strategyOption.COMPETITOR_AVAILABLE_AMOUNT,
            strategyOption.COMPETITOR_SALES_AMOUNT,
            strategyOption.UZUM_ACCOUNT_SHOP_ITEM_STRATEGY_ID
        ).values(
            strategy.maximumThreshold.toBigDecimal().movePointRight(2).toLong(),
            strategy.minimumThreshold.toBigDecimal().movePointRight(2).toLong(),
            strategy.discount?.intValueExact(),
            strategy.changeNotAvailableItemPrice,
            strategy.competitorAvailableAmount,
            strategy.competitorSalesAmount,
            id
        ).returningResult(strategyOption.ID)
            .fetchOne()!!.getValue(strategyOption.ID)
    }

    override fun <T : Strategy> update(id: Long, t: T): Int {
        val strategy = t as EqualPriceStrategy
        val strategyOption = StrategyOption.STRATEGY_OPTION
        return dsl.update(strategyOption)
            .set(strategyOption.MAXIMUM_THRESHOLD, strategy.maximumThreshold.toBigDecimal().movePointRight(2).toLong())
            .set(strategyOption.MINIMUM_THRESHOLD, strategy.minimumThreshold.toBigDecimal().movePointRight(2).toLong())
            .set(strategyOption.DISCOUNT, strategy.discount?.intValueExact())
            .set(strategyOption.CHANGE_NOT_AVAILABLE_ITEM_PRICE, strategy.changeNotAvailableItemPrice)
            .set(strategyOption.COMPETITOR_AVAILABLE_AMOUNT, strategy.competitorAvailableAmount)
            .set(strategyOption.COMPETITOR_SALES_AMOUNT, strategy.competitorSalesAmount)
            .where(strategyOption.ID.eq(id))
            .execute()
    }

    override fun strategyType(): StrategyType {
        return StrategyType.equal_price
    }

}