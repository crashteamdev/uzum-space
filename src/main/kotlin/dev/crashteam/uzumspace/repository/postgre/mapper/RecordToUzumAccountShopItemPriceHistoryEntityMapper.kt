package dev.crashteam.uzumspace.repository.postgre.mapper

import dev.crashteam.uzumspace.db.model.tables.UzumAccountShopItemPriceHistory.UZUM_ACCOUNT_SHOP_ITEM_PRICE_HISTORY
import dev.crashteam.uzumspace.repository.postgre.entity.UzumShopItemPriceHistoryEntity
import org.springframework.stereotype.Component
import org.jooq.Record

@Component
class RecordToUzumAccountShopItemPriceHistoryEntityMapper :
    RecordMapper<UzumShopItemPriceHistoryEntity> {

    override fun convert(record: Record): UzumShopItemPriceHistoryEntity {
        return UzumShopItemPriceHistoryEntity(
            uzumAccountShopItemId = record.getValue(UZUM_ACCOUNT_SHOP_ITEM_PRICE_HISTORY.UZUM_ACCOUNT_SHOP_ITEM_ID),
            uzumAccountShopItemCompetitorId = record.getValue(UZUM_ACCOUNT_SHOP_ITEM_PRICE_HISTORY.UZUM_ACCOUNT_SHOP_ITEM_COMPETITOR_ID),
            changeTime = record.getValue(UZUM_ACCOUNT_SHOP_ITEM_PRICE_HISTORY.CHANGE_TIME),
            oldPrice = record.getValue(UZUM_ACCOUNT_SHOP_ITEM_PRICE_HISTORY.OLD_PRICE),
            price = record.getValue(UZUM_ACCOUNT_SHOP_ITEM_PRICE_HISTORY.PRICE)
        )
    }
}
