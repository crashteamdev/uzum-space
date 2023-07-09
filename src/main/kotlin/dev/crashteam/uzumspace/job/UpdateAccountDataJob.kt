package dev.crashteam.uzumspace.job

import dev.crashteam.uzumspace.client.uzum.UzumLkClient
import dev.crashteam.uzumspace.db.model.enums.UpdateState
import dev.crashteam.uzumspace.extensions.getApplicationContext
import dev.crashteam.uzumspace.repository.postgre.UzumAccountRepository
import dev.crashteam.uzumspace.service.UzumSecureService
import dev.crashteam.uzumspace.service.UpdateUzumAccountService
import mu.KotlinLogging
import org.quartz.JobExecutionContext
import org.springframework.retry.support.RetryTemplate
import org.springframework.scheduling.quartz.QuartzJobBean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime
import java.util.*

private val log = KotlinLogging.logger {}

class UpdateAccountDataJob : QuartzJobBean() {

    override fun executeInternal(context: JobExecutionContext) {
        val applicationContext = context.getApplicationContext()
        val uzumAccountRepository = applicationContext.getBean(UzumAccountRepository::class.java)
        val updateUzumAccountService = applicationContext.getBean(UpdateUzumAccountService::class.java)
        val transactionManager = applicationContext.getBean(PlatformTransactionManager::class.java)
        val kazanExpressSecureService = applicationContext.getBean(UzumSecureService::class.java)
        val kazanExpressLkClient = applicationContext.getBean(UzumLkClient::class.java)
        val retryTemplate = applicationContext.getBean(RetryTemplate::class.java)
        val userId = context.jobDetail.jobDataMap["userId"] as? String
            ?: throw IllegalStateException("userId can't be null")
        val uzumAccountId = context.jobDetail.jobDataMap["uzumAccountId"] as? UUID
            ?: throw IllegalStateException("uzumAccountId can't be null")
        try {
            retryTemplate.execute<Void, Exception> {
                log.info { "Execute update ke account job. userId=$userId;uzumAccountId=$uzumAccountId" }
                uzumAccountRepository.changeUpdateState(userId, uzumAccountId, UpdateState.in_progress)
                TransactionTemplate(transactionManager).execute {
                    val accessToken = kazanExpressSecureService.authUser(userId, uzumAccountId)
                    val checkToken = kazanExpressLkClient.checkToken(userId, accessToken).body!!
                    val kazanExpressAccount = uzumAccountRepository.getUzumAccount(userId, uzumAccountId)!!.copy(
                        externalAccountId = checkToken.accountId,
                        name = checkToken.firstName,
                        email = checkToken.email
                    )
                    uzumAccountRepository.save(kazanExpressAccount)
                    log.info { "Update shops. userId=$userId;uzumAccountId=$uzumAccountId" }
                    updateUzumAccountService.updateShops(userId, uzumAccountId)
                    log.info { "Update shop items. userId=$userId;uzumAccountId=$uzumAccountId" }
                    updateUzumAccountService.updateShopItems(userId, uzumAccountId)
                    log.info { "Change update state to finished. userId=$userId;uzumAccountId=$uzumAccountId" }
                    uzumAccountRepository.changeUpdateState(
                        userId,
                        uzumAccountId,
                        UpdateState.finished,
                        LocalDateTime.now()
                    )
                    null
                }
            }
        } catch (e: Exception) {
            log.warn(e) { "Failed to update account data for userId=$userId;uzumAccountId=$uzumAccountId"  }
            uzumAccountRepository.changeUpdateState(userId, uzumAccountId, UpdateState.error)
        }
    }
}
