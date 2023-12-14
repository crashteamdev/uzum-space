package dev.crashteam.uzumspace.payment

import dev.crashteam.payment.PaymentEvent
import dev.crashteam.uzumspace.stream.payment.PaymentEventHandler
import mu.KotlinLogging
import org.springframework.data.redis.connection.stream.ObjectRecord
import org.springframework.data.redis.stream.StreamListener
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class PaymentStreamListener(
    private val paymentEventHandlers: List<PaymentEventHandler>
) : StreamListener<String, ObjectRecord<String, ByteArray>> {

    override fun onMessage(message: ObjectRecord<String, ByteArray>) {
        val paymentEvent = PaymentEvent.parseFrom(message.value)
        log.info { "Listen payment event: $paymentEvent" }
        paymentEventHandlers.find { it.isHandle(paymentEvent) }?.handle(listOf(paymentEvent))
    }
}
