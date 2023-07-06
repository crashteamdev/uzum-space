package dev.crashteam.uzumspace.converter

import dev.crashteam.openapi.kerepricer.model.KeAccountShop
import dev.crashteam.uzumspace.repository.postgre.entity.UzumAccountShopEntity
import org.springframework.stereotype.Component

@Component
class UzumAccountShopEntityToViewConverter : DataConverter<UzumAccountShopEntity, KeAccountShop> {

    override fun convert(source: UzumAccountShopEntity): KeAccountShop {
        return KeAccountShop().apply {
            id = source.id
            name = source.name
            skuTitle = source.skuTitle
        }
    }
}
