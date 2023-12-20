package dev.crashteam.repricer.repository.postgre.entity

data class RestrictionEntity(
    val userId: String,
    val uzumAccountLimit: Int,
    val uzumAccountLimitCurrent: Int,
    val itemPoolLimit: Int,
    val itemPoolLimitCurrent: Int,
    val itemCompetitorLimit: Int,
    val itemCompetitorLimitCurrent: Int
)
