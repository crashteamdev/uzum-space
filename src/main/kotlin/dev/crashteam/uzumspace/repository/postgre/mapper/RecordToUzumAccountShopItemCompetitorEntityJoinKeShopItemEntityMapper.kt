package dev.crashteam.uzumspace.repository.postgre.mapper

import dev.crashteam.uzumspace.db.model.tables.KeAccountShopItemCompetitor.KE_ACCOUNT_SHOP_ITEM_COMPETITOR
import dev.crashteam.uzumspace.db.model.tables.KeShopItem.KE_SHOP_ITEM
import dev.crashteam.uzumspace.repository.postgre.entity.UzumAccountShopItemCompetitorEntityJoinKeShopItemEntity
import org.springframework.stereotype.Component

@Component
class RecordToUzumAccountShopItemCompetitorEntityJoinKeShopItemEntityMapper :
    RecordMapper<UzumAccountShopItemCompetitorEntityJoinKeShopItemEntity> {

    override fun convert(record: Record): UzumAccountShopItemCompetitorEntityJoinKeShopItemEntity {
        return UzumAccountShopItemCompetitorEntityJoinKeShopItemEntity(
            id = record.getValue(KE_ACCOUNT_SHOP_ITEM_COMPETITOR.ID),
            keAccountShopItemId = record.getValue(KE_ACCOUNT_SHOP_ITEM_COMPETITOR.KE_ACCOUNT_SHOP_ITEM_ID),
            productId = record.getValue(KE_ACCOUNT_SHOP_ITEM_COMPETITOR.PRODUCT_ID),
            skuId = record.getValue(KE_ACCOUNT_SHOP_ITEM_COMPETITOR.SKU_ID),
            name = record.getValue(KE_SHOP_ITEM.NAME),
            availableAmount = record.getValue(KE_SHOP_ITEM.AVAILABLE_AMOUNT),
            price = record.getValue(KE_SHOP_ITEM.PRICE),
            photoKey = record.getValue(KE_SHOP_ITEM.PHOTO_KEY)
        )
    }
}
