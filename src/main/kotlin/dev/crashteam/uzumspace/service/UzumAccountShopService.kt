package dev.crashteam.uzumspace.service

import dev.crashteam.uzumspace.client.uzum.UzumWebClient
import dev.crashteam.uzumspace.repository.postgre.*
import dev.crashteam.uzumspace.repository.postgre.entity.*
import dev.crashteam.uzumspace.restriction.AccountSubscriptionRestrictionValidator
import dev.crashteam.uzumspace.service.error.AccountItemCompetitorLimitExceededException
import dev.crashteam.uzumspace.service.error.AccountItemPoolLimitExceededException
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

    fun getKeAccountShops(userId: String, keAccountId: UUID): List<UzumAccountShopEntity> {
        log.debug { "Get ke account shops. userId=$userId; keAccountId=${keAccountId}" }
        return uzumAccountShopRepository.getKeAccountShops(userId, keAccountId)
    }

    fun getKeAccountShopItem(
        userId: String,
        keAccountId: UUID,
        shopItemId: UUID,
    ): UzumAccountShopItemEntity? {
        log.debug {
            "Get ke account shop item. userId=$userId; keAccountId=${keAccountId}; shopItemId=${shopItemId}"
        }
        return uzumAccountShopItemRepository.findShopItem(keAccountId, shopItemId)
    }

    fun getKeAccountShopItems(
        userId: String,
        keAccountId: UUID,
        keAccountShopId: UUID,
        filter: Condition? = null,
        sortFields: List<Pair<Field<*>, SortType>>? = null,
        limit: Long,
        offset: Long
    ): List<PaginateEntity<UzumAccountShopItemEntity>> {
        log.debug {
            "Get ke account shop items. userId=$userId; keAccountId=${keAccountId};" +
                    " keAccountShopId=$keAccountShopId; limit=$limit; offset=$offset"
        }
        return uzumAccountShopItemRepository.findShopItems(
            keAccountId,
            keAccountShopId,
            filter,
            sortFields,
            limit,
            offset
        )
    }

    fun addShopItemIntoPool(
        userId: String,
        keAccountId: UUID,
        keAccountShopId: UUID,
        keAccountShopItemId: UUID
    ) {
        log.debug {
            "Add shop item into pool. userId=$userId; keAccountId=${keAccountId}; keAccountShopId=$keAccountShopId"
        }
        val isValidPoolItemCount = accountSubscriptionRestrictionValidator.validateItemInPoolCount(userId)

        if (!isValidPoolItemCount)
            throw AccountItemPoolLimitExceededException("Pool limit exceeded for user. userId=$userId")

        val kazanExpressAccountShopItemPoolEntity = UzumAccountShopItemPoolEntity(keAccountShopItemId)
        uzumAccountShopItemPoolRepository.save(kazanExpressAccountShopItemPoolEntity)
    }

    @Transactional
    fun addShopItemCompetitor(
        userId: String,
        keAccountId: UUID,
        keAccountShopId: UUID,
        keAccountShopItemId: UUID,
        productId: Long,
        skuId: Long
    ) {
        log.debug {
            "Add shop item competitor. userId=$userId; keAccountId=$keAccountId;" +
                    " keAccountShopId=${keAccountShopId}; keAccountShopItemId=${keAccountShopItemId}"
        }
        val isValidCompetitorItemCount =
            accountSubscriptionRestrictionValidator.validateItemCompetitorCount(userId, keAccountShopItemId)
        if (!isValidCompetitorItemCount)
            throw AccountItemCompetitorLimitExceededException("Pool limit exceeded for user. userId=$userId")

        val kazanExpressAccountShopItemEntity =
            uzumAccountShopItemRepository.findShopItem(keAccountId, keAccountShopId, keAccountShopItemId)
                ?: throw IllegalArgumentException(
                    "Not found shop item." +
                            " keAccountId=${keAccountId};keAccountShopId=${keAccountShopId};keAccountShopItemId=${keAccountShopItemId}"
                )
        val shopItemEntity = uzumShopItemRepository.findByProductIdAndSkuId(productId, skuId)
        if (shopItemEntity == null) {
            val productInfo = kazanExpressWebClient.getProductInfo(productId.toString())
            if (productInfo?.payload == null) {
                throw IllegalArgumentException("Not found shop item by productId=$productId; skuId=$skuId")
            }
            val productData = productInfo.payload.data
            uzumShopItemService.addShopItemFromKeData(productData)
        }
        val kazanExpressAccountShopItemCompetitorEntity = UzumAccountShopItemCompetitorEntity(
            id = UUID.randomUUID(),
            keAccountShopItemId = kazanExpressAccountShopItemEntity.id,
            productId = productId,
            skuId = skuId,
        )
        uzumAccountShopItemCompetitorRepository.save(kazanExpressAccountShopItemCompetitorEntity)
    }

    fun removeShopItemCompetitor(
        userId: String,
        keAccountId: UUID,
        keAccountShopItemId: UUID,
        competitorId: UUID,
    ): Int {
        log.debug {
            "Remove shop item competitor. userId=$userId; keAccountId=$keAccountId;" +
                    " keAccountShopItemId=$keAccountShopItemId; competitorId=$competitorId"
        }
        return uzumAccountShopItemCompetitorRepository.removeShopItemCompetitor(keAccountShopItemId, competitorId)
    }

    fun getShopItemCompetitors(
        userId: String,
        keAccountId: UUID,
        keShopId: UUID,
        keAccountShopItemId: UUID,
        filter: Condition? = null,
        sortFields: List<Pair<Field<*>, SortType>>? = null,
        limit: Long,
        offset: Long
    ): List<PaginateEntity<UzumAccountShopItemCompetitorEntityJoinKeShopItemEntity>> {
        log.debug {
            "Get shop item competitors. userId=$userId; keAccountId=$keAccountId;" +
                    " keShopId=$keShopId; keAccountShopItemId=$keAccountShopItemId; limit=$limit; offset=$offset"
        }
        return uzumAccountShopItemCompetitorRepository.findShopItemCompetitors(
            keAccountShopItemId,
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
        keAccountId: UUID,
        keShopId: UUID,
        filter: Condition? = null,
        sortFields: List<Pair<Field<*>, SortType>>? = null,
        limit: Long,
        offset: Long
    ): List<PaginateEntity<UzumAccountShopItemEntity>> {
        log.debug { "Get user shop items in pool. userId=$userId; keAccountId=$keAccountId; keShopId=$keShopId" }
        return uzumAccountShopItemPoolRepository.findShopItemInPool(
            userId,
            keAccountId,
            keShopId,
            filter,
            sortFields,
            limit,
            offset
        )
    }

    fun removeShopItemFromPool(
        userId: String,
        keAccountId: UUID,
        keAccountShopId: UUID,
        keShopItemId: UUID,
    ): Int {
        log.debug {
            "Remove shop item from pool. userId=$userId; keAccountId=$keAccountId;" +
                    " keAccountShopId=$keAccountShopId; keShopItemId=$keShopItemId;"
        }
        return uzumAccountShopItemPoolRepository.delete(keShopItemId)
    }

    fun changeShopItemPriceOptions(
        keAccountId: UUID,
        keAccountShopItemId: UUID,
        step: Int,
        minimumThreshold: Long,
        maximumThreshold: Long,
        discount: BigDecimal
    ): Int {
        log.debug { "Change shop item price options. keAccountId=$keAccountId; keAccountShopItemId=$keAccountShopItemId" }
        return uzumAccountShopItemRepository.updatePriceChangeOptions(
            keAccountId,
            keAccountShopItemId,
            step,
            minimumThreshold,
            maximumThreshold,
            discount
        )
    }

}
