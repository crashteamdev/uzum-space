package dev.crashteam.uzumspace.repository.postgre.mapper

import dev.crashteam.uzumspace.db.model.tables.KeAccountShop.KE_ACCOUNT_SHOP
import dev.crashteam.uzumspace.db.model.tables.KeAccountShopItem.KE_ACCOUNT_SHOP_ITEM
import dev.crashteam.uzumspace.db.model.tables.KeAccountShopItemPriceHistory.KE_ACCOUNT_SHOP_ITEM_PRICE_HISTORY
import dev.crashteam.uzumspace.repository.postgre.entity.UzumShopItemPriceHistoryEntityJointItemAndShopEntity
import org.springframework.stereotype.Component

@Component
class RecordToUzumAccountShopItemPriceHistoryEntityJoinShopItemAndShopMapper :
    RecordMapper<UzumShopItemPriceHistoryEntityJointItemAndShopEntity> {

    override fun convert(record: Record): UzumShopItemPriceHistoryEntityJointItemAndShopEntity {
        return UzumShopItemPriceHistoryEntityJointItemAndShopEntity(
            keAccountShopItemId = record.getValue(KE_ACCOUNT_SHOP_ITEM_PRICE_HISTORY.KE_ACCOUNT_SHOP_ITEM_ID),
            keAccountShopItemCompetitorId = record.getValue(KE_ACCOUNT_SHOP_ITEM_PRICE_HISTORY.KE_ACCOUNT_SHOP_ITEM_COMPETITOR_ID),
            productId = record.getValue(KE_ACCOUNT_SHOP_ITEM.PRODUCT_ID),
            skuId = record.getValue(KE_ACCOUNT_SHOP_ITEM.SKU_ID),
            shopName = record.getValue(KE_ACCOUNT_SHOP.NAME.`as`("shop_name")),
            itemName = record.getValue(KE_ACCOUNT_SHOP_ITEM.NAME.`as`("item_name")),
            changeTime = record.getValue(KE_ACCOUNT_SHOP_ITEM_PRICE_HISTORY.CHANGE_TIME),
            oldPrice = record.getValue(KE_ACCOUNT_SHOP_ITEM_PRICE_HISTORY.OLD_PRICE),
            price = record.getValue(KE_ACCOUNT_SHOP_ITEM_PRICE_HISTORY.PRICE),
            barcode = record.getValue(KE_ACCOUNT_SHOP_ITEM.BARCODE)
        )
    }
}
