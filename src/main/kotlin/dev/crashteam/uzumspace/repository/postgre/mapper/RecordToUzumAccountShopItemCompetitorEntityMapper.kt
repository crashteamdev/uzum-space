package dev.crashteam.uzumspace.repository.postgre.mapper

import dev.crashteam.uzumspace.db.model.tables.KeAccountShopItemCompetitor.KE_ACCOUNT_SHOP_ITEM_COMPETITOR
import dev.crashteam.uzumspace.repository.postgre.entity.UzumAccountShopItemCompetitorEntity
import org.springframework.stereotype.Component

@Component
class RecordToUzumAccountShopItemCompetitorEntityMapper :
    RecordMapper<UzumAccountShopItemCompetitorEntity> {

    override fun convert(record: Record): UzumAccountShopItemCompetitorEntity {
        return UzumAccountShopItemCompetitorEntity(
            id = record.getValue(KE_ACCOUNT_SHOP_ITEM_COMPETITOR.ID),
            keAccountShopItemId = record.getValue(KE_ACCOUNT_SHOP_ITEM_COMPETITOR.KE_ACCOUNT_SHOP_ITEM_ID),
            productId = record.getValue(KE_ACCOUNT_SHOP_ITEM_COMPETITOR.PRODUCT_ID),
            skuId = record.getValue(KE_ACCOUNT_SHOP_ITEM_COMPETITOR.SKU_ID),
        )
    }
}
