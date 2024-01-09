package dev.crashteam.uzumspace.controller

import dev.crashteam.openapi.space.api.StrategiesApi
import dev.crashteam.openapi.space.model.*
import dev.crashteam.uzumspace.service.UzumShopItemStrategyService
import mu.KotlinLogging
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

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/v1")
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
            val strategy = uzumShopItemStrategyService.findStrategy(it.accountShopItemId)
            val itemStrategy = conversionService.convert(strategy, UzumAccountShopItemStrategy::class.java)
            return@flatMap ResponseEntity.status(HttpStatus.CREATED).body(itemStrategy).toMono()
        }.doOnError {
            log.warn(it) { "Failed to add strategy" }
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
        }.doOnError {
            log.warn(it) { "Failed to delete strategy. accountShopItemId=$shopItemId" }
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
        }.doOnError {
            log.warn(it) { "Failed to get strategy. uzumAccountShopItemId=$shopItemId" }
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
        }.doOnError {
            log.warn(it) { "Failed to patch strategy. accountShopItemId=$shopItemId" }
        }
    }

    override fun getStrategyTypes(exchange: ServerWebExchange?): Mono<ResponseEntity<Flux<StrategyType>>> {
        return Mono.just(
            ResponseEntity.ok(
                Flux.fromIterable(
                    listOf(
                        StrategyType.CLOSE_TO_MINIMAL,
                        StrategyType.EQUAL_PRICE
                    )
                )
            )
        )
    }
}
