package dev.crashteam.uzumspace.job

import dev.crashteam.uzumspace.client.uzum.UzumLkClient
import dev.crashteam.uzumspace.db.model.enums.InitializeState
import dev.crashteam.uzumspace.extensions.getApplicationContext
import dev.crashteam.uzumspace.repository.postgre.UzumAccountRepository
import dev.crashteam.uzumspace.service.UpdateUzumAccountService
import dev.crashteam.uzumspace.service.encryption.AESPasswordEncryptor
import mu.KotlinLogging
import org.quartz.JobExecutionContext
import org.springframework.retry.support.RetryTemplate
import org.springframework.scheduling.quartz.QuartzJobBean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.util.*

private val log = KotlinLogging.logger {}

class UzumAccountInitializeJob : QuartzJobBean() {

    override fun executeInternal(context: JobExecutionContext) {
        val applicationContext = context.getApplicationContext()
        val uzumAccountRepository = applicationContext.getBean(UzumAccountRepository::class.java)
        val kazanExpressLkClient = applicationContext.getBean(UzumLkClient::class.java)
        val updateUzumAccountService = applicationContext.getBean(UpdateUzumAccountService::class.java)
        val aesPasswordEncryptor = applicationContext.getBean(AESPasswordEncryptor::class.java)
        val transactionManager = applicationContext.getBean(PlatformTransactionManager::class.java)
        val retryTemplate = applicationContext.getBean(RetryTemplate::class.java)
        val uzumAccountId = context.jobDetail.jobDataMap["uzumAccountId"] as? UUID
            ?: throw IllegalStateException("uzumAccountId can't be null")
        val userId = context.jobDetail.jobDataMap["userId"] as? String
            ?: throw IllegalStateException("userId can't be null")
        try {
            val kazanExpressAccount = uzumAccountRepository.getUzumAccount(uzumAccountId)!!
            val password = Base64.getDecoder().decode(kazanExpressAccount.uzumAccountEntity.password.toByteArray())
            val decryptedPassword = aesPasswordEncryptor.decryptPassword(password)
            retryTemplate.execute<Void, Exception> {
                TransactionTemplate(transactionManager).execute {
                    val authResponse = kazanExpressLkClient.auth(
                        userId,
                        kazanExpressAccount.uzumAccountEntity.login,
                        decryptedPassword
                    )
                    val checkTokenResponse = kazanExpressLkClient.checkToken(userId, authResponse.accessToken).body!!
                    uzumAccountRepository.save(
                        kazanExpressAccount.uzumAccountEntity.copy(
                            name = checkTokenResponse.firstName,
                            externalAccountId = checkTokenResponse.accountId,
                            email = checkTokenResponse.email,
                        )
                    )
                    uzumAccountRepository.changeInitializeState(userId, uzumAccountId, InitializeState.finished)
                    updateUzumAccountService.executeUpdateJob(userId, uzumAccountId)
                    null
                }
            }
        } catch (e: Exception) {
            log.warn(e) { "Failed to initialize Uzum account" }
            uzumAccountRepository.changeInitializeState(userId, uzumAccountId, InitializeState.error)
        }

    }
}
