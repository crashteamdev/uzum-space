package dev.crashteam.repricer.repository.postgre


import dev.crashteam.openapi.space.model.Strategy
import dev.crashteam.uzumspace.db.model.enums.StrategyType
import java.util.UUID

interface StrategyOptionRepository {

    fun <T: Strategy> save(id: Long, t: T): Long

    fun <T: Strategy> update(id: Long, t: T): Int

    fun strategyType(): StrategyType
}