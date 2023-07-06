package dev.crashteam.uzumspace.client.uzum.model.lk

data class CheckTokenResponse(
    val accountId: Long,
    val active: Boolean,
    val firstName: String,
    val email: String,
    val sellerId: Long
)
