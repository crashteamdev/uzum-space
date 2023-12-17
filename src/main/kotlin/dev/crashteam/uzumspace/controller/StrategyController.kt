package dev.crashteam.uzumspace.controller

import dev.crashteam.openapi.space.api.StrategiesApi
import dev.crashteam.openapi.space.model.*
import dev.crashteam.uzumspace.service.UzumShopItemStrategyService
import org.springframework.core.convert.ConversionService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import java.security.Principal
import java.util.*

@RestController
@RequestMapping("/v1/repricer")
class StrategyController(
    private val uzumShopItemStrategyService: UzumShopItemStrategyService,
    private val conversionService: ConversionService
) : StrategiesApi {

    override fun addStrategy(
        xRequestID: UUID,
        addStrategyRequest: Mono<AddStrategyRequest>,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<UzumAccountShopItemStrategy>> {
        return addStrategyRequest.flatMap {
            uzumShopItemStrategyService.saveStrategy(it)
            val strategy = uzumShopItemStrategyService.findStrategy(it.uzumAccountShopItemId)
            val itemStrategy = conversionService.convert(strategy, UzumAccountShopItemStrategy::class.java)
            return@flatMap ResponseEntity.status(HttpStatus.CREATED).body(itemStrategy).toMono()
        }
    }

    override fun deleteStrategy(
        xRequestID: UUID,
        shopItemId: UUID,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Void>> {
        return exchange.getPrincipal<Principal>().flatMap {
            uzumShopItemStrategyService.deleteStrategy(shopItemId)
            return@flatMap ResponseEntity.noContent().build<Void>().toMono()
        }
    }

    override fun getStrategy(
        xRequestID: UUID,
        shopItemId: UUID,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<UzumAccountShopItemStrategy>> {
        return exchange.getPrincipal<Principal>().flatMap {
            val strategy = uzumShopItemStrategyService.findStrategy(shopItemId)
            val strategyDto = conversionService.convert(strategy, UzumAccountShopItemStrategy::class.java)
            return@flatMap ResponseEntity.ok().body(strategyDto).toMono()
        }
    }

    override fun patchStrategy(
        xRequestID: UUID,
        shopItemId: UUID,
        patchStrategy: Mono<PatchStrategy>,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<UzumAccountShopItemStrategy>> {
        return patchStrategy.flatMap {
            uzumShopItemStrategyService.updateStrategy(shopItemId, it)
            val strategy = uzumShopItemStrategyService.findStrategy(shopItemId)
            val itemStrategy = conversionService.convert(strategy, UzumAccountShopItemStrategy::class.java)
            return@flatMap ResponseEntity.ok().body(itemStrategy).toMono()
        }
    }

    override fun getStrategyTypes(exchange: ServerWebExchange?): Mono<ResponseEntity<Flux<StrategyType>>> {
        return Mono.just(ResponseEntity.ok(Flux.fromIterable(StrategyType.values().toList())))
    }
}
