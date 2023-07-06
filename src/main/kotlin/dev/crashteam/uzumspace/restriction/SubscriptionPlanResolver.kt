package dev.crashteam.uzumspace.restriction

import dev.crashteam.uzumspace.db.model.enums.SubscriptionPlan

interface SubscriptionPlanResolver {
    fun toAccountRestriction(subscriptionPlan: SubscriptionPlan): AccountRestriction
}
