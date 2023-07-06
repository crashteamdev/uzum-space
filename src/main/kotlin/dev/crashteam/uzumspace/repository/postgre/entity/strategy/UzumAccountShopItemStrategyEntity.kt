package dev.crashteam.uzumspace.repository.postgre.entity.strategy


data class UzumAccountShopItemStrategyEntity(
    val id: Long,
    val strategyType: String,
    val strategyOptionId: Long,
    val minimumThreshold: Long?,
    val maximumThreshold: Long?,
    val step: Int?
)