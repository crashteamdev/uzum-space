package dev.crashteam.uzumspace.stream.scheduler

import dev.crashteam.uzumspace.config.properties.RedisProperties
import dev.crashteam.uzumspace.extensions.getApplicationContext
import dev.crashteam.uzumspace.payment.PaymentStreamListener
import dev.crashteam.uzumspace.service.PendingMessageService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.quartz.DisallowConcurrentExecution
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.springframework.data.redis.connection.stream.ObjectRecord
import org.springframework.data.redis.stream.StreamListener

private val log = KotlinLogging.logger {}

@DisallowConcurrentExecution
class PendingMessageScheduler : Job {

    override fun execute(context: JobExecutionContext) {
        val appContext = context.getApplicationContext()
        val pendingMessageService = appContext.getBean(PendingMessageService::class.java)
        val redisProperties = appContext.getBean(RedisProperties::class.java)
        val paymentStreamListener = appContext.getBean(PaymentStreamListener::class.java)
        runBlocking {
            val paymentTask = async {
                processPendingMessage(
                    streamKey = redisProperties.stream.payment.streamName,
                    consumerGroup = redisProperties.stream.payment.consumerGroup,
                    consumerName = redisProperties.stream.payment.consumerName,
                    listener = paymentStreamListener,
                    pendingMessageService = pendingMessageService,
                    targetType = ByteArray::class.java
                )
            }
            awaitAll(paymentTask)
        }
    }

    private suspend fun <V> processPendingMessage(
        streamKey: String,
        consumerGroup: String,
        consumerName: String,
        listener: StreamListener<String, ObjectRecord<String, V>>,
        pendingMessageService: PendingMessageService,
        targetType: Class<V>
    ) {
        try {
            log.info { "Processing pending message by consumer $consumerName" }
            pendingMessageService.receivePendingMessages(
                streamKey = streamKey,
                consumerGroupName = consumerGroup,
                consumerName = consumerName,
                listener = listener,
                targetType = targetType,
            )
        } catch (e: Exception) {
            log.error(e) { "Processing pending message failed" }
        }
    }
}
