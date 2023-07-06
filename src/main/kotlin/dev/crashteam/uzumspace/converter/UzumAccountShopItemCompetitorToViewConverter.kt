package dev.crashteam.uzumspace.converter

import dev.crashteam.openapi.kerepricer.model.KeAccountCompetitorShopItem
import dev.crashteam.uzumspace.repository.postgre.entity.UzumAccountShopItemCompetitorEntityJoinKeShopItemEntity
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class UzumAccountShopItemCompetitorToViewConverter :
    DataConverter<UzumAccountShopItemCompetitorEntityJoinKeShopItemEntity, KeAccountCompetitorShopItem> {
    override fun convert(source: UzumAccountShopItemCompetitorEntityJoinKeShopItemEntity): KeAccountCompetitorShopItem {
        return KeAccountCompetitorShopItem().apply {
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
