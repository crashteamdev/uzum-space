package dev.crashteam.uzumspace.job

import dev.crashteam.uzumspace.client.uzum.UzumLkClient
import dev.crashteam.uzumspace.db.model.enums.UpdateState
import dev.crashteam.uzumspace.extensions.getApplicationContext
import dev.crashteam.uzumspace.repository.postgre.UzumAccountRepository
import dev.crashteam.uzumspace.service.UzumSecureService
import dev.crashteam.uzumspace.service.UpdateKeAccountService
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
        val updateKeAccountService = applicationContext.getBean(UpdateKeAccountService::class.java)
        val transactionManager = applicationContext.getBean(PlatformTransactionManager::class.java)
        val kazanExpressSecureService = applicationContext.getBean(UzumSecureService::class.java)
        val kazanExpressLkClient = applicationContext.getBean(UzumLkClient::class.java)
        val retryTemplate = applicationContext.getBean(RetryTemplate::class.java)
        val userId = context.jobDetail.jobDataMap["userId"] as? String
            ?: throw IllegalStateException("userId can't be null")
        val keAccountId = context.jobDetail.jobDataMap["keAccountId"] as? UUID
            ?: throw IllegalStateException("keAccountId can't be null")
        try {
            retryTemplate.execute<Void, Exception> {
                log.info { "Execute update ke account job. userId=$userId;keAccountId=$keAccountId" }
                uzumAccountRepository.changeUpdateState(userId, keAccountId, UpdateState.in_progress)
                TransactionTemplate(transactionManager).execute {
                    val accessToken = kazanExpressSecureService.authUser(userId, keAccountId)
                    val checkToken = kazanExpressLkClient.checkToken(userId, accessToken).body!!
                    val kazanExpressAccount = uzumAccountRepository.getUzumAccount(userId, keAccountId)!!.copy(
                        externalAccountId = checkToken.accountId,
                        name = checkToken.firstName,
                        email = checkToken.email
                    )
                    uzumAccountRepository.save(kazanExpressAccount)
                    log.info { "Update shops. userId=$userId;keAccountId=$keAccountId" }
                    updateKeAccountService.updateShops(userId, keAccountId)
                    log.info { "Update shop items. userId=$userId;keAccountId=$keAccountId" }
                    updateKeAccountService.updateShopItems(userId, keAccountId)
                    log.info { "Change update state to finished. userId=$userId;keAccountId=$keAccountId" }
                    uzumAccountRepository.changeUpdateState(
                        userId,
                        keAccountId,
                        UpdateState.finished,
                        LocalDateTime.now()
                    )
                    null
                }
            }
        } catch (e: Exception) {
            log.warn(e) { "Failed to update account data for userId=$userId;keAccountId=$keAccountId"  }
            uzumAccountRepository.changeUpdateState(userId, keAccountId, UpdateState.error)
        }
    }
}
