package dev.crashteam.uzumspace.controller

import dev.crashteam.openapi.space.api.AccountsApi
import dev.crashteam.openapi.space.model.*
import dev.crashteam.uzumspace.db.model.tables.UzumAccountShopItemPriceHistory.UZUM_ACCOUNT_SHOP_ITEM_PRICE_HISTORY
import dev.crashteam.uzumspace.db.model.enums.MonitorState
import dev.crashteam.uzumspace.db.model.tables.UzumAccountShop.*
import dev.crashteam.uzumspace.db.model.tables.UzumAccountShopItem.UZUM_ACCOUNT_SHOP_ITEM
import dev.crashteam.uzumspace.db.model.tables.UzumAccountShopItemCompetitor.UZUM_ACCOUNT_SHOP_ITEM_COMPETITOR
import dev.crashteam.uzumspace.repository.postgre.UzumShopItemPriceHistoryRepository
import dev.crashteam.uzumspace.service.*
import dev.crashteam.uzumspace.service.error.AccountItemCompetitorLimitExceededException
import dev.crashteam.uzumspace.service.error.AccountItemPoolLimitExceededException
import dev.crashteam.uzumspace.service.error.UserNotFoundException
import dev.crashteam.uzumspace.service.resolver.UrlToProductResolver
import mu.KotlinLogging
import org.springframework.core.convert.ConversionService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux
import reactor.core.publisher.toMono
import java.security.Principal
import java.util.*

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("reprider/v1")
class AccountsController(
    private val uzumAccountService: UzumAccountService,
    private val uzumAccountShopService: UzumAccountShopService,
    private val updateUzumAccountService: UpdateUzumAccountService,
    private val uzumShopItemService: UzumShopItemService,
    private val uzumShopItemPriceHistoryRepository: UzumShopItemPriceHistoryRepository,
    private val conversionService: ConversionService,
    private val urlToProductResolver: UrlToProductResolver
) : AccountsApi {

    override fun addUzumAccount(
        xRequestID: UUID,
        addUzumAccountRequest: Mono<AddUzumAccountRequest>,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<UzumAccount>> {
        return exchange.getPrincipal<Principal>().flatMap { principal ->
            addUzumAccountRequest.flatMap { request ->
                try {
                    if (request.login.isNullOrEmpty() || request.password.isNullOrEmpty()) {
                        return@flatMap ResponseEntity.badRequest().build<UzumAccount>().toMono()
                    }
                    val uzumAccountEntity = uzumAccountService.addUzumAccount(principal.name, request.login, request.password)
                    val uzumAccount = conversionService.convert(uzumAccountEntity, UzumAccount::class.java)
                    ResponseEntity.ok(uzumAccount).toMono()
                } catch (e: AccountItemPoolLimitExceededException) {
                    return@flatMap ResponseEntity.status(HttpStatus.FORBIDDEN).build<UzumAccount>().toMono()
                } catch (e: IllegalArgumentException) {
                    return@flatMap ResponseEntity.status(HttpStatus.FORBIDDEN).build<UzumAccount>().toMono()
                }
            }
        }.doOnError {
            log.warn(it) { "Failed to add ke account" }
        }
    }

    override fun addUzumAccountShopItemCompetitor(
        xRequestID: UUID,
        id: UUID,
        addUzumAccountShopItemCompetitorRequest: Mono<AddUzumAccountShopItemCompetitorRequest>,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Void>> {
        return exchange.getPrincipal<Principal>().flatMap { principal ->
            addUzumAccountShopItemCompetitorRequest.flatMap { request ->
                var productId = request.competitorProductId?.toLong()
                var skuId = request.competitorSkuId?.toLong()
                if (request.url != null) {
                    val resolvedUzumProduct = urlToProductResolver.resolve(request.url)
                    if (resolvedUzumProduct != null) {
                        productId = resolvedUzumProduct.productId.toLong()
                        skuId = resolvedUzumProduct.skuId.toLong()
                    }
                }
                if (productId == null || skuId == null) {
                    return@flatMap ResponseEntity.badRequest().build<Void>().toMono()
                }
                try {
                    uzumAccountShopService.addShopItemCompetitor(
                        principal.name,
                        id,
                        request.shopItemRef.shopId,
                        request.shopItemRef.shopItemId,
                        productId,
                        skuId
                    )
                } catch (e: AccountItemCompetitorLimitExceededException) {
                    return@flatMap ResponseEntity.status(HttpStatus.FORBIDDEN).build<Void>().toMono()
                }
                ResponseEntity.status(HttpStatus.OK).build<Void>().toMono()
            }
        }.doOnError {
            log.warn(it) { "Failed to add ke account shop item competitor. uzumAccountId=$id" }
        }
    }

    override fun addUzumAccountShopItemPool(
        xRequestID: UUID,
        id: UUID,
        addUzumAccountShopItemPoolRequest: Mono<AddUzumAccountShopItemPoolRequest>,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Void>> {
        return exchange.getPrincipal<Principal>().flatMap { principal ->
            addUzumAccountShopItemPoolRequest.flatMap { request ->
                try {
                    uzumAccountShopService.addShopItemIntoPool(principal.name, id, request.shopId, request.shopItemId)
                } catch (e: AccountItemPoolLimitExceededException) {
                    return@flatMap ResponseEntity.status(HttpStatus.FORBIDDEN).build<Void>().toMono()
                } catch (e: IllegalArgumentException) {
                    return@flatMap ResponseEntity.status(HttpStatus.FORBIDDEN).build<Void>().toMono()
                }
                ResponseEntity.status(HttpStatus.OK).build<Void>().toMono()
            }
        }.doOnError {
            log.warn(it) { "Failed to add shop item into pool. uzumAccountId=$id" }
        }
    }

    override fun deleteUzumAccount(
        xRequestID: UUID,
        id: UUID,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Void>> {
        return exchange.getPrincipal<Principal>().flatMap {
            val removeUzumAccountCount = uzumAccountService.removeUzumAccount(it.name, id)
            if (removeUzumAccountCount > 0) {
                return@flatMap ResponseEntity.ok().build<Void>().toMono()
            }
            ResponseEntity.notFound().build<Void>().toMono()
        }.doOnError {
            log.warn(it) { "Failed to delete ke account. uzumAccountId=$id" }
        }
    }

    override fun getUzumAccount(
        xRequestID: UUID,
        id: UUID,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<UzumAccount>> {
        return exchange.getPrincipal<Principal>().flatMap {
            val uzumAccountEntity = uzumAccountService.getUzumAccount(it.name, id)
            val uzumAccount = conversionService.convert(uzumAccountEntity, UzumAccount::class.java)

            ResponseEntity.ok(uzumAccount).toMono()
        }.doOnError {
            log.warn(it) { "Failed to get ke account. uzumAccountId=$id" }
        }
    }

    override fun getUzumAccountCompetitorShopItems(
        xRequestID: UUID,
        id: UUID,
        shopId: UUID,
        shopItemId: UUID,
        limit: Int?,
        offset: Int?,
        filter: String?,
        sort: MutableList<String>?,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Flux<UzumAccountCompetitorShopItem>>> {
        return exchange.getPrincipal<Principal>().flatMap {
            val limit = limit ?: 10
            val offset = offset ?: 0
            val mapFields = mapOf(
                "name" to StringTableFieldMapper(UZUM_ACCOUNT_SHOP_ITEM.NAME),
                "productId" to LongTableFieldMapper(UZUM_ACCOUNT_SHOP_ITEM_COMPETITOR.PRODUCT_ID),
                "skuId" to LongTableFieldMapper(UZUM_ACCOUNT_SHOP_ITEM_COMPETITOR.SKU_ID),
                "price" to LongTableFieldMapper(UZUM_ACCOUNT_SHOP_ITEM.PRICE),
                "availableAmount" to LongTableFieldMapper(UZUM_ACCOUNT_SHOP_ITEM.AVAILABLE_AMOUNT),
            )
            val filterCondition = filter?.let {
                FilterOperation.parse(filter, mapFields)
            }
            val sortFields = if (sort != null) {
                SortOperation.parse(sort, mapFields)
            } else null

            val shopItemCompetitors = uzumAccountShopService.getShopItemCompetitors(
                it.name, id, shopId, shopItemId, filterCondition, sortFields, limit.toLong(), offset.toLong()
            )
            if (shopItemCompetitors.isEmpty()) {
                return@flatMap ResponseEntity.ok(emptyList<UzumAccountCompetitorShopItem>().toFlux()).toMono()
            }

            val paginateEntity = shopItemCompetitors.first()
            val httpHeaders = HttpHeaders().apply {
                add("Pagination-Total", paginateEntity.total.toString())
                add("Pagination-Limit", paginateEntity.limit.toString())
                add("Pagination-Offset", paginateEntity.offset.toString())
            }
            ResponseEntity(shopItemCompetitors.map {
                conversionService.convert(
                    it.item,
                    UzumAccountCompetitorShopItem::class.java
                )!!
            }.toFlux(), httpHeaders, HttpStatus.OK).toMono()
        }.doOnError {
            log.warn(it) {
                "Failed to get ke account shop item competitors." +
                        " uzumAccountId=$id;shopId=$shopId;shopItemId=$shopItemId;limit=$limit;offset=$offset;filter=$filter;sort=$sort"
            }
        }
    }

    override fun getAccountShopItemPoolCount(
        xRequestID: UUID,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<GetAccountShopItemPoolCount200Response>> {
        return exchange.getPrincipal<Principal>().flatMap {
            val shopItemPoolCount = uzumAccountShopService.getShopItemPoolCount(it.name)
            val response = GetAccountShopItemPoolCount200Response().apply {
                this.count = shopItemPoolCount
            }
            ResponseEntity.ok(response).toMono()
        }.doOnError {
            log.warn(it) { "Failed to get shop item pool count" }
        }
    }

    override fun getUzumAccountShopItems(
        xRequestID: UUID,
        id: UUID,
        shopId: UUID,
        limit: Int?,
        offset: Int?,
        filter: String?,
        sort: MutableList<String>?,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Flux<UzumAccountShopItem>>> {
        return exchange.getPrincipal<Principal>().flatMap {
            val limit = limit ?: 10
            val offset = offset ?: 0
            val mapFields = mapOf(
                "productId" to LongTableFieldMapper(UZUM_ACCOUNT_SHOP_ITEM.PRODUCT_ID),
                "skuId" to LongTableFieldMapper(UZUM_ACCOUNT_SHOP_ITEM.SKU_ID),
                "skuTitle" to StringTableFieldMapper(UZUM_ACCOUNT_SHOP_ITEM.SKU_TITLE),
                "name" to StringTableFieldMapper(UZUM_ACCOUNT_SHOP_ITEM.NAME),
                "photoKey" to StringTableFieldMapper(UZUM_ACCOUNT_SHOP_ITEM.PHOTO_KEY),
                "purchasePrice" to LongTableFieldMapper(UZUM_ACCOUNT_SHOP_ITEM.PURCHASE_PRICE),
                "price" to LongTableFieldMapper(UZUM_ACCOUNT_SHOP_ITEM.PRICE),
                "barCode" to LongTableFieldMapper(UZUM_ACCOUNT_SHOP_ITEM.BARCODE),
                "availableAmount" to LongTableFieldMapper(UZUM_ACCOUNT_SHOP_ITEM.AVAILABLE_AMOUNT),
                "minimumThreshold" to LongTableFieldMapper(UZUM_ACCOUNT_SHOP_ITEM.MINIMUM_THRESHOLD),
                "maximumThreshold" to LongTableFieldMapper(UZUM_ACCOUNT_SHOP_ITEM.MAXIMUM_THRESHOLD),
                "step" to IntegerTableFieldMapper(UZUM_ACCOUNT_SHOP_ITEM.STEP),
                "discount" to BigIntegerTableFieldMapper(UZUM_ACCOUNT_SHOP_ITEM.DISCOUNT)
            )
            val filterCondition = filter?.let {
                FilterOperation.parse(filter, mapFields)
            }
            val sortFields = if (sort != null) {
                SortOperation.parse(sort, mapFields)
            } else null
            val shopItemCompetitors = uzumAccountShopService.getUzumAccountShopItems(
                it.name,
                id,
                shopId,
                filterCondition,
                sortFields,
                limit.toLong(),
                offset.toLong()
            )
            if (shopItemCompetitors.isEmpty()) {
                return@flatMap ResponseEntity.ok(emptyList<UzumAccountShopItem>().toFlux()).toMono()
            }
            val paginateEntity = shopItemCompetitors.first()
            val httpHeaders = HttpHeaders().apply {
                add("Pagination-Total", paginateEntity.total.toString())
                add("Pagination-Limit", paginateEntity.limit.toString())
                add("Pagination-Offset", paginateEntity.offset.toString())
            }
            val uzumAccountShopItems =
                shopItemCompetitors.map { it.item }
                    .map { conversionService.convert(it, UzumAccountShopItem::class.java)!! }
            ResponseEntity(uzumAccountShopItems.toFlux(), httpHeaders, HttpStatus.OK).toMono()
        }.doOnError {
            log.warn(it) { "Failed to get ke account shop items. uzumAccountId=$id;shopId=$shopId;limit=$limit;offset=$offset;filter=$filter;sort=$sort" }
        }
    }

    override fun getUzumAccountShopItemsPool(
        xRequestID: UUID,
        id: UUID,
        shopId: UUID,
        limit: Int?,
        offset: Int?,
        filter: String?,
        sort: MutableList<String>?,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Flux<UzumAccountShopItem>>> {
        return exchange.getPrincipal<Principal>().flatMap {
            val limit = limit ?: 10
            val offset = offset ?: 0
            val mapFields = mapOf(
                "productId" to LongTableFieldMapper(UZUM_ACCOUNT_SHOP_ITEM.PRODUCT_ID),
                "skuId" to LongTableFieldMapper(UZUM_ACCOUNT_SHOP_ITEM.SKU_ID),
                "name" to StringTableFieldMapper(UZUM_ACCOUNT_SHOP_ITEM.NAME),
                "photoKey" to StringTableFieldMapper(UZUM_ACCOUNT_SHOP_ITEM.PHOTO_KEY),
                "purchasePrice" to LongTableFieldMapper(UZUM_ACCOUNT_SHOP_ITEM.PURCHASE_PRICE),
                "price" to LongTableFieldMapper(UZUM_ACCOUNT_SHOP_ITEM.PRICE),
                "barCode" to LongTableFieldMapper(UZUM_ACCOUNT_SHOP_ITEM.BARCODE),
                "availableAmount" to LongTableFieldMapper(UZUM_ACCOUNT_SHOP_ITEM.AVAILABLE_AMOUNT),
                "minimumThreshold" to LongTableFieldMapper(UZUM_ACCOUNT_SHOP_ITEM.MINIMUM_THRESHOLD),
                "maximumThreshold" to LongTableFieldMapper(UZUM_ACCOUNT_SHOP_ITEM.MAXIMUM_THRESHOLD),
                "step" to IntegerTableFieldMapper(UZUM_ACCOUNT_SHOP_ITEM.STEP)
            )
            val filterCondition = filter?.let {
                FilterOperation.parse(filter, mapFields)
            }
            val sortFields = if (sort != null) {
                SortOperation.parse(sort, mapFields)
            } else null
            val shopItemPaginateEntities =
                uzumAccountShopService.getShopItemsInPool(
                    it.name,
                    id,
                    shopId,
                    filterCondition,
                    sortFields,
                    limit.toLong(),
                    offset.toLong()
                )
            if (shopItemPaginateEntities.isEmpty()) {
                return@flatMap ResponseEntity.ok(emptyList<UzumAccountShopItem>().toFlux()).toMono()
            }
            val paginateEntity = shopItemPaginateEntities.first()
            val httpHeaders = HttpHeaders().apply {
                add("Pagination-Total", paginateEntity.total.toString())
                add("Pagination-Limit", paginateEntity.limit.toString())
                add("Pagination-Offset", paginateEntity.offset.toString())
            }
            val uzumAccountShopItems =
                shopItemPaginateEntities.map { it.item }
                    .map { conversionService.convert(it, UzumAccountShopItem::class.java)!! }
            ResponseEntity(uzumAccountShopItems.toFlux(), httpHeaders, HttpStatus.OK).toMono()
        }.doOnError {
            log.warn(it) { "Failed to ke account shop item pool. uzumAccountId=$id;shopId=$shopId;limit=$limit;offset=$offset;filter=$filter;sort=$sort" }
        }
    }

    override fun getUzumAccountShops(
        xRequestID: UUID,
        id: UUID,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Flux<UzumAccountShop>>> {
        return exchange.getPrincipal<Principal>().flatMap {
            val kazanExpressAccountShopEntities = uzumAccountShopService.getUzumAccountShops(it.name, id)
            val uzumAccountShops =
                kazanExpressAccountShopEntities.map { conversionService.convert(it, UzumAccountShop::class.java)!! }
            ResponseEntity.ok(uzumAccountShops.toFlux()).toMono()
        }.doOnError {
            log.warn(it) { "Failed to get ke account shops" }
        }
    }

    override fun getUzumAccounts(
        xRequestID: UUID,
        exchange: ServerWebExchange,
    ): Mono<ResponseEntity<Flux<UzumAccount>>> {
        return exchange.getPrincipal<Principal>().flatMap {
            val uzumAccounts = uzumAccountService.getUzumAccounts(it.name)
            val uzumAccountList = uzumAccounts.map { conversionService.convert(it, UzumAccount::class.java)!! }
            ResponseEntity.ok(uzumAccountList.toFlux()).toMono()
        }.doOnError {
            log.warn(it) { "Failed to get ke accounts" }
        }
    }

    override fun patchUzumAccount(
        xRequestID: UUID,
        id: UUID,
        patchUzumAccount: Mono<PatchUzumAccount>,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Void>> {
        return exchange.getPrincipal<Principal>().flatMap { principal ->
            patchUzumAccount.flatMap { request ->
                val response = try {
                    uzumAccountService.editUzumAccount(principal.name, id, request.login, request.password)
                    uzumAccountService.initializeUzumAccountJob(principal.name, id)
                    ResponseEntity.ok().build()
                } catch (e: UserNotFoundException) {
                    ResponseEntity.notFound().build<Void>()
                } catch (e: IllegalArgumentException) {
                    ResponseEntity(HttpStatus.CONFLICT)
                }
                response.toMono()
            }
        }.doOnError {
            log.warn(it) { "Failed to change ke account credentials. uzumAccountId=$id" }
        }
    }

    override fun patchUzumAccountMonitoringState(
        xRequestID: UUID,
        id: UUID,
        patchUzumAccountMonitoringState: Mono<PatchUzumAccountMonitoringState>,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Void>> {
        return exchange.getPrincipal<Principal>().flatMap { principal ->
            patchUzumAccountMonitoringState.flatMap { request ->
                val monitorState = when (request.state) {
                    PatchUzumAccountMonitoringState.StateEnum.ACTIVATE -> MonitorState.active
                    PatchUzumAccountMonitoringState.StateEnum.SUSPEND -> MonitorState.suspended
                    else -> return@flatMap Mono.error(IllegalArgumentException("Unknown request state: ${request.state}"))
                }
                val changeUzumAccountMonitoringState =
                    uzumAccountService.changeUzumAccountMonitoringState(principal.name, id, monitorState)
                return@flatMap if (changeUzumAccountMonitoringState > 0) {
                    ResponseEntity.ok().build<Void>().toMono()
                } else {
                    ResponseEntity.notFound().build<Void>().toMono()
                }
            }
        }.doOnError {
            log.warn(it) { "Failed to change ke account monitor state. uzumAccountId=$id" }
        }
    }

    override fun removeUzumAccountShopItemCompetitor(
        xRequestID: UUID,
        id: UUID,
        removeUzumAccountShopItemCompetitorRequest: Mono<RemoveUzumAccountShopItemCompetitorRequest>,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Void>> {
        return exchange.getPrincipal<Principal>().flatMap { principal ->
            removeUzumAccountShopItemCompetitorRequest.flatMap { request ->
                val removeShopItemCompetitorCount =
                    uzumAccountShopService.removeShopItemCompetitor(
                        principal.name,
                        id,
                        request.shopItemId,
                        request.competitorId
                    )
                if (removeShopItemCompetitorCount > 0) {
                    ResponseEntity.ok().build<Void>().toMono()
                } else {
                    ResponseEntity.notFound().build<Void>().toMono()
                }
            }
        }.doOnError {
            log.warn(it) { "Failed to remove ke account shop item competitor. uzumAccountId=$id" }
        }
    }

    override fun removeUzumAccountShopItemFromPool(
        xRequestID: UUID,
        id: UUID,
        addUzumAccountShopItemPoolRequest: Mono<AddUzumAccountShopItemPoolRequest>,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Void>> {
        return exchange.getPrincipal<Principal>().flatMap { principal ->
            addUzumAccountShopItemPoolRequest.flatMap { request ->
                val removeShopItemFromPoolCount =
                    uzumAccountShopService.removeShopItemFromPool(principal.name, id, request.shopId, request.shopItemId)
                if (removeShopItemFromPoolCount > 0) {
                    ResponseEntity.ok().build<Void>().toMono()
                } else {
                    ResponseEntity.notFound().build<Void>().toMono()
                }
            }
        }.doOnError {
            log.warn(it) { "Failed to remove ke account shop item from pool. uzumAccountId=$id" }
        }
    }

    override fun updateUzumAccountData(
        xRequestID: UUID,
        id: UUID,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Void>> {
        return exchange.getPrincipal<Principal>().flatMap { principal ->
            val result = updateUzumAccountService.executeUpdateJob(principal.name, id)
            if (!result) {
                ResponseEntity<Void>(HttpStatus.CONFLICT).toMono()
            } else {
                ResponseEntity.ok().build<Void>().toMono()
            }
        }.doOnError {
            log.warn(it) { "Exception during update ke account data. uzumAccountId=$id" }
        }
    }

    override fun getUzumAccountShopItem(
        xRequestID: UUID,
        id: UUID,
        shopItemId: UUID,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<UzumAccountShopItem>> {
        return exchange.getPrincipal<Principal>().flatMap { principal ->
            val uzumAccountShopItem = uzumAccountShopService.getUzumAccountShopItem(principal.name, id, shopItemId)
                ?: return@flatMap ResponseEntity.notFound().build<UzumAccountShopItem>().toMono()
            ResponseEntity.ok(conversionService.convert(uzumAccountShopItem, UzumAccountShopItem::class.java)).toMono()
        }.doOnError {
            log.warn(it) { "Failed to get ke account shop item. uzumAccountId=$id;shopItemId=$shopItemId" }
        }
    }

    override fun getUzumAccountShopItemPriceChangeHistory(
        xRequestID: UUID,
        id: UUID,
        limit: Int?,
        offset: Int?,
        filter: String?,
        sort: MutableList<String>?,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Flux<UzumAccountPriceChangeHistory>>> {
        return exchange.getPrincipal<Principal>().flatMap { principal ->
            val limit = limit ?: 10
            val offset = offset ?: 0
            val mapFields = mapOf(
                "productId" to LongTableFieldMapper(UZUM_ACCOUNT_SHOP_ITEM.PRODUCT_ID),
                "skuId" to LongTableFieldMapper(UZUM_ACCOUNT_SHOP_ITEM.SKU_ID),
                "shopName" to StringTableFieldMapper(UZUM_ACCOUNT_SHOP.NAME),
                "itemName" to StringTableFieldMapper(UZUM_ACCOUNT_SHOP_ITEM.NAME),
                "oldPrice" to LongTableFieldMapper(UZUM_ACCOUNT_SHOP_ITEM_PRICE_HISTORY.OLD_PRICE),
                "newPrice" to LongTableFieldMapper(UZUM_ACCOUNT_SHOP_ITEM_PRICE_HISTORY.PRICE),
                "barcode" to LongTableFieldMapper(UZUM_ACCOUNT_SHOP_ITEM.BARCODE),
            )
            val filterCondition = filter?.let {
                FilterOperation.parse(filter, mapFields)
            }
            val sortFields = if (sort != null) {
                SortOperation.parse(sort, mapFields)
            } else null

            val shopItemPriceHistoryPaginateEntities = uzumShopItemPriceHistoryRepository.findHistoryByUzumAccountId(
                id,
                filterCondition,
                sortFields,
                limit.toLong(),
                offset.toLong()
            )
            if (shopItemPriceHistoryPaginateEntities.isEmpty()) {
                return@flatMap ResponseEntity(emptyList<UzumAccountPriceChangeHistory>().toFlux(), HttpStatus.OK).toMono()
            }
            val paginateEntity = shopItemPriceHistoryPaginateEntities.first()
            val httpHeaders = HttpHeaders().apply {
                add("Pagination-Total", paginateEntity.total.toString())
                add("Pagination-Limit", paginateEntity.limit.toString())
                add("Pagination-Offset", paginateEntity.offset.toString())
            }
            val uzumAccountShopItems =
                shopItemPriceHistoryPaginateEntities.map { it.item }
                    .map { conversionService.convert(it, UzumAccountPriceChangeHistory::class.java)!! }

            ResponseEntity(uzumAccountShopItems.toFlux(), httpHeaders, HttpStatus.OK).toMono()
        }.doOnError {
            log.warn(it) { "Failed to get ke account price history. uzumAccountId=$id;limit=$limit;offset=$offset;filter=$filter;sort=$sort" }
        }
    }

    override fun getUzumAccountShopItemSimilar(
        xRequestID: UUID,
        id: UUID,
        shopId: UUID,
        shopItemId: UUID,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Flux<SimilarItem>>> {
        return exchange.getPrincipal<Principal>().flatMap { principal ->
            val uzumAccountShopItem = uzumAccountShopService.getUzumAccountShopItem(principal.name, id, shopItemId)
                ?: return@flatMap ResponseEntity.notFound().build<Flux<SimilarItem>>().toMono()
            val similarItems =
                uzumShopItemService.findSimilarItems(
                    shopItemId,
                    uzumAccountShopItem.productId,
                    uzumAccountShopItem.skuId,
                    uzumAccountShopItem.categoryId,
                    uzumAccountShopItem.name
                ).map {
                    SimilarItem().apply {
                        this.productId = it.productId
                        this.skuId = it.skuId
                        this.name = it.name
                        this.photoKey = it.photoKey
                    }
                }

            ResponseEntity.ok(similarItems.toFlux()).toMono()
        }.doOnError {
            log.warn(it) {
                "Failed to get ke account item similar items" +
                        ". uzumAccountId=$id;shopId=$shopId;shopItemId=$shopItemId"
            }
        }
    }

    override fun patchUzumAccountInitializationState(
        xRequestID: UUID,
        id: UUID,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Void>> {
        return exchange.getPrincipal<Principal>().flatMap { principal ->
            try {
                val initializeUzumAccountJob = uzumAccountService.initializeUzumAccountJob(principal.name, id)
                if (initializeUzumAccountJob) {
                    ResponseEntity.ok().build<Void>().toMono()
                } else ResponseEntity<Void>(HttpStatus.CONFLICT).toMono()
            } catch (e: IllegalArgumentException) {
                return@flatMap ResponseEntity.notFound().build<Void>().toMono()
            }
        }.doOnError {
            log.warn(it) { "Exception during reinitialize ke account" }
        }
    }

    override fun patchUzumAccountShopitem(
        xRequestID: UUID,
        id: UUID,
        shopItemId: UUID,
        patchUzumAccountShopItem: Mono<PatchUzumAccountShopItem>,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<UzumAccountShopItem>> {
        return exchange.getPrincipal<Principal>().flatMap { principal ->
            patchUzumAccountShopItem.flatMap { request ->
                val changeCount = uzumAccountShopService.changeShopItemPriceOptions(
                    id,
                    shopItemId,
                    request.step,
                    request.minimumThreshold,
                    request.maximumThreshold,
                    request.discount
                )
                if (changeCount <= 0) {
                    ResponseEntity.notFound().build<UzumAccountShopItem>().toMono()
                } else {
                    val uzumAccountShopItem = uzumAccountShopService.getUzumAccountShopItem(principal.name, id, shopItemId)
                    val shopItem = conversionService.convert(uzumAccountShopItem, UzumAccountShopItem::class.java)
                    ResponseEntity.ok(shopItem).toMono()
                }
            }
        }
    }
}
