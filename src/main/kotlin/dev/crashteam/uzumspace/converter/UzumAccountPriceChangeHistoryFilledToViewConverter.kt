package dev.crashteam.uzumspace.converter

import dev.crashteam.openapi.space.model.UzumAccountPriceChangeHistory
import dev.crashteam.uzumspace.repository.postgre.entity.UzumShopItemPriceHistoryEntityJointItemAndShopEntity
import org.springframework.stereotype.Component
import java.time.ZoneOffset

@Component
class UzumAccountPriceChangeHistoryFilledToViewConverter :
    DataConverter<UzumShopItemPriceHistoryEntityJointItemAndShopEntity, UzumAccountPriceChangeHistory> {

    override fun convert(source: UzumShopItemPriceHistoryEntityJointItemAndShopEntity): UzumAccountPriceChangeHistory {
        return UzumAccountPriceChangeHistory().apply {
            this.id = source.uzumAccountShopItemId
            this.productId = source.productId
            this.skuId = source.skuId
            this.shopName = source.shopName
            this.itemName = source.itemName
            this.oldPrice = source.oldPrice.toBigDecimal().setScale(2).toDouble()
            this.newPrice = source.price.toBigDecimal().setScale(2).toDouble()
            this.barcode = source.barcode
            this.changeTime = source.changeTime.atOffset(ZoneOffset.UTC)
            this.photoKey = source.photoKey
        }
    }
}
