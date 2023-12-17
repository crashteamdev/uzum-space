package dev.crashteam.uzumspace.repository.postgre.mapper

import dev.crashteam.uzumspace.db.model.tables.UzumAccountShopItemStrategy.UZUM_ACCOUNT_SHOP_ITEM_STRATEGY
import dev.crashteam.uzumspace.db.model.tables.StrategyOption.STRATEGY_OPTION
import dev.crashteam.uzumspace.repository.postgre.entity.strategy.UzumAccountShopItemStrategyEntity
import org.springframework.stereotype.Component
import org.jooq.Record

@Component
class RecordToUzumAccountShopItemStrategyEntityMapper : RecordMapper<UzumAccountShopItemStrategyEntity> {
    override fun convert(record: Record): UzumAccountShopItemStrategyEntity {
        return UzumAccountShopItemStrategyEntity(
            record.getValue(UZUM_ACCOUNT_SHOP_ITEM_STRATEGY.ID),
            record.getValue(UZUM_ACCOUNT_SHOP_ITEM_STRATEGY.STRATEGY_TYPE).literal,
            record.getValue(STRATEGY_OPTION.ID),
            record.getValue(STRATEGY_OPTION.MINIMUM_THRESHOLD),
            record.getValue(STRATEGY_OPTION.MAXIMUM_THRESHOLD),
            record.getValue(STRATEGY_OPTION.STEP),
            record.getValue(STRATEGY_OPTION.DISCOUNT),
            record.getValue(UZUM_ACCOUNT_SHOP_ITEM_STRATEGY.UZUM_ACCOUNT_SHOP_ITEM_ID)
        )
    }
}