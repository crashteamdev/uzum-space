package dev.crashteam.uzumspace.converter

import dev.crashteam.openapi.space.model.ShopData
import dev.crashteam.openapi.space.model.UzumAccountShop
import dev.crashteam.repricer.repository.postgre.entity.UzumAccountShopEntityWithData
import org.springframework.stereotype.Component

@Component
class UzumAccountShopEntityWithDataToViewConverter : DataConverter<UzumAccountShopEntityWithData, UzumAccountShop> {

    override fun convert(source: UzumAccountShopEntityWithData): UzumAccountShop {
        return UzumAccountShop().apply {
            id = source.id
            name = source.name
            skuTitle = source.skuTitle
            shopData = ShopData().apply {
                poolItems = source.uzumAccountShopData?.countPoolItems?.toBigDecimal()
                products = source.uzumAccountShopData?.countProducts?.toBigDecimal()
                skus = source.uzumAccountShopData?.countSkus?.toBigDecimal()
            }
        }
    }
}
