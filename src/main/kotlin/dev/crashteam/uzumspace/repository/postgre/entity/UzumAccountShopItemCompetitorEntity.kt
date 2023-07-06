package dev.crashteam.uzumspace.repository.postgre.entity

import java.util.*

data class UzumAccountShopItemCompetitorEntity(
    val id: UUID,
    val keAccountShopItemId: UUID,
    val productId: Long,
    val skuId: Long,
)
