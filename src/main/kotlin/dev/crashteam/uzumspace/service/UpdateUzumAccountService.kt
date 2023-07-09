package dev.crashteam.uzumspace.service

import dev.crashteam.uzumspace.client.uzum.UzumWebClient
import dev.crashteam.uzumspace.db.model.enums.UpdateState
import dev.crashteam.uzumspace.job.UpdateAccountDataJob
import dev.crashteam.uzumspace.repository.postgre.UzumAccountRepository
import dev.crashteam.uzumspace.repository.postgre.UzumAccountShopItemRepository
import dev.crashteam.uzumspace.repository.postgre.UzumAccountShopRepository
import dev.crashteam.uzumspace.repository.postgre.entity.UzumAccountShopEntity
import dev.crashteam.uzumspace.repository.postgre.entity.UzumAccountShopItemEntity
import mu.KotlinLogging
import org.quartz.JobBuilder
import org.quartz.Scheduler
import org.quartz.SimpleTrigger
import org.springframework.retry.support.RetryTemplate
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

private val log = KotlinLogging.logger {}

@Service
class UpdateUzumAccountService(
    private val uzumAccountRepository: UzumAccountRepository,
    private val uzumAccountShopRepository: UzumAccountShopRepository,
    private val uzumAccountShopItemRepository: UzumAccountShopItemRepository,
    private val kazanExpressSecureService: UzumSecureService,
    private val kazanExpressWebClient: UzumWebClient,
    private val uzumShopItemService: UzumShopItemService,
    private val scheduler: Scheduler,
    private val retryTemplate: RetryTemplate
) {

    @Transactional
    fun executeUpdateJob(userId: String, uzumAccountId: UUID): Boolean {
        val kazanExpressAccount = uzumAccountRepository.getUzumAccount(userId, uzumAccountId)!!
        if (kazanExpressAccount.updateState == UpdateState.in_progress) {
            log.debug { "Uzum account update already in progress. userId=$userId;uzumAccountId=$uzumAccountId" }
            return false
        }
        if (kazanExpressAccount.lastUpdate != null) {
            val lastUpdate = kazanExpressAccount.lastUpdate.plusMinutes(10)
            if (lastUpdate?.isBefore(LocalDateTime.now()) == false) {
                log.debug { "Uzum account update data was done recently. Need to try again later. userId=$userId;uzumAccountId=$uzumAccountId" }
                return false
            }
        }
        val jobIdentity = "ke-account-update-job-$uzumAccountId"
        val jobDetail =
            JobBuilder.newJob(UpdateAccountDataJob::class.java).withIdentity(jobIdentity).build()
        val triggerFactoryBean = SimpleTriggerFactoryBean().apply {
            setName(jobIdentity)
            setStartTime(Date())
            setRepeatInterval(0L)
            setRepeatCount(0)
            setPriority(Int.MAX_VALUE / 2)
            setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW)
            afterPropertiesSet()
        }.getObject()
        jobDetail.jobDataMap["userId"] = userId
        jobDetail.jobDataMap["uzumAccountId"] = uzumAccountId
        scheduler.scheduleJob(jobDetail, triggerFactoryBean)

        return true
    }

    @Transactional
    fun updateShops(userId: String, uzumAccountId: UUID) {
        val accountShops = kazanExpressSecureService.getAccountShops(userId, uzumAccountId)
        val shopIdSet = accountShops.map { it.id }.toHashSet()
        val uzumAccountShops = uzumAccountShopRepository.getUzumAccountShops(userId, uzumAccountId)
        val kazanExpressAccountShopIdsToRemove =
            uzumAccountShops.filter { !shopIdSet.contains(it.externalShopId) }.map { it.externalShopId }
        uzumAccountShopRepository.deleteByShopIds(kazanExpressAccountShopIdsToRemove)
        for (accountShop in accountShops) {
            val kazanExpressAccountShopEntity =
                uzumAccountShopRepository.getUzumAccountShopByShopId(uzumAccountId, accountShop.id)
            if (kazanExpressAccountShopEntity != null) {
                val updateUzumShopEntity =
                    kazanExpressAccountShopEntity.copy(
                        externalShopId = accountShop.id,
                        name = accountShop.shopTitle,
                        skuTitle = accountShop.skuTitle
                    )
                uzumAccountShopRepository.save(updateUzumShopEntity)
            } else {
                uzumAccountShopRepository.save(
                    UzumAccountShopEntity(
                        id = UUID.randomUUID(),
                        uzumAccountId = uzumAccountId,
                        externalShopId = accountShop.id,
                        name = accountShop.shopTitle,
                        skuTitle = accountShop.skuTitle
                    )
                )
            }
        }
    }

    @Transactional
    fun updateShopItems(userId: String, uzumAccountId: UUID) {
        val uzumAccountShops = uzumAccountShopRepository.getUzumAccountShops(userId, uzumAccountId)
        for (uzumAccountShop in uzumAccountShops) {
            var page = 0
            val shopUpdateTime = LocalDateTime.now()
            var isActive = true
            while (isActive) {
                log.debug { "Iterate through uzumAccountShop. shopId=${uzumAccountShop.externalShopId}; page=$page" }
                retryTemplate.execute<Void, Exception> {
                    Thread.sleep(Random().nextLong(1000, 4000))
                    log.debug { "Update account shop items by shopId=${uzumAccountShop.externalShopId}" }
                    val accountShopItems = kazanExpressSecureService.getAccountShopItems(
                        userId,
                        uzumAccountId,
                        uzumAccountShop.externalShopId,
                        page
                    )

                    if (accountShopItems.isEmpty()) {
                        log.debug { "The list of shops is over. shopId=${uzumAccountShop.externalShopId}" }
                        isActive = false
                        return@execute null
                    }
                    log.debug { "Iterate through accountShopItems. shopId=${uzumAccountShop.externalShopId}; size=${accountShopItems.size}" }
                    val shopItemEntities = accountShopItems.flatMap { accountShopItem ->
                        // Update product data from web KE
                        val productResponse = kazanExpressWebClient.getProductInfo(accountShopItem.productId.toString())
                        if (productResponse?.payload?.data != null) {
                            uzumShopItemService.addShopItemFromUzumData(productResponse.payload.data)
                        }
                        // Update product data from LK KE
                        val productInfo = kazanExpressSecureService.getProductInfo(
                            userId,
                            uzumAccountId,
                            uzumAccountShop.externalShopId,
                            accountShopItem.productId
                        )
                        val kazanExpressAccountShopItemEntities = accountShopItem.skuList.map { shopItemSku ->
                            val kazanExpressAccountShopItemEntity = uzumAccountShopItemRepository.findShopItem(
                                uzumAccountId,
                                uzumAccountShop.id!!,
                                accountShopItem.productId,
                                shopItemSku.skuId
                            )
                            val photoKey = accountShopItem.image.split("/")[3]
                            UzumAccountShopItemEntity(
                                id = kazanExpressAccountShopItemEntity?.id ?: UUID.randomUUID(),
                                uzumAccountId = uzumAccountId,
                                uzumAccountShopId = uzumAccountShop.id,
                                categoryId = productInfo.category.id,
                                productId = accountShopItem.productId,
                                skuId = shopItemSku.skuId,
                                name = shopItemSku.productTitle,
                                photoKey = photoKey,
                                purchasePrice = shopItemSku.purchasePrice?.movePointRight(2)?.toLong(),
                                price = shopItemSku.price.movePointRight(2).toLong(),
                                barCode = shopItemSku.barcode,
                                productSku = accountShopItem.skuTitle,
                                skuTitle = shopItemSku.skuFullTitle,
                                availableAmount = shopItemSku.quantityActive + shopItemSku.quantityAdditional,
                                lastUpdate = shopUpdateTime,
                                strategyId = kazanExpressAccountShopItemEntity?.strategyId
                            )
                        }
                        kazanExpressAccountShopItemEntities
                    }
                    log.debug { "Save new shop items. size=${shopItemEntities.size}" }
                    uzumAccountShopItemRepository.saveBatch(shopItemEntities)
                    page += 1
                    null
                }
                val oldItemDeletedCount = uzumAccountShopItemRepository.deleteWhereOldLastUpdate(
                    uzumAccountId,
                    uzumAccountShop.id!!,
                    shopUpdateTime
                )
                log.debug { "Deleted $oldItemDeletedCount old products" }
            }
        }
    }

}
