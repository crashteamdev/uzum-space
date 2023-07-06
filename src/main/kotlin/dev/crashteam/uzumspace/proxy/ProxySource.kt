package dev.crashteam.uzumspace.proxy

import dev.crashteam.uzumspace.proxy.model.ProxyAddress

interface ProxySource {
    fun getProxies(): List<ProxyAddress>
}
