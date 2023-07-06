package dev.crashteam.uzumspace.controller

import dev.crashteam.openapi.kerepricer.api.StrategiesApi
import dev.crashteam.openapi.kerepricer.model.AddStrategyRequest
import dev.crashteam.openapi.kerepricer.model.KeAccountShopItemStrategy
import dev.crashteam.openapi.kerepricer.model.PatchStrategy
import dev.crashteam.openapi.kerepricer.model.StrategyType
import dev.crashteam.uzumspace.service.UzumShopItemStrategyService
import org.springframework.core.convert.ConversionService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import java.security.Principal
import java.util.*

@RestController
@RequestMapping("/v1")
class StrategyController(
    private val uzumShopItemStrategyService: UzumShopItemStrategyService,
    private val conversionService: ConversionService
) : StrategiesApi {

    override fun addStrategy(
        xRequestID: UUID?,
        addStrategyRequest: Mono<AddStrategyRequest>?,
        exchange: ServerWebExchange?
    ): Mono<ResponseEntity<KeAccountShopItemStrategy>>? {
        return addStrategyRequest?.flatMap {
            val strategyId = uzumShopItemStrategyService.saveStrategy(it)
            val strategy = uzumShopItemStrategyService.findStrategy(strategyId)
            val itemStrategy = conversionService.convert(strategy, KeAccountShopItemStrategy::class.java)
            return@flatMap ResponseEntity.status(HttpStatus.CREATED).body(itemStrategy).toMono()
        }
    }

    override fun deleteStrategy(
        xRequestID: UUID?,
        shopItemStrategyId: Long?,
        exchange: ServerWebExchange?
    ): Mono<ResponseEntity<Void>>? {
        if (shopItemStrategyId != null) {
            return exchange?.getPrincipal<Principal>()?.flatMap {
                uzumShopItemStrategyService.deleteStrategy(shopItemStrategyId)
                return@flatMap ResponseEntity.noContent().build<Void>().toMono()
            }
        }
        return Mono.just(ResponseEntity.noContent().build())

    }

    override fun getStrategy(
        xRequestID: UUID?,
        shopItemStrategyId: Long?,
        exchange: ServerWebExchange?
    ): Mono<ResponseEntity<KeAccountShopItemStrategy>> {
        if (shopItemStrategyId != null) {
            exchange?.getPrincipal<Principal>()?.flatMap {
                val strategy = uzumShopItemStrategyService.findStrategy(shopItemStrategyId)
                val strategyDto = conversionService.convert(strategy, KeAccountShopItemStrategy::class.java)
                return@flatMap ResponseEntity.ok().body(strategyDto).toMono()
            }
        }
        throw IllegalArgumentException("shopItemStrategyId can't be null")
    }

    override fun patchStrategy(
        xRequestID: UUID?,
        shopItemStrategyId: Long?,
        patchStrategy: Mono<PatchStrategy>?,
        exchange: ServerWebExchange?
    ): Mono<ResponseEntity<KeAccountShopItemStrategy>> {
        if (shopItemStrategyId != null) {
            patchStrategy?.flatMap {
                uzumShopItemStrategyService.updateStrategy(shopItemStrategyId, it)
                val strategy = uzumShopItemStrategyService.findStrategy(shopItemStrategyId)
                val itemStrategy = conversionService.convert(strategy, KeAccountShopItemStrategy::class.java)

                uzumShopItemStrategyService.deleteStrategy(shopItemStrategyId)
                return@flatMap ResponseEntity.ok().body(itemStrategy).toMono()
            }
        }
        return Mono.just(ResponseEntity.badRequest().build())
    }

    override fun getStrategyTypes(exchange: ServerWebExchange?): Mono<ResponseEntity<StrategyType>> {
        return super.getStrategyTypes(exchange)
    }
}