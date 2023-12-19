package dev.crashteam.uzumspace.filter

import dev.crashteam.uzumspace.controller.*
import dev.crashteam.uzumspace.db.model.tables.UzumAccountShopItem
import dev.crashteam.uzumspace.db.model.tables.records.UzumAccountShopItemRecord

class KeAccountShopItemFilterRecordMapper : FilterRecordMapper {
    override fun recordMapper(): Map<String, ViewFieldToTableFieldMapper<UzumAccountShopItemRecord, out Comparable<*>>> {
        return mapOf(
            "productId" to LongTableFieldMapper(UzumAccountShopItem.UZUM_ACCOUNT_SHOP_ITEM.PRODUCT_ID),
            "skuId" to LongTableFieldMapper(UzumAccountShopItem.UZUM_ACCOUNT_SHOP_ITEM.SKU_ID),
            "skuTitle" to StringTableFieldMapper(UzumAccountShopItem.UZUM_ACCOUNT_SHOP_ITEM.SKU_TITLE),
            "name" to StringTableFieldMapper(UzumAccountShopItem.UZUM_ACCOUNT_SHOP_ITEM.NAME),
            "photoKey" to StringTableFieldMapper(UzumAccountShopItem.UZUM_ACCOUNT_SHOP_ITEM.PHOTO_KEY),
            "purchasePrice" to LongTableFieldMapper(UzumAccountShopItem.UZUM_ACCOUNT_SHOP_ITEM.PURCHASE_PRICE),
            "price" to LongTableFieldMapper(UzumAccountShopItem.UZUM_ACCOUNT_SHOP_ITEM.PRICE),
            "barCode" to LongTableFieldMapper(UzumAccountShopItem.UZUM_ACCOUNT_SHOP_ITEM.BARCODE),
            "availableAmount" to LongTableFieldMapper(UzumAccountShopItem.UZUM_ACCOUNT_SHOP_ITEM.AVAILABLE_AMOUNT),
            "minimumThreshold" to LongTableFieldMapper(UzumAccountShopItem.UZUM_ACCOUNT_SHOP_ITEM.MINIMUM_THRESHOLD),
            "maximumThreshold" to LongTableFieldMapper(UzumAccountShopItem.UZUM_ACCOUNT_SHOP_ITEM.MAXIMUM_THRESHOLD),
            "step" to IntegerTableFieldMapper(UzumAccountShopItem.UZUM_ACCOUNT_SHOP_ITEM.STEP),
            "discount" to BigIntegerTableFieldMapper(UzumAccountShopItem.UZUM_ACCOUNT_SHOP_ITEM.DISCOUNT)
        )
    }
}
