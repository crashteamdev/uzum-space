package dev.crashteam.uzumspace.repository.postgre

import dev.crashteam.openapi.kerepricer.model.AddStrategyRequest
import dev.crashteam.openapi.kerepricer.model.PatchStrategy
import dev.crashteam.openapi.kerepricer.model.Strategy
import dev.crashteam.uzumspace.db.model.enums.StrategyType
import dev.crashteam.uzumspace.db.model.tables.KeAccountShopItemStrategy.KE_ACCOUNT_SHOP_ITEM_STRATEGY
import dev.crashteam.uzumspace.db.model.tables.StrategyOption.STRATEGY_OPTION
import dev.crashteam.uzumspace.repository.postgre.entity.strategy.UzumAccountShopItemStrategyEntity
import dev.crashteam.uzumspace.repository.postgre.mapper.RecordToUzumAccountShopItemStrategyEntityMapper
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class UzumAccountShopItemStrategyRepository(
    private val dsl: DSLContext,
    private val strategiesMap: Map<StrategyType, StrategyOptionRepository>,
    private val strategyMapper: RecordToUzumAccountShopItemStrategyEntityMapper
) {

    @Transactional
    fun save(strategyRequest: AddStrategyRequest): Long {
        val itemStrategy = KE_ACCOUNT_SHOP_ITEM_STRATEGY
        val strategyType = StrategyType.valueOf(strategyRequest.strategy.strategyType)

        val optionId = saveOption(strategyRequest.strategy)
        return dsl.insertInto(
            itemStrategy,
            itemStrategy.STRATEGY_TYPE,
            itemStrategy.STRATEGY_OPTION_ID
        ).values(
            strategyType,
            optionId
        ).returningResult(itemStrategy.ID)
            .fetchOne()!!
            .getValue(itemStrategy.ID)
    }

    @Transactional
    fun update(shopItemStrategyId: Long, patchStrategy: PatchStrategy): Int {
        val strategyType = StrategyType.valueOf(patchStrategy.strategy.strategyType)
        val itemStrategy = KE_ACCOUNT_SHOP_ITEM_STRATEGY
        val strategyOptionId = dsl.update(itemStrategy)
            .set(itemStrategy.STRATEGY_TYPE, strategyType)
            .where(itemStrategy.ID.eq(shopItemStrategyId))
            .returningResult(itemStrategy.STRATEGY_OPTION_ID)
            .fetchOne()!!.getValue(itemStrategy.STRATEGY_OPTION_ID)
        return updateOption(strategyOptionId, patchStrategy.strategy)
    }

    @Transactional
    fun saveOption(strategy: Strategy): Long {
        val strategyEntityType = StrategyType.valueOf(strategy.strategyType)
        return strategiesMap[strategyEntityType]!!.save(strategy)
    }

    @Transactional
    fun updateOption(shopItemStrategyId: Long, strategy: Strategy): Int {
        val strategyEntityType = StrategyType.valueOf(strategy.strategyType)
        return strategiesMap[strategyEntityType]!!.update(shopItemStrategyId, strategy)
    }

    fun findById(id: Long): UzumAccountShopItemStrategyEntity? {
        val i = KE_ACCOUNT_SHOP_ITEM_STRATEGY
        val o = STRATEGY_OPTION
        return dsl.select()
            .from(i.leftJoin(o).on(i.STRATEGY_OPTION_ID.eq(o.ID)))
            .where(i.ID.eq(id))
            .fetchOne()?.map { strategyMapper.convert(it) }
    }

    fun deleteById(id: Long): Int {
        val i = KE_ACCOUNT_SHOP_ITEM_STRATEGY
        return dsl.deleteFrom(i).where(i.ID.eq(id)).execute();
    }
}