package dev.crashteam.uzumspace.repository.postgre

import dev.crashteam.repricer.repository.postgre.entity.UzumAccountShopEntityWithData
import dev.crashteam.uzumspace.db.model.Tables.UZUM_ACCOUNT_SHOP_ITEM
import dev.crashteam.uzumspace.db.model.Tables.UZUM_ACCOUNT_SHOP_ITEM_POOL
import dev.crashteam.uzumspace.db.model.enums.SubscriptionPlan
import dev.crashteam.uzumspace.db.model.tables.Subscription.SUBSCRIPTION
import dev.crashteam.uzumspace.db.model.tables.Account.ACCOUNT
import dev.crashteam.uzumspace.db.model.tables.UzumAccount.UZUM_ACCOUNT
import dev.crashteam.uzumspace.db.model.tables.UzumAccountShop.UZUM_ACCOUNT_SHOP
import dev.crashteam.uzumspace.db.model.tables.UzumAccountShopItemCompetitor.UZUM_ACCOUNT_SHOP_ITEM_COMPETITOR
import dev.crashteam.uzumspace.db.model.tables.records.UzumAccountShopRecord
import dev.crashteam.uzumspace.repository.postgre.entity.UzumAccountShopEntity
import dev.crashteam.uzumspace.repository.postgre.mapper.RecordToUzumAccountShopEntityDataMapper
import dev.crashteam.uzumspace.repository.postgre.mapper.RecordToUzumAccountShopEntityMapper
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.impl.DSL.*
import org.springframework.stereotype.Repository
import java.util.*


@Repository
class UzumAccountShopRepository(
    private val dsl: DSLContext,
    private val recordToUzumAccountShopEntityMapper: RecordToUzumAccountShopEntityMapper,
    private val recordToUzumAccountShopEntityDataMapper: RecordToUzumAccountShopEntityDataMapper
) {

    fun save(uzumAccountShopEntity: UzumAccountShopEntity): UUID? {
        val s = UZUM_ACCOUNT_SHOP
        return dsl.insertInto(
            s,
            s.ID,
            s.UZUM_ACCOUNT_ID,
            s.EXTERNAL_SHOP_ID,
            s.NAME,
            s.SKU_TITLE
        ).values(
            uzumAccountShopEntity.id,
            uzumAccountShopEntity.uzumAccountId,
            uzumAccountShopEntity.externalShopId,
            uzumAccountShopEntity.name,
            uzumAccountShopEntity.skuTitle
        ).onDuplicateKeyUpdate()
            .set(
                mapOf(
                    s.EXTERNAL_SHOP_ID to uzumAccountShopEntity.externalShopId,
                    s.NAME to uzumAccountShopEntity.name,
                    s.SKU_TITLE to uzumAccountShopEntity.skuTitle
                )
            )
            .returningResult(s.ID)
            .fetchOne()!!.getValue(s.ID)
    }

    fun saveBatch(uzumAccountShopEntity: List<UzumAccountShopEntity>) {
        val records = uzumAccountShopEntity.map {
            UzumAccountShopRecord(
                it.id,
                it.uzumAccountId,
                it.externalShopId,
                it.name,
                it.skuTitle
            )
        }
        dsl.batchInsert(records).execute()
    }

    fun getKeAccountShopsWithData(userId: String, keAccountId: UUID): List<UzumAccountShopEntityWithData> {
        val s = UZUM_ACCOUNT_SHOP
        val k = UZUM_ACCOUNT
        val p = UZUM_ACCOUNT_SHOP_ITEM_POOL
        val i = UZUM_ACCOUNT_SHOP_ITEM
        val a = ACCOUNT

        val productCount: Field<Int> = field(
            select(countDistinct(i.PRODUCT_ID))
                .from(i)
                .where(
                    i.UZUM_ACCOUNT_SHOP_ID.eq(UZUM_ACCOUNT_SHOP.ID)
                        .and(i.UZUM_ACCOUNT_ID.eq(UZUM_ACCOUNT.ID))
                )
        ).`as`("product_count")

        val skuCount = field(
            select(countDistinct(i.SKU_ID))
                .from(i)
                .where(
                    i.UZUM_ACCOUNT_SHOP_ID.eq(UZUM_ACCOUNT_SHOP.ID)
                        .and(i.UZUM_ACCOUNT_ID.eq(UZUM_ACCOUNT.ID))
                )
        ).`as`("sku_count")

        val poolCount = field(
            select(countDistinct(p.UZUM_ACCOUNT_SHOP_ITEM_ID))
                .from(p)
                .join(i)
                .on(i.ID.eq(p.UZUM_ACCOUNT_SHOP_ITEM_ID))
                .where(
                    i.UZUM_ACCOUNT_SHOP_ID.eq(UZUM_ACCOUNT_SHOP.ID)
                        .and(i.UZUM_ACCOUNT_ID.eq(UZUM_ACCOUNT.ID))
                )
        ).`as`("pool_count")

        val records = dsl.select(
            UZUM_ACCOUNT_SHOP.ID,
            UZUM_ACCOUNT_SHOP.UZUM_ACCOUNT_ID,
            UZUM_ACCOUNT_SHOP.EXTERNAL_SHOP_ID,
            UZUM_ACCOUNT_SHOP.NAME,
            UZUM_ACCOUNT_SHOP.SKU_TITLE,
            productCount,
            skuCount,
            poolCount
        ).from(s)
            .innerJoin(k).on(k.ID.eq(s.UZUM_ACCOUNT_ID))
            .innerJoin(a).on(k.ACCOUNT_ID.eq(a.ID))
            .where(a.USER_ID.eq(userId).and(s.UZUM_ACCOUNT_ID.eq(keAccountId)))
            .fetch()

        return records.map { recordToUzumAccountShopEntityDataMapper.convert(it) }.toList()
    }

    fun countKeAccountShopItemsInPool(userId: String): Int {
        val p = UZUM_ACCOUNT_SHOP_ITEM_POOL
        val a = ACCOUNT
        val i = UZUM_ACCOUNT_SHOP_ITEM
        val ka = UZUM_ACCOUNT

        return dsl.select(countDistinct(p.UZUM_ACCOUNT_SHOP_ITEM_ID))
            .from(p)
            .innerJoin(i).on(i.ID.eq(p.UZUM_ACCOUNT_SHOP_ITEM_ID))
            .innerJoin(ka).on(ka.ID.eq(i.UZUM_ACCOUNT_ID))
            .innerJoin(a).on(ka.ACCOUNT_ID.eq(a.ID))
            .where(a.USER_ID.eq(userId)).fetchOne()?.getValue(countDistinct(p.UZUM_ACCOUNT_SHOP_ITEM_ID)) as Int
    }

    fun getUserSubscriptionPlan(userId: String): SubscriptionPlan? {
        val a = ACCOUNT
        val s = SUBSCRIPTION
        val plan = dsl.select().from(s).innerJoin(a).on(a.SUBSCRIPTION_ID.eq(s.ID))
            .where(a.USER_ID.eq(userId)).fetchOne(s.PLAN)
        return plan
    }

    fun countAccounts(userId: String): Int {
        val a = ACCOUNT
        val ka = UZUM_ACCOUNT
        return dsl.select(countDistinct(ka.ACCOUNT_ID))
            .from(ka)
            .innerJoin(a).on(a.ID.eq(ka.ACCOUNT_ID))
            .where(a.USER_ID.eq(userId)).fetchOne()?.getValue(countDistinct(ka.ACCOUNT_ID)) as Int
    }

    fun countCompetitors(userId: String): Int {
        val c = UZUM_ACCOUNT_SHOP_ITEM_COMPETITOR
        val si = UZUM_ACCOUNT_SHOP_ITEM
        val ka = UZUM_ACCOUNT
        val a = ACCOUNT

        return dsl.select(countDistinct(c.ID))
            .from(c)
            .innerJoin(si).on(si.ID.eq(c.UZUM_ACCOUNT_SHOP_ITEM_ID))
            .innerJoin(ka).on(ka.ID.eq(si.UZUM_ACCOUNT_ID))
            .innerJoin(a).on(a.ID.eq(ka.ACCOUNT_ID))
            .where(a.USER_ID.eq(userId)).fetchOne()?.getValue(countDistinct(c.ID)) as Int
    }

    fun getUzumAccountShops(userId: String, uzumAccountId: UUID): List<UzumAccountShopEntity> {
        val s = UZUM_ACCOUNT_SHOP
        val k = UZUM_ACCOUNT
        val a = ACCOUNT
        val records = dsl.select()
            .from(s)
            .innerJoin(k).on(k.ID.eq(s.UZUM_ACCOUNT_ID))
            .innerJoin(a).on(k.ACCOUNT_ID.eq(a.ID))
            .where(a.USER_ID.eq(userId).and(s.UZUM_ACCOUNT_ID.eq(uzumAccountId)))
            .fetch()
        return records.map { recordToUzumAccountShopEntityMapper.convert(it) }.toList()
    }

    fun getUzumAccountShops(uzumAccountId: UUID): List<UzumAccountShopEntity> {
        val s = UZUM_ACCOUNT_SHOP
        val k = UZUM_ACCOUNT
        val records = dsl.select()
            .from(s)
            .innerJoin(k).on(k.ID.eq(s.UZUM_ACCOUNT_ID))
            .where(s.UZUM_ACCOUNT_ID.eq(uzumAccountId))
            .fetch()
        return records.map { recordToUzumAccountShopEntityMapper.convert(it) }.toList()
    }

    fun getUzumAccountShopByShopId(uzumAccountId: UUID, shopId: Long): UzumAccountShopEntity? {
        val s = UZUM_ACCOUNT_SHOP
        val k = UZUM_ACCOUNT
        val record = dsl.select(*s.fields())
            .from(s)
            .join(k).on(s.UZUM_ACCOUNT_ID.eq(k.ID))
            .where(k.ID.eq(uzumAccountId).and(s.EXTERNAL_SHOP_ID.eq(shopId)))
            .fetchOne() ?: return null
        return recordToUzumAccountShopEntityMapper.convert(record)
    }

    fun deleteByShopIds(shopIds: List<Long>): Int {
        val s = UZUM_ACCOUNT_SHOP
        return dsl.deleteFrom(s)
            .where(s.EXTERNAL_SHOP_ID.`in`(shopIds))
            .execute()
    }

}
