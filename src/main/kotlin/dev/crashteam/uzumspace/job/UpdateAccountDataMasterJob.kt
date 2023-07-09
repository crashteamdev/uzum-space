package dev.crashteam.uzumspace.job

import dev.crashteam.uzumspace.config.properties.RepricerProperties
import dev.crashteam.uzumspace.extensions.getApplicationContext
import dev.crashteam.uzumspace.repository.postgre.UzumAccountRepository
import dev.crashteam.uzumspace.service.UpdateUzumAccountService
import mu.KotlinLogging
import org.quartz.DisallowConcurrentExecution
import org.quartz.JobExecutionContext
import org.springframework.scheduling.quartz.QuartzJobBean
import java.time.LocalDateTime

private val log = KotlinLogging.logger {}

@DisallowConcurrentExecution
class UpdateAccountDataMasterJob : QuartzJobBean() {

    override fun executeInternal(context: JobExecutionContext) {
        val applicationContext = context.getApplicationContext()
        val uzumAccountRepository = applicationContext.getBean(UzumAccountRepository::class.java)
        val updateUzumAccountService = applicationContext.getBean(UpdateUzumAccountService::class.java)
        val repricerProperties = applicationContext.getBean(RepricerProperties::class.java)
        val uzumAccountUpdateInProgressCount = uzumAccountRepository.findAccountUpdateInProgressCount()

        if (uzumAccountUpdateInProgressCount >= (repricerProperties.maxUpdateInProgress ?: 3)) {
            log.info { "Too mutch account update in progress - $uzumAccountUpdateInProgressCount" }
            return
        }

        val kazanExpressAccountEntities =
            uzumAccountRepository.findAccountUpdateNotInProgress(LocalDateTime.now().minusHours(6))
        log.info { "Execute update account job for ${kazanExpressAccountEntities.size} ke account" }
        for (kazanExpressAccountEntity in kazanExpressAccountEntities) {
            val updateJob = updateUzumAccountService.executeUpdateJob(
                kazanExpressAccountEntity.userId,
                kazanExpressAccountEntity.uzumAccountEntity.id!!
            )
            if (!updateJob) {
                log.info {
                    "Update ke account job already started for account" +
                            " userId=${kazanExpressAccountEntity.userId}; uzumAccountId=${kazanExpressAccountEntity.uzumAccountEntity.id}"
                }
            } else {
                log.info {
                    "Add update ke account job " +
                            "userId=${kazanExpressAccountEntity.userId}; uzumAccountId=${kazanExpressAccountEntity.uzumAccountEntity.id}"
                }
            }

            if (uzumAccountRepository.findAccountUpdateInProgressCount() >=
                (repricerProperties.maxUpdateInProgress ?: 3)
            ) break
        }
    }
}
