package dev.crashteam.uzumspace.filter

import dev.crashteam.uzumspace.ContainerConfiguration
import dev.crashteam.uzumspace.db.model.enums.MonitorState
import dev.crashteam.uzumspace.db.model.enums.SubscriptionPlan
import dev.crashteam.uzumspace.db.model.enums.UpdateState
import dev.crashteam.uzumspace.repository.postgre.*
import dev.crashteam.uzumspace.repository.postgre.entity.AccountEntity
import dev.crashteam.uzumspace.repository.postgre.entity.UzumAccountEntity
import dev.crashteam.uzumspace.repository.postgre.entity.UzumAccountShopEntity
import dev.crashteam.uzumspace.repository.postgre.entity.UzumAccountShopItemEntity
import dev.crashteam.uzumspace.service.UzumAccountShopService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDateTime
import java.util.*

@Testcontainers
@SpringBootTest
class QueryFilterParserTest : ContainerConfiguration() {

    @Autowired
    lateinit var accountSubscriptionRepository: SubscriptionRepository

    @Autowired
    lateinit var accountRepository: AccountRepository

    @Autowired
    lateinit var uzumAccountRepository: UzumAccountRepository

    @Autowired
    lateinit var uzumAccountShopRepository: UzumAccountShopRepository

    @Autowired
    lateinit var uzumAccountShopItemRepository: UzumAccountShopItemRepository

    @Autowired
    lateinit var uzumAccountShopService: UzumAccountShopService

    @Autowired
    lateinit var queryFilterParser: QueryFilterParser

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    private val userId = UUID.randomUUID().toString()

    @BeforeEach
    fun setUp() {
        jdbcTemplate.execute("DELETE FROM account CASCADE")
        val subscriptionEntity = accountSubscriptionRepository.findSubscriptionByPlan(SubscriptionPlan.default_)
        accountRepository.save(
            AccountEntity(
                userId = userId,
                subscription = subscriptionEntity,
                subscriptionValidUntil = LocalDateTime.now().plusDays(30)
            )
        )
    }

    @Test
    fun `get shop items with filter`() {
        // Given
        val keAccountId = UUID.randomUUID()
        val keAccountShopId = UUID.randomUUID()
        val keAccountShopItemId = UUID.randomUUID()
        val accountId = accountRepository.getAccount(userId)!!.id!!
        val kazanExpressAccountEntity = UzumAccountEntity(
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
        val kazanExpressAccountShopEntity = UzumAccountShopEntity(
            id = keAccountShopId,
            uzumAccountId = kazanExpressAccountEntity.id!!,
            externalShopId = 123432,
            name = "Test",
            skuTitle = "TEST-SHOP"
        )
        uzumAccountShopRepository.save(kazanExpressAccountShopEntity)
        uzumAccountShopItemRepository.save(
            UzumAccountShopItemEntity(
                id = keAccountShopItemId,
                uzumAccountId = kazanExpressAccountEntity.id!!,
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
                maximumThreshold = 2000,
                step = 10
            )
        )


        // When
        val parseFilter = queryFilterParser.parseFilter(
            "skuId:789;name:testName", null, KeAccountShopItemFilterRecordMapper()
        )
        val shopItemCompetitors = uzumAccountShopService.getUzumAccountShopItems(
            userId,
            keAccountId,
            keAccountShopId,
            parseFilter.filterCondition,
            parseFilter.sortFields,
            100,
            0
        )

        // Then
        Assertions.assertTrue(shopItemCompetitors.size == 1)
    }


}
