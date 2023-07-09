package dev.crashteam.uzumspace.repository.postgre.mapper

import dev.crashteam.uzumspace.db.model.tables.UzumAccountShopItem.UZUM_ACCOUNT_SHOP_ITEM
import dev.crashteam.uzumspace.db.model.tables.UzumAccountShopItemPool
import dev.crashteam.uzumspace.repository.postgre.entity.UzumAccountShopItemEntity
import org.springframework.stereotype.Component
import org.jooq.Record

@Component
class RecordToUzumAccountShopItemEntityMapper : RecordMapper<UzumAccountShopItemEntity> {

    override fun convert(record: Record): UzumAccountShopItemEntity {
        return UzumAccountShopItemEntity(
            id = record.getValue(UZUM_ACCOUNT_SHOP_ITEM.ID),
            uzumAccountId = record.getValue(UZUM_ACCOUNT_SHOP_ITEM.UZUM_ACCOUNT_ID),
            uzumAccountShopId = record.getValue(UZUM_ACCOUNT_SHOP_ITEM.UZUM_ACCOUNT_SHOP_ID),
            categoryId = record.getValue(UZUM_ACCOUNT_SHOP_ITEM.CATEGORY_ID),
            productId = record.getValue(UZUM_ACCOUNT_SHOP_ITEM.PRODUCT_ID),
            skuId = record.getValue(UZUM_ACCOUNT_SHOP_ITEM.SKU_ID),
            name = record.getValue(UZUM_ACCOUNT_SHOP_ITEM.NAME),
            photoKey = record.getValue(UZUM_ACCOUNT_SHOP_ITEM.PHOTO_KEY),
            purchasePrice = record.getValue(UZUM_ACCOUNT_SHOP_ITEM.PURCHASE_PRICE),
            price = record.getValue(UZUM_ACCOUNT_SHOP_ITEM.PRICE),
            barCode = record.getValue(UZUM_ACCOUNT_SHOP_ITEM.BARCODE),
            productSku = record.getValue(UZUM_ACCOUNT_SHOP_ITEM.PRODUCT_SKU),
            skuTitle = record.getValue(UZUM_ACCOUNT_SHOP_ITEM.SKU_TITLE),
            availableAmount = record.getValue(UZUM_ACCOUNT_SHOP_ITEM.AVAILABLE_AMOUNT),
            minimumThreshold = record.getValue(UZUM_ACCOUNT_SHOP_ITEM.MINIMUM_THRESHOLD),
            maximumThreshold = record.getValue(UZUM_ACCOUNT_SHOP_ITEM.MAXIMUM_THRESHOLD),
            step = record.getValue(UZUM_ACCOUNT_SHOP_ITEM.STEP),
            lastUpdate = record.getValue(UZUM_ACCOUNT_SHOP_ITEM.LAST_UPDATE),
            discount = record.getValue(UZUM_ACCOUNT_SHOP_ITEM.DISCOUNT),
            isInPool = record.get(UzumAccountShopItemPool.UZUM_ACCOUNT_SHOP_ITEM_POOL.UZUM_ACCOUNT_SHOP_ITEM_ID) != null,
            strategyId = record.getValue(UZUM_ACCOUNT_SHOP_ITEM.UZUM_ACCOUNT_SHOP_ITEM_STRATEGY_ID)
        )
    }
}
