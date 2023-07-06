package dev.crashteam.uzumspace.restriction

import dev.crashteam.uzumspace.db.model.enums.SubscriptionPlan
import org.springframework.stereotype.Component

@Component
class SubscriptionPlanResolverImpl : SubscriptionPlanResolver {

    override fun toAccountRestriction(subscriptionPlan: SubscriptionPlan): AccountRestriction {
        return when (subscriptionPlan) {
            SubscriptionPlan.default_ -> DefaultAccountRestriction()
            SubscriptionPlan.advanced -> AdvancedAccountRestriction()
            SubscriptionPlan.pro -> ProAccountRestriction()
        }
    }

}
