package dev.crashteam.uzumspace.repository.postgre.entity

import java.util.*

data class UzumAccountShopItemCompetitorEntityJoinKeShopItemEntity(
    val id: UUID,
    val keAccountShopItemId: UUID,
    val productId: Long,
    val skuId: Long,
    val name: String,
    val availableAmount: Long,
    val price: Long,
    val photoKey: String,
)
