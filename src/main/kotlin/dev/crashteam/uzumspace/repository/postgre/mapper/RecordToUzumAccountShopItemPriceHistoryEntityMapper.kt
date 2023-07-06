package dev.crashteam.uzumspace.repository.postgre.mapper

import dev.crashteam.uzumspace.db.model.tables.KeAccountShopItemPriceHistory.KE_ACCOUNT_SHOP_ITEM_PRICE_HISTORY
import dev.crashteam.uzumspace.repository.postgre.entity.UzumShopItemPriceHistoryEntity
import org.springframework.stereotype.Component

@Component
class RecordToUzumAccountShopItemPriceHistoryEntityMapper :
    RecordMapper<UzumShopItemPriceHistoryEntity> {

    override fun convert(record: Record): UzumShopItemPriceHistoryEntity {
        return UzumShopItemPriceHistoryEntity(
            keAccountShopItemId = record.getValue(KE_ACCOUNT_SHOP_ITEM_PRICE_HISTORY.KE_ACCOUNT_SHOP_ITEM_ID),
            keAccountShopItemCompetitorId = record.getValue(KE_ACCOUNT_SHOP_ITEM_PRICE_HISTORY.KE_ACCOUNT_SHOP_ITEM_COMPETITOR_ID),
            changeTime = record.getValue(KE_ACCOUNT_SHOP_ITEM_PRICE_HISTORY.CHANGE_TIME),
            oldPrice = record.getValue(KE_ACCOUNT_SHOP_ITEM_PRICE_HISTORY.OLD_PRICE),
            price = record.getValue(KE_ACCOUNT_SHOP_ITEM_PRICE_HISTORY.PRICE)
        )
    }
}
