package dev.crashteam.uzumspace.repository.postgre.mapper

import dev.crashteam.uzumspace.db.model.tables.UzumShopItem.UZUM_SHOP_ITEM
import dev.crashteam.uzumspace.repository.postgre.entity.UzumShopItemEntity
import org.springframework.stereotype.Component
import org.jooq.Record

@Component
class RecordToUzumShopItemMapper : RecordMapper<UzumShopItemEntity> {

    override fun convert(record: Record): UzumShopItemEntity {
        return UzumShopItemEntity(
            productId = record.getValue(UZUM_SHOP_ITEM.PRODUCT_ID),
            skuId = record.getValue(UZUM_SHOP_ITEM.SKU_ID),
            categoryId = record.getValue(UZUM_SHOP_ITEM.CATEGORY_ID),
            name = record.getValue(UZUM_SHOP_ITEM.NAME),
            photoKey = record.getValue(UZUM_SHOP_ITEM.PHOTO_KEY),
            avgHashFingerprint = record.getValue(UZUM_SHOP_ITEM.AVG_HASH_FINGERPRINT),
            pHashFingerprint = record.getValue(UZUM_SHOP_ITEM.P_HASH_FINGERPRINT),
            price = record.getValue(UZUM_SHOP_ITEM.PRICE),
            lastUpdate = record.getValue(UZUM_SHOP_ITEM.LAST_UPDATE),
            availableAmount = record.getValue(UZUM_SHOP_ITEM.AVAILABLE_AMOUNT)
        )
    }
}
