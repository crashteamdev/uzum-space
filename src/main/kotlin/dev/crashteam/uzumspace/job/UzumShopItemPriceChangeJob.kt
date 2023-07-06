package dev.crashteam.uzumspace.job

import dev.crashteam.uzumspace.extensions.getApplicationContext
import dev.crashteam.uzumspace.repository.postgre.UzumAccountRepository
import dev.crashteam.uzumspace.service.PriceChangeService
import mu.KotlinLogging
import org.quartz.JobExecutionContext
import org.springframework.scheduling.quartz.QuartzJobBean
import java.util.*

private val log = KotlinLogging.logger {}

class UzumShopItemPriceChangeJob : QuartzJobBean() {

    override fun executeInternal(context: JobExecutionContext) {
        val applicationContext = context.getApplicationContext()
        val priceChangeService = applicationContext.getBean(PriceChangeService::class.java)
        val uzumAccountRepository = applicationContext.getBean(UzumAccountRepository::class.java)
        val keAccountId = context.jobDetail.jobDataMap["keAccountId"] as? UUID
            ?: throw IllegalStateException("keAccountId can't be null")
        val kazanExpressAccountEntity = uzumAccountRepository.getUzumAccount(keAccountId)!!
        log.info { "Check account items price for userId=${kazanExpressAccountEntity.userId}; keAccountId=$keAccountId" }
        priceChangeService.recalculateUserShopItemPrice(kazanExpressAccountEntity.userId, keAccountId)
    }
}
