package dev.crashteam.uzumspace.service

import dev.crashteam.uzumspace.ContainerConfiguration
import dev.crashteam.uzumspace.client.uzum.KazanExpressLkClient
import dev.crashteam.uzumspace.client.uzum.model.StyxResponse
import dev.crashteam.uzumspace.client.uzum.model.lk.AuthResponse
import dev.crashteam.uzumspace.client.uzum.model.lk.CheckTokenResponse
import dev.crashteam.uzumspace.db.model.enums.MonitorState
import dev.crashteam.uzumspace.db.model.enums.SubscriptionPlan
import dev.crashteam.uzumspace.repository.postgre.AccountRepository
import dev.crashteam.uzumspace.repository.postgre.SubscriptionRepository
import dev.crashteam.uzumspace.repository.postgre.entity.AccountEntity
import dev.crashteam.uzumspace.service.encryption.PasswordEncryptor
import dev.crashteam.uzumspace.service.error.AccountItemPoolLimitExceededException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.anyString
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.*

@Testcontainers
class UzumAccountServiceTest : ContainerConfiguration() {

    @MockBean
    lateinit var kazanExpressLkClient: KazanExpressLkClient

    @Autowired
    lateinit var uzumAccountService: UzumAccountService

    @Autowired
    lateinit var accountRepository: AccountRepository

    @Autowired
    lateinit var accountSubscriptionRepository: SubscriptionRepository

    @Autowired
    lateinit var passwordEncryptor: PasswordEncryptor

    val userId = UUID.randomUUID().toString()

    @BeforeEach
    internal fun setUp() {
        whenever(kazanExpressLkClient.auth(anyString(),  anyString(), anyString())).then {
            AuthResponse(
                accessToken = "testAccessToken",
                expiresIn = 99999,
                refreshToken = "testRefreshToken",
                scope = "testScope",
                tokenType = "JWT"
            )
        }
        whenever(kazanExpressLkClient.checkToken(anyString(), anyString())).then {
            StyxResponse(
                code = 200,
                originalStatus = 200,
                url = "testUrl",
                body = CheckTokenResponse(
                    accountId = 1234567 + Random().nextLong(1, 100),
                    active = true,
                    firstName = "testFirstName",
                    email = "testEmail",
                    sellerId = 123456789
                )
            )
        }
        accountRepository.deleteByUserId(userId)
        accountRepository.save(AccountEntity(userId = userId))
    }

    @Test
    fun `add ke account fail case of none subscription`() {
        // Given
        val login = "testLogin"
        val password = "testPassword"

        // When
        assertThrows(AccountItemPoolLimitExceededException::class.java) {
            uzumAccountService.addKeAccount(userId, login, password)
        }
    }

    @Test
    fun `add ke account`() {
        // Given
        val login = "testLogin"
        val password = "testPassword"

        // When
        val accountEntity = accountRepository.getAccount(userId)!!
        val subscriptionEntity = accountSubscriptionRepository.findSubscriptionByPlan(SubscriptionPlan.default_)
        accountRepository.save(accountEntity.copy(subscription = subscriptionEntity))
        val kazanExpressAccountEntity = uzumAccountService.addKeAccount(userId, login, password)
        val kePassword = Base64.getDecoder().decode(kazanExpressAccountEntity.password)
        val decryptPassword = passwordEncryptor.decryptPassword(kePassword)

        // Then
        assertEquals(login, kazanExpressAccountEntity.login)
        assertEquals(password, decryptPassword)
    }

    @Test
    fun `add ke account limit exceeded for subscription`() {
        // Given
        val login = "testLogin"
        val password = "testPassword"

        // When
        val accountEntity = accountRepository.getAccount(userId)!!
        val subscriptionEntity = accountSubscriptionRepository.findSubscriptionByPlan(SubscriptionPlan.default_)
        accountRepository.save(accountEntity.copy(subscription = subscriptionEntity))
        uzumAccountService.addKeAccount(userId, login, password)
        uzumAccountService.addKeAccount(userId, "$login-2", password)
        assertThrows(AccountItemPoolLimitExceededException::class.java) {
            uzumAccountService.addKeAccount(userId, login, password)
        }
    }

    @Test
    fun `edit ke account login password`() {
        // Given
        val login = "testLogin"
        val password = "testPassword"
        val newLogin = "newTestLogin"
        val newPassword = "newTestPassword"

        // When
        val accountEntity = accountRepository.getAccount(userId)!!
        val subscriptionEntity = accountSubscriptionRepository.findSubscriptionByPlan(SubscriptionPlan.default_)
        accountRepository.save(accountEntity.copy(subscription = subscriptionEntity))
        val kazanExpressAccountEntity = uzumAccountService.addKeAccount(userId, login, password)
        uzumAccountService.editKeAccount(userId, kazanExpressAccountEntity.id!!, newLogin, newPassword)
        val keAccount = uzumAccountService.getKeAccount(userId, kazanExpressAccountEntity.id!!)

        // Then
        assertEquals(newLogin, keAccount?.login)
        val decryptPassword = passwordEncryptor.decryptPassword(Base64.getDecoder().decode(keAccount?.password))
        assertEquals(newPassword, decryptPassword)
    }

    @Test
    fun `change account monitor state`() {
        // Given
        val login = "testLogin"
        val password = "testPassword"
        val nextMonitorState = MonitorState.active

        // When
        val accountEntity = accountRepository.getAccount(userId)!!
        val subscriptionEntity = accountSubscriptionRepository.findSubscriptionByPlan(SubscriptionPlan.default_)
        accountRepository.save(accountEntity.copy(subscription = subscriptionEntity))
        val kazanExpressAccountEntity = uzumAccountService.addKeAccount(userId, login, password)
        uzumAccountService.changeKeAccountMonitoringState(userId, kazanExpressAccountEntity.id!!, nextMonitorState)
        val keAccount = uzumAccountService.getKeAccount(userId, kazanExpressAccountEntity.id!!)

        // Then
        assertEquals(nextMonitorState, keAccount?.monitorState)
    }

    @Test
    fun `remove one ke account from user`() {
        // Given
        val login = "testLogin"
        val password = "testPassword"

        // When
        val accountEntity = accountRepository.getAccount(userId)!!
        val subscriptionEntity = accountSubscriptionRepository.findSubscriptionByPlan(SubscriptionPlan.default_)
        accountRepository.save(accountEntity.copy(subscription = subscriptionEntity))
        uzumAccountService.addKeAccount(userId, login, password)
        val keAccount = uzumAccountService.addKeAccount(userId, "$login-2", password)
        uzumAccountService.removeKeAccount(userId, keAccount.id!!)
        val keAccountCount = uzumAccountService.getKeAccounts(userId).size

        // Then
        assertEquals(1, keAccountCount)
    }

    @Test
    fun `purge all user ke accounts`() {
        // Given
        val login = "testLogin"
        val password = "testPassword"

        // When
        val accountEntity = accountRepository.getAccount(userId)!!
        val subscriptionEntity = accountSubscriptionRepository.findSubscriptionByPlan(SubscriptionPlan.default_)
        accountRepository.save(accountEntity.copy(subscription = subscriptionEntity))
        uzumAccountService.addKeAccount(userId, login, password)
        accountRepository.deleteByUserId(userId)
        val keAccountCount = uzumAccountService.getKeAccounts(userId).size

        // Then
        assertEquals(0, keAccountCount)
    }

}
