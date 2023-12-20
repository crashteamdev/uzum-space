package dev.crashteam.repricer.repository.postgre.entity

import java.util.*

data class UzumAccountShopEntityWithData(
    val id: UUID? = null,
    val uzumAccountId: UUID,
    val externalShopId: Long,
    val name: String,
    val skuTitle: String?,
    val uzumAccountShopData: UzumAccountShopData?
)
