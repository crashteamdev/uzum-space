package dev.crashteam.uzumspace.restriction

import dev.crashteam.uzumspace.db.model.enums.SubscriptionPlan
import dev.crashteam.uzumspace.repository.postgre.AccountRepository
import dev.crashteam.uzumspace.repository.postgre.UzumAccountRepository
import dev.crashteam.uzumspace.repository.postgre.UzumAccountShopItemCompetitorRepository
import dev.crashteam.uzumspace.repository.postgre.UzumAccountShopItemPoolRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class AccountSubscriptionRestrictionValidator(
    private val accountRepository: AccountRepository,
    private val uzumAccountShopItemPoolRepository: UzumAccountShopItemPoolRepository,
    private val uzumAccountRepository: UzumAccountRepository,
    private val uzumAccountShopItemCompetitorRepository: UzumAccountShopItemCompetitorRepository,
    private val subscriptionPlanResolver: SubscriptionPlanResolver
) {

    fun validateItemInPoolCount(userId: String): Boolean {
        val accountEntity = accountRepository.getAccount(userId)!!

        if (accountEntity.subscription == null) return false

        val itemsInPoolCount = uzumAccountShopItemPoolRepository.findCountShopItemsInPoolForUser(userId)
        val accountRestriction = subscriptionPlanResolver.toAccountRestriction(accountEntity.subscription.plan)
        val itemPoolLimit = accountRestriction.itemPoolLimit()
        val poolLimitExceeded = itemsInPoolCount >= itemPoolLimit

        return !poolLimitExceeded
    }

    fun validateKeAccountCount(userId: String): Boolean {
        val accountEntity = accountRepository.getAccount(userId)!!

        if (accountEntity.subscription == null) return false

        val keAccountCount = uzumAccountRepository.getUzumAccountsCount(userId)
        val accountRestriction = subscriptionPlanResolver.toAccountRestriction(accountEntity.subscription.plan)
        val keAccountLimit = accountRestriction.keAccountLimit()
        val isKeAccountLimitExceeded = keAccountCount >= keAccountLimit

        return !isKeAccountLimitExceeded
    }

    fun validateItemCompetitorCount(userId: String, shopItemId: UUID): Boolean {
        val accountEntity = accountRepository.getAccount(userId)!!

        if (accountEntity.subscription == null) return false

        val itemCompetitorsCount =
            uzumAccountShopItemCompetitorRepository.findShopItemCompetitorsCount(shopItemId)
        val accountRestriction = subscriptionPlanResolver.toAccountRestriction(accountEntity.subscription.plan)
        val itemCompetitorLimit = accountRestriction.itemCompetitorLimit()
        val isItemCompetitorLimitExceeded = itemCompetitorsCount >= itemCompetitorLimit

        return !isItemCompetitorLimitExceeded
    }

    fun validateChangeSubscriptionLevel(userId: String, targetSubscriptionPlan: SubscriptionPlan): Boolean {
        val keAccountCount = uzumAccountRepository.getUzumAccountsCount(userId)
        val itemsInPoolCount = uzumAccountShopItemPoolRepository.findCountShopItemsInPoolForUser(userId)
        val accountRestriction = subscriptionPlanResolver.toAccountRestriction(targetSubscriptionPlan)
        // TODO: add item competitor limit
        val keAccountLimit = accountRestriction.keAccountLimit()
        val itemPoolLimit = accountRestriction.itemPoolLimit()
        if (keAccountCount > keAccountLimit) {
            return false
        }
        if (itemsInPoolCount > itemPoolLimit) {
            return false
        }
        return true
    }

}
