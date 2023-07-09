package dev.crashteam.uzumspace.job

import dev.crashteam.uzumspace.client.uzum.UzumWebClient
import dev.crashteam.uzumspace.extensions.getApplicationContext
import dev.crashteam.uzumspace.service.UzumShopItemService
import mu.KotlinLogging
import org.quartz.JobExecutionContext
import org.springframework.scheduling.quartz.QuartzJobBean
import kotlin.random.Random

private val log = KotlinLogging.logger {}

class UzumShopItemJob : QuartzJobBean() {

    override fun executeInternal(context: JobExecutionContext) {
        val applicationContext = context.getApplicationContext()
        val kazanExpressClient = applicationContext.getBean(UzumWebClient::class.java)
        val uzumShopItemService = applicationContext.getBean(UzumShopItemService::class.java)
        val categoryId = context.jobDetail.jobDataMap["categoryId"] as? Long
            ?: throw IllegalStateException("categoryId can't be null")
        log.info { "Start category product collect for $categoryId" }
        var offset = context.jobDetail.jobDataMap["offset"] as? Int ?: 0
        while (true) {
            val categoryResponse =
                kazanExpressClient.getCategoryGraphQL(categoryId = categoryId.toString(), limit = 48, offset = 0)
            log.info { "Category response. itemsSize=${categoryResponse?.items?.size}" }
            if (categoryResponse?.items.isNullOrEmpty()) break
            val products = categoryResponse?.items ?: break
            products.mapNotNull { categoryProduct ->
                Thread.sleep(Random.nextLong(50, 500))
                val productInfo = kazanExpressClient.getProductInfo(categoryProduct.catalogCard.productId.toString())
                if (productInfo?.payload == null) {
                    log.warn { "Product info payload can't be empty. productId=${categoryProduct.catalogCard.productId}" }
                    return@mapNotNull null // skip bad product
                }
                val productData = productInfo.payload.data
                uzumShopItemService.addShopItemFromUzumData(productData)
            }
            offset += 48
            context.jobDetail.jobDataMap["offset"] = offset
        }
        context.jobDetail.jobDataMap["offset"] = 0
        log.info { "Complete category product collect for $categoryId" }
    }
}
