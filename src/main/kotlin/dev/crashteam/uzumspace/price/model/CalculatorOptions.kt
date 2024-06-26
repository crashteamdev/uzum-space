package dev.crashteam.uzumspace.price.model

data class CalculatorOptions(
    val step: Int? = null,
    val minimumThreshold: Long? = null,
    val maximumThreshold: Long? = null,
    val changeNotAvailableItemPrice: Boolean? = null,
    val competitorAvailableAmount: Int? = null,
    val competitorSalesAmount: Int? = null
)
