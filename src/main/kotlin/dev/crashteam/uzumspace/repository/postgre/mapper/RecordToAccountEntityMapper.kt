package dev.crashteam.uzumspace.repository.postgre.mapper

import dev.crashteam.uzumspace.db.model.tables.Account.ACCOUNT
import dev.crashteam.uzumspace.db.model.tables.Subscription.SUBSCRIPTION
import dev.crashteam.uzumspace.repository.postgre.entity.AccountEntity
import dev.crashteam.uzumspace.repository.postgre.entity.SubscriptionEntity
import org.springframework.stereotype.Component

@Component
class RecordToAccountEntityMapper : RecordMapper<AccountEntity> {

    override fun convert(record: Record): AccountEntity {
        return AccountEntity(
            id = record.getValue(ACCOUNT.ID),
            userId = record.getValue(ACCOUNT.USER_ID),
            subscription = record.getValue(SUBSCRIPTION.ID)?.let {
                SubscriptionEntity(
                    id = record.getValue(SUBSCRIPTION.ID),
                    name = record.getValue(SUBSCRIPTION.NAME),
                    plan = record.getValue(SUBSCRIPTION.PLAN),
                    price = record.getValue(SUBSCRIPTION.PRICE)
                )
            },
            subscriptionValidUntil = record.getValue(ACCOUNT.SUBSCRIPTION_VALID_UNTIL),
        )
    }
}
