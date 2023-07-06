package dev.crashteam.uzumspace.client.uzum.model

data class StyxResponse<T>(
    val code: Int,
    val originalStatus: Int,
    val message: String? = null,
    val url: String,
    val body: T? = null,
)
