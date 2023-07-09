package dev.crashteam.uzumspace.repository.postgre.mapper

import dev.crashteam.uzumspace.db.model.tables.UzumAccountShopItemCompetitor.UZUM_ACCOUNT_SHOP_ITEM_COMPETITOR
import dev.crashteam.uzumspace.repository.postgre.entity.UzumAccountShopItemCompetitorEntity
import org.springframework.stereotype.Component
import org.jooq.Record

@Component
class RecordToUzumAccountShopItemCompetitorEntityMapper :
    RecordMapper<UzumAccountShopItemCompetitorEntity> {

    override fun convert(record: Record): UzumAccountShopItemCompetitorEntity {
        return UzumAccountShopItemCompetitorEntity(
            id = record.getValue(UZUM_ACCOUNT_SHOP_ITEM_COMPETITOR.ID),
            uzumAccountShopItemId = record.getValue(UZUM_ACCOUNT_SHOP_ITEM_COMPETITOR.UZUM_ACCOUNT_SHOP_ITEM_ID),
            productId = record.getValue(UZUM_ACCOUNT_SHOP_ITEM_COMPETITOR.PRODUCT_ID),
            skuId = record.getValue(UZUM_ACCOUNT_SHOP_ITEM_COMPETITOR.SKU_ID),
        )
    }
}
