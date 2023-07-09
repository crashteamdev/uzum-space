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
        val uzumAccountId = context.jobDetail.jobDataMap["uzumAccountId"] as? UUID
            ?: throw IllegalStateException("uzumAccountId can't be null")
        val kazanExpressAccountEntity = uzumAccountRepository.getUzumAccount(uzumAccountId)!!
        log.info { "Check account items price for userId=${kazanExpressAccountEntity.userId}; uzumAccountId=$uzumAccountId" }
        priceChangeService.recalculateUserShopItemPrice(kazanExpressAccountEntity.userId, uzumAccountId)
    }
}
