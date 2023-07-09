package dev.crashteam.uzumspace.repository.postgre.mapper

import dev.crashteam.uzumspace.db.model.tables.UzumAccountShop.UZUM_ACCOUNT_SHOP
import dev.crashteam.uzumspace.db.model.tables.UzumAccountShopItem.UZUM_ACCOUNT_SHOP_ITEM
import dev.crashteam.uzumspace.db.model.tables.UzumAccountShopItemPriceHistory.UZUM_ACCOUNT_SHOP_ITEM_PRICE_HISTORY
import dev.crashteam.uzumspace.repository.postgre.entity.UzumShopItemPriceHistoryEntityJointItemAndShopEntity
import org.springframework.stereotype.Component
import org.jooq.Record

@Component
class RecordToUzumAccountShopItemPriceHistoryEntityJoinShopItemAndShopMapper :
    RecordMapper<UzumShopItemPriceHistoryEntityJointItemAndShopEntity> {

    override fun convert(record: Record): UzumShopItemPriceHistoryEntityJointItemAndShopEntity {
        return UzumShopItemPriceHistoryEntityJointItemAndShopEntity(
            uzumAccountShopItemId = record.getValue(UZUM_ACCOUNT_SHOP_ITEM_PRICE_HISTORY.UZUM_ACCOUNT_SHOP_ITEM_ID),
            uzumAccountShopItemCompetitorId = record.getValue(UZUM_ACCOUNT_SHOP_ITEM_PRICE_HISTORY.UZUM_ACCOUNT_SHOP_ITEM_COMPETITOR_ID),
            productId = record.getValue(UZUM_ACCOUNT_SHOP_ITEM.PRODUCT_ID),
            skuId = record.getValue(UZUM_ACCOUNT_SHOP_ITEM.SKU_ID),
            shopName = record.getValue(UZUM_ACCOUNT_SHOP.NAME.`as`("shop_name")),
            itemName = record.getValue(UZUM_ACCOUNT_SHOP_ITEM.NAME.`as`("item_name")),
            changeTime = record.getValue(UZUM_ACCOUNT_SHOP_ITEM_PRICE_HISTORY.CHANGE_TIME),
            oldPrice = record.getValue(UZUM_ACCOUNT_SHOP_ITEM_PRICE_HISTORY.OLD_PRICE),
            price = record.getValue(UZUM_ACCOUNT_SHOP_ITEM_PRICE_HISTORY.PRICE),
            barcode = record.getValue(UZUM_ACCOUNT_SHOP_ITEM.BARCODE)
        )
    }
}
