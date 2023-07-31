package dev.crashteam.uzumspace.client.uzum

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.crashteam.uzumspace.client.uzum.model.ProxyRequestBody
import dev.crashteam.uzumspace.client.uzum.model.ProxyRequestContext
import dev.crashteam.uzumspace.client.uzum.model.StyxResponse
import dev.crashteam.uzumspace.client.uzum.model.lk.*
import dev.crashteam.uzumspace.config.properties.ServiceProperties
import dev.crashteam.uzumspace.service.util.RandomUserAgent
import mu.KotlinLogging
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.*
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import java.net.URLEncoder
import java.util.*


private val log = KotlinLogging.logger {}

@Component
class UzumLkClient(
    private val lkRestTemplate: RestTemplate,
    private val restTemplate: RestTemplate,
    private val serviceProperties: ServiceProperties,
) : UzumClient {

    override fun getAccountShops(userId: String, userToken: String): List<AccountShop> {
        val headers = HttpHeaders().apply {
            set("Authorization", "Bearer $userToken")
            set("User-Agent", RandomUserAgent.getRandomUserAgent())
            set(USER_ID_HEADER, userId)
        }
        val responseEntity =
            lkRestTemplate.exchange<List<AccountShop>>(
                "https://api-seller.uzum.uz/api/seller/shop/",
                HttpMethod.GET,
                HttpEntity<Void>(headers)
            )
        log.debug { "Get account shops. userId=$userId; userToken=$userToken; response=$responseEntity" }

        return handleResponse(responseEntity)
    }

    override fun getAccountShopItems(
        userId: String,
        userToken: String,
        shopId: Long,
        page: Int
    ): List<AccountShopItem> {
        val headers = HttpHeaders().apply {
            set("Authorization", "Bearer $userToken")
            set("User-Agent", RandomUserAgent.getRandomUserAgent())
            set(USER_ID_HEADER, userId)
        }
        val responseEntity =
            lkRestTemplate.exchange<AccountShopItemWrapper>(
                "https://api-seller.uzum.uz/api/seller/shop/$shopId/product/getProducts?" +
                        "searchQuery=&filter=active&sortBy=id&order=descending&size=99&page=$page",
                HttpMethod.GET,
                HttpEntity<Void>(headers)
            )
        log.debug {
            "Get account shop items response." +
                    " userId=$userId; userToken=$userToken; shopId=$shopId; response=$responseEntity"
        }

        return handleResponse(responseEntity).productList
    }

    override fun changeAccountShopItemPrice(
        userId: String,
        userToken: String,
        shopId: Long,
        payload: ShopItemPriceChangePayload
    ): Boolean {
        val headers = HttpHeaders().apply {
            set("Authorization", "Bearer $userToken")
            set("User-Agent", RandomUserAgent.getRandomUserAgent())
            set("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            set(USER_ID_HEADER, userId)
        }
        val responseEntity =
            lkRestTemplate.exchange<Any>(
                "https://api-seller.uzum.uz/api/seller/shop/$shopId/product/sendSkuData",
                HttpMethod.POST,
                HttpEntity<ShopItemPriceChangePayload>(payload, headers)
            )
        log.debug {
            "Change account shop item price response." +
                    " userId=$userId; userToken=$userToken; shopId=$shopId; response=$responseEntity"
        }
        if (!responseEntity.statusCode.is2xxSuccessful) {
            log.warn {
                "Bad response while trying to change item price." +
                        " statusCode=${responseEntity.statusCode};responseBody=${responseEntity.body};" +
                        "requestBody=${jacksonObjectMapper().writeValueAsString(payload)}"
            }
        }

        return responseEntity.statusCode.is2xxSuccessful
    }

    override fun getProductInfo(userId: String, userToken: String, shopId: Long, productId: Long): AccountProductInfo {
        val headers = HttpHeaders().apply {
            set("Authorization", "Bearer $userToken")
            set("User-Agent", RandomUserAgent.getRandomUserAgent())
            set(USER_ID_HEADER, userId)
        }
        val responseEntity =
            lkRestTemplate.exchange<AccountProductInfo>(
                "https://api-seller.uzum.uz/api/seller/shop/$shopId/product?productId=$productId",
                HttpMethod.GET,
                HttpEntity<Void>(headers)
            )
        log.debug {
            "Get product info response." +
                    " userId=$userId; userToken=$userToken; shopId=$shopId; productId=$productId; response=$responseEntity"
        }

        return handleResponse(responseEntity)
    }

    override fun getProductDescription(
        userId: String,
        userToken: String,
        shopId: Long,
        productId: Long
    ): AccountProductDescription {
        val headers = HttpHeaders().apply {
            set("Authorization", "Bearer $userToken")
            set("User-Agent", RandomUserAgent.getRandomUserAgent())
            set(USER_ID_HEADER, userId)
        }
        val responseEntity =
            lkRestTemplate.exchange<AccountProductDescription>(
                "https://api-seller.uzum.uz/api/seller/shop/$shopId/product/$productId/description-response",
                HttpMethod.GET,
                HttpEntity<Void>(headers)
            )
        log.debug {
            "Get product description response." +
                    " userId=$userId; userToken=$userToken; shopId=$shopId; response=$responseEntity"
        }

        return handleResponse(responseEntity)
    }

    override fun auth(userId: String, username: String, password: String): AuthResponse {
        val map = HashMap<String, String>().apply {
            set("grant_type", "password")
            set("username", username)
            set("password", password)
        }

        val urlEncodedString = getUrlEncodedString(map)
        val proxyRequestBody = ProxyRequestBody(
            url = "https://api-seller.uzum.uz/api/oauth/token",
            httpMethod = "POST",
            context = listOf(
                ProxyRequestContext(
                    key = "headers",
                    value = mapOf(
                        "User-Agent" to RandomUserAgent.getRandomUserAgent(),
                        "Authorization" to "Basic $basicAuthToken",
                        "Content-Type" to MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                        USER_ID_HEADER to userId
                    )
                ),
                ProxyRequestContext("content", Base64.getEncoder().encodeToString(urlEncodedString.encodeToByteArray()))
            )
        )
        val responseType: ParameterizedTypeReference<StyxResponse<AuthResponse>> =
            object : ParameterizedTypeReference<StyxResponse<AuthResponse>>() {}
        val styxResponse = restTemplate.exchange(
            "${serviceProperties.proxy!!.url}/v2/proxy",
            HttpMethod.POST,
            HttpEntity<ProxyRequestBody>(proxyRequestBody),
            responseType
        ).body
        log.debug { "Get styx auth response. userId=$userId; username=$username; response=$styxResponse" }

        return handleProxyResponse(styxResponse!!)!!
    }

    override fun refreshAuth(userId: String, refreshToken: String): ResponseEntity<AuthResponse> {
        val map = LinkedMultiValueMap<Any, Any>().apply {
            set("grant_type", "refresh_token")
            set("refresh_token", refreshToken)
        }
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
            set("Authorization", "Basic $basicAuthToken")
            set("User-Agent", RandomUserAgent.getRandomUserAgent())
            set(USER_ID_HEADER, userId)
        }
        val responseEntity =
            lkRestTemplate.exchange<AuthResponse>(
                "https://api-seller.uzum.uz/api/oauth/token",
                HttpMethod.POST,
                HttpEntity(map, headers)
            )
        log.debug { "Get refresh auth response. userId=$userId; refreshToken=$refreshToken; response=$responseEntity" }

        return responseEntity
    }

    override fun checkToken(userId: String, token: String): ResponseEntity<CheckTokenResponse> {
        val map = LinkedMultiValueMap<Any, Any>().apply {
            set("token", token)
        }
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
            set("Authorization", "Basic $basicAuthToken")
            set("User-Agent", RandomUserAgent.getRandomUserAgent())
            set(USER_ID_HEADER, userId)
        }
        val responseEntity =
            lkRestTemplate.exchange<CheckTokenResponse>(
                "https://api-seller.uzum.uz/api/auth/seller/check_token",
                HttpMethod.POST,
                HttpEntity(map, headers)
            )
        log.debug { "Check token response. userId=$userId; token=$token; response=$responseEntity" }

        return responseEntity
    }

    private fun <T> handleResponse(responseEntity: ResponseEntity<T>): T {
        val statusCode = responseEntity.statusCode
        val isError = statusCode.series() == HttpStatus.Series.CLIENT_ERROR
                || statusCode.series() == HttpStatus.Series.SERVER_ERROR
        if (isError) {
            throw UzumClientException(statusCode.value())
        }
        return responseEntity.body!!
    }

    private fun <T> handleProxyResponse(styxResponse: StyxResponse<T>): T? {
        val originalStatus = styxResponse.originalStatus
        val statusCode = HttpStatus.resolve(originalStatus)
        val isError = statusCode == null
                || statusCode.series() == HttpStatus.Series.CLIENT_ERROR
                || statusCode.series() == HttpStatus.Series.SERVER_ERROR
        if (isError) {
            throw UzumProxyClientException(
                originalStatus,
                styxResponse.body.toString(),
                "Bad response. StyxStatus=${styxResponse.code}; Status=$originalStatus; Body=${styxResponse.body.toString()}"
            )
        }
        if (styxResponse.code != 0) {
            log.warn { "Bad proxy status - ${styxResponse.code}" }
        }
        return styxResponse.body
    }

    private fun getUrlEncodedString(params: HashMap<String, String>): String {
        val result = StringBuilder()
        var first = true
        for ((key, value) in params) {
            if (first) first = false else result.append("&")
            result.append(URLEncoder.encode(key, "UTF-8"))
            result.append("=")
            result.append(URLEncoder.encode(value, "UTF-8"))
        }
        return result.toString()
    }

    companion object {
        const val basicAuthToken = "YjJjLWZyb250OmNsaWVudFNlY3JldA=="
        const val USER_ID_HEADER = "X-USER-ID"
    }
}
