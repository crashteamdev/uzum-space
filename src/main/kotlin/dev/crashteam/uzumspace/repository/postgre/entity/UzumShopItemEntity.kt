package dev.crashteam.uzumspace.repository.postgre.entity

import java.time.LocalDateTime

data class UzumShopItemEntity(
    val productId: Long,
    val skuId: Long,
    val categoryId: Long,
    val name: String,
    val photoKey: String,
    val avgHashFingerprint: String?,
    val pHashFingerprint: String?,
    val price: Long,
    val availableAmount: Long,
    val lastUpdate: LocalDateTime = LocalDateTime.now(),
)
