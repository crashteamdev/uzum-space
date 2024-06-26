package dev.crashteam.uzumspace.repository.postgre.entity.strategy

import java.util.*


data class UzumAccountShopItemStrategyEntity(
    val id: Long,
    val strategyType: String,
    val strategyOptionId: Long,
    val minimumThreshold: Long?,
    val maximumThreshold: Long?,
    val step: Int?,
    val discount: Int?,
    val accountShopItemId: UUID,
    val changeNotAvailableItemPrice: Boolean? = null,
    val competitorAvailableAmount: Int? = null,
    val competitorSalesAmount: Int? = null
)