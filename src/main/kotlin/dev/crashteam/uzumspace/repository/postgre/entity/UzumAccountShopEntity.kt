package dev.crashteam.uzumspace.repository.postgre.entity

import java.util.*

data class UzumAccountShopEntity(
    val id: UUID? = null,
    val uzumAccountId: UUID,
    val externalShopId: Long,
    val name: String,
    val skuTitle: String?
)
