package dev.crashteam.uzumspace.service.model

import dev.crashteam.uzumspace.repository.postgre.entity.UzumAccountShopItemCompetitorEntity
import dev.crashteam.uzumspace.repository.postgre.entity.UzumShopItemEntity

data class ShopItemCompetitor(
    val shopItemEntity: UzumShopItemEntity,
    val competitorEntity: UzumAccountShopItemCompetitorEntity
)
