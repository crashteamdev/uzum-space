package dev.crashteam.uzumspace.config

import dev.crashteam.uzumspace.config.retry.ErrorRetryListener
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.retry.backoff.FixedBackOffPolicy
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate

@Configuration
class AppConfig {

    @Bean
    fun retryTemplate(): RetryTemplate {
        val retryTemplate = RetryTemplate()
        val fixedBackOffPolicy = FixedBackOffPolicy()
        fixedBackOffPolicy.backOffPeriod = 30000L
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy)
        val retryPolicy = SimpleRetryPolicy()
        retryPolicy.maxAttempts = 10
        retryTemplate.setRetryPolicy(retryPolicy)
        retryTemplate.setListeners(arrayOf(ErrorRetryListener()))

        return retryTemplate
    }

}
