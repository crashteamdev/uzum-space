package dev.crashteam.uzumspace.repository.postgre

import dev.crashteam.uzumspace.db.model.tables.UzumAccountShopItemCompetitor.UZUM_ACCOUNT_SHOP_ITEM_COMPETITOR
import dev.crashteam.uzumspace.db.model.tables.UzumShopItem.UZUM_SHOP_ITEM
import dev.crashteam.uzumspace.db.model.tables.UzumAccountShopItemCompetitor.UZUM_ACCOUNT_SHOP_ITEM_COMPETITOR
import dev.crashteam.uzumspace.extensions.paginate
import dev.crashteam.uzumspace.repository.postgre.entity.UzumAccountShopItemCompetitorEntity
import dev.crashteam.uzumspace.repository.postgre.entity.UzumAccountShopItemCompetitorEntityJoinUzumShopItemEntity
import dev.crashteam.uzumspace.repository.postgre.entity.PaginateEntity
import dev.crashteam.uzumspace.repository.postgre.mapper.RecordToUzumAccountShopItemCompetitorEntityJoinUzumShopItemEntityMapper
import dev.crashteam.uzumspace.repository.postgre.mapper.RecordToUzumAccountShopItemCompetitorEntityMapper
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class UzumAccountShopItemCompetitorRepository(
    private val dsl: DSLContext,
    private val recordToUzumAccountShopItemCompetitorMapper: RecordToUzumAccountShopItemCompetitorEntityMapper,
    private val recordToUzumAccountShopItemCompetitorEntityJoinUzumShopItemEntityMapper: RecordToUzumAccountShopItemCompetitorEntityJoinUzumShopItemEntityMapper
) {

    fun save(competitorEntity: UzumAccountShopItemCompetitorEntity): Int {
        val c = UZUM_ACCOUNT_SHOP_ITEM_COMPETITOR
        return dsl.insertInto(
            c,
            c.ID,
            c.UZUM_ACCOUNT_SHOP_ITEM_ID,
            c.PRODUCT_ID,
            c.SKU_ID
        ).values(
            competitorEntity.id,
            competitorEntity.uzumAccountShopItemId,
            competitorEntity.productId,
            competitorEntity.skuId
        ).execute()
    }

    fun findShopItemCompetitors(
        uzumAccountShopItemId: UUID,
        filter: Condition? = null,
        sortFields: List<Pair<Field<*>, SortType>>? = null,
        limit: Long,
        offset: Long,
    ): List<PaginateEntity<UzumAccountShopItemCompetitorEntityJoinUzumShopItemEntity>> {
        val s = UZUM_SHOP_ITEM
        val c = UZUM_ACCOUNT_SHOP_ITEM_COMPETITOR
        var select = dsl.select(
            s.PRODUCT_ID,
            s.SKU_ID,
            s.CATEGORY_ID,
            s.NAME,
            s.AVAILABLE_AMOUNT,
            s.PRICE,
            s.PHOTO_KEY,
            c.ID,
            c.UZUM_ACCOUNT_SHOP_ITEM_ID,
        )
            .from(c.join(s).on(s.PRODUCT_ID.eq(c.PRODUCT_ID).and(s.SKU_ID.eq(c.SKU_ID))))
            .where(c.UZUM_ACCOUNT_SHOP_ITEM_ID.eq(uzumAccountShopItemId))
        if (filter != null) {
            select = select.and(filter)
        }
        val sortFields = sortFields ?: listOf(UZUM_ACCOUNT_SHOP_ITEM_COMPETITOR.PRODUCT_ID to SortType.ASC)
        val records = dsl.paginate(select, sortFields, limit, offset).fetch()
        return records.map {
            PaginateEntity(
                item = recordToUzumAccountShopItemCompetitorEntityJoinUzumShopItemEntityMapper.convert(it),
                limit = limit,
                offset = offset,
                total = it.get("total_rows", Long::class.java),
                row = it.get("row", Long::class.java)
            )
        }
    }

    fun findShopItemCompetitors(
        uzumAccountShopItemId: UUID,
    ): List<UzumAccountShopItemCompetitorEntity> {
        val c = UZUM_ACCOUNT_SHOP_ITEM_COMPETITOR
        val records = dsl.selectFrom(c)
            .where(c.UZUM_ACCOUNT_SHOP_ITEM_ID.eq(uzumAccountShopItemId))
            .fetch()

        return records.map { recordToUzumAccountShopItemCompetitorMapper.convert(it) }
    }

    fun findShopItemCompetitorsCount(
        uzumAccountShopItemId: UUID
    ): Int {
        val c = UZUM_ACCOUNT_SHOP_ITEM_COMPETITOR
        return dsl.selectCount()
            .from(UZUM_ACCOUNT_SHOP_ITEM_COMPETITOR)
            .where(c.UZUM_ACCOUNT_SHOP_ITEM_ID.eq(uzumAccountShopItemId))
            .fetchOne(0, Int::class.java) ?: 0
    }

    fun findShopItemCompetitorsWithData(
        uzumAccountShopItemId: UUID,
    ): List<UzumAccountShopItemCompetitorEntityJoinUzumShopItemEntity> {
        val s = UZUM_SHOP_ITEM
        val c = UZUM_ACCOUNT_SHOP_ITEM_COMPETITOR
        val records = dsl.selectFrom(c.join(s).on(s.PRODUCT_ID.eq(c.PRODUCT_ID).and(s.SKU_ID.eq(c.SKU_ID))))
            .where(c.UZUM_ACCOUNT_SHOP_ITEM_ID.eq(uzumAccountShopItemId))
            .fetch()

        return records.map { recordToUzumAccountShopItemCompetitorEntityJoinUzumShopItemEntityMapper.convert(it) }
    }

    fun removeShopItemCompetitor(
        uzumAccountShopItemId: UUID,
        competitorId: UUID
    ): Int {
        val c = UZUM_ACCOUNT_SHOP_ITEM_COMPETITOR
        return dsl.deleteFrom(c)
            .where(c.UZUM_ACCOUNT_SHOP_ITEM_ID.eq(uzumAccountShopItemId).and(c.ID.eq(competitorId)))
            .execute()
    }

}
