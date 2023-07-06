package dev.crashteam.uzumspace.service

import dev.crashteam.openapi.kerepricer.model.AddStrategyRequest
import dev.crashteam.openapi.kerepricer.model.CloseToMinimalStrategy
import dev.crashteam.uzumspace.ContainerConfiguration
import dev.crashteam.uzumspace.client.uzum.KazanExpressWebClient
import dev.crashteam.uzumspace.client.uzum.model.web.*
import dev.crashteam.uzumspace.db.model.enums.MonitorState
import dev.crashteam.uzumspace.db.model.enums.SubscriptionPlan
import dev.crashteam.uzumspace.db.model.enums.UpdateState
import dev.crashteam.uzumspace.repository.postgre.*
import dev.crashteam.uzumspace.repository.postgre.entity.*
import dev.crashteam.uzumspace.service.loader.RemoteImageLoader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.io.Resource
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@Testcontainers
class UzumAccountShopServiceTest : ContainerConfiguration() {

    @Autowired
    lateinit var accountRepository: AccountRepository

    @Autowired
    lateinit var uzumAccountRepository: UzumAccountRepository

    @Autowired
    lateinit var uzumAccountShopRepository: UzumAccountShopRepository

    @Autowired
    lateinit var uzumAccountShopItemRepository: UzumAccountShopItemRepository

    @Autowired
    lateinit var uzumAccountShopItemCompetitorRepository: UzumAccountShopItemCompetitorRepository

    @Autowired
    lateinit var accountSubscriptionRepository: SubscriptionRepository

    @Autowired
    lateinit var uzumShopItemRepository: UzumShopItemRepository

    @Autowired
    lateinit var uzumAccountShopService: UzumAccountShopService

    @Autowired
    lateinit var strategyRepository: UzumAccountShopItemStrategyRepository

    @MockBean
    lateinit var kazanExpressWebClient: KazanExpressWebClient

    @MockBean
    lateinit var remoteImageLoader: RemoteImageLoader

    @Value("classpath:cc1j1sp1ati4tcj33p5g-original.jpg")
    lateinit var keImage: Resource

    val userId = UUID.randomUUID().toString()

    val keAccountId = UUID.randomUUID()

    val keAccountShopId = UUID.randomUUID()

    val keAccountShopItemId = UUID.randomUUID()

    @BeforeEach
    internal fun setUp() {
        val subscriptionEntity = accountSubscriptionRepository.findSubscriptionByPlan(SubscriptionPlan.default_)
        accountRepository.deleteByUserId(userId)

        accountRepository.save(
            AccountEntity(
                userId = userId,
                subscription = subscriptionEntity,
                subscriptionValidUntil = LocalDateTime.now().plusDays(30)
            )
        )
        val accountId = accountRepository.getAccount(userId)!!.id!!
        val kazanExpressAccountEntity = KazanExpressAccountEntity(
            id = keAccountId,
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
        val kazanExpressAccountShopEntity = KazanExpressAccountShopEntity(
            id = keAccountShopId,
            keAccountId = kazanExpressAccountEntity.id!!,
            externalShopId = 123432,
            name = "Test",
            skuTitle = "TEST-SHOP"
        )
        uzumAccountShopRepository.save(kazanExpressAccountShopEntity)
        uzumAccountShopItemRepository.save(
            KazanExpressAccountShopItemEntity(
                id = keAccountShopItemId,
                keAccountId = kazanExpressAccountEntity.id!!,
                keAccountShopId = kazanExpressAccountShopEntity.id!!,
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
                maximumThreshold = 2000,
                step = 10,
                strategyId = null
            )
        )
        val closeToMinimalStrategy = CloseToMinimalStrategy(10, "close_to_minimal", 100.0, 100.0)
        val strategyRequest = AddStrategyRequest(keAccountShopItemId, closeToMinimalStrategy)
        uzumAccountShopItemRepository.saveStrategy(strategyRequest)
    }

    @Test
    fun `check if strategy exists`() {
        val shopItem = uzumAccountShopItemRepository.findShopItem(keAccountId, keAccountShopItemId)
        assertNotEquals(null, shopItem?.strategyId)
        val strategyEntity = shopItem!!.strategyId?.let { strategyRepository.findById(it) }
        assertEquals(strategyEntity?.strategyType, "close_to_minimal")
    }

    @Test
    fun `add shop item into pool`() {
        // When
        uzumAccountShopService.addShopItemIntoPool(userId, keAccountId, keAccountShopId, keAccountShopItemId)
        val keAccountShopItems =
            uzumAccountShopService.getShopItemsInPool(userId, keAccountId, keAccountShopId, limit = 10, offset = 0)
        val keAccountShopItem = keAccountShopItems.first().item

        // Then
        assertEquals(1, keAccountShopItems.size)
        assertEquals(keAccountShopItemId, keAccountShopItem.id)
    }

    @Test
    fun `remove shop item from pool`() {
        // When
        uzumAccountShopService.addShopItemIntoPool(userId, keAccountId, keAccountShopId, keAccountShopItemId)
        uzumAccountShopService.removeShopItemFromPool(userId, keAccountId, keAccountShopId, keAccountShopItemId)
        val shopItemPoolCount = uzumAccountShopService.getShopItemPoolCount(userId)

        // Then
        assertEquals(0, shopItemPoolCount)
    }

    @Test
    fun `add shop item competitor with exists shop item`() {
        // Given
        val productId = 5462623L
        val skuId = 7456462345L
        val keShopItem = KazanExpressShopItemEntity(
            productId = productId,
            skuId = skuId,
            categoryId = 12345,
            name = "testName",
            photoKey = "43Fkk10LE",
            avgHashFingerprint = "test",
            pHashFingerprint = "test",
            price = 1000,
            availableAmount = 10,
        )

        // When
        uzumShopItemRepository.save(keShopItem)
        uzumAccountShopService.addShopItemCompetitor(
            userId,
            keAccountId,
            keAccountShopId,
            keAccountShopItemId,
            productId,
            skuId
        )
        val shopItemCompetitors = uzumAccountShopItemCompetitorRepository.findShopItemCompetitors(keAccountShopItemId)

        // Then
        assertEquals(1, shopItemCompetitors.size)
        assertEquals(productId, shopItemCompetitors.first().productId)
        assertEquals(skuId, shopItemCompetitors.first().skuId)
    }

    @Test
    fun `add shop item competitor without exist shop item`() {
        // Given
        val productId = 5462623L
        val skuId = 7564265423L
        whenever(kazanExpressWebClient.getProductInfo(any())).then { buildProductResponse(productId, skuId) }
        whenever(remoteImageLoader.loadResource(any())).then { keImage.inputStream.readAllBytes() }

        // When
        uzumAccountShopService.addShopItemCompetitor(
            userId,
            keAccountId,
            keAccountShopId,
            keAccountShopItemId,
            productId,
            skuId
        )
        val shopItemCompetitors = uzumAccountShopItemCompetitorRepository.findShopItemCompetitors(keAccountShopItemId)

        // Then
        assertEquals(1, shopItemCompetitors.size)
        assertEquals(productId, shopItemCompetitors.first().productId)
        assertEquals(skuId, shopItemCompetitors.first().skuId)

    }

    private fun buildProductResponse(productId: Long, skuId: Long): ProductResponse {
        return ProductResponse(
            payload = ProductDataWrapper(
                data = ProductData(
                    id = productId,
                    title = "test",
                    category = ProductCategory(
                        id = 12343,
                        title = "test",
                        productAmount = 10,
                    ),
                    reviewsAmount = 10,
                    ordersAmount = 100,
                    rOrdersAmount = 150,
                    rating = BigDecimal.valueOf(5),
                    totalAvailableAmount = 350,
                    charityCommission = 50,
                    description = "test",
                    attributes = listOf("test"),
                    tags = listOf("test"),
                    photos = listOf(
                        ProductPhoto(
                            photo = mapOf("test" to ProductPhotoQuality("800", "400")),
                            photoKey = "test",
                            color = "black"
                        )
                    ),
                    characteristics = listOf(
                        ProductCharacteristic(
                            "black",
                            listOf(CharacteristicValue("black", "black"))
                        )
                    ),
                    skuList = listOf(
                        ProductSplit(
                            id = skuId,
                            characteristics = listOf(ProductSplitCharacteristic(charIndex = 0, valueIndex = 0)),
                            availableAmount = 10,
                            fullPrice = BigDecimal.TEN,
                            purchasePrice = BigDecimal.ONE
                        )
                    ),
                    seller = Seller(
                        id = 12341,
                        title = "test",
                        link = "test",
                        description = "test",
                        rating = BigDecimal.TEN,
                        sellerAccountId = 423542161,
                        isEco = false,
                        adultCategory = true,
                        contacts = listOf(Contact("test", "test"))
                    )
                )
            )
        )
    }
}
