package dev.crashteam.uzumspace.controller

import dev.crashteam.openapi.kerepricer.api.SubscriptionsApi
import dev.crashteam.openapi.kerepricer.model.Subscription
import dev.crashteam.uzumspace.db.model.enums.SubscriptionPlan
import dev.crashteam.uzumspace.repository.postgre.SubscriptionRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux
import reactor.core.publisher.toMono
import java.security.Principal
import java.util.*

@RestController
@RequestMapping("/v1")
class SubscriptionsController(
    private val subscriptionRepository: SubscriptionRepository,
) : SubscriptionsApi {

    override fun getSubscriptions(
        xRequestID: UUID,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Flux<Subscription>>> {
        return exchange.getPrincipal<Principal>().flatMap { principal ->
            val subscriptions = subscriptionRepository.getAllSubscriptions().map {
                Subscription().apply {
                    this.name = it.name
                    this.plan = when (it.plan) {
                        SubscriptionPlan.default_ -> dev.crashteam.openapi.kerepricer.model.SubscriptionPlan.DEFAULT
                        SubscriptionPlan.pro -> dev.crashteam.openapi.kerepricer.model.SubscriptionPlan.PRO
                        SubscriptionPlan.advanced -> dev.crashteam.openapi.kerepricer.model.SubscriptionPlan.ADVANCED
                    }
                    this.price = it.price
                }
            }
            ResponseEntity.ok(subscriptions.toFlux()).toMono()
        }
    }
}
