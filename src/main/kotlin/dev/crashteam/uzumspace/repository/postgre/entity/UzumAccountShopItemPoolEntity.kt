package dev.crashteam.uzumspace.repository.postgre.entity

import java.time.LocalDateTime
import java.util.*

data class UzumAccountShopItemPoolEntity(
    val uzumAccountShopItemId: UUID,
    val lastCheck: LocalDateTime? = null
)
