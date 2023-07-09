package dev.crashteam.uzumspace.repository.postgre.entity

import java.math.BigInteger
import java.time.LocalDateTime
import java.util.*

data class UzumAccountShopItemPoolFilledEntity(
    val uzumAccountShopItemId: UUID,
    val uzumAccountId: UUID,
    val uzumAccountShopId: UUID,
    val productId: Long,
    val skuId: Long,
    val productSku: String,
    val price: Long,
    val purchasePrice: Long?,
    val externalShopId: Long,
    val discount: BigInteger? = null,
    val step: Int? = null,
    val minimumThreshold: Long? = null,
    val maximumThreshold: Long? = null,
    val skuTitle: String,
    val barcode: Long,
    val lastCheck: LocalDateTime? = null,
    val strategyId: Long?
)
