package dev.crashteam.uzumspace.job

import dev.crashteam.uzumspace.client.uzum.UzumLkClient
import dev.crashteam.uzumspace.db.model.enums.UpdateState
import dev.crashteam.uzumspace.extensions.getApplicationContext
import dev.crashteam.uzumspace.repository.postgre.UzumAccountRepository
import dev.crashteam.uzumspace.service.UzumSecureService
import dev.crashteam.uzumspace.service.UpdateUzumAccountService
import dev.crashteam.uzumspace.service.UzumAccountService
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
        val uzumAccountService = applicationContext.getBean(UzumAccountService::class.java)
        val userId = context.jobDetail.jobDataMap["userId"] as? String
            ?: throw IllegalStateException("userId can't be null")
        val uzumAccountId = context.jobDetail.jobDataMap["uzumAccountId"] as? UUID
            ?: throw IllegalStateException("uzumAccountId can't be null")
        try {
            log.info { "Execute update ke account job. userId=$userId;uzumAccountId=$uzumAccountId" }
            uzumAccountRepository.changeUpdateState(userId, uzumAccountId, UpdateState.in_progress)
            uzumAccountService.syncAccount(userId, uzumAccountId)
        } catch (e: Exception) {
            log.warn(e) { "Failed to update account data for userId=$userId;uzumAccountId=$uzumAccountId"  }
            uzumAccountRepository.changeUpdateState(userId, uzumAccountId, UpdateState.error)
        }
    }
}
