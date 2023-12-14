package dev.crashteam.uzumspace.repository.postgre

import dev.crashteam.openapi.space.model.QuantityDependentStrategy
import dev.crashteam.openapi.space.model.Strategy
import dev.crashteam.uzumspace.db.model.enums.StrategyType
import dev.crashteam.uzumspace.db.model.tables.StrategyOption
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class QuantityDependentStrategyOptionRepository(private val dsl: DSLContext) :
    StrategyOptionRepository {
    override fun <T: Strategy> save(t: T): Long {
        val strategyOption = StrategyOption.STRATEGY_OPTION
        val strategy = t as QuantityDependentStrategy
        return dsl.insertInto(
            strategyOption,
            strategyOption.MAXIMUM_THRESHOLD,
            strategyOption.MINIMUM_THRESHOLD,
            strategyOption.STEP
        ).values(
            strategy.maximumThreshold.toBigDecimal().movePointRight(2).toLong(),
            strategy.minimumThreshold.toBigDecimal().movePointRight(2).toLong(),
            strategy.step
        ).returningResult(strategyOption.ID)
            .fetchOne()!!.getValue(strategyOption.ID)
    }

    override fun <T : Strategy> update(id: Long, t: T): Int {
        val strategy = t as QuantityDependentStrategy
        val strategyOption = StrategyOption.STRATEGY_OPTION
        return dsl.update(strategyOption)
            .set(strategyOption.MAXIMUM_THRESHOLD, strategy.minimumThreshold.toBigDecimal().movePointRight(2).toLong())
            .set(strategyOption.MINIMUM_THRESHOLD, strategy.minimumThreshold.toBigDecimal().movePointRight(2).toLong())
            .set(strategyOption.STEP, strategy.step)
            .where(strategyOption.ID.eq(id))
            .execute()
    }

    override fun strategyType(): StrategyType {
        return StrategyType.quantity_dependent
    }

}