package dev.crashteam.uzumspace.controller

import dev.crashteam.openapi.space.api.UserApi
import dev.crashteam.openapi.space.model.AccountSubscription
import dev.crashteam.openapi.space.model.LimitData
import dev.crashteam.uzumspace.db.model.enums.SubscriptionPlan
import dev.crashteam.uzumspace.repository.postgre.AccountRepository
import dev.crashteam.uzumspace.service.UserService
import org.springframework.core.convert.ConversionService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import java.math.BigDecimal
import java.security.Principal
import java.time.ZoneOffset
import java.util.*

@RestController
@RequestMapping("/v1/repricer")
class UserController(
    private val accountRepository: AccountRepository,
    private val userService: UserService,
    private val conversionService: ConversionService
) : UserApi {

    override fun getUserSubscription(
        xRequestID: UUID,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<AccountSubscription>> {
        return exchange.getPrincipal<Principal>().flatMap { principal ->
            val accountEntity = accountRepository.getAccount(principal.name)
                ?: return@flatMap ResponseEntity.notFound().build<AccountSubscription>().toMono()
            if (accountEntity.subscription == null) {
                return@flatMap ResponseEntity.notFound().build<AccountSubscription>().toMono()
            }
            val accountSubscription = AccountSubscription().apply {
                this.plan = when (accountEntity.subscription.plan) {
                    SubscriptionPlan.default_ -> dev.crashteam.openapi.space.model.SubscriptionPlan.DEFAULT
                    SubscriptionPlan.pro -> dev.crashteam.openapi.space.model.SubscriptionPlan.PRO
                    SubscriptionPlan.advanced -> dev.crashteam.openapi.space.model.SubscriptionPlan.ADVANCED
                }
                this.validUntil = accountEntity.subscriptionValidUntil!!.atOffset(ZoneOffset.UTC)
            }
            ResponseEntity.ok(accountSubscription).toMono()
        }
    }

    override fun getAccountShopItemsInPool(exchange: ServerWebExchange): Mono<ResponseEntity<BigDecimal>> {
        return exchange.getPrincipal<Principal>().flatMap { principal ->
            val itemsInPool = userService.countKeAccountShopItemsInPool(principal.name)
            ResponseEntity.ok(itemsInPool.toBigDecimal()).toMono()
        }
    }

    override fun getUserLimits(exchange: ServerWebExchange): Mono<ResponseEntity<LimitData>> {
        return exchange.getPrincipal<Principal>().flatMap { principal ->
            val restrictionEntity = userService.getUserSubscriptionRestrictions(principal.name)
            ResponseEntity.ok(conversionService.convert(restrictionEntity, LimitData::class.java)).toMono()
        }
    }
}
