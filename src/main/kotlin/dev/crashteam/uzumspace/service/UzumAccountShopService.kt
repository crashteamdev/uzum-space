package dev.crashteam.uzumspace.service

import dev.crashteam.repricer.repository.postgre.entity.UzumAccountShopEntityWithData
import dev.crashteam.uzumspace.client.uzum.UzumWebClient
import dev.crashteam.uzumspace.repository.postgre.*
import dev.crashteam.uzumspace.repository.postgre.entity.*
import dev.crashteam.uzumspace.restriction.AccountSubscriptionRestrictionValidator
import dev.crashteam.uzumspace.service.error.AccountItemCompetitorLimitExceededException
import dev.crashteam.uzumspace.service.error.AccountItemPoolLimitExceededException
import dev.crashteam.uzumspace.service.error.CompetitorItemAlreadyExistsException
import mu.KotlinLogging
import org.jooq.Condition
import org.jooq.Field
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.*

private val log = KotlinLogging.logger {}

@Service
class UzumAccountShopService(
    private val uzumAccountShopRepository: UzumAccountShopRepository,
    private val uzumAccountShopItemRepository: UzumAccountShopItemRepository,
    private val uzumAccountShopItemPoolRepository: UzumAccountShopItemPoolRepository,
    private val uzumAccountShopItemCompetitorRepository: UzumAccountShopItemCompetitorRepository,
    private val uzumShopItemRepository: UzumShopItemRepository,
    private val kazanExpressWebClient: UzumWebClient,
    private val uzumShopItemService: UzumShopItemService,
    private val accountSubscriptionRestrictionValidator: AccountSubscriptionRestrictionValidator,
) {

    fun getUzumAccountShops(userId: String, uzumAccountId: UUID): List<UzumAccountShopEntityWithData> {
        log.debug { "Get ke account shops. userId=$userId; uzumAccountId=${uzumAccountId}" }
        return uzumAccountShopRepository.getUzumAccountShopsWithData(userId, uzumAccountId)
    }

    fun getUzumAccountShopItem(
        userId: String,
        uzumAccountId: UUID,
        shopItemId: UUID,
    ): UzumAccountShopItemEntity? {
        log.debug {
            "Get ke account shop item. userId=$userId; uzumAccountId=${uzumAccountId}; shopItemId=${shopItemId}"
        }
        return uzumAccountShopItemRepository.findShopItem(uzumAccountId, shopItemId)
    }

    fun getUzumAccountShopItemWithLimitData(
        userId: String,
        uzumAccountId: UUID,
        shopItemId: UUID,
    ): UzumAccountShopItemEntityWithLimitData? {
        log.debug {
            "Get ke account shop item. userId=$userId; uzumAccountId=${uzumAccountId}; shopItemId=${shopItemId}"
        }
        return uzumAccountShopItemRepository.findShopItem(userId, uzumAccountId, shopItemId)
    }

    fun getUzumAccountShopItems(
        userId: String,
        uzumAccountId: UUID,
        uzumAccountShopId: UUID,
        filter: Condition? = null,
        sortFields: List<Pair<Field<*>, SortType>>? = null,
        limit: Long,
        offset: Long
    ): List<PaginateEntity<UzumAccountShopItemEntity>> {
        log.debug {
            "Get ke account shop items. userId=$userId; uzumAccountId=${uzumAccountId};" +
                    " uzumAccountShopId=$uzumAccountShopId; limit=$limit; offset=$offset"
        }
        return uzumAccountShopItemRepository.findShopItems(
            uzumAccountId,
            uzumAccountShopId,
            filter,
            sortFields,
            limit,
            offset
        )
    }

    fun addShopItemIntoPool(
        userId: String,
        uzumAccountId: UUID,
        uzumAccountShopId: UUID,
        uzumAccountShopItemId: UUID
    ) {
        log.debug {
            "Add shop item into pool. userId=$userId; uzumAccountId=${uzumAccountId}; uzumAccountShopId=$uzumAccountShopId"
        }
        val isValidPoolItemCount = accountSubscriptionRestrictionValidator.validateItemInPoolCount(userId, 1)

        if (!isValidPoolItemCount)
            throw AccountItemPoolLimitExceededException("Pool limit exceeded for user. userId=$userId")

        val kazanExpressAccountShopItemPoolEntity = UzumAccountShopItemPoolEntity(uzumAccountShopItemId)
        uzumAccountShopItemPoolRepository.save(kazanExpressAccountShopItemPoolEntity)
    }

    @Transactional
    fun addShopItemIntoPoolBulk(
        userId: String,
        uzumAccountId: UUID,
        uzumAccountShopId: UUID,
        uzumAccountShopItemIds: List<UUID>,
    ) {
        log.debug {
            "Add shop items into pool. userId=$userId; uzumAccountId=${uzumAccountId};" +
                    " uzumAccountShopId=$uzumAccountShopId itemSize=${uzumAccountShopItemIds.stream()}"
        }
        val isValidPoolItemCount =
            accountSubscriptionRestrictionValidator.validateItemInPoolCount(userId, uzumAccountShopItemIds.size)

        if (!isValidPoolItemCount)
            throw AccountItemPoolLimitExceededException("Pool limit exceeded for user. userId=$userId")

        val kazanExpressAccountShopItemPoolEntities =
            uzumAccountShopItemIds.map { UzumAccountShopItemPoolEntity(it) }
        uzumAccountShopItemPoolRepository.saveBatch(kazanExpressAccountShopItemPoolEntities)
    }

    fun removeShopItemsFromPool(
        userId: String,
        uzumAccountId: UUID,
        uzumAccountShopId: UUID,
        uzumShopItemIds: List<UUID>,
    ): Int {
        log.debug {
            "Remove shop items from pool. userId=$userId; uzumAccountId=$uzumAccountId;" +
                    " uzumAccountShopId=$uzumAccountShopId; uzumShopItemIdsCount=${uzumShopItemIds.size};"
        }
        return uzumAccountShopItemPoolRepository.delete(uzumShopItemIds)
    }

    @Transactional
    fun addShopItemCompetitor(
        userId: String,
        uzumAccountId: UUID,
        uzumAccountShopId: UUID,
        uzumAccountShopItemId: UUID,
        productId: Long,
        skuId: Long
    ) {
        log.debug {
            "Add shop item competitor. userId=$userId; uzumAccountId=$uzumAccountId;" +
                    " uzumAccountShopId=${uzumAccountShopId}; uzumAccountShopItemId=${uzumAccountShopItemId}"
        }
        val isValidCompetitorItemCount =
            accountSubscriptionRestrictionValidator.validateItemCompetitorCount(userId, uzumAccountShopItemId)
        if (!isValidCompetitorItemCount)
            throw AccountItemCompetitorLimitExceededException("Pool limit exceeded for user. userId=$userId")

        val shopItemCompetitor =
            uzumAccountShopItemCompetitorRepository.findShopItemCompetitorForUpdate(
                uzumAccountShopItemId,
                productId,
                skuId
            )
        if (shopItemCompetitor != null) {
            throw CompetitorItemAlreadyExistsException()
        }

        val kazanExpressAccountShopItemEntity =
            uzumAccountShopItemRepository.findShopItem(uzumAccountId, uzumAccountShopId, uzumAccountShopItemId)
                ?: throw IllegalArgumentException(
                    "Not found shop item." +
                            " uzumAccountId=${uzumAccountId};uzumAccountShopId=${uzumAccountShopId};uzumAccountShopItemId=${uzumAccountShopItemId}"
                )
        val shopItemEntity = uzumShopItemRepository.findByProductIdAndSkuId(productId, skuId)
        if (shopItemEntity == null) {
            val productInfo = kazanExpressWebClient.getProductInfo(productId.toString())
            if (productInfo?.payload == null) {
                throw IllegalArgumentException("Not found shop item by productId=$productId; skuId=$skuId")
            }
            val productData = productInfo.payload.data
            uzumShopItemService.addShopItemFromUzumData(productData)
        }
        val kazanExpressAccountShopItemCompetitorEntity = UzumAccountShopItemCompetitorEntity(
            id = UUID.randomUUID(),
            uzumAccountShopItemId = kazanExpressAccountShopItemEntity.id,
            productId = productId,
            skuId = skuId,
        )
        uzumAccountShopItemCompetitorRepository.save(kazanExpressAccountShopItemCompetitorEntity)
    }

    fun removeShopItemCompetitor(
        userId: String,
        uzumAccountId: UUID,
        uzumAccountShopItemId: UUID,
        competitorId: UUID,
    ): Int {
        log.debug {
            "Remove shop item competitor. userId=$userId; uzumAccountId=$uzumAccountId;" +
                    " uzumAccountShopItemId=$uzumAccountShopItemId; competitorId=$competitorId"
        }
        return uzumAccountShopItemCompetitorRepository.removeShopItemCompetitor(uzumAccountShopItemId, competitorId)
    }

    fun getShopItemCompetitors(
        userId: String,
        uzumAccountId: UUID,
        keShopId: UUID,
        uzumAccountShopItemId: UUID,
        filter: Condition? = null,
        sortFields: List<Pair<Field<*>, SortType>>? = null,
        limit: Long,
        offset: Long
    ): List<PaginateEntity<UzumAccountShopItemCompetitorEntityJoinUzumShopItemEntity>> {
        log.debug {
            "Get shop item competitors. userId=$userId; uzumAccountId=$uzumAccountId;" +
                    " keShopId=$keShopId; uzumAccountShopItemId=$uzumAccountShopItemId; limit=$limit; offset=$offset"
        }
        return uzumAccountShopItemCompetitorRepository.findShopItemCompetitors(
            uzumAccountShopItemId,
            filter,
            sortFields,
            limit,
            offset
        )
    }

    fun getShopItemPoolCount(userId: String): Int {
        log.debug { "Get user shop item pool count. userId=$userId" }
        return uzumAccountShopItemPoolRepository.findCountShopItemsInPoolForUser(userId)
    }

    fun getShopItemsInPool(
        userId: String,
        uzumAccountId: UUID,
        keShopId: UUID,
        filter: Condition? = null,
        sortFields: List<Pair<Field<*>, SortType>>? = null,
        limit: Long,
        offset: Long
    ): List<PaginateEntity<UzumAccountShopItemEntity>> {
        log.debug { "Get user shop items in pool. userId=$userId; uzumAccountId=$uzumAccountId; keShopId=$keShopId" }
        return uzumAccountShopItemPoolRepository.findShopItemInPool(
            userId,
            uzumAccountId,
            keShopId,
            filter,
            sortFields,
            limit,
            offset
        )
    }

    fun removeShopItemFromPool(
        userId: String,
        uzumAccountId: UUID,
        uzumAccountShopId: UUID,
        keShopItemId: UUID,
    ): Int {
        log.debug {
            "Remove shop item from pool. userId=$userId; uzumAccountId=$uzumAccountId;" +
                    " uzumAccountShopId=$uzumAccountShopId; keShopItemId=$keShopItemId;"
        }
        return uzumAccountShopItemPoolRepository.delete(keShopItemId)
    }

    fun changeShopItemPriceOptions(
        uzumAccountId: UUID,
        uzumAccountShopItemId: UUID,
        step: Int,
        minimumThreshold: Long,
        maximumThreshold: Long,
        discount: BigDecimal
    ): Int {
        log.debug { "Change shop item price options. uzumAccountId=$uzumAccountId; uzumAccountShopItemId=$uzumAccountShopItemId" }
        return uzumAccountShopItemRepository.updatePriceChangeOptions(
            uzumAccountId,
            uzumAccountShopItemId,
            step,
            minimumThreshold,
            maximumThreshold,
            discount
        )
    }

}
