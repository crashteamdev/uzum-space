package dev.crashteam.uzumspace.converter

import dev.crashteam.openapi.space.model.UzumAccountShopItem
import dev.crashteam.uzumspace.repository.postgre.entity.UzumAccountShopItemEntity
import dev.crashteam.uzumspace.repository.postgre.entity.UzumAccountShopItemEntityWithLimitData
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class UzumAccountShopItemEntityWithLimitDataToViewConverter :
    DataConverter<UzumAccountShopItemEntityWithLimitData, UzumAccountShopItem> {

    override fun convert(source: UzumAccountShopItemEntityWithLimitData): UzumAccountShopItem {
        return UzumAccountShopItem().apply {
            this.id = source.id
            this.productId = source.productId
            this.skuId = source.skuId
            this.skuTitle = source.skuTitle
            this.name = source.name
            this.photoKey = source.photoKey
            this.price = BigDecimal.valueOf(source.price).movePointLeft(2).toDouble()
            this.barcode = source.barCode
            this.availableAmount = source.availableAmount
            this.minimumThreshold = source.minimumThreshold?.let { BigDecimal.valueOf(it).movePointLeft(2).toDouble() }
            this.maximumThreshold = source.maximumThreshold?.let { BigDecimal.valueOf(it).movePointLeft(2).toDouble() }
            this.step = source.step
            this.discount = source.discount?.toBigDecimal()
            this.isInPool = source.isInPool
            this.competitorsCurrent = source.competitorsCurrent
            this.availableCompetitors = source.availableCompetitors
        }
    }
}
