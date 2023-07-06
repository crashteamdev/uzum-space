package dev.crashteam.uzumspace.repository.postgre.mapper

import dev.crashteam.uzumspace.db.model.tables.KeAccountShop.KE_ACCOUNT_SHOP
import dev.crashteam.uzumspace.repository.postgre.entity.UzumAccountShopEntity
import org.springframework.stereotype.Component

@Component
class RecordToUzumAccountShopEntityMapper : RecordMapper<UzumAccountShopEntity> {

    override fun convert(record: Record): UzumAccountShopEntity {
        return UzumAccountShopEntity(
            id = record.getValue(KE_ACCOUNT_SHOP.ID),
            keAccountId = record.getValue(KE_ACCOUNT_SHOP.KE_ACCOUNT_ID),
            externalShopId = record.getValue(KE_ACCOUNT_SHOP.EXTERNAL_SHOP_ID),
            name = record.getValue(KE_ACCOUNT_SHOP.NAME),
            skuTitle = record.get(KE_ACCOUNT_SHOP.SKU_TITLE)
        )
    }
}
