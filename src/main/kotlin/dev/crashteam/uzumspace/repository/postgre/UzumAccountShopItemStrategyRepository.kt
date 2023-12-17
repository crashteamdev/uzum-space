package dev.crashteam.uzumspace.repository.postgre

import dev.crashteam.openapi.space.model.AddStrategyRequest
import dev.crashteam.openapi.space.model.PatchStrategy
import dev.crashteam.openapi.space.model.Strategy
import dev.crashteam.repricer.repository.postgre.StrategyOptionRepository
import dev.crashteam.uzumspace.db.model.enums.StrategyType
import dev.crashteam.uzumspace.db.model.tables.StrategyOption.STRATEGY_OPTION
import dev.crashteam.uzumspace.db.model.tables.UzumAccountShopItemStrategy.UZUM_ACCOUNT_SHOP_ITEM_STRATEGY
import dev.crashteam.uzumspace.repository.postgre.entity.strategy.UzumAccountShopItemStrategyEntity
import dev.crashteam.uzumspace.repository.postgre.mapper.RecordToUzumAccountShopItemStrategyEntityMapper
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class UzumAccountShopItemStrategyRepository(
    private val dsl: DSLContext,
    private val strategiesMap: Map<StrategyType, StrategyOptionRepository>,
    private val strategyMapper: RecordToUzumAccountShopItemStrategyEntityMapper
) {

    fun save(strategyRequest: AddStrategyRequest): Long {
        val itemStrategy = UZUM_ACCOUNT_SHOP_ITEM_STRATEGY
        val strategyType = StrategyType.valueOf(strategyRequest.strategy.strategyType)
        val strategyId = dsl.insertInto(
            itemStrategy,
            itemStrategy.STRATEGY_TYPE,
            itemStrategy.UZUM_ACCOUNT_SHOP_ITEM_ID
        ).values(
            strategyType,
            strategyRequest.uzumAccountShopItemId
        ).returningResult(itemStrategy.ID)
            .fetchOne()!!
            .getValue(itemStrategy.ID)
        saveOption(strategyId, strategyRequest.strategy)
        return strategyId
    }

    fun update(id: UUID, patchStrategy: PatchStrategy): Long {
        val strategyType = StrategyType.valueOf(patchStrategy.strategy.strategyType)
        val itemStrategy = UZUM_ACCOUNT_SHOP_ITEM_STRATEGY
        val o = STRATEGY_OPTION

        dsl.update(itemStrategy)
            .set(itemStrategy.STRATEGY_TYPE, strategyType)
            .where(itemStrategy.UZUM_ACCOUNT_SHOP_ITEM_ID.eq(id))
            .returningResult(itemStrategy.ID)
            .execute()

        val strategyOptionId = dsl.select()
            .from(itemStrategy.innerJoin(o).on(itemStrategy.ID.eq(o.UZUM_ACCOUNT_SHOP_ITEM_STRATEGY_ID)))
            .fetchOne()!!.getValue(o.ID)
        updateOption(strategyOptionId, patchStrategy.strategy)
        return strategyOptionId
    }

    fun saveOption(id: Long, strategy: Strategy): Long {
        val strategyEntityType = StrategyType.valueOf(strategy.strategyType)
        return strategiesMap[strategyEntityType]!!.save(id, strategy)
    }

    fun updateOption(strategyOptionId: Long, strategy: Strategy): Int {
        val strategyEntityType = StrategyType.valueOf(strategy.strategyType)
        return strategiesMap[strategyEntityType]!!.update(strategyOptionId, strategy)
    }

    fun findById(keAccountShopItemId: UUID): UzumAccountShopItemStrategyEntity? {
        val i = UZUM_ACCOUNT_SHOP_ITEM_STRATEGY
        val o = STRATEGY_OPTION
        return dsl.select()
            .from(i.leftJoin(o).on(i.ID.eq(o.UZUM_ACCOUNT_SHOP_ITEM_STRATEGY_ID)))
            .where(i.UZUM_ACCOUNT_SHOP_ITEM_ID.eq(keAccountShopItemId))
            .fetchOne()?.map { strategyMapper.convert(it) }
    }

    fun deleteById(keAccountShopItemId: UUID): Int {
        val i = UZUM_ACCOUNT_SHOP_ITEM_STRATEGY
        return dsl.deleteFrom(i).where(i.UZUM_ACCOUNT_SHOP_ITEM_ID.eq(keAccountShopItemId)).execute();
    }
}