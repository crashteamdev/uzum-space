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
    fun recalculateUserShopItemPrice(userId: String, uzumAccountId: UUID) {
        val poolItem = uzumAccountShopItemPoolRepository.findShopItemInPool(userId, uzumAccountId)
        log.debug { "Found ${poolItem.size} pool items. userId=$userId;uzumAccountId=$uzumAccountId" }
        for (poolFilledEntity in poolItem) {
            try {
                log.debug {
                    "Begin calculate item price. uzumAccountShopItemId=${poolFilledEntity.uzumAccountShopItemId};" +
                            ";productId=${poolFilledEntity.productId};skuId=${poolFilledEntity.skuId}"
                }
                val calculationResult = calculationResult(poolFilledEntity)
                log.debug {
                    "Calculation result = $calculationResult. uzumAccountShopItemId=${poolFilledEntity.uzumAccountShopItemId};" +
                            ";productId=${poolFilledEntity.productId};skuId=${poolFilledEntity.skuId}"
                }
                if (calculationResult == null) {
                    log.info {
                        "No need to change item price. uzumAccountShopItemId=${poolFilledEntity.uzumAccountShopItemId};" +
                                "productId=${poolFilledEntity.productId};skuId=${poolFilledEntity.skuId}"
                    }
                    continue
                }
                val accountProductDescription = retryTemplate.execute<AccountProductDescription, Exception> {
                    kazanExpressSecureService.getProductDescription(
                        userId = userId,
                        uzumAccountId = uzumAccountId,
                        shopId = poolFilledEntity.externalShopId,
                        productId = poolFilledEntity.productId
                    )
                }

                val strategy = strategyService.findStrategy(poolFilledEntity.uzumAccountShopItemId)
                val newSkuList = buildNewSkuList(
                    userId, uzumAccountId, poolFilledEntity,
                    calculationResult, strategy?.minimumThreshold, strategy?.discount?.toBigDecimal()
                )
                log.debug {
                    "Trying to change account shop item price. " +
                            "uzumAccountShopItemId=${poolFilledEntity.uzumAccountShopItemId};" +
                            ";productId=${poolFilledEntity.productId};skuId=${poolFilledEntity.skuId}"
                }
                val changeAccountShopItemPrice = kazanExpressSecureService.changeAccountShopItemPrice(
                    userId = userId,
                    uzumAccountId = uzumAccountId,
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
                                " id=${poolFilledEntity.uzumAccountShopItemId}; productId=${poolFilledEntity.productId}; skuId=${poolFilledEntity.skuId}"
                    }
                } else {
                    val lastCheckTime = LocalDateTime.now()
                    uzumShopItemPriceHistoryRepository.save(
                        UzumShopItemPriceHistoryEntity(
                            uzumAccountShopItemId = poolFilledEntity.uzumAccountShopItemId,
                            uzumAccountShopItemCompetitorId = calculationResult.competitorId,
                            changeTime = lastCheckTime,
                            oldPrice = poolFilledEntity.price,
                            price = calculationResult.newPriceMinor.toLong()
                        )
                    )
                    val shopItemEntity =
                        uzumAccountShopItemRepository.findShopItem(
                            uzumAccountId,
                            poolFilledEntity.uzumAccountShopItemId
                        )!!
                    uzumAccountShopItemRepository.save(shopItemEntity.copy(price = calculationResult.newPriceMinor.toLong()))
                    uzumAccountShopItemPoolRepository.updateLastCheck(
                        poolFilledEntity.uzumAccountShopItemId,
                        lastCheckTime
                    )
                    log.info {
                        "Successfully change price for item. " +
                                "id=${poolFilledEntity.uzumAccountShopItemId}; productId=${poolFilledEntity.productId}; skuId=${poolFilledEntity.skuId}"
                    }
                }
            } catch (e: Exception) {
                log.warn(e) {
                    "Failed to change item price. uzumAccountShopItemId=${poolFilledEntity.uzumAccountShopItemId}" +
                            ";productId=${poolFilledEntity.productId};skuId=${poolFilledEntity.skuId}, cause - ${e.cause?.message}"
                }
            }
        }
    }

    private fun buildNewSkuList(
        userId: String,
        uzumAccountId: UUID,
        poolFilledEntity: UzumAccountShopItemPoolFilledEntity,
        calculationResult: CalculationResult,
        minimumThreshold: Long?,
        discount: BigDecimal?
    ): List<SkuPriceChangeSku> {
        val accountProductDescription = retryTemplate.execute<AccountProductDescription, Exception> {
            kazanExpressSecureService.getProductDescription(
                userId = userId,
                uzumAccountId = uzumAccountId,
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
            sellPrice = calculateDiscountPrice(discount, minimumThreshold, calculationResult.newPriceMinor),
            skuTitle = poolFilledEntity.skuTitle,
            barCode = poolFilledEntity.barcode.toString(),
        )

        return skuList.toMutableList().apply {
            add(changeSku)
        }
    }

    private fun calculationResult(poolFilledEntity: UzumAccountShopItemPoolFilledEntity): CalculationResult? {
        val strategy = strategyService.findStrategy(poolFilledEntity.uzumAccountShopItemId)
        val calculatorStrategy = calculators[StrategyType.valueOf(strategy!!.strategyType)]
        return calculatorStrategy!!.calculatePrice(
            poolFilledEntity.uzumAccountShopItemId,
            BigDecimal.valueOf(poolFilledEntity.price),
            CalculatorOptions(
                step = strategy.step,
                minimumThreshold = strategy.minimumThreshold,
                maximumThreshold = strategy.maximumThreshold
            )
        )
    }

    private fun calculateDiscountPrice(discount: BigDecimal?, minimumThreshold: Long?, newPriceMinor: BigDecimal): Long {
        return if (discount != null && !discount.equals(0)) {
            val discountedPrice = (newPriceMinor - ((newPriceMinor * discount) / BigDecimal(100)))
                .movePointLeft(2).toLong()
            if (minimumThreshold != null && discountedPrice < minimumThreshold) {
                newPriceMinor.movePointLeft(2).toLong()
            } else {
                discountedPrice
            }
        } else newPriceMinor.movePointLeft(2).toLong()
    }

}
