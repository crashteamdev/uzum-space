package dev.crashteam.uzumspace.job

import dev.crashteam.uzumspace.extensions.getApplicationContext
import dev.crashteam.uzumspace.repository.postgre.UzumAccountRepository
import dev.crashteam.uzumspace.service.UzumAccountService
import mu.KotlinLogging
import org.quartz.DisallowConcurrentExecution
import org.quartz.JobExecutionContext
import org.springframework.scheduling.quartz.QuartzJobBean

private val log = KotlinLogging.logger {}

@DisallowConcurrentExecution
class UzumAccountInitializeMasterJob : QuartzJobBean() {

    override fun executeInternal(context: JobExecutionContext) {
        val applicationContext = context.getApplicationContext()
        val uzumAccountRepository = applicationContext.getBean(UzumAccountRepository::class.java)
        val uzumAccountService = applicationContext.getBean(UzumAccountService::class.java)
        val uzumAccounts = uzumAccountRepository.findNotInitializedAccount()
        for (uzumAccount in uzumAccounts) {
            val initializeKeAccount =
                uzumAccountService.initializeUzumAccountJob(uzumAccount.userId, uzumAccount.uzumAccountEntity.id!!)
            if (initializeKeAccount) {
                log.info { "KE account initialization job successfully created" }
            } else {
                log.info { "Can't create KE account initialization job. Maybe its already created" }
            }
        }
    }
}
