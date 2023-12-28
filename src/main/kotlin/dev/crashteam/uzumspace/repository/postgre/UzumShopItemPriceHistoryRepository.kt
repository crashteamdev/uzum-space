package dev.crashteam.uzumspace.repository.postgre

import dev.crashteam.uzumspace.db.model.tables.UzumAccount.*
import dev.crashteam.uzumspace.db.model.tables.UzumAccountShop.UZUM_ACCOUNT_SHOP
import dev.crashteam.uzumspace.db.model.tables.UzumAccountShopItem.UZUM_ACCOUNT_SHOP_ITEM
import dev.crashteam.uzumspace.db.model.tables.UzumAccountShopItemPriceHistory.UZUM_ACCOUNT_SHOP_ITEM_PRICE_HISTORY
import dev.crashteam.uzumspace.extensions.paginate
import dev.crashteam.uzumspace.repository.postgre.entity.UzumShopItemPriceHistoryEntity
import dev.crashteam.uzumspace.repository.postgre.entity.UzumShopItemPriceHistoryEntityJointItemAndShopEntity
import dev.crashteam.uzumspace.repository.postgre.entity.PaginateEntity
import dev.crashteam.uzumspace.repository.postgre.mapper.RecordToUzumAccountShopItemPriceHistoryEntityJoinShopItemAndShopMapper
import dev.crashteam.uzumspace.repository.postgre.mapper.RecordToUzumAccountShopItemPriceHistoryEntityMapper
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class UzumShopItemPriceHistoryRepository(
    private val dsl: DSLContext,
    private val recordMapper: RecordToUzumAccountShopItemPriceHistoryEntityMapper,
    private val recordPriceHistoryShopItemShopMapper: RecordToUzumAccountShopItemPriceHistoryEntityJoinShopItemAndShopMapper
) {

    fun save(keShopItemPriceHistoryEntity: UzumShopItemPriceHistoryEntity): Int {
        val s = UZUM_ACCOUNT_SHOP_ITEM_PRICE_HISTORY
        return dsl.insertInto(
            s,
            s.UZUM_ACCOUNT_SHOP_ITEM_ID,
            s.UZUM_ACCOUNT_SHOP_ITEM_COMPETITOR_ID,
            s.CHANGE_TIME,
            s.OLD_PRICE,
            s.PRICE
        )
            .values(
                keShopItemPriceHistoryEntity.uzumAccountShopItemId,
                keShopItemPriceHistoryEntity.uzumAccountShopItemCompetitorId,
                keShopItemPriceHistoryEntity.changeTime,
                keShopItemPriceHistoryEntity.oldPrice,
                keShopItemPriceHistoryEntity.price
            ).execute()
    }

    fun findHistoryByUzumAccountId(
        uzumAccountId: UUID,
        filter: Condition? = null,
        sortFields: List<Pair<Field<*>, SortType>>? = null,
        limit: Long,
        offset: Long
    ): List<PaginateEntity<UzumShopItemPriceHistoryEntityJointItemAndShopEntity>> {
        val a = UZUM_ACCOUNT
        val i = UZUM_ACCOUNT_SHOP_ITEM
        val s = UZUM_ACCOUNT_SHOP
        val p = UZUM_ACCOUNT_SHOP_ITEM_PRICE_HISTORY
        var select = dsl.select(
            p.UZUM_ACCOUNT_SHOP_ITEM_ID,
            p.UZUM_ACCOUNT_SHOP_ITEM_COMPETITOR_ID,
            p.OLD_PRICE,
            p.PRICE,
            p.CHANGE_TIME,
            i.PRODUCT_ID,
            i.SKU_ID,
            i.NAME.`as`("item_name"),
            i.BARCODE,
            s.NAME.`as`("shop_name"),
            i.PHOTO_KEY,
        )
            .from(p)
            .join(i).on(p.UZUM_ACCOUNT_SHOP_ITEM_ID.eq(i.ID))
            .join(s).on(i.UZUM_ACCOUNT_SHOP_ID.eq(s.ID))
            .join(a).on(i.UZUM_ACCOUNT_ID.eq(a.ID))
            .where(a.ID.eq(uzumAccountId))
        if (filter != null) {
            select = select.and(filter)
        }
        val sortFields = sortFields ?: listOf(p.CHANGE_TIME to SortType.DESC)
        val records = dsl.paginate(select, sortFields, limit, offset).fetch()

        return records.map {
            PaginateEntity(
                item = recordPriceHistoryShopItemShopMapper.convert(it),
                limit = limit,
                offset = offset,
                total = it.get("total_rows", Long::class.java),
                row = it.get("row", Long::class.java)
            )
        }
    }

    fun findHistoryByShopItemId(
        shopItemId: UUID,
        filter: Condition? = null,
        sortFields: List<Pair<Field<*>, SortType>>? = null,
        limit: Long,
        offset: Long
    ): List<PaginateEntity<UzumShopItemPriceHistoryEntityJointItemAndShopEntity>> {
        val i = UZUM_ACCOUNT_SHOP_ITEM
        val s = UZUM_ACCOUNT_SHOP
        val p = UZUM_ACCOUNT_SHOP_ITEM_PRICE_HISTORY
        var select = dsl.select(
            p.UZUM_ACCOUNT_SHOP_ITEM_ID,
            p.UZUM_ACCOUNT_SHOP_ITEM_COMPETITOR_ID,
            p.OLD_PRICE,
            p.PRICE,
            p.CHANGE_TIME,
            i.PRODUCT_ID,
            i.SKU_ID,
            i.NAME.`as`("item_name"),
            i.BARCODE,
            s.NAME.`as`("shop_name"),
        )
            .from(p)
            .join(i).on(p.UZUM_ACCOUNT_SHOP_ITEM_ID.eq(i.ID))
            .join(s).on(i.UZUM_ACCOUNT_SHOP_ID.eq(s.ID))
            .where(p.UZUM_ACCOUNT_SHOP_ITEM_ID.eq(shopItemId))
        if (filter != null) {
            select = select.and(filter)
        }
        val sortFields = sortFields ?: listOf(p.CHANGE_TIME to SortType.ASC)
        val records = dsl.paginate(select, sortFields, limit, offset).fetch()

        return records.map {
            PaginateEntity(
                item = recordPriceHistoryShopItemShopMapper.convert(it),
                limit = limit,
                offset = offset,
                total = it.get("total_rows", Long::class.java),
                row = it.get("row", Long::class.java)
            )
        }
    }

}
