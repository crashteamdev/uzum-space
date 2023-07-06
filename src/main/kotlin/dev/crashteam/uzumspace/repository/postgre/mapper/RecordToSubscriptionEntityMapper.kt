package dev.crashteam.uzumspace.repository.postgre.mapper

import dev.crashteam.uzumspace.db.model.tables.Subscription.SUBSCRIPTION
import dev.crashteam.uzumspace.repository.postgre.entity.SubscriptionEntity
import org.springframework.stereotype.Component

@Component
class RecordToSubscriptionEntityMapper : RecordMapper<SubscriptionEntity> {

    override fun convert(record: Record): SubscriptionEntity {
        return SubscriptionEntity(
            id = record.getValue(SUBSCRIPTION.ID),
            name = record.getValue(SUBSCRIPTION.NAME),
            plan = record.getValue(SUBSCRIPTION.PLAN),
            price = record.getValue(SUBSCRIPTION.PRICE),
        )
    }
}
