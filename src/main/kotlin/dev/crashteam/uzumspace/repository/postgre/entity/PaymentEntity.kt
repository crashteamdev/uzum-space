package dev.crashteam.uzumspace.repository.postgre.entity

import dev.crashteam.uzumspace.db.model.enums.PaymentStatus
import dev.crashteam.uzumspace.db.model.enums.SubscriptionPlan
import java.util.*

data class PaymentEntity(
    val id: UUID,
    val userId: String,
    val externalId: String,
    val amount: Long,
    val subscriptionPlan: SubscriptionPlan,
    val status: PaymentStatus,
    val multiply: Short
)
