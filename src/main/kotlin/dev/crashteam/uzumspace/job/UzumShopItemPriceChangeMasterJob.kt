package dev.crashteam.uzumspace.job

import dev.crashteam.uzumspace.extensions.getApplicationContext
import dev.crashteam.uzumspace.repository.postgre.UzumAccountRepository
import mu.KotlinLogging
import org.quartz.*
import org.springframework.scheduling.quartz.QuartzJobBean
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean
import java.util.*

private val log = KotlinLogging.logger {}

@DisallowConcurrentExecution
class UzumShopItemPriceChangeMasterJob : QuartzJobBean() {

    override fun executeInternal(context: JobExecutionContext) {
        val applicationContext = context.getApplicationContext()
        val uzumAccountRepository = applicationContext.getBean(UzumAccountRepository::class.java)
        val kazanExpressAccountEntities = uzumAccountRepository.findAccountWhereMonitorActiveWithValidSubscription()
        for (kazanExpressAccountEntity in kazanExpressAccountEntities) {
            val jobIdentity = "${kazanExpressAccountEntity.id}-ke-account-price-change-job"
            val jobDetail =
                JobBuilder.newJob(UzumShopItemPriceChangeJob::class.java).withIdentity(jobIdentity).build()
            val triggerFactoryBean = SimpleTriggerFactoryBean().apply {
                setName(jobIdentity)
                setStartTime(Date())
                setRepeatInterval(0L)
                setRepeatCount(0)
                setPriority(Int.MAX_VALUE)
                setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW)
                afterPropertiesSet()
            }.getObject()
            jobDetail.jobDataMap["keAccountId"] = kazanExpressAccountEntity.id
            try {
                val schedulerFactoryBean = applicationContext.getBean(Scheduler::class.java)
                schedulerFactoryBean.scheduleJob(jobDetail, triggerFactoryBean)
            } catch (e: ObjectAlreadyExistsException) {
                log.warn { "Task still in progress: $jobIdentity" }
            } catch (e: Exception) {
                log.error(e) { "Failed to start scheduler job" }
            }
        }
    }
}
