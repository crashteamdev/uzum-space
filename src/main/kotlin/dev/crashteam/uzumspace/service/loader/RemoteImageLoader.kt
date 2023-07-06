package dev.crashteam.uzumspace.service.loader

interface RemoteImageLoader {
    fun loadResource(imageUrl: String): ByteArray
}
