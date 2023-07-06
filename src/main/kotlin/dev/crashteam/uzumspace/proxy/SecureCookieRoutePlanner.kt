package dev.crashteam.uzumspace.proxy

import dev.crashteam.uzumspace.client.uzum.UzumLkClient
import dev.crashteam.uzumspace.repository.redis.CookieRepository
import org.apache.http.HttpHost
import org.apache.http.HttpRequest
import org.apache.http.impl.conn.DefaultRoutePlanner
import org.apache.http.impl.conn.DefaultSchemePortResolver
import org.apache.http.protocol.HttpContext
import org.springframework.stereotype.Component

@Component
class SecureCookieRoutePlanner(
    private val cookieRepository: CookieRepository
) : DefaultRoutePlanner(DefaultSchemePortResolver()) {

    override fun determineProxy(target: HttpHost, request: HttpRequest, context: HttpContext): HttpHost? {
        val userId = request.getLastHeader(UzumLkClient.USER_ID_HEADER).value
        val cookieEntity = cookieRepository.findById(userId).orElse(null) ?: return null
        val proxyAddress = cookieEntity.proxyAddress

        //return null // TODO
        return HttpHost(proxyAddress.host, proxyAddress.port!!)
    }
}
