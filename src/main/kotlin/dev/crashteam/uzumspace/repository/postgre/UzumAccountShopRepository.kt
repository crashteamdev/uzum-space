package dev.crashteam.uzumspace.repository.postgre

import dev.crashteam.uzumspace.db.model.tables.Account.ACCOUNT
import dev.crashteam.uzumspace.db.model.tables.UzumAccount.UZUM_ACCOUNT
import dev.crashteam.uzumspace.db.model.tables.UzumAccountShop.UZUM_ACCOUNT_SHOP
import dev.crashteam.uzumspace.db.model.tables.records.UzumAccountShopRecord
import dev.crashteam.uzumspace.repository.postgre.entity.UzumAccountShopEntity
import dev.crashteam.uzumspace.repository.postgre.mapper.RecordToUzumAccountShopEntityMapper
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class UzumAccountShopRepository(
    private val dsl: DSLContext,
    private val recordToUzumAccountShopEntityMapper: RecordToUzumAccountShopEntityMapper
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
