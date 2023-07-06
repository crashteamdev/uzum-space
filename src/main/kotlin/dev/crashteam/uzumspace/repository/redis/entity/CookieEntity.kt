package dev.crashteam.uzumspace.repository.redis.entity

import dev.crashteam.uzumspace.proxy.model.ProxyAddress
import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash
import java.time.LocalDateTime

@RedisHash(value = "SecureCookie", timeToLive = 900)
data class CookieEntity(
    @Id
    val userId: String,
    val proxyAddress: ProxyAddress,
    val name: String,
    val value: String,
    val expiryAt: LocalDateTime
)
