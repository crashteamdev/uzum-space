package dev.crashteam.uzumspace.client.uzum

class UzumProxyClientException(val status: Int, val rawResponseBody: String, message: String) :
    RuntimeException(message)
