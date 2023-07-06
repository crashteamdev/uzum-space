package dev.crashteam.uzumspace.repository.postgre.entity

import dev.crashteam.uzumspace.db.model.enums.SubscriptionPlan

data class SubscriptionEntity(
    val id: Long,
    val name: String,
    val plan: SubscriptionPlan,
    val price: Long
)
