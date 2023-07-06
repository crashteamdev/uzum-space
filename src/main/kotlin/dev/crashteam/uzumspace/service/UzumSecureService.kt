package dev.crashteam.uzumspace.service

import dev.crashteam.uzumspace.client.uzum.UzumLkClient
import dev.crashteam.uzumspace.client.uzum.model.lk.*
import dev.crashteam.uzumspace.repository.postgre.UzumAccountRepository
import dev.crashteam.uzumspace.repository.redis.UzumUserTokenRepository
import dev.crashteam.uzumspace.repository.redis.entity.UserTokenEntity
import dev.crashteam.uzumspace.service.encryption.PasswordEncryptor
import dev.crashteam.uzumspace.service.error.UzumUserAuthException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import java.util.*

@Component
class UzumSecureService(
    private val kazanExpressClient: UzumLkClient,
    private val accountRepository: UzumAccountRepository,
    private val uzumUserTokenRepository: UzumUserTokenRepository,
    private val passwordEncryptor: PasswordEncryptor,
) {

    fun getAccountShops(userId: String, keAccountId: UUID): List<AccountShop> {
        val userToken = authUser(userId, keAccountId)
        return kazanExpressClient.getAccountShops(userId, userToken)
    }

    fun getAccountShopItems(userId: String, keAccountId: UUID, shopId: Long, page: Int): List<AccountShopItem> {
        val userToken = authUser(userId, keAccountId)
        return kazanExpressClient.getAccountShopItems(userId, userToken, shopId, page)
    }

    fun getProductInfo(userId: String, keAccountId: UUID, shopId: Long, productId: Long): AccountProductInfo {
        val userToken = authUser(userId, keAccountId)
        return kazanExpressClient.getProductInfo(userId, userToken, shopId, productId)
    }

    fun changeAccountShopItemPrice(
        userId: String,
        keAccountId: UUID,
        shopId: Long,
        payload: ShopItemPriceChangePayload
    ): Boolean {
        val userToken = authUser(userId, keAccountId)
        return kazanExpressClient.changeAccountShopItemPrice(userId, userToken, shopId, payload)
    }

    fun getProductDescription(
        userId: String,
        keAccountId: UUID,
        shopId: Long,
        productId: Long
    ): AccountProductDescription {
        val userToken = authUser(userId, keAccountId)
        return kazanExpressClient.getProductDescription(userId, userToken, shopId, productId)
    }

    fun authUser(
        userId: String,
        keAccountId: UUID,
    ): String {
        val userTokenEntity = uzumUserTokenRepository.findById(userId + keAccountId.toString()).orElse(null)
        val accessToken = userTokenEntity?.accessToken
        val refreshToken = userTokenEntity?.refreshToken
        val recentUserToken = if (accessToken != null && refreshToken != null) {
            val checkTokenResponseStyxResponseEntity = kazanExpressClient.checkToken(userId, accessToken)
            val httpSeries = HttpStatus.Series.resolve(checkTokenResponseStyxResponseEntity.statusCodeValue)
            if (httpSeries == HttpStatus.Series.CLIENT_ERROR) {
                val refreshAuthResponseEntity = kazanExpressClient.refreshAuth(userId, refreshToken)
                val refreshAuthHttpSeries = HttpStatus.Series.resolve(refreshAuthResponseEntity.statusCodeValue)
                if (refreshAuthHttpSeries == HttpStatus.Series.CLIENT_ERROR) {
                    val kazanExpressAccount = accountRepository.getUzumAccount(userId, keAccountId)
                        ?: throw UzumUserAuthException("Not found user by userId=$userId; keAccountId=$keAccountId")
                    val password = Base64.getDecoder().decode(kazanExpressAccount.password.toByteArray())
                    val decryptedPassword = passwordEncryptor.decryptPassword(password)
                    val authResponse = kazanExpressClient.auth(userId, kazanExpressAccount.name!!, decryptedPassword)
                    RecentAuthUserToken(authResponse.accessToken, authResponse.refreshToken)
                } else {
                    RecentAuthUserToken(
                        refreshAuthResponseEntity.body!!.accessToken,
                        refreshAuthResponseEntity.body!!.refreshToken
                    )
                }
            } else {
                RecentAuthUserToken(accessToken, refreshToken)
            }
        } else {
            val kazanExpressAccount = accountRepository.getUzumAccount(userId, keAccountId)
                ?: throw UzumUserAuthException("Not found user by userId=$userId; keAccountId=$keAccountId")
            val kePassword = Base64.getDecoder().decode(kazanExpressAccount.password)
            val password = passwordEncryptor.decryptPassword(kePassword)
            val authResponse = kazanExpressClient.auth(userId, kazanExpressAccount.login, password)
            uzumUserTokenRepository.save(
                UserTokenEntity(
                    userId + keAccountId.toString(),
                    userId,
                    keAccountId.toString(),
                    authResponse.accessToken,
                    authResponse.refreshToken
                )
            )
            RecentAuthUserToken(authResponse.accessToken, authResponse.refreshToken)
        }

        return recentUserToken.accessToken
    }

    private data class RecentAuthUserToken(
        val accessToken: String,
        val refreshToken: String,
    )
}
