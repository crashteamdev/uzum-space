package dev.crashteam.uzumspace.service

import dev.crashteam.uzumspace.client.uzum.model.lk.*
import dev.crashteam.uzumspace.db.model.enums.StrategyType
import dev.crashteam.uzumspace.price.CloseToMinimalPriceChangeCalculatorStrategy
import dev.crashteam.uzumspace.price.PriceChangeCalculatorStrategy
import dev.crashteam.uzumspace.price.model.CalculationResult
import dev.crashteam.uzumspace.price.model.CalculatorOptions
import dev.crashteam.uzumspace.repository.postgre.UzumAccountShopItemPoolRepository
import dev.crashteam.uzumspace.repository.postgre.UzumAccountShopItemRepository
import dev.crashteam.uzumspace.repository.postgre.UzumShopItemPriceHistoryRepository
import dev.crashteam.uzumspace.repository.postgre.entity.UzumAccountShopItemPoolFilledEntity
import dev.crashteam.uzumspace.repository.postgre.entity.UzumShopItemPriceHistoryEntity
import mu.KotlinLogging
import org.springframework.retry.support.RetryTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDateTime
import java.util.*

private val log = KotlinLogging.logger {}

@Service
class PriceChangeService(
    private val uzumAccountShopItemPoolRepository: UzumAccountShopItemPoolRepository,
    private val kazanExpressSecureService: UzumSecureService,
    private val uzumShopItemPriceHistoryRepository: UzumShopItemPriceHistoryRepository,
    private val uzumAccountShopItemRepository: UzumAccountShopItemRepository,
    private val closeToMinimalCalculatorStrategy: CloseToMinimalPriceChangeCalculatorStrategy,
    private val retryTemplate: RetryTemplate,
    private val strategyService: UzumShopItemStrategyService,
    private val calculators: Map<StrategyType, PriceChangeCalculatorStrategy>
) {

    @Transactional
    fun recalculateUserShopItemPrice(userId: String, keAccountId: UUID) {
        val poolItem = uzumAccountShopItemPoolRepository.findShopItemInPool(userId, keAccountId)
        log.debug { "Found ${poolItem.size} pool items. userId=$userId;keAccountId=$keAccountId" }
        for (poolFilledEntity in poolItem) {
            try {
                log.debug { "Begin calculate item price. keAccountShopItemId=${poolFilledEntity.keAccountShopItemId};" +
                        ";productId=${poolFilledEntity.productId};skuId=${poolFilledEntity.skuId}" }
                val calculationResult = calculationResult(poolFilledEntity)
                log.debug { "Calculation result = $calculationResult. keAccountShopItemId=${poolFilledEntity.keAccountShopItemId};" +
                        ";productId=${poolFilledEntity.productId};skuId=${poolFilledEntity.skuId}" }
                if (calculationResult == null) {
                    log.info { "No need to change item price. keAccountShopItemId=${poolFilledEntity.keAccountShopItemId};" +
                            "productId=${poolFilledEntity.productId};skuId=${poolFilledEntity.skuId}" }
                    continue
                }
                val accountProductDescription = retryTemplate.execute<AccountProductDescription, Exception> {
                    kazanExpressSecureService.getProductDescription(
                        userId = userId,
                        keAccountId = keAccountId,
                        shopId = poolFilledEntity.externalShopId,
                        productId = poolFilledEntity.productId
                    )
                }
                val newSkuList = buildNewSkuList(userId, keAccountId, poolFilledEntity,
                    calculationResult, poolFilledEntity.minimumThreshold)
                log.debug { "Trying to change account shop item price. " +
                        "keAccountShopItemId=${poolFilledEntity.keAccountShopItemId};" +
                        ";productId=${poolFilledEntity.productId};skuId=${poolFilledEntity.skuId}" }
                val changeAccountShopItemPrice = kazanExpressSecureService.changeAccountShopItemPrice(
                    userId = userId,
                    keAccountId = keAccountId,
                    shopId = poolFilledEntity.externalShopId,
                    payload = ShopItemPriceChangePayload(
                        productId = poolFilledEntity.productId,
                        skuForProduct = poolFilledEntity.productSku,
                        skuList = newSkuList,
                        skuTitlesForCustomCharacteristics = if (accountProductDescription.hasCustomCharacteristics) {
                            accountProductDescription.customCharacteristicList.map { customCharacteristic ->
                                SkuTitleCharacteristic(
                                    customCharacteristic.characteristicTitle,
                                    customCharacteristic.characteristicValues.map {
                                        CustomCharacteristicSkuValue(
                                            it.title,
                                            it.skuValue
                                        )
                                    })
                            }
                        } else emptyList()
                    )
                )
                if (!changeAccountShopItemPrice) {
                    log.warn {
                        "Failed to change price for item." +
                                " id=${poolFilledEntity.keAccountShopItemId}; productId=${poolFilledEntity.productId}; skuId=${poolFilledEntity.skuId}"
                    }
                } else {
                    val lastCheckTime = LocalDateTime.now()
                    uzumShopItemPriceHistoryRepository.save(
                        UzumShopItemPriceHistoryEntity(
                            keAccountShopItemId = poolFilledEntity.keAccountShopItemId,
                            keAccountShopItemCompetitorId = calculationResult.competitorId,
                            changeTime = lastCheckTime,
                            oldPrice = poolFilledEntity.price,
                            price = calculationResult.newPriceMinor.toLong()
                        )
                    )
                    val shopItemEntity =
                        uzumAccountShopItemRepository.findShopItem(
                            keAccountId,
                            poolFilledEntity.keAccountShopItemId
                        )!!
                    uzumAccountShopItemRepository.save(shopItemEntity.copy(price = calculationResult.newPriceMinor.toLong()))
                    uzumAccountShopItemPoolRepository.updateLastCheck(
                        poolFilledEntity.keAccountShopItemId,
                        lastCheckTime
                    )
                    log.info {
                        "Successfully change price for item. " +
                                "id=${poolFilledEntity.keAccountShopItemId}; productId=${poolFilledEntity.productId}; skuId=${poolFilledEntity.skuId}"
                    }
                }
            } catch (e: Exception) {
                log.warn(e) { "Failed to change item price. keAccountShopItemId=${poolFilledEntity.keAccountShopItemId}" +
                        ";productId=${poolFilledEntity.productId};skuId=${poolFilledEntity.skuId}, cause - ${e.cause?.message}" }
            }
        }
    }

    private fun buildNewSkuList(
        userId: String,
        keAccountId: UUID,
        poolFilledEntity: UzumAccountShopItemPoolFilledEntity,
        calculationResult: CalculationResult,
        minimumThreshold: Long?
    ): List<SkuPriceChangeSku> {
        val accountProductDescription = retryTemplate.execute<AccountProductDescription, Exception> {
            kazanExpressSecureService.getProductDescription(
                userId = userId,
                keAccountId = keAccountId,
                shopId = poolFilledEntity.externalShopId,
                productId = poolFilledEntity.productId
            )
        }
        val skuList = accountProductDescription.skuList.filter { it.id != poolFilledEntity.skuId }.map {
            SkuPriceChangeSku(
                id = it.id,
                fullPrice = it.fullPrice,
                sellPrice = it.sellPrice,
                skuTitle = it.skuTitle,
                barCode = it.barcode,
                skuCharacteristicList = it.skuCharacteristicList.map {
                    SkuCharacteristic(
                        it.characteristicTitle,
                        it.definedType,
                        it.characteristicValue
                    )
                }
            )
        }
        val changeSku = SkuPriceChangeSku(
            id = poolFilledEntity.skuId,
            fullPrice = calculationResult.newPriceMinor.movePointLeft(2).toLong(),
            sellPrice = calculateDiscountPrice(poolFilledEntity.discount, minimumThreshold, calculationResult.newPriceMinor),
            skuTitle = poolFilledEntity.skuTitle,
            barCode = poolFilledEntity.barcode.toString(),
        )

        return skuList.toMutableList().apply {
            add(changeSku)
        }
    }

    private fun calculationResult(poolFilledEntity: UzumAccountShopItemPoolFilledEntity): CalculationResult? {
        if (poolFilledEntity.strategyId != null) {
            val strategy = strategyService.findStrategy(poolFilledEntity.strategyId)
            val calculatorStrategy = calculators[StrategyType.valueOf(strategy!!.strategyType)]
            return calculatorStrategy!!.calculatePrice(
                poolFilledEntity.keAccountShopItemId,
                BigDecimal.valueOf(poolFilledEntity.price),
                CalculatorOptions(
                    step = strategy.step,
                    minimumThreshold = strategy.minimumThreshold,
                    maximumThreshold = poolFilledEntity.maximumThreshold
                )
            )
        } else {
            return closeToMinimalCalculatorStrategy.calculatePrice(
                poolFilledEntity.keAccountShopItemId,
                BigDecimal.valueOf(poolFilledEntity.price),
                CalculatorOptions(
                    step = poolFilledEntity.step,
                    minimumThreshold = poolFilledEntity.minimumThreshold,
                    maximumThreshold = poolFilledEntity.maximumThreshold
                )
            )
        }
    }

    private fun calculateDiscountPrice(discount: BigInteger?, minimumThreshold: Long?, newPriceMinor: BigDecimal): Long {
        return if (discount != null) {
            val discountedPrice = (newPriceMinor - ((newPriceMinor * discount.toBigDecimal()) / BigDecimal(
                100
            ))).movePointLeft(2).toLong()
            if (minimumThreshold != null && discountedPrice < minimumThreshold) {
                newPriceMinor.movePointLeft(2).toLong()
            } else {
                discountedPrice
            }
        } else newPriceMinor.movePointLeft(2).toLong()
    }

}
