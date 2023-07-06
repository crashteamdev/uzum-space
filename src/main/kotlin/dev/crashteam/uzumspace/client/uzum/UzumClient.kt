package dev.crashteam.uzumspace.client.uzum

import dev.crashteam.uzumspace.client.uzum.model.lk.*
import org.springframework.http.ResponseEntity

interface UzumClient {
    fun getAccountShops(userId: String, userToken: String): List<AccountShop>
    fun getAccountShopItems(userId: String, userToken: String, shopId: Long, page: Int = 0): List<AccountShopItem>
    fun changeAccountShopItemPrice(
        userId: String,
        userToken: String,
        shopId: Long,
        payload: ShopItemPriceChangePayload
    ): Boolean
    fun getProductInfo(userId: String, userToken: String, shopId: Long, productId: Long): AccountProductInfo
    fun getProductDescription(userId: String, userToken: String, shopId: Long, productId: Long): AccountProductDescription
    fun auth(userId: String, username: String, password: String): AuthResponse
    fun refreshAuth(userId: String, refreshToken: String): ResponseEntity<AuthResponse>?
    fun checkToken(userId: String, token: String): ResponseEntity<CheckTokenResponse>
}
