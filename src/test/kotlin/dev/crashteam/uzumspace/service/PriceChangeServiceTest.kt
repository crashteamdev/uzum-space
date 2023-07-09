package dev.crashteam.uzumspace.service

import dev.crashteam.openapi.space.model.AddStrategyRequest
import dev.crashteam.openapi.space.model.EqualPriceStrategy
import dev.crashteam.uzumspace.ContainerConfiguration
import dev.crashteam.uzumspace.client.uzum.model.lk.AccountProductDescription
import dev.crashteam.uzumspace.db.model.enums.MonitorState
import dev.crashteam.uzumspace.db.model.enums.UpdateState
import dev.crashteam.uzumspace.repository.postgre.*
import dev.crashteam.uzumspace.repository.postgre.entity.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDateTime
import java.util.*

@Testcontainers
class PriceChangeServiceTest : ContainerConfiguration() {

    @Autowired
    lateinit var priceChangeService: PriceChangeService

    @Autowired
    lateinit var accountRepository: AccountRepository

    @Autowired
    lateinit var uzumAccountRepository: UzumAccountRepository

    @Autowired
    lateinit var uzumAccountShopRepository: UzumAccountShopRepository

    @Autowired
    lateinit var uzumAccountShopItemRepository: UzumAccountShopItemRepository

    @Autowired
    lateinit var uzumShopItemPoolRepository: UzumAccountShopItemPoolRepository

    @Autowired
    lateinit var uzumShopItemRepository: UzumShopItemRepository

    @Autowired
    lateinit var uzumAccountShopItemCompetitorRepository: UzumAccountShopItemCompetitorRepository

    @Autowired
    lateinit var priceHistoryRepository: UzumShopItemPriceHistoryRepository

    @MockBean
    lateinit var uzumSecureService: UzumSecureService

    val userId = UUID.randomUUID().toString()

    val uzumAccountId = UUID.randomUUID()

    val uzumAccountShopId = UUID.randomUUID()

    val uzumAccountShopItemId = UUID.randomUUID()

    @BeforeEach
    internal fun setUp() {
        accountRepository.deleteByUserId(userId)
        accountRepository.save(AccountEntity(userId = userId))
        val accountEntity = accountRepository.getAccount(userId)
        uzumAccountRepository.save(
            UzumAccountEntity(
                id = uzumAccountId,
                accountId = accountEntity?.id!!,
                externalAccountId = 634542345L,
                name = "test",
                login = "test",
                password = "test",
                monitorState = MonitorState.active,
                updateState = UpdateState.not_started,
            )
        )
        val kazanExpressAccountShopEntity = UzumAccountShopEntity(
            id = uzumAccountShopId,
            uzumAccountId = uzumAccountId,
            externalShopId = 123432,
            name = "Test",
            skuTitle = "TEST-SHOP"
        )
        uzumAccountShopRepository.save(kazanExpressAccountShopEntity)
        uzumAccountShopItemRepository.save(
            UzumAccountShopItemEntity(
                id = uzumAccountShopItemId,
                uzumAccountId = uzumAccountId,
                uzumAccountShopId = kazanExpressAccountShopEntity.id!!,
                categoryId = 123,
                productId = 123456,
                skuId = 789,
                name = "testName",
                photoKey = "gfdfqkowef",
                purchasePrice = 50000,
                price = 10000,
                barCode = 4535643512893379581L,
                availableAmount = 10,
                lastUpdate = LocalDateTime.now(),
                productSku = "testProductSku",
                skuTitle = "testSkuTitle",
                minimumThreshold = 1000,
                maximumThreshold = 6000,
                step = 10,
                strategyId = null
            )
        )
    }

    @Test
    fun `check equal price strategy`() {

        val kazanExpressAccountShopItemCompetitorEntity = UzumAccountShopItemCompetitorEntity(
            id = UUID.randomUUID(),
            uzumAccountShopItemId = uzumAccountShopItemId,
            productId = 635243L,
            skuId = 4231453L
        )
        val competitorKeShopItemEntity = UzumShopItemEntity(
            productId = 635243L,
            skuId = 4231453L,
            categoryId = 556231L,
            name = "test",
            photoKey = "test",
            avgHashFingerprint = "test",
            pHashFingerprint = "test",
            price = 7000,
            availableAmount = 10,
            lastUpdate = LocalDateTime.now()
        )
        uzumShopItemPoolRepository.save(UzumAccountShopItemPoolEntity(uzumAccountShopItemId))

        uzumShopItemRepository.save(competitorKeShopItemEntity)
        uzumAccountShopItemCompetitorRepository.save(kazanExpressAccountShopItemCompetitorEntity)

        val equalPriceStrategy = EqualPriceStrategy()
        equalPriceStrategy.maximumThreshold = 6000.0
        equalPriceStrategy.minimumThreshold = 100.0
        equalPriceStrategy.strategyType = "equal_price"

        val strategyRequest = AddStrategyRequest(uzumAccountShopItemId, equalPriceStrategy)
        uzumAccountShopItemRepository.saveStrategy(strategyRequest)

        whenever(uzumSecureService.getProductDescription(any(), any(), any(), any() )).then {
            AccountProductDescription(
                id = 12345L,
                shopSkuTitle = "skuTitle",
                title = "justTitle",
                productSkuTitle = "productSkuTitle",
                commission = 1,
                hasActiveCalendarEvents = true,
                hasCustomCharacteristics = false,
                definedCharacteristicList = emptyList(),
                customCharacteristicList = emptyList(),
                skuList = emptyList()
            )
        }
        whenever(uzumSecureService.changeAccountShopItemPrice(any(), any(), any(), any())).then { true }

        priceChangeService.recalculateUserShopItemPrice(userId, uzumAccountId)
        val paginateEntities =
            priceHistoryRepository.findHistoryByShopItemId(uzumAccountShopItemId, limit = 10, offset = 0)
        val shopItemEntity = uzumAccountShopItemRepository.findShopItem(uzumAccountId, uzumAccountShopId, uzumAccountShopItemId)
        val shopItemPoolFilledEntity = uzumShopItemPoolRepository.findShopItemInPool(userId, uzumAccountId).first()

        // Then
        assertEquals(1, paginateEntities.size)
        assertEquals(10000, paginateEntities.first().item.oldPrice)
        assertEquals(6000, paginateEntities.first().item.price)
        assertTrue(shopItemPoolFilledEntity.lastCheck != null)
        assertEquals(6000, shopItemEntity?.price)

    }

    @Test
    fun `change user pool item price`() {

        val kazanExpressAccountShopItemCompetitorEntity = UzumAccountShopItemCompetitorEntity(
            id = UUID.randomUUID(),
            uzumAccountShopItemId = uzumAccountShopItemId,
            productId = 635242L,
            skuId = 4231456L
        )
        val competitorKeShopItemEntity = UzumShopItemEntity(
            productId = 635242L,
            skuId = 4231456L,
            categoryId = 556235L,
            name = "test",
            photoKey = "test",
            avgHashFingerprint = "test",
            pHashFingerprint = "test",
            price = 5000,
            availableAmount = 10,
            lastUpdate = LocalDateTime.now()
        )
        uzumShopItemPoolRepository.save(UzumAccountShopItemPoolEntity(uzumAccountShopItemId))

        uzumShopItemRepository.save(competitorKeShopItemEntity)
        uzumAccountShopItemCompetitorRepository.save(kazanExpressAccountShopItemCompetitorEntity)

        whenever(uzumSecureService.getProductDescription(any(), any(), any(), any() )).then {
            AccountProductDescription(
                id = 12345L,
                shopSkuTitle = "skuTitle",
                title = "justTitle",
                productSkuTitle = "productSkuTitle",
                commission = 1,
                hasActiveCalendarEvents = true,
                hasCustomCharacteristics = false,
                definedCharacteristicList = emptyList(),
                customCharacteristicList = emptyList(),
                skuList = emptyList()
            )
        }
        whenever(uzumSecureService.changeAccountShopItemPrice(any(), any(), any(), any())).then { true }

        priceChangeService.recalculateUserShopItemPrice(userId, uzumAccountId)
        val paginateEntities =
            priceHistoryRepository.findHistoryByShopItemId(uzumAccountShopItemId, limit = 10, offset = 0)
        val shopItemEntity = uzumAccountShopItemRepository.findShopItem(uzumAccountId, uzumAccountShopId, uzumAccountShopItemId)
        val shopItemPoolFilledEntity = uzumShopItemPoolRepository.findShopItemInPool(userId, uzumAccountId).first()

        // Then
        assertEquals(1, paginateEntities.size)
        assertEquals(10000, paginateEntities.first().item.oldPrice)
        assertEquals(4000, paginateEntities.first().item.price)
        assertTrue(shopItemPoolFilledEntity.lastCheck != null)
        assertEquals(4000, shopItemEntity?.price)
    }

}
