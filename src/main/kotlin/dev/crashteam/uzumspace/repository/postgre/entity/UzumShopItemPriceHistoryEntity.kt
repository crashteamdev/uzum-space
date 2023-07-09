package dev.crashteam.uzumspace.repository.postgre.entity

import java.time.LocalDateTime
import java.util.*

data class UzumShopItemPriceHistoryEntity(
    val uzumAccountShopItemId: UUID,
    val uzumAccountShopItemCompetitorId: UUID,
    val changeTime: LocalDateTime,
    val oldPrice: Long,
    val price: Long
)
