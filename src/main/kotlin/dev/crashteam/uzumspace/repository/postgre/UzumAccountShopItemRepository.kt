package dev.crashteam.uzumspace.repository.postgre

import dev.crashteam.openapi.space.model.AddStrategyRequest
import dev.crashteam.uzumspace.db.model.tables.UzumAccountShopItem.UZUM_ACCOUNT_SHOP_ITEM
import dev.crashteam.uzumspace.db.model.tables.UzumAccountShopItemPool.*
import dev.crashteam.uzumspace.extensions.paginate
import dev.crashteam.uzumspace.repository.postgre.entity.UzumAccountShopItemEntity
import dev.crashteam.uzumspace.repository.postgre.entity.PaginateEntity
import dev.crashteam.uzumspace.repository.postgre.mapper.RecordToUzumAccountShopItemEntityMapper
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@Repository
class UzumAccountShopItemRepository(
    private val dsl: DSLContext,
    private val recordToUzumAccountShopItemEntityMapper: RecordToUzumAccountShopItemEntityMapper,
    private val strategyRepository: UzumAccountShopItemStrategyRepository
) {

    @Transactional
    fun saveStrategy(strategyRequest: AddStrategyRequest): Long {
        val i = UZUM_ACCOUNT_SHOP_ITEM
        val strategyId = strategyRepository.save(strategyRequest)
        dsl.update(i)
            .set(i.UZUM_ACCOUNT_SHOP_ITEM_STRATEGY_ID, strategyId)
            .where(i.ID.eq(strategyRequest.uzumAccountShopItemId))
            .execute()
        return strategyId
    }

    fun save(uzumAccountShopItemEntity: UzumAccountShopItemEntity): UUID? {
        val i = UZUM_ACCOUNT_SHOP_ITEM
        return dsl.insertInto(
            i,
            i.ID,
            i.UZUM_ACCOUNT_ID,
            i.UZUM_ACCOUNT_SHOP_ID,
            i.CATEGORY_ID,
            i.PRODUCT_ID,
            i.SKU_ID,
            i.NAME,
            i.PHOTO_KEY,
            i.PRICE,
            i.PURCHASE_PRICE,
            i.BARCODE,
            i.PRODUCT_SKU,
            i.SKU_TITLE,
            i.AVAILABLE_AMOUNT,
            i.MINIMUM_THRESHOLD,
            i.MAXIMUM_THRESHOLD,
            i.STEP,
            i.DISCOUNT,
            i.LAST_UPDATE
        ).values(
            uzumAccountShopItemEntity.id,
            uzumAccountShopItemEntity.uzumAccountId,
            uzumAccountShopItemEntity.uzumAccountShopId,
            uzumAccountShopItemEntity.categoryId,
            uzumAccountShopItemEntity.productId,
            uzumAccountShopItemEntity.skuId,
            uzumAccountShopItemEntity.name,
            uzumAccountShopItemEntity.photoKey,
            uzumAccountShopItemEntity.price,
            uzumAccountShopItemEntity.purchasePrice,
            uzumAccountShopItemEntity.barCode,
            uzumAccountShopItemEntity.productSku,
            uzumAccountShopItemEntity.skuTitle,
            uzumAccountShopItemEntity.availableAmount,
            uzumAccountShopItemEntity.minimumThreshold,
            uzumAccountShopItemEntity.maximumThreshold,
            uzumAccountShopItemEntity.step,
            uzumAccountShopItemEntity.discount,
            uzumAccountShopItemEntity.lastUpdate
        ).onDuplicateKeyUpdate()
            .set(
                mapOf(
                    i.CATEGORY_ID to uzumAccountShopItemEntity.categoryId,
                    i.PRODUCT_ID to uzumAccountShopItemEntity.productId,
                    i.SKU_ID to uzumAccountShopItemEntity.skuId,
                    i.NAME to uzumAccountShopItemEntity.name,
                    i.PHOTO_KEY to uzumAccountShopItemEntity.photoKey,
                    i.PRICE to uzumAccountShopItemEntity.price,
                    i.PURCHASE_PRICE to uzumAccountShopItemEntity.purchasePrice,
                    i.BARCODE to uzumAccountShopItemEntity.barCode,
                    i.PRODUCT_SKU to uzumAccountShopItemEntity.productSku,
                    i.SKU_TITLE to uzumAccountShopItemEntity.skuTitle,
                    i.AVAILABLE_AMOUNT to uzumAccountShopItemEntity.availableAmount,
                    i.LAST_UPDATE to uzumAccountShopItemEntity.lastUpdate
                )
            ).returningResult(i.ID)
            .fetchOne()!!.getValue(i.ID)
    }

    fun saveBatch(uzumAccountShopItemEntities: List<UzumAccountShopItemEntity>): IntArray {
        val i = UZUM_ACCOUNT_SHOP_ITEM
        return dsl.batch(
            uzumAccountShopItemEntities.map { uzumAccountShopItemEntity ->
                dsl.insertInto(
                    i,
                    i.ID,
                    i.UZUM_ACCOUNT_ID,
                    i.UZUM_ACCOUNT_SHOP_ID,
                    i.CATEGORY_ID,
                    i.PRODUCT_ID,
                    i.SKU_ID,
                    i.NAME,
                    i.PHOTO_KEY,
                    i.PRICE,
                    i.PURCHASE_PRICE,
                    i.BARCODE,
                    i.PRODUCT_SKU,
                    i.SKU_TITLE,
                    i.AVAILABLE_AMOUNT,
                    i.MINIMUM_THRESHOLD,
                    i.MAXIMUM_THRESHOLD,
                    i.STEP,
                    i.DISCOUNT,
                    i.LAST_UPDATE
                ).values(
                    uzumAccountShopItemEntity.id,
                    uzumAccountShopItemEntity.uzumAccountId,
                    uzumAccountShopItemEntity.uzumAccountShopId,
                    uzumAccountShopItemEntity.categoryId,
                    uzumAccountShopItemEntity.productId,
                    uzumAccountShopItemEntity.skuId,
                    uzumAccountShopItemEntity.name,
                    uzumAccountShopItemEntity.photoKey,
                    uzumAccountShopItemEntity.price,
                    uzumAccountShopItemEntity.purchasePrice,
                    uzumAccountShopItemEntity.barCode,
                    uzumAccountShopItemEntity.productSku,
                    uzumAccountShopItemEntity.skuTitle,
                    uzumAccountShopItemEntity.availableAmount,
                    uzumAccountShopItemEntity.minimumThreshold,
                    uzumAccountShopItemEntity.maximumThreshold,
                    uzumAccountShopItemEntity.step,
                    uzumAccountShopItemEntity.discount,
                    uzumAccountShopItemEntity.lastUpdate
                ).onDuplicateKeyUpdate()
                    .set(
                        mapOf(
                            i.CATEGORY_ID to uzumAccountShopItemEntity.categoryId,
                            i.NAME to uzumAccountShopItemEntity.name,
                            i.PHOTO_KEY to uzumAccountShopItemEntity.photoKey,
                            i.PRICE to uzumAccountShopItemEntity.price,
                            i.PURCHASE_PRICE to uzumAccountShopItemEntity.purchasePrice,
                            i.BARCODE to uzumAccountShopItemEntity.barCode,
                            i.AVAILABLE_AMOUNT to uzumAccountShopItemEntity.availableAmount,
                            i.LAST_UPDATE to uzumAccountShopItemEntity.lastUpdate,
                            i.PRODUCT_SKU to uzumAccountShopItemEntity.productSku,
                            i.SKU_TITLE to uzumAccountShopItemEntity.skuTitle
                        )
                    )
            }
        ).execute()
    }

    fun findShopItem(
        uzumAccountId: UUID,
        uzumAccountShopId: UUID,
        productId: Long,
        skuId: Long
    ): UzumAccountShopItemEntity? {
        val i = UZUM_ACCOUNT_SHOP_ITEM
        val p = UZUM_ACCOUNT_SHOP_ITEM_POOL
        val record = dsl.select()
            .from(i.leftJoin(p).on(p.UZUM_ACCOUNT_SHOP_ITEM_ID.eq(i.ID)))
            .where(
                i.UZUM_ACCOUNT_ID.eq(uzumAccountId),
                i.UZUM_ACCOUNT_SHOP_ID.eq(uzumAccountShopId),
                i.PRODUCT_ID.eq(productId),
                i.SKU_ID.eq(skuId)
            ).fetchOne() ?: return null
        return recordToUzumAccountShopItemEntityMapper.convert(record)
    }

    fun findShopItem(
        uzumAccountId: UUID,
        uzumAccountShopId: UUID,
        uzumAccountShopItemId: UUID
    ): UzumAccountShopItemEntity? {
        val i = UZUM_ACCOUNT_SHOP_ITEM
        val p = UZUM_ACCOUNT_SHOP_ITEM_POOL
        val record = dsl.select()
            .from(i.leftJoin(p).on(p.UZUM_ACCOUNT_SHOP_ITEM_ID.eq(i.ID)))
            .where(
                i.UZUM_ACCOUNT_ID.eq(uzumAccountId),
                i.UZUM_ACCOUNT_SHOP_ID.eq(uzumAccountShopId),
                i.ID.eq(uzumAccountShopItemId),
            ).fetchOne() ?: return null
        return recordToUzumAccountShopItemEntityMapper.convert(record)
    }

    fun findShopItem(
        uzumAccountId: UUID,
        uzumAccountShopItemId: UUID
    ): UzumAccountShopItemEntity? {
        val i = UZUM_ACCOUNT_SHOP_ITEM
        val p = UZUM_ACCOUNT_SHOP_ITEM_POOL
        val record = dsl.select()
            .from(i.leftJoin(p).on(p.UZUM_ACCOUNT_SHOP_ITEM_ID.eq(i.ID)))
            .where(
                i.UZUM_ACCOUNT_ID.eq(uzumAccountId),
                i.ID.eq(uzumAccountShopItemId),
            ).fetchOne() ?: return null
        return recordToUzumAccountShopItemEntityMapper.convert(record)
    }

    fun findAllItems(
        uzumAccountId: UUID,
        uzumAccountShopId: UUID
    ): MutableList<UzumAccountShopItemEntity> {
        val i = UZUM_ACCOUNT_SHOP_ITEM
        val records = dsl.selectFrom(i)
            .where(
                i.UZUM_ACCOUNT_ID.eq(uzumAccountId),
                i.UZUM_ACCOUNT_SHOP_ID.eq(uzumAccountShopId)
            ).fetch()
        return records.map { recordToUzumAccountShopItemEntityMapper.convert(it) }
    }

    fun findShopItems(
        uzumAccountId: UUID,
        uzumAccountShopId: UUID,
        filter: Condition? = null,
        sortFields: List<Pair<Field<*>, SortType>>? = null,
        limit: Long,
        offset: Long,
    ): MutableList<PaginateEntity<UzumAccountShopItemEntity>> {
        val i = UZUM_ACCOUNT_SHOP_ITEM
        val p = UZUM_ACCOUNT_SHOP_ITEM_POOL
        var select = dsl.selectFrom(i.leftJoin(p).on(p.UZUM_ACCOUNT_SHOP_ITEM_ID.eq(i.ID)))
            .where(
                i.UZUM_ACCOUNT_ID.eq(uzumAccountId),
                i.UZUM_ACCOUNT_SHOP_ID.eq(uzumAccountShopId)
            )
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

    fun findShopItems(
        uzumAccountId: UUID,
        uzumAccountShopId: UUID
    ): List<UzumAccountShopItemEntity> {
        val i = UZUM_ACCOUNT_SHOP_ITEM
        val p = UZUM_ACCOUNT_SHOP_ITEM_POOL
        val records = dsl.selectFrom(i.leftJoin(p).on(p.UZUM_ACCOUNT_SHOP_ITEM_ID.eq(i.ID)))
            .where(
                i.UZUM_ACCOUNT_ID.eq(uzumAccountId),
                i.UZUM_ACCOUNT_SHOP_ID.eq(uzumAccountShopId)
            )
            .fetch()

        return records.map { recordToUzumAccountShopItemEntityMapper.convert(it) }
    }

    fun deleteWhereOldLastUpdate(uzumAccountId: UUID, uzumAccountShopId: UUID, lastUpdateTime: LocalDateTime): Int {
        val i = UZUM_ACCOUNT_SHOP_ITEM
        return dsl.deleteFrom(i)
            .where(
                i.UZUM_ACCOUNT_ID.eq(uzumAccountId)
                    .and(i.UZUM_ACCOUNT_SHOP_ID.eq(uzumAccountShopId))
                    .and(i.LAST_UPDATE.lessThan(lastUpdateTime))
            ).execute()
    }

    fun updatePriceChangeOptions(
        uzumAccountId: UUID,
        uzumAccountShopItemId: UUID,
        step: Int,
        minimumThreshold: Long,
        maximumThreshold: Long,
        discount: BigDecimal,
    ): Int {
        val i = UZUM_ACCOUNT_SHOP_ITEM
        return dsl.update(i)
            .set(
                mapOf(
                    i.STEP to step,
                    i.MINIMUM_THRESHOLD to minimumThreshold,
                    i.MAXIMUM_THRESHOLD to maximumThreshold,
                    i.DISCOUNT to discount,
                )
            )
            .where(i.UZUM_ACCOUNT_ID.eq(uzumAccountId).and(i.ID.eq(uzumAccountShopItemId)))
            .execute()
    }

}
