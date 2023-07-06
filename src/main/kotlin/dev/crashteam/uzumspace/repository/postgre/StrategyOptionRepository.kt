package dev.crashteam.uzumspace.repository.postgre

import dev.crashteam.openapi.kerepricer.model.Strategy
import dev.crashteam.uzumspace.db.model.enums.StrategyType

interface StrategyOptionRepository {

    fun <T: Strategy> save(t: T): Long

    fun <T: Strategy> update(id: Long, t: T): Int

    fun strategyType(): StrategyType
}