package dev.crashteam.uzumspace.converter

import dev.crashteam.openapi.space.model.UzumAccountCompetitorShopItem
import dev.crashteam.uzumspace.repository.postgre.entity.UzumAccountShopItemCompetitorEntityJoinUzumShopItemEntity
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class UzumAccountShopItemCompetitorToViewConverter :
    DataConverter<UzumAccountShopItemCompetitorEntityJoinUzumShopItemEntity, UzumAccountCompetitorShopItem> {
    override fun convert(source: UzumAccountShopItemCompetitorEntityJoinUzumShopItemEntity): UzumAccountCompetitorShopItem {
        return UzumAccountCompetitorShopItem().apply {
            this.id = source.id
            this.name = source.name
            this.productId = source.productId
            this.skuId = source.skuId
            this.price = BigDecimal.valueOf(source.price, 2).toDouble()
            this.availableAmount = source.availableAmount
            this.photoKey = source.photoKey
        }
    }
}
