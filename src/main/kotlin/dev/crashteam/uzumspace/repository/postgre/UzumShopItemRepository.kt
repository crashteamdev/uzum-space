package dev.crashteam.uzumspace.repository.postgre

import dev.crashteam.uzumspace.db.model.tables.KeAccount.KE_ACCOUNT
import dev.crashteam.uzumspace.db.model.tables.KeAccountShopItem.KE_ACCOUNT_SHOP_ITEM
import dev.crashteam.uzumspace.db.model.tables.KeAccountShopItemCompetitor.KE_ACCOUNT_SHOP_ITEM_COMPETITOR
import dev.crashteam.uzumspace.db.model.tables.KeShopItem.KE_SHOP_ITEM
import dev.crashteam.uzumspace.repository.postgre.entity.UzumShopItemEntity
import dev.crashteam.uzumspace.repository.postgre.mapper.RecordToUzumShopItemMapper
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class UzumShopItemRepository(
    private val dsl: DSLContext,
    private val recordToUzumShopItemMapper: RecordToUzumShopItemMapper
) {

    fun save(keShopItem: UzumShopItemEntity): Int {
        val s = KE_SHOP_ITEM
        return dsl.insertInto(
            s,
            s.PRODUCT_ID,
            s.SKU_ID,
            s.CATEGORY_ID,
            s.NAME,
            s.PHOTO_KEY,
            s.AVG_HASH_FINGERPRINT,
            s.P_HASH_FINGERPRINT,
            s.PRICE,
            s.LAST_UPDATE,
            s.AVAILABLE_AMOUNT
        )
            .values(
                keShopItem.productId,
                keShopItem.skuId,
                keShopItem.categoryId,
                keShopItem.name,
                keShopItem.photoKey,
                keShopItem.avgHashFingerprint,
                keShopItem.pHashFingerprint,
                keShopItem.price,
                keShopItem.lastUpdate,
                keShopItem.availableAmount
            )
            .onDuplicateKeyUpdate()
            .set(
                mapOf(
                    s.CATEGORY_ID to keShopItem.categoryId,
                    s.NAME to keShopItem.name,
                    s.PHOTO_KEY to keShopItem.photoKey,
                    s.AVG_HASH_FINGERPRINT to keShopItem.avgHashFingerprint,
                    s.P_HASH_FINGERPRINT to keShopItem.pHashFingerprint,
                    s.PRICE to keShopItem.price,
                    s.LAST_UPDATE to keShopItem.lastUpdate,
                    s.AVAILABLE_AMOUNT to keShopItem.availableAmount
                )
            ).execute()
    }

    fun saveBatch(keShopItems: List<UzumShopItemEntity>): IntArray {
        val s = KE_SHOP_ITEM
        return dsl.batch(
            keShopItems.map { keShopItem ->
                dsl.insertInto(
                    s,
                    s.PRODUCT_ID,
                    s.SKU_ID,
                    s.CATEGORY_ID,
                    s.NAME,
                    s.PHOTO_KEY,
                    s.AVG_HASH_FINGERPRINT,
                    s.P_HASH_FINGERPRINT,
                    s.PRICE,
                    s.LAST_UPDATE,
                    s.AVAILABLE_AMOUNT
                )
                    .values(
                        keShopItem.productId,
                        keShopItem.skuId,
                        keShopItem.categoryId,
                        keShopItem.name,
                        keShopItem.photoKey,
                        keShopItem.avgHashFingerprint,
                        keShopItem.pHashFingerprint,
                        keShopItem.price,
                        keShopItem.lastUpdate,
                        keShopItem.availableAmount
                    )
                    .onDuplicateKeyUpdate()
                    .set(
                        mapOf(
                            s.CATEGORY_ID to keShopItem.categoryId,
                            s.NAME to keShopItem.name,
                            s.PHOTO_KEY to keShopItem.photoKey,
                            s.AVG_HASH_FINGERPRINT to keShopItem.avgHashFingerprint,
                            s.P_HASH_FINGERPRINT to keShopItem.pHashFingerprint,
                            s.PRICE to keShopItem.price,
                            s.LAST_UPDATE to keShopItem.lastUpdate,
                            s.AVAILABLE_AMOUNT to keShopItem.availableAmount
                        )
                    )
            }
        ).execute()
    }

    fun findByProductIdAndSkuId(productId: Long, skuId: Long): UzumShopItemEntity? {
        val s = KE_SHOP_ITEM
        return dsl.selectFrom(s)
            .where(s.PRODUCT_ID.eq(productId).and(s.SKU_ID.eq(skuId)))
            .fetchOne()?.map { recordToUzumShopItemMapper.convert(it) }
    }

    fun findByProductId(productId: Long): List<UzumShopItemEntity> {
        val s = KE_SHOP_ITEM
        return dsl.selectFrom(s)
            .where(s.PRODUCT_ID.eq(productId))
            .fetch().map { recordToUzumShopItemMapper.convert(it) }
    }

    fun findSimilarItemsByNameAndHashAndCategoryId(
        shopItemId: UUID,
        productId: Long,
        skuId: Long,
        name: String,
        categoryId: Long,
    ): List<UzumShopItemEntity> {
        val a = KE_ACCOUNT
        val i = KE_ACCOUNT_SHOP_ITEM
        val c = KE_ACCOUNT_SHOP_ITEM_COMPETITOR
        val s = KE_SHOP_ITEM
        val p = s.`as`("p")
        val nested = dsl.select(
            s.PRODUCT_ID,
            s.SKU_ID,
            s.CATEGORY_ID,
            s.NAME,
            s.PHOTO_KEY,
            s.AVG_HASH_FINGERPRINT,
            s.P_HASH_FINGERPRINT,
            s.PRICE,
            s.AVAILABLE_AMOUNT,
            s.LAST_UPDATE,
            DSL.field(
                "similarity({0}, {1})",
                Double::class.java, s.NAME, name
            ).`as`("name_similarity")
        ).from(s).where(s.CATEGORY_ID.eq(categoryId))
            .and(s.PRODUCT_ID.notEqual(productId).and(s.SKU_ID.notEqual(skuId)))
            .andNotExists(
                dsl.selectOne().from(c).where(
                    c.KE_ACCOUNT_SHOP_ITEM_ID.eq(shopItemId).and(c.PRODUCT_ID.eq(s.PRODUCT_ID))
                        .and(c.SKU_ID.eq(s.SKU_ID))
                )
            )
            .andNotExists(
                dsl.selectOne().from(i.join(a).on(i.KE_ACCOUNT_ID.eq(a.ID)))
                    .where(i.PRODUCT_ID.eq(s.PRODUCT_ID)).and(i.SKU_ID.eq(s.SKU_ID))
            )
            .asTable("nested")
        val subQuery = dsl.select(*nested.fields())
            .from(nested)
            .where(
                nested.field("name_similarity", Double::class.java)!!.greaterThan(0.5).or(
                    nested.field(s.P_HASH_FINGERPRINT)!!.`in`(
                        dsl.select(p.P_HASH_FINGERPRINT).from(p).where(
                            p.CATEGORY_ID.eq(categoryId).and(
                                DSL.field(
                                    "1 - bit_count(('x' || {0})::bit(16) # ('x' || {1})::bit(16))::decimal / 64",
                                    Double::class.java, nested.field(s.P_HASH_FINGERPRINT), p.P_HASH_FINGERPRINT
                                ).greaterThan(0.9)
                            )
                        )
                    )
                )
            ).limit(30)
        val records = dsl.select(*subQuery.fields())
            .distinctOn(subQuery.field(s.PRODUCT_ID))
            .from(subQuery)
            .orderBy(subQuery.field("product_id"), subQuery.field("name_similarity"))
            .fetch()

        return records.map { recordToUzumShopItemMapper.convert(it) }
    }

    fun findSimilarItemsByNameAndCategoryId(
        shopItemId: UUID,
        productId: Long,
        skuId: Long,
        name: String,
        categoryId: Long,
    ): List<UzumShopItemEntity> {
        val a = KE_ACCOUNT
        val i = KE_ACCOUNT_SHOP_ITEM
        val c = KE_ACCOUNT_SHOP_ITEM_COMPETITOR
        val s = KE_SHOP_ITEM
        val nested = dsl.select(
            s.PRODUCT_ID,
            s.SKU_ID,
            s.CATEGORY_ID,
            s.NAME,
            s.PHOTO_KEY,
            s.AVG_HASH_FINGERPRINT,
            s.P_HASH_FINGERPRINT,
            s.PRICE,
            s.AVAILABLE_AMOUNT,
            s.LAST_UPDATE,
            DSL.field(
                "similarity({0}, {1})",
                Double::class.java, s.NAME, name
            ).`as`("name_similarity")
        ).from(s).where(s.CATEGORY_ID.eq(categoryId))
            .and(s.PRODUCT_ID.notEqual(productId).and(s.SKU_ID.notEqual(skuId)))
            .andNotExists(
                dsl.selectOne().from(c).where(
                    c.KE_ACCOUNT_SHOP_ITEM_ID.eq(shopItemId).and(c.PRODUCT_ID.eq(s.PRODUCT_ID))
                        .and(c.SKU_ID.eq(s.SKU_ID))
                )
            )
            .andNotExists(
                dsl.selectOne().from(i.join(a).on(i.KE_ACCOUNT_ID.eq(a.ID)))
                    .where(i.PRODUCT_ID.eq(s.PRODUCT_ID)).and(i.SKU_ID.eq(s.SKU_ID))
            )
            .asTable("nested")
        val subQuery = dsl.select(*nested.fields())
            .from(nested)
            .where(
                nested.field("name_similarity", Double::class.java)!!.greaterThan(0.5)
            ).limit(30)
        val records = dsl.select(*subQuery.fields())
            .distinctOn(subQuery.field(s.PRODUCT_ID))
            .from(subQuery)
            .orderBy(subQuery.field("product_id"), subQuery.field("name_similarity"))
            .fetch()

        return records.map { recordToUzumShopItemMapper.convert(it) }
    }

    fun findSimilarItemsByProductIdAndSkuId(
        productId: Long,
        skuId: Long,
        name: String,
        categoryId: Long,
    ): List<UzumShopItemEntity> {
        val s = KE_SHOP_ITEM
        val p = s.`as`("p")
        val nested = dsl.select(
            s.PRODUCT_ID,
            s.SKU_ID,
            s.CATEGORY_ID,
            s.NAME,
            s.PHOTO_KEY,
            s.AVG_HASH_FINGERPRINT,
            s.P_HASH_FINGERPRINT,
            s.PRICE,
            s.AVAILABLE_AMOUNT,
            s.LAST_UPDATE,
            DSL.field(
                "similarity({0}, {1})",
                Double::class.java, s.NAME, name
            ).`as`("name_similarity")
        ).from(s).where(s.CATEGORY_ID.eq(categoryId)).and(s.PRODUCT_ID.notEqual(productId).and(s.SKU_ID.notEqual(skuId)))
            .asTable("nested")
        val subQuery = dsl.select(*nested.fields())
            .from(nested)
            .where(
                nested.field("name_similarity", Double::class.java)!!.greaterThan(0.5).or(
                    nested.field(s.P_HASH_FINGERPRINT)!!.`in`(
                        dsl.select(p.P_HASH_FINGERPRINT).from(p).where(
                            p.CATEGORY_ID.eq(categoryId).and(
                                DSL.field(
                                    "1 - bit_count(('x' || {0})::bit(16) # ('x' || {1})::bit(16))::decimal / 64",
                                    Double::class.java, nested.field(s.P_HASH_FINGERPRINT), p.P_HASH_FINGERPRINT
                                ).greaterThan(0.9)
                            )
                        )
                    )
                )
            ).limit(50)
        val records = dsl.select(*subQuery.fields())
            .distinctOn(subQuery.field(s.PRODUCT_ID))
            .from(subQuery)
            .orderBy(subQuery.field("product_id"), subQuery.field("name_similarity"))
            .fetch()

        return records.map { recordToUzumShopItemMapper.convert(it) }
    }

}
