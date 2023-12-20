package dev.crashteam.uzumspace.repository.postgre.mapper

import dev.crashteam.repricer.repository.postgre.entity.UzumAccountShopEntityWithData
import dev.crashteam.repricer.repository.postgre.entity.UzumAccountShopData
import dev.crashteam.uzumspace.db.model.tables.UzumAccountShop.UZUM_ACCOUNT_SHOP
import org.jooq.Record
import org.springframework.stereotype.Component

@Component
class RecordToUzumAccountShopEntityDataMapper : RecordMapper<UzumAccountShopEntityWithData> {

    override fun convert(record: Record): UzumAccountShopEntityWithData {
        val shopData = UzumAccountShopData(countPoolItems = record.getValue("pool_count") as Int,
            countProducts = record.getValue("product_count") as Int,
            countSkus = record.getValue("sku_count") as Int)
        return UzumAccountShopEntityWithData(
            id = record.getValue(UZUM_ACCOUNT_SHOP.ID),
            uzumAccountId = record.getValue(UZUM_ACCOUNT_SHOP.UZUM_ACCOUNT_ID),
            externalShopId = record.getValue(UZUM_ACCOUNT_SHOP.EXTERNAL_SHOP_ID),
            name = record.getValue(UZUM_ACCOUNT_SHOP.NAME),
            skuTitle = record.get(UZUM_ACCOUNT_SHOP.SKU_TITLE),
            uzumAccountShopData = shopData
        )
    }
}
