package dev.crashteam.uzumspace.repository.postgre

import dev.crashteam.uzumspace.db.model.Tables.UZUM_ACCOUNT_SHOP_ITEM_STRATEGY
import dev.crashteam.uzumspace.db.model.tables.Account.ACCOUNT
import dev.crashteam.uzumspace.db.model.tables.UzumAccount.UZUM_ACCOUNT
import dev.crashteam.uzumspace.db.model.tables.UzumAccountShop.UZUM_ACCOUNT_SHOP
import dev.crashteam.uzumspace.db.model.tables.UzumAccountShopItem.UZUM_ACCOUNT_SHOP_ITEM
import dev.crashteam.uzumspace.db.model.tables.UzumAccountShopItemPool.UZUM_ACCOUNT_SHOP_ITEM_POOL
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
        val p = UZUM_ACCOUNT_SHOP_ITEM_POOL
        return dsl.insertInto(
            p,
            p.UZUM_ACCOUNT_SHOP_ITEM_ID,
            p.LAST_CHECK
        ).values(
            kazanExpressAccountShopItemPoolEntity.uzumAccountShopItemId,
            kazanExpressAccountShopItemPoolEntity.lastCheck
        ).onDuplicateKeyIgnore().execute()
    }

    fun saveBatch(kazanExpressAccountShopItemPoolEntities: List<UzumAccountShopItemPoolEntity>): IntArray {
        val p = UZUM_ACCOUNT_SHOP_ITEM_POOL
        return dsl.batch(
            kazanExpressAccountShopItemPoolEntities.map { kazanExpressAccountShopItemPoolEntity ->
                dsl.insertInto(
                    p,
                    p.UZUM_ACCOUNT_SHOP_ITEM_ID,
                    p.LAST_CHECK
                ).values(
                    kazanExpressAccountShopItemPoolEntity.uzumAccountShopItemId,
                    kazanExpressAccountShopItemPoolEntity.lastCheck
                ).onDuplicateKeyIgnore()
            }
        ).execute()
    }

    fun updateLastCheck(uzumAccountShopItemId: UUID, lastCheck: LocalDateTime): Int {
        val p = UZUM_ACCOUNT_SHOP_ITEM_POOL
        return dsl.update(p)
            .set(p.LAST_CHECK, lastCheck)
            .where(p.UZUM_ACCOUNT_SHOP_ITEM_ID.eq(uzumAccountShopItemId))
            .execute()
    }

    fun delete(uzumAccountShopItemId: UUID): Int {
        val p = UZUM_ACCOUNT_SHOP_ITEM_POOL
        return dsl.deleteFrom(p)
            .where(p.UZUM_ACCOUNT_SHOP_ITEM_ID.eq(uzumAccountShopItemId))
            .execute()
    }

    fun delete(keAccountShopItemId: List<UUID>): Int {
        val p = UZUM_ACCOUNT_SHOP_ITEM_POOL
        return dsl.deleteFrom(p)
            .where(p.UZUM_ACCOUNT_SHOP_ITEM_ID.`in`(keAccountShopItemId))
            .execute()
    }

    fun findCountShopItemsInPoolForUser(userId: String): Int {
        val a = ACCOUNT
        val k = UZUM_ACCOUNT
        val s = UZUM_ACCOUNT_SHOP
        val i = UZUM_ACCOUNT_SHOP_ITEM
        val p = UZUM_ACCOUNT_SHOP_ITEM_POOL
        return dsl.fetchCount(
            dsl.selectFrom(
                a.join(k).on(k.ACCOUNT_ID.eq(a.ID))
                    .join(s).on(s.UZUM_ACCOUNT_ID.eq(k.ID))
                    .join(i).on(s.ID.eq(i.UZUM_ACCOUNT_SHOP_ID))
                    .join(p).on(i.ID.eq(p.UZUM_ACCOUNT_SHOP_ITEM_ID))
            )
                .where(a.USER_ID.eq(userId))
        )
    }

    fun findShopItemInPool(
        userId: String,
        uzumAccountId: UUID,
        uzumAccountShopId: UUID,
        filter: Condition? = null,
        sortFields: List<Pair<Field<*>, SortType>>? = null,
        limit: Long,
        offset: Long
    ): List<PaginateEntity<UzumAccountShopItemEntity>> {
        val a = ACCOUNT
        val k = UZUM_ACCOUNT
        val s = UZUM_ACCOUNT_SHOP
        val i = UZUM_ACCOUNT_SHOP_ITEM
        val p = UZUM_ACCOUNT_SHOP_ITEM_POOL
        var select = dsl.select(*UZUM_ACCOUNT_SHOP_ITEM.fields(), p.UZUM_ACCOUNT_SHOP_ITEM_ID)
            .from(i)
            .join(p).on(p.UZUM_ACCOUNT_SHOP_ITEM_ID.eq(i.ID))
            .join(s).on(i.UZUM_ACCOUNT_SHOP_ID.eq(s.ID))
            .join(k).on(s.UZUM_ACCOUNT_ID.eq(k.ID))
            .join(a).on(k.ACCOUNT_ID.eq(a.ID))
            .where(a.USER_ID.eq(userId).and(k.ID.eq(uzumAccountId).and(s.ID.eq(uzumAccountShopId))))
        if (filter != null) {
            select = select.and(filter)
        }
        val sortFields = sortFields ?: listOf(UZUM_ACCOUNT_SHOP_ITEM.ID to SortType.ASC)
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
        uzumAccountId: UUID
    ): List<UzumAccountShopItemPoolFilledEntity> {
        val a = ACCOUNT
        val k = UZUM_ACCOUNT
        val s = UZUM_ACCOUNT_SHOP
        val i = UZUM_ACCOUNT_SHOP_ITEM
        val p = UZUM_ACCOUNT_SHOP_ITEM_POOL
        val st = UZUM_ACCOUNT_SHOP_ITEM_STRATEGY
        return dsl.select(
            p.UZUM_ACCOUNT_SHOP_ITEM_ID,
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
            p.LAST_CHECK)
            .from(i)
            .innerJoin(st).on(st.UZUM_ACCOUNT_SHOP_ITEM_ID.eq(i.ID))
            .join(p).on(p.UZUM_ACCOUNT_SHOP_ITEM_ID.eq(i.ID))
            .join(s).on(i.UZUM_ACCOUNT_SHOP_ID.eq(s.ID))
            .join(k).on(s.UZUM_ACCOUNT_ID.eq(k.ID))
            .join(a).on(k.ACCOUNT_ID.eq(a.ID))
            .where(a.USER_ID.eq(userId).and(k.ID.eq(uzumAccountId)))
            .fetch().map { record ->
                UzumAccountShopItemPoolFilledEntity(
                    uzumAccountShopItemId = record.getValue(p.UZUM_ACCOUNT_SHOP_ITEM_ID),
                    uzumAccountId = record.getValue(k.ID.`as`("ke_account_id")),
                    uzumAccountShopId = record.getValue(s.ID.`as`("ke_account_shop_id")),
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
                    lastCheck = record.getValue(p.LAST_CHECK)
                )
            }
    }

}
