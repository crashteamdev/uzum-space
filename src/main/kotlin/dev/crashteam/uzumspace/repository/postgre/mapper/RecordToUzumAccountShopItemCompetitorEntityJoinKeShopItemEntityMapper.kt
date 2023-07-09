package dev.crashteam.uzumspace.repository.postgre.mapper

import dev.crashteam.uzumspace.db.model.tables.UzumAccountShopItemCompetitor.UZUM_ACCOUNT_SHOP_ITEM_COMPETITOR
import dev.crashteam.uzumspace.db.model.tables.UzumShopItem.UZUM_SHOP_ITEM
import dev.crashteam.uzumspace.repository.postgre.entity.UzumAccountShopItemCompetitorEntityJoinUzumShopItemEntity
import org.springframework.stereotype.Component
import org.jooq.Record

@Component
class RecordToUzumAccountShopItemCompetitorEntityJoinUzumShopItemEntityMapper :
    RecordMapper<UzumAccountShopItemCompetitorEntityJoinUzumShopItemEntity> {

    override fun convert(record: Record): UzumAccountShopItemCompetitorEntityJoinUzumShopItemEntity {
        return UzumAccountShopItemCompetitorEntityJoinUzumShopItemEntity(
            id = record.getValue(UZUM_ACCOUNT_SHOP_ITEM_COMPETITOR.ID),
            uzumAccountShopItemId = record.getValue(UZUM_ACCOUNT_SHOP_ITEM_COMPETITOR.UZUM_ACCOUNT_SHOP_ITEM_ID),
            productId = record.getValue(UZUM_ACCOUNT_SHOP_ITEM_COMPETITOR.PRODUCT_ID),
            skuId = record.getValue(UZUM_ACCOUNT_SHOP_ITEM_COMPETITOR.SKU_ID),
            name = record.getValue(UZUM_SHOP_ITEM.NAME),
            availableAmount = record.getValue(UZUM_SHOP_ITEM.AVAILABLE_AMOUNT),
            price = record.getValue(UZUM_SHOP_ITEM.PRICE),
            photoKey = record.getValue(UZUM_SHOP_ITEM.PHOTO_KEY)
        )
    }
}
