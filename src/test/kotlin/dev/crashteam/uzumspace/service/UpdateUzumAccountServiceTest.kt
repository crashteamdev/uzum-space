package dev.crashteam.uzumspace.service

import dev.crashteam.uzumspace.ContainerConfiguration
import dev.crashteam.uzumspace.client.uzum.UzumLkClient
import dev.crashteam.uzumspace.client.uzum.UzumWebClient
import dev.crashteam.uzumspace.client.uzum.model.lk.*
import dev.crashteam.uzumspace.db.model.enums.MonitorState
import dev.crashteam.uzumspace.db.model.enums.SubscriptionPlan
import dev.crashteam.uzumspace.db.model.enums.UpdateState
import dev.crashteam.uzumspace.repository.postgre.*
import dev.crashteam.uzumspace.repository.postgre.entity.AccountEntity
import dev.crashteam.uzumspace.repository.postgre.entity.UzumAccountEntity
import dev.crashteam.uzumspace.repository.postgre.entity.UzumAccountShopEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.ResponseEntity
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@Testcontainers
@SpringBootTest
class UpdateUzumAccountServiceTest : ContainerConfiguration() {

    @Autowired
    lateinit var updateUzumAccountService: UpdateUzumAccountService

    @Autowired
    lateinit var uzumAccountService: UzumAccountService

    @Autowired
    lateinit var accountRepository: AccountRepository

    @Autowired
    lateinit var uzumAccountRepository: UzumAccountRepository

    @Autowired
    lateinit var uzumAccountShopRepository: UzumAccountShopRepository

    @Autowired
    lateinit var uzumAccountShopItemRepository: UzumAccountShopItemRepository

    @Autowired
    lateinit var subscriptionRepository: SubscriptionRepository

    @MockBean
    lateinit var uzumSecureService: UzumSecureService

    @MockBean
    lateinit var uzumWebClient: UzumWebClient

    @MockBean
    lateinit var uzumLkClient: UzumLkClient

    val userId = UUID.randomUUID().toString()

    val uzumAccountId = UUID.randomUUID()

    @BeforeEach
    internal fun setUp() {
        val subscriptionEntity = subscriptionRepository.findSubscriptionByPlan(SubscriptionPlan.default_)
        accountRepository.deleteByUserId(userId)
        accountRepository.save(
            AccountEntity(
                userId = userId,
                subscription = subscriptionEntity,
                subscriptionValidUntil = LocalDateTime.now().plusDays(30)
            )
        )
        val accountId = accountRepository.getAccount(userId)!!.id!!
        val kazanExpressAccountEntity = UzumAccountEntity(
            id = uzumAccountId,
            accountId = accountId,
            externalAccountId = 14,
            name = "account name",
            lastUpdate = LocalDateTime.now(),
            monitorState = MonitorState.suspended,
            login = "test",
            password = "test",
            updateState = UpdateState.not_started
        )
        uzumAccountRepository.save(kazanExpressAccountEntity)
    }

    @Test
    fun `update uzum account shops data with new data`() {
        // Given
        val firstAccountShop = AccountShop(
            id = 1,
            shopTitle = "test",
            urlTitle = "testUrl",
            skuTitle = "testSkuTitle"
        )
        val secondAccountShop = AccountShop(
            id = 2,
            shopTitle = "test2",
            urlTitle = "testUrl2",
            skuTitle = "testSkuTitle2"
        )
        whenever(uzumSecureService.getAccountShops(any(), any())).then {
            listOf(firstAccountShop, secondAccountShop)
        }

        // When
        updateUzumAccountService.updateShops(userId, uzumAccountId)
        val uzumAccountShops = uzumAccountShopRepository.getUzumAccountShops(uzumAccountId)

        // Then
        assertEquals(2, uzumAccountShops.size)
        assertTrue(uzumAccountShops.find { it.externalShopId == firstAccountShop.id } != null)
        assertTrue(uzumAccountShops.find { it.externalShopId == secondAccountShop.id } != null)
    }

    @Test
    fun `update uzum account shops data with removing old shop`() {
        // Given
        val firstAccountShop = AccountShop(
            id = 1,
            shopTitle = "test",
            urlTitle = "testUrl",
            skuTitle = "testSkuTitle"
        )
        val secondAccountShop = AccountShop(
            id = 2,
            shopTitle = "test2",
            urlTitle = "testUrl2",
            skuTitle = "testSkuTitle2"
        )
        uzumAccountShopRepository.save(
            UzumAccountShopEntity(
                id = UUID.randomUUID(),
                uzumAccountId = uzumAccountId,
                externalShopId = firstAccountShop.id,
                name = firstAccountShop.shopTitle,
                skuTitle = firstAccountShop.skuTitle
            )
        )
        uzumAccountShopRepository.save(
            UzumAccountShopEntity(
                id = UUID.randomUUID(),
                uzumAccountId = uzumAccountId,
                externalShopId = secondAccountShop.id,
                name = secondAccountShop.shopTitle,
                skuTitle = secondAccountShop.skuTitle
            )
        )
        whenever(uzumSecureService.getAccountShops(any(), any())).then {
            listOf(firstAccountShop)
        }

        // When
        updateUzumAccountService.updateShops(userId, uzumAccountId)
        val uzumAccountShops = uzumAccountShopRepository.getUzumAccountShops(uzumAccountId)

        // Then
        assertEquals(1, uzumAccountShops.size)
        assertTrue(uzumAccountShops.find { it.externalShopId == firstAccountShop.id } != null)
    }

    @Test
    fun `update shop items`() {
        // Given
        val firstAccountShop = AccountShop(
            id = 1,
            shopTitle = "test",
            urlTitle = "testUrl",
            skuTitle = "testSkuTitle"
        )
        val uzumAccountShopEntity = UzumAccountShopEntity(
            id = UUID.randomUUID(),
            uzumAccountId = uzumAccountId,
            externalShopId = 1,
            name = "test",
            skuTitle = "TEST-SHOP"
        )
        uzumAccountShopRepository.save(uzumAccountShopEntity)
        val uzumShopItem = AccountShopItem(
            productId = 4132,
            title = "test",
            skuTitle = "test",
            category = "test",
            status = ShopItemStatus("test", "test"),
            moderationStatus = ShopItemModerationStatus("test", "test"),
            commission = BigDecimal.TEN,
            commissionDto = ShopItemCommission(1000, 2000),
            skuList = listOf(
                ShopItemSku(
                    skuTitle = "test",
                    skuFullTitle = "testFull",
                    productTitle = "test",
                    skuId = 54124,
                    barcode = 54321623L,
                    purchasePrice = BigDecimal.TEN,
                    price = BigDecimal.TEN,
                    quantityActive = 10,
                    quantityAdditional = 10,
                )

            ),
            image = "https://ke-images.servicecdn.ru/cbtma55i6omb975ssukg/t_product_240_low.jpg"
        )
        whenever(uzumSecureService.authUser(any(), any())).thenReturn("test")
        whenever(uzumLkClient.checkToken(any(), any())).thenReturn(
            ResponseEntity.ok(
                CheckTokenResponse(14L, true, "test", "test", 123L)
            )
        )
        whenever(uzumSecureService.getAccountShops(any(), any())).then {
            listOf(firstAccountShop)
        }
        whenever(
            uzumSecureService.getAccountShopItems(any(), any(), any(), any())
        ).then { listOf(uzumShopItem) }.then { emptyList<AccountShopItem>() }
        val accountProductInfo = AccountProductInfo(
            category = AccountProductCategory(1, "test"),
            title = AccountTitleInfo("test", "test"),
            skuTitle = "test"
        )
        whenever(
            uzumSecureService.getProductInfo(any(), any(), any(), any())
        ).then { accountProductInfo }
        whenever(uzumSecureService.getAccountShops(any(), any())).then {
            listOf(firstAccountShop)
        }
        whenever(
            uzumSecureService.getAccountShopItems(any(), any(), any(), any())
        ).then { listOf(uzumShopItem) }.then { emptyList<AccountShopItem>() }
        whenever(
            uzumWebClient.getProductInfo(any())
        ).then {
            null
        }

        // When
        uzumAccountService.syncAccount(userId, uzumAccountId)
        val shopItems = uzumAccountShopItemRepository.findShopItems(uzumAccountId, uzumAccountShopEntity.id!!)

        // Then
        assertEquals(1, shopItems.size)
        assertTrue(shopItems.find { it.productId == uzumShopItem.productId } != null)
    }
}
