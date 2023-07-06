package dev.crashteam.uzumspace.repository.postgre.entity

import java.time.LocalDateTime
import java.util.*

data class UzumAccountShopItemPoolEntity(
    val keAccountShopItemId: UUID,
    val lastCheck: LocalDateTime? = null
)
