package dev.crashteam.uzumspace.repository.postgre

import dev.crashteam.uzumspace.db.model.tables.Account.ACCOUNT
import dev.crashteam.uzumspace.db.model.tables.KeAccount.KE_ACCOUNT
import dev.crashteam.uzumspace.db.model.tables.KeAccountShop.KE_ACCOUNT_SHOP
import dev.crashteam.uzumspace.db.model.tables.KeAccountShopItem.KE_ACCOUNT_SHOP_ITEM
import dev.crashteam.uzumspace.db.model.tables.KeAccountShopItemPool.KE_ACCOUNT_SHOP_ITEM_POOL
import dev.crashteam.uzumspace.extensions.paginate
import dev.crashteam.uzumspace.repository.postgre.entity.UzumAccountShopItemEntity
import dev.crashteam.uzumspace.repository.postgre.entity.UzumAccountShopItemPoolEntity
import dev.crashteam.uzumspace.repository.postgre.entity.UzumAccountShopItemPoolFilledEntity
import dev.crashteam.uzumspace.repository.postgre.entity.PaginateEntity
import dev.crashteam.uzumspace.repository.postgre.mapper.RecordToUzumAccountShopItemEntityMapper
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

@Repository
class UzumAccountShopItemPoolRepository(
    private val dsl: DSLContext,
    private val recordToUzumAccountShopItemEntityMapper: RecordToUzumAccountShopItemEntityMapper
) {

    fun save(kazanExpressAccountShopItemPoolEntity: UzumAccountShopItemPoolEntity): Int {
        val p = KE_ACCOUNT_SHOP_ITEM_POOL
        return dsl.insertInto(
            p,
            p.KE_ACCOUNT_SHOP_ITEM_ID,
            p.LAST_CHECK
        ).values(
            kazanExpressAccountShopItemPoolEntity.keAccountShopItemId,
            kazanExpressAccountShopItemPoolEntity.lastCheck
        ).onDuplicateKeyIgnore().execute()
    }

    fun updateLastCheck(keAccountShopItemId: UUID, lastCheck: LocalDateTime): Int {
        val p = KE_ACCOUNT_SHOP_ITEM_POOL
        return dsl.update(p)
            .set(p.LAST_CHECK, lastCheck)
            .where(p.KE_ACCOUNT_SHOP_ITEM_ID.eq(keAccountShopItemId))
            .execute()
    }

    fun delete(keAccountShopItemId: UUID): Int {
        val p = KE_ACCOUNT_SHOP_ITEM_POOL
        return dsl.deleteFrom(p)
            .where(p.KE_ACCOUNT_SHOP_ITEM_ID.eq(keAccountShopItemId))
            .execute()
    }

    fun findCountShopItemsInPoolForUser(userId: String): Int {
        val a = ACCOUNT
        val k = KE_ACCOUNT
        val s = KE_ACCOUNT_SHOP
        val i = KE_ACCOUNT_SHOP_ITEM
        val p = KE_ACCOUNT_SHOP_ITEM_POOL
        return dsl.fetchCount(
            dsl.selectFrom(
                a.join(k).on(k.ACCOUNT_ID.eq(a.ID))
                    .join(s).on(s.KE_ACCOUNT_ID.eq(k.ID))
                    .join(i).on(s.ID.eq(i.KE_ACCOUNT_SHOP_ID))
                    .join(p).on(i.ID.eq(p.KE_ACCOUNT_SHOP_ITEM_ID))
            )
                .where(a.USER_ID.eq(userId))
        )
    }

    fun findShopItemInPool(
        userId: String,
        keAccountId: UUID,
        keAccountShopId: UUID,
        filter: Condition? = null,
        sortFields: List<Pair<Field<*>, SortType>>? = null,
        limit: Long,
        offset: Long
    ): List<PaginateEntity<UzumAccountShopItemEntity>> {
        val a = ACCOUNT
        val k = KE_ACCOUNT
        val s = KE_ACCOUNT_SHOP
        val i = KE_ACCOUNT_SHOP_ITEM
        val p = KE_ACCOUNT_SHOP_ITEM_POOL
        var select = dsl.select(*KE_ACCOUNT_SHOP_ITEM.fields(), p.KE_ACCOUNT_SHOP_ITEM_ID)
            .from(i)
            .join(p).on(p.KE_ACCOUNT_SHOP_ITEM_ID.eq(i.ID))
            .join(s).on(i.KE_ACCOUNT_SHOP_ID.eq(s.ID))
            .join(k).on(s.KE_ACCOUNT_ID.eq(k.ID))
            .join(a).on(k.ACCOUNT_ID.eq(a.ID))
            .where(a.USER_ID.eq(userId).and(k.ID.eq(keAccountId).and(s.ID.eq(keAccountShopId))))
        if (filter != null) {
            select = select.and(filter)
        }
        val sortFields = sortFields ?: listOf(KE_ACCOUNT_SHOP_ITEM.ID to SortType.ASC)
        val records = dsl.paginate(select, sortFields, limit, offset).fetch()

        val items = records.map {
            PaginateEntity(
                item = recordToUzumAccountShopItemEntityMapper.convert(it),
                limit = limit,
                offset = offset,
                total = it.get("total_rows", Long::class.java),
                row = it.get("row", Long::class.java)
            )
        }
        return items
    }

    fun findShopItemInPool(
        userId: String,
        keAccountId: UUID
    ): List<UzumAccountShopItemPoolFilledEntity> {
        val a = ACCOUNT
        val k = KE_ACCOUNT
        val s = KE_ACCOUNT_SHOP
        val i = KE_ACCOUNT_SHOP_ITEM
        val p = KE_ACCOUNT_SHOP_ITEM_POOL
        return dsl.select(
            p.KE_ACCOUNT_SHOP_ITEM_ID,
            k.ID.`as`("ke_account_id"),
            s.ID.`as`("ke_account_shop_id"),
            i.PRODUCT_ID,
            i.SKU_ID,
            i.PRODUCT_SKU,
            i.PRICE,
            i.PURCHASE_PRICE,
            s.EXTERNAL_SHOP_ID,
            i.DISCOUNT,
            i.STEP,
            i.MINIMUM_THRESHOLD,
            i.MAXIMUM_THRESHOLD,
            i.SKU_TITLE,
            i.BARCODE,
            p.LAST_CHECK,
            i.KE_ACCOUNT_SHOP_ITEM_STRATEGY_ID
        )
            .from(i)
            .join(p).on(p.KE_ACCOUNT_SHOP_ITEM_ID.eq(i.ID))
            .join(s).on(i.KE_ACCOUNT_SHOP_ID.eq(s.ID))
            .join(k).on(s.KE_ACCOUNT_ID.eq(k.ID))
            .join(a).on(k.ACCOUNT_ID.eq(a.ID))
            .where(a.USER_ID.eq(userId).and(k.ID.eq(keAccountId)))
            .fetch().map { record ->
                UzumAccountShopItemPoolFilledEntity(
                    keAccountShopItemId = record.getValue(p.KE_ACCOUNT_SHOP_ITEM_ID),
                    keAccountId = record.getValue(k.ID.`as`("ke_account_id")),
                    keAccountShopId = record.getValue(s.ID.`as`("ke_account_shop_id")),
                    productId = record.getValue(i.PRODUCT_ID),
                    skuId = record.getValue(i.SKU_ID),
                    productSku = record.getValue(i.PRODUCT_SKU),
                    price = record.getValue(i.PRICE),
                    purchasePrice = record.getValue(i.PURCHASE_PRICE),
                    externalShopId = record.getValue(s.EXTERNAL_SHOP_ID),
                    discount = record.getValue(i.DISCOUNT),
                    step = record.getValue(i.STEP),
                    minimumThreshold = record.getValue(i.MINIMUM_THRESHOLD),
                    maximumThreshold = record.getValue(i.MAXIMUM_THRESHOLD),
                    skuTitle = record.getValue(i.SKU_TITLE),
                    barcode = record.getValue(i.BARCODE),
                    lastCheck = record.getValue(p.LAST_CHECK),
                    strategyId = record.getValue(i.KE_ACCOUNT_SHOP_ITEM_STRATEGY_ID)
                )
            }
    }

}
