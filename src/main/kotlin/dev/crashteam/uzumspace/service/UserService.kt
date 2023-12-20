package dev.crashteam.uzumspace.service

import dev.crashteam.repricer.repository.postgre.entity.RestrictionEntity
import dev.crashteam.uzumspace.repository.postgre.UzumAccountShopRepository
import dev.crashteam.uzumspace.restriction.SubscriptionPlanResolver
import org.springframework.stereotype.Service

@Service
class UserService(
    private val uzumAccountShopRepository: UzumAccountShopRepository,
    private val subscriptionPlanResolver: SubscriptionPlanResolver
) {
    fun getUserSubscriptionRestrictions(userId: String): RestrictionEntity? {
        val plan = uzumAccountShopRepository.getUserSubscriptionPlan(userId)
        if (plan != null) {
            val accountRestriction = subscriptionPlanResolver.toAccountRestriction(plan)
            return RestrictionEntity(
                userId = userId,
                uzumAccountLimit = accountRestriction.uzumAccountLimit(),
                uzumAccountLimitCurrent = accountRestriction.uzumAccountLimit() - uzumAccountShopRepository.countAccounts(userId),
                itemPoolLimit = accountRestriction.itemPoolLimit(),
                itemPoolLimitCurrent = accountRestriction.itemPoolLimit() - uzumAccountShopRepository.countKeAccountShopItemsInPool(userId),
                itemCompetitorLimit = accountRestriction.itemCompetitorLimit(),
                itemCompetitorLimitCurrent = accountRestriction.itemCompetitorLimit() - uzumAccountShopRepository.countCompetitors(userId)
            )
        }
        return null
    }

    fun countKeAccountShopItemsInPool(userId: String): Int {
        return uzumAccountShopRepository.countKeAccountShopItemsInPool(userId)
    }
}