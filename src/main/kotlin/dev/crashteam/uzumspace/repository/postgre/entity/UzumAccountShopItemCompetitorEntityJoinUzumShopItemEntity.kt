package dev.crashteam.uzumspace.repository.postgre.entity

import java.util.*

data class UzumAccountShopItemCompetitorEntityJoinUzumShopItemEntity(
    val id: UUID,
    val uzumAccountShopItemId: UUID,
    val productId: Long,
    val skuId: Long,
    val name: String,
    val availableAmount: Long,
    val price: Long,
    val photoKey: String,
)
