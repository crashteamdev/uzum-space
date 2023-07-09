package dev.crashteam.uzumspace.repository.postgre

import dev.crashteam.openapi.space.model.CloseToMinimalStrategy
import dev.crashteam.openapi.space.model.Strategy
import dev.crashteam.uzumspace.db.model.enums.StrategyType
import dev.crashteam.uzumspace.db.model.tables.StrategyOption
import org.jooq.DSLContext


class CloseToMinimalStrategyOptionRepository(private val dsl: DSLContext) :
    StrategyOptionRepository {
    override fun <T: Strategy> save(t: T): Long {
        val strategy = t as CloseToMinimalStrategy
        val strategyOption = StrategyOption.STRATEGY_OPTION
        return dsl.insertInto(
            strategyOption,
            strategyOption.MAXIMUM_THRESHOLD,
            strategyOption.MINIMUM_THRESHOLD,
            strategyOption.STEP
        ).values(
            strategy.maximumThreshold.toLong(),
            strategy.minimumThreshold.toLong(),
            strategy.step
        ).returningResult(strategyOption.ID)
            .fetchOne()!!.getValue(strategyOption.ID)
    }

    override fun <T : Strategy> update(id: Long, t: T): Int {
        val strategy = t as CloseToMinimalStrategy
        val strategyOption = StrategyOption.STRATEGY_OPTION
        return dsl.update(strategyOption)
            .set(strategyOption.MAXIMUM_THRESHOLD, strategy.minimumThreshold.toLong())
            .set(strategyOption.MINIMUM_THRESHOLD, strategy.minimumThreshold.toLong())
            .set(strategyOption.STEP, strategy.step)
            .where(strategyOption.ID.eq(id))
            .execute()
    }

    override fun strategyType(): StrategyType {
        return StrategyType.close_to_minimal
    }
}