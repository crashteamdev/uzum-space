package dev.crashteam.uzumspace.job

import dev.crashteam.uzumspace.db.model.enums.InitializeState
import dev.crashteam.uzumspace.db.model.enums.UpdateState
import dev.crashteam.uzumspace.extensions.getApplicationContext
import dev.crashteam.uzumspace.repository.postgre.UzumAccountRepository
import org.quartz.DisallowConcurrentExecution
import org.quartz.JobExecutionContext
import org.springframework.scheduling.quartz.QuartzJobBean
import java.time.LocalDateTime

@DisallowConcurrentExecution
class RepairStuckStateJob : QuartzJobBean() {

    override fun executeInternal(context: JobExecutionContext) {
        val applicationContext = context.getApplicationContext()
        val uzumAccountRepository = applicationContext.getBean(UzumAccountRepository::class.java)
        uzumAccountRepository.findAccountByUpdateStateInProgressAndLastUpdateLessThan(
            LocalDateTime.now().minusMinutes(60)
        ).forEach {
            uzumAccountRepository.changeUpdateState(it.userId, it.keAccountEntity.id!!, UpdateState.error)
        }
        uzumAccountRepository.findAccountByInitializeStateInProgressAndLastUpdateLessThan(
            LocalDateTime.now().minusMinutes(60)
        ).forEach {
            uzumAccountRepository.changeInitializeState(it.userId, it.keAccountEntity.id!!, InitializeState.error)
        }
    }
}
