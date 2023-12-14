package dev.crashteam.uzumspace.stream.payment

import dev.crashteam.payment.PaymentEvent
import dev.crashteam.payment.UzumRepricerContext.UzumRepricerPlan
import dev.crashteam.uzumspace.db.model.enums.PaymentStatus
import dev.crashteam.uzumspace.db.model.enums.SubscriptionPlan
import dev.crashteam.uzumspace.repository.postgre.PaymentRepository
import dev.crashteam.uzumspace.repository.postgre.entity.PaymentEntity
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import java.util.*

@Component
class UzumPaymentCreatedEventHandler(
    private val paymentRepository: PaymentRepository,
) : PaymentEventHandler {

    override fun handle(events: List<PaymentEvent>) {
        runBlocking {
            for (paymentEvent in events) {
                val paymentCreated = paymentEvent.payload.paymentChange.paymentCreated
                val paymentId = UUID.fromString(paymentCreated.paymentId)
                val paymentDocument = paymentRepository.findById(paymentId)

                if (paymentDocument != null) continue

                val subscriptionPlan =
                    mapProtoSubscriptionPlan(paymentCreated.userPaidService.paidService.context.uzumRepricerContext.plan.planCase)
                paymentRepository.save(
                    PaymentEntity(
                        id = paymentId,
                        userId = paymentCreated.userId,
                        externalId = "none",
                        amount = paymentCreated.amount.value,
                        subscriptionPlan = subscriptionPlan,
                        status = PaymentStatus.pending,
                        multiply = paymentCreated.userPaidService.paidService.context.multiply.toShort()
                    )
                )
            }
        }
    }

    override fun isHandle(event: PaymentEvent): Boolean {
        return event.payload.hasPaymentChange() &&
                event.payload.paymentChange.hasPaymentCreated() &&
                event.payload.paymentChange.paymentCreated.hasUserPaidService() &&
                event.payload.paymentChange.paymentCreated.userPaidService.paidService.context.hasUzumRepricerContext()
    }

    private fun mapProtoSubscriptionPlan(uzumRepricerPlan: UzumRepricerPlan.PlanCase): SubscriptionPlan {
        return when (uzumRepricerPlan) {
            UzumRepricerPlan.PlanCase.DEFAULT_PLAN -> SubscriptionPlan.default_
            else -> throw IllegalStateException("Unknown plan type: $")
        }
    }
}
