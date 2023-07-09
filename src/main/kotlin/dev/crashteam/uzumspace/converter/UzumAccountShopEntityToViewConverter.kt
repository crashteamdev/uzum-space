package dev.crashteam.uzumspace.converter

import dev.crashteam.openapi.space.model.UzumAccountShop
import dev.crashteam.uzumspace.repository.postgre.entity.UzumAccountShopEntity
import org.springframework.stereotype.Component

@Component
class UzumAccountShopEntityToViewConverter : DataConverter<UzumAccountShopEntity, UzumAccountShop> {

    override fun convert(source: UzumAccountShopEntity): UzumAccountShop {
        return UzumAccountShop().apply {
            id = source.id
            name = source.name
            skuTitle = source.skuTitle
        }
    }
}
