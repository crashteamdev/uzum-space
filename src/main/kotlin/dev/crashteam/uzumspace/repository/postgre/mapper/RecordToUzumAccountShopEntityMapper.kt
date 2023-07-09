package dev.crashteam.uzumspace.repository.postgre.mapper

import dev.crashteam.uzumspace.db.model.tables.UzumAccountShop.UZUM_ACCOUNT_SHOP
import dev.crashteam.uzumspace.repository.postgre.entity.UzumAccountShopEntity
import org.springframework.stereotype.Component
import org.jooq.Record

@Component
class RecordToUzumAccountShopEntityMapper : RecordMapper<UzumAccountShopEntity> {

    override fun convert(record: Record): UzumAccountShopEntity {
        return UzumAccountShopEntity(
            id = record.getValue(UZUM_ACCOUNT_SHOP.ID),
            uzumAccountId = record.getValue(UZUM_ACCOUNT_SHOP.UZUM_ACCOUNT_ID),
            externalShopId = record.getValue(UZUM_ACCOUNT_SHOP.EXTERNAL_SHOP_ID),
            name = record.getValue(UZUM_ACCOUNT_SHOP.NAME),
            skuTitle = record.get(UZUM_ACCOUNT_SHOP.SKU_TITLE)
        )
    }
}
