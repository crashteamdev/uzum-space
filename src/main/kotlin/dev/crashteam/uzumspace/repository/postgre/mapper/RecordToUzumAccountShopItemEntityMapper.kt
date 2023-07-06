package dev.crashteam.uzumspace.repository.postgre.mapper

import dev.crashteam.uzumspace.db.model.tables.KeAccountShopItem.KE_ACCOUNT_SHOP_ITEM
import dev.crashteam.uzumspace.db.model.tables.KeAccountShopItemPool
import dev.crashteam.uzumspace.repository.postgre.entity.UzumAccountShopItemEntity
import org.springframework.stereotype.Component

@Component
class RecordToUzumAccountShopItemEntityMapper : RecordMapper<UzumAccountShopItemEntity> {

    override fun convert(record: Record): UzumAccountShopItemEntity {
        return UzumAccountShopItemEntity(
            id = record.getValue(KE_ACCOUNT_SHOP_ITEM.ID),
            keAccountId = record.getValue(KE_ACCOUNT_SHOP_ITEM.KE_ACCOUNT_ID),
            keAccountShopId = record.getValue(KE_ACCOUNT_SHOP_ITEM.KE_ACCOUNT_SHOP_ID),
            categoryId = record.getValue(KE_ACCOUNT_SHOP_ITEM.CATEGORY_ID),
            productId = record.getValue(KE_ACCOUNT_SHOP_ITEM.PRODUCT_ID),
            skuId = record.getValue(KE_ACCOUNT_SHOP_ITEM.SKU_ID),
            name = record.getValue(KE_ACCOUNT_SHOP_ITEM.NAME),
            photoKey = record.getValue(KE_ACCOUNT_SHOP_ITEM.PHOTO_KEY),
            purchasePrice = record.getValue(KE_ACCOUNT_SHOP_ITEM.PURCHASE_PRICE),
            price = record.getValue(KE_ACCOUNT_SHOP_ITEM.PRICE),
            barCode = record.getValue(KE_ACCOUNT_SHOP_ITEM.BARCODE),
            productSku = record.getValue(KE_ACCOUNT_SHOP_ITEM.PRODUCT_SKU),
            skuTitle = record.getValue(KE_ACCOUNT_SHOP_ITEM.SKU_TITLE),
            availableAmount = record.getValue(KE_ACCOUNT_SHOP_ITEM.AVAILABLE_AMOUNT),
            minimumThreshold = record.getValue(KE_ACCOUNT_SHOP_ITEM.MINIMUM_THRESHOLD),
            maximumThreshold = record.getValue(KE_ACCOUNT_SHOP_ITEM.MAXIMUM_THRESHOLD),
            step = record.getValue(KE_ACCOUNT_SHOP_ITEM.STEP),
            lastUpdate = record.getValue(KE_ACCOUNT_SHOP_ITEM.LAST_UPDATE),
            discount = record.getValue(KE_ACCOUNT_SHOP_ITEM.DISCOUNT),
            isInPool = record.get(KeAccountShopItemPool.KE_ACCOUNT_SHOP_ITEM_POOL.KE_ACCOUNT_SHOP_ITEM_ID) != null,
            strategyId = record.getValue(KE_ACCOUNT_SHOP_ITEM.KE_ACCOUNT_SHOP_ITEM_STRATEGY_ID)
        )
    }
}
