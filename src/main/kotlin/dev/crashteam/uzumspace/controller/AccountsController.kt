package dev.crashteam.uzumspace.controller

import dev.crashteam.openapi.kerepricer.api.AccountsApi
import dev.crashteam.openapi.kerepricer.model.*
import dev.crashteam.uzumspace.db.model.tables.KeAccountShop.*
import dev.crashteam.uzumspace.db.model.tables.KeAccountShopItem.KE_ACCOUNT_SHOP_ITEM
import dev.crashteam.uzumspace.db.model.tables.KeAccountShopItemCompetitor.KE_ACCOUNT_SHOP_ITEM_COMPETITOR
import dev.crashteam.uzumspace.db.model.tables.KeAccountShopItemPriceHistory.KE_ACCOUNT_SHOP_ITEM_PRICE_HISTORY
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
@RequestMapping("/v1")
class AccountsController(
    private val uzumAccountService: UzumAccountService,
    private val uzumAccountShopService: UzumAccountShopService,
    private val updateKeAccountService: UpdateKeAccountService,
    private val uzumShopItemService: UzumShopItemService,
    private val keShopItemPriceChangeRepository: UzumShopItemPriceHistoryRepository,
    private val conversionService: ConversionService,
    private val urlToProductResolver: UrlToProductResolver
) : AccountsApi {

    override fun addKeAccount(
        xRequestID: UUID,
        addKeAccountRequest: Mono<AddKeAccountRequest>,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<KeAccount>> {
        return exchange.getPrincipal<Principal>().flatMap { principal ->
            addKeAccountRequest.flatMap { request ->
                try {
                    if (request.login.isNullOrEmpty() || request.password.isNullOrEmpty()) {
                        return@flatMap ResponseEntity.badRequest().build<KeAccount>().toMono()
                    }
                    val keAccountEntity = uzumAccountService.addKeAccount(principal.name, request.login, request.password)
                    val keAccount = conversionService.convert(keAccountEntity, KeAccount::class.java)
                    ResponseEntity.ok(keAccount).toMono()
                } catch (e: AccountItemPoolLimitExceededException) {
                    return@flatMap ResponseEntity.status(HttpStatus.FORBIDDEN).build<KeAccount>().toMono()
                } catch (e: IllegalArgumentException) {
                    return@flatMap ResponseEntity.status(HttpStatus.FORBIDDEN).build<KeAccount>().toMono()
                }
            }
        }.doOnError {
            log.warn(it) { "Failed to add ke account" }
        }
    }

    override fun addKeAccountShopItemCompetitor(
        xRequestID: UUID,
        id: UUID,
        addKeAccountShopItemCompetitorRequest: Mono<AddKeAccountShopItemCompetitorRequest>,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Void>> {
        return exchange.getPrincipal<Principal>().flatMap { principal ->
            addKeAccountShopItemCompetitorRequest.flatMap { request ->
                var productId = request.competitorProductId?.toLong()
                var skuId = request.competitorSkuId?.toLong()
                if (request.url != null) {
                    val resolvedKeProduct = urlToProductResolver.resolve(request.url)
                    if (resolvedKeProduct != null) {
                        productId = resolvedKeProduct.productId.toLong()
                        skuId = resolvedKeProduct.skuId.toLong()
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
            log.warn(it) { "Failed to add ke account shop item competitor. keAccountId=$id" }
        }
    }

    override fun addKeAccountShopItemPool(
        xRequestID: UUID,
        id: UUID,
        addKeAccountShopItemPoolRequest: Mono<AddKeAccountShopItemPoolRequest>,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Void>> {
        return exchange.getPrincipal<Principal>().flatMap { principal ->
            addKeAccountShopItemPoolRequest.flatMap { request ->
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
            log.warn(it) { "Failed to add shop item into pool. keAccountId=$id" }
        }
    }

    override fun deleteKeAccount(
        xRequestID: UUID,
        id: UUID,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Void>> {
        return exchange.getPrincipal<Principal>().flatMap {
            val removeKeAccountCount = uzumAccountService.removeKeAccount(it.name, id)
            if (removeKeAccountCount > 0) {
                return@flatMap ResponseEntity.ok().build<Void>().toMono()
            }
            ResponseEntity.notFound().build<Void>().toMono()
        }.doOnError {
            log.warn(it) { "Failed to delete ke account. keAccountId=$id" }
        }
    }

    override fun getKeAccount(
        xRequestID: UUID,
        id: UUID,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<KeAccount>> {
        return exchange.getPrincipal<Principal>().flatMap {
            val kazanExpressAccountEntity = uzumAccountService.getKeAccount(it.name, id)
            val keAccount = conversionService.convert(kazanExpressAccountEntity, KeAccount::class.java)

            ResponseEntity.ok(keAccount).toMono()
        }.doOnError {
            log.warn(it) { "Failed to get ke account. keAccountId=$id" }
        }
    }

    override fun getKeAccountCompetitorShopItems(
        xRequestID: UUID,
        id: UUID,
        shopId: UUID,
        shopItemId: UUID,
        limit: Int?,
        offset: Int?,
        filter: String?,
        sort: MutableList<String>?,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Flux<KeAccountCompetitorShopItem>>> {
        return exchange.getPrincipal<Principal>().flatMap {
            val limit = limit ?: 10
            val offset = offset ?: 0
            val mapFields = mapOf(
                "name" to StringTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.NAME),
                "productId" to LongTableFieldMapper(KE_ACCOUNT_SHOP_ITEM_COMPETITOR.PRODUCT_ID),
                "skuId" to LongTableFieldMapper(KE_ACCOUNT_SHOP_ITEM_COMPETITOR.SKU_ID),
                "price" to LongTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.PRICE),
                "availableAmount" to LongTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.AVAILABLE_AMOUNT),
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
                return@flatMap ResponseEntity.ok(emptyList<KeAccountCompetitorShopItem>().toFlux()).toMono()
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
                    KeAccountCompetitorShopItem::class.java
                )!!
            }.toFlux(), httpHeaders, HttpStatus.OK).toMono()
        }.doOnError {
            log.warn(it) {
                "Failed to get ke account shop item competitors." +
                        " keAccountId=$id;shopId=$shopId;shopItemId=$shopItemId;limit=$limit;offset=$offset;filter=$filter;sort=$sort"
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

    override fun getKeAccountShopItems(
        xRequestID: UUID,
        id: UUID,
        shopId: UUID,
        limit: Int?,
        offset: Int?,
        filter: String?,
        sort: MutableList<String>?,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Flux<KeAccountShopItem>>> {
        return exchange.getPrincipal<Principal>().flatMap {
            val limit = limit ?: 10
            val offset = offset ?: 0
            val mapFields = mapOf(
                "productId" to LongTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.PRODUCT_ID),
                "skuId" to LongTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.SKU_ID),
                "skuTitle" to StringTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.SKU_TITLE),
                "name" to StringTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.NAME),
                "photoKey" to StringTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.PHOTO_KEY),
                "purchasePrice" to LongTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.PURCHASE_PRICE),
                "price" to LongTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.PRICE),
                "barCode" to LongTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.BARCODE),
                "availableAmount" to LongTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.AVAILABLE_AMOUNT),
                "minimumThreshold" to LongTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.MINIMUM_THRESHOLD),
                "maximumThreshold" to LongTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.MAXIMUM_THRESHOLD),
                "step" to IntegerTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.STEP),
                "discount" to BigIntegerTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.DISCOUNT)
            )
            val filterCondition = filter?.let {
                FilterOperation.parse(filter, mapFields)
            }
            val sortFields = if (sort != null) {
                SortOperation.parse(sort, mapFields)
            } else null
            val shopItemCompetitors = uzumAccountShopService.getKeAccountShopItems(
                it.name,
                id,
                shopId,
                filterCondition,
                sortFields,
                limit.toLong(),
                offset.toLong()
            )
            if (shopItemCompetitors.isEmpty()) {
                return@flatMap ResponseEntity.ok(emptyList<KeAccountShopItem>().toFlux()).toMono()
            }
            val paginateEntity = shopItemCompetitors.first()
            val httpHeaders = HttpHeaders().apply {
                add("Pagination-Total", paginateEntity.total.toString())
                add("Pagination-Limit", paginateEntity.limit.toString())
                add("Pagination-Offset", paginateEntity.offset.toString())
            }
            val keAccountShopItems =
                shopItemCompetitors.map { it.item }
                    .map { conversionService.convert(it, KeAccountShopItem::class.java)!! }
            ResponseEntity(keAccountShopItems.toFlux(), httpHeaders, HttpStatus.OK).toMono()
        }.doOnError {
            log.warn(it) { "Failed to get ke account shop items. keAccountId=$id;shopId=$shopId;limit=$limit;offset=$offset;filter=$filter;sort=$sort" }
        }
    }

    override fun getKeAccountShopItemsPool(
        xRequestID: UUID,
        id: UUID,
        shopId: UUID,
        limit: Int?,
        offset: Int?,
        filter: String?,
        sort: MutableList<String>?,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Flux<KeAccountShopItem>>> {
        return exchange.getPrincipal<Principal>().flatMap {
            val limit = limit ?: 10
            val offset = offset ?: 0
            val mapFields = mapOf(
                "productId" to LongTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.PRODUCT_ID),
                "skuId" to LongTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.SKU_ID),
                "name" to StringTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.NAME),
                "photoKey" to StringTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.PHOTO_KEY),
                "purchasePrice" to LongTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.PURCHASE_PRICE),
                "price" to LongTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.PRICE),
                "barCode" to LongTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.BARCODE),
                "availableAmount" to LongTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.AVAILABLE_AMOUNT),
                "minimumThreshold" to LongTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.MINIMUM_THRESHOLD),
                "maximumThreshold" to LongTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.MAXIMUM_THRESHOLD),
                "step" to IntegerTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.STEP)
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
                return@flatMap ResponseEntity.ok(emptyList<KeAccountShopItem>().toFlux()).toMono()
            }
            val paginateEntity = shopItemPaginateEntities.first()
            val httpHeaders = HttpHeaders().apply {
                add("Pagination-Total", paginateEntity.total.toString())
                add("Pagination-Limit", paginateEntity.limit.toString())
                add("Pagination-Offset", paginateEntity.offset.toString())
            }
            val keAccountShopItems =
                shopItemPaginateEntities.map { it.item }
                    .map { conversionService.convert(it, KeAccountShopItem::class.java)!! }
            ResponseEntity(keAccountShopItems.toFlux(), httpHeaders, HttpStatus.OK).toMono()
        }.doOnError {
            log.warn(it) { "Failed to ke account shop item pool. keAccountId=$id;shopId=$shopId;limit=$limit;offset=$offset;filter=$filter;sort=$sort" }
        }
    }

    override fun getKeAccountShops(
        xRequestID: UUID,
        id: UUID,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Flux<KeAccountShop>>> {
        return exchange.getPrincipal<Principal>().flatMap {
            val kazanExpressAccountShopEntities = uzumAccountShopService.getKeAccountShops(it.name, id)
            val keAccountShops =
                kazanExpressAccountShopEntities.map { conversionService.convert(it, KeAccountShop::class.java)!! }
            ResponseEntity.ok(keAccountShops.toFlux()).toMono()
        }.doOnError {
            log.warn(it) { "Failed to get ke account shops" }
        }
    }

    override fun getKeAccounts(
        xRequestID: UUID,
        exchange: ServerWebExchange,
    ): Mono<ResponseEntity<Flux<KeAccount>>> {
        return exchange.getPrincipal<Principal>().flatMap {
            val keAccounts = uzumAccountService.getKeAccounts(it.name)
            val keAccountList = keAccounts.map { conversionService.convert(it, KeAccount::class.java)!! }
            ResponseEntity.ok(keAccountList.toFlux()).toMono()
        }.doOnError {
            log.warn(it) { "Failed to get ke accounts" }
        }
    }

    override fun patchKeAccount(
        xRequestID: UUID,
        id: UUID,
        patchKeAccount: Mono<PatchKeAccount>,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Void>> {
        return exchange.getPrincipal<Principal>().flatMap { principal ->
            patchKeAccount.flatMap { request ->
                val response = try {
                    uzumAccountService.editKeAccount(principal.name, id, request.login, request.password)
                    uzumAccountService.initializeKeAccountJob(principal.name, id)
                    ResponseEntity.ok().build()
                } catch (e: UserNotFoundException) {
                    ResponseEntity.notFound().build<Void>()
                } catch (e: IllegalArgumentException) {
                    ResponseEntity(HttpStatus.CONFLICT)
                }
                response.toMono()
            }
        }.doOnError {
            log.warn(it) { "Failed to change ke account credentials. keAccountId=$id" }
        }
    }

    override fun patchKeAccountMonitoringState(
        xRequestID: UUID,
        id: UUID,
        patchKeAccountMonitoringState: Mono<PatchKeAccountMonitoringState>,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Void>> {
        return exchange.getPrincipal<Principal>().flatMap { principal ->
            patchKeAccountMonitoringState.flatMap { request ->
                val monitorState = when (request.state) {
                    PatchKeAccountMonitoringState.StateEnum.ACTIVATE -> MonitorState.active
                    PatchKeAccountMonitoringState.StateEnum.SUSPEND -> MonitorState.suspended
                    else -> return@flatMap Mono.error(IllegalArgumentException("Unknown request state: ${request.state}"))
                }
                val changeKeAccountMonitoringState =
                    uzumAccountService.changeKeAccountMonitoringState(principal.name, id, monitorState)
                return@flatMap if (changeKeAccountMonitoringState > 0) {
                    ResponseEntity.ok().build<Void>().toMono()
                } else {
                    ResponseEntity.notFound().build<Void>().toMono()
                }
            }
        }.doOnError {
            log.warn(it) { "Failed to change ke account monitor state. keAccountId=$id" }
        }
    }

    override fun removeKeAccountShopItemCompetitor(
        xRequestID: UUID,
        id: UUID,
        removeKeAccountShopItemCompetitorRequest: Mono<RemoveKeAccountShopItemCompetitorRequest>,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Void>> {
        return exchange.getPrincipal<Principal>().flatMap { principal ->
            removeKeAccountShopItemCompetitorRequest.flatMap { request ->
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
            log.warn(it) { "Failed to remove ke account shop item competitor. keAccountId=$id" }
        }
    }

    override fun removeKeAccountShopItemFromPool(
        xRequestID: UUID,
        id: UUID,
        addKeAccountShopItemPoolRequest: Mono<AddKeAccountShopItemPoolRequest>,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Void>> {
        return exchange.getPrincipal<Principal>().flatMap { principal ->
            addKeAccountShopItemPoolRequest.flatMap { request ->
                val removeShopItemFromPoolCount =
                    uzumAccountShopService.removeShopItemFromPool(principal.name, id, request.shopId, request.shopItemId)
                if (removeShopItemFromPoolCount > 0) {
                    ResponseEntity.ok().build<Void>().toMono()
                } else {
                    ResponseEntity.notFound().build<Void>().toMono()
                }
            }
        }.doOnError {
            log.warn(it) { "Failed to remove ke account shop item from pool. keAccountId=$id" }
        }
    }

    override fun updateKeAccountData(
        xRequestID: UUID,
        id: UUID,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Void>> {
        return exchange.getPrincipal<Principal>().flatMap { principal ->
            val result = updateKeAccountService.executeUpdateJob(principal.name, id)
            if (!result) {
                ResponseEntity<Void>(HttpStatus.CONFLICT).toMono()
            } else {
                ResponseEntity.ok().build<Void>().toMono()
            }
        }.doOnError {
            log.warn(it) { "Exception during update ke account data. keAccountId=$id" }
        }
    }

    override fun getKeAccountShopItem(
        xRequestID: UUID,
        id: UUID,
        shopItemId: UUID,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<KeAccountShopItem>> {
        return exchange.getPrincipal<Principal>().flatMap { principal ->
            val keAccountShopItem = uzumAccountShopService.getKeAccountShopItem(principal.name, id, shopItemId)
                ?: return@flatMap ResponseEntity.notFound().build<KeAccountShopItem>().toMono()
            ResponseEntity.ok(conversionService.convert(keAccountShopItem, KeAccountShopItem::class.java)).toMono()
        }.doOnError {
            log.warn(it) { "Failed to get ke account shop item. keAccountId=$id;shopItemId=$shopItemId" }
        }
    }

    override fun getKeAccountShopItemPriceChangeHistory(
        xRequestID: UUID,
        id: UUID,
        limit: Int?,
        offset: Int?,
        filter: String?,
        sort: MutableList<String>?,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Flux<KeAccountPriceChangeHistory>>> {
        return exchange.getPrincipal<Principal>().flatMap { principal ->
            val limit = limit ?: 10
            val offset = offset ?: 0
            val mapFields = mapOf(
                "productId" to LongTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.PRODUCT_ID),
                "skuId" to LongTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.SKU_ID),
                "shopName" to StringTableFieldMapper(KE_ACCOUNT_SHOP.NAME),
                "itemName" to StringTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.NAME),
                "oldPrice" to LongTableFieldMapper(KE_ACCOUNT_SHOP_ITEM_PRICE_HISTORY.OLD_PRICE),
                "newPrice" to LongTableFieldMapper(KE_ACCOUNT_SHOP_ITEM_PRICE_HISTORY.PRICE),
                "barcode" to LongTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.BARCODE),
            )
            val filterCondition = filter?.let {
                FilterOperation.parse(filter, mapFields)
            }
            val sortFields = if (sort != null) {
                SortOperation.parse(sort, mapFields)
            } else null

            val shopItemPriceHistoryPaginateEntities = keShopItemPriceChangeRepository.findHistoryByKeAccountId(
                id,
                filterCondition,
                sortFields,
                limit.toLong(),
                offset.toLong()
            )
            if (shopItemPriceHistoryPaginateEntities.isEmpty()) {
                return@flatMap ResponseEntity(emptyList<KeAccountPriceChangeHistory>().toFlux(), HttpStatus.OK).toMono()
            }
            val paginateEntity = shopItemPriceHistoryPaginateEntities.first()
            val httpHeaders = HttpHeaders().apply {
                add("Pagination-Total", paginateEntity.total.toString())
                add("Pagination-Limit", paginateEntity.limit.toString())
                add("Pagination-Offset", paginateEntity.offset.toString())
            }
            val keAccountShopItems =
                shopItemPriceHistoryPaginateEntities.map { it.item }
                    .map { conversionService.convert(it, KeAccountPriceChangeHistory::class.java)!! }

            ResponseEntity(keAccountShopItems.toFlux(), httpHeaders, HttpStatus.OK).toMono()
        }.doOnError {
            log.warn(it) { "Failed to get ke account price history. keAccountId=$id;limit=$limit;offset=$offset;filter=$filter;sort=$sort" }
        }
    }

    override fun getKeAccountShopItemSimilar(
        xRequestID: UUID,
        id: UUID,
        shopId: UUID,
        shopItemId: UUID,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Flux<SimilarItem>>> {
        return exchange.getPrincipal<Principal>().flatMap { principal ->
            val keAccountShopItem = uzumAccountShopService.getKeAccountShopItem(principal.name, id, shopItemId)
                ?: return@flatMap ResponseEntity.notFound().build<Flux<SimilarItem>>().toMono()
            val similarItems =
                uzumShopItemService.findSimilarItems(
                    shopItemId,
                    keAccountShopItem.productId,
                    keAccountShopItem.skuId,
                    keAccountShopItem.categoryId,
                    keAccountShopItem.name
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
                        ". keAccountId=$id;shopId=$shopId;shopItemId=$shopItemId"
            }
        }
    }

    override fun patchKeAccountInitializationState(
        xRequestID: UUID,
        id: UUID,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Void>> {
        return exchange.getPrincipal<Principal>().flatMap { principal ->
            try {
                val initializeKeAccountJob = uzumAccountService.initializeKeAccountJob(principal.name, id)
                if (initializeKeAccountJob) {
                    ResponseEntity.ok().build<Void>().toMono()
                } else ResponseEntity<Void>(HttpStatus.CONFLICT).toMono()
            } catch (e: IllegalArgumentException) {
                return@flatMap ResponseEntity.notFound().build<Void>().toMono()
            }
        }.doOnError {
            log.warn(it) { "Exception during reinitialize ke account" }
        }
    }

    override fun patchKeAccountShopitem(
        xRequestID: UUID,
        id: UUID,
        shopItemId: UUID,
        patchKeAccountShopItem: Mono<PatchKeAccountShopItem>,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<KeAccountShopItem>> {
        return exchange.getPrincipal<Principal>().flatMap { principal ->
            patchKeAccountShopItem.flatMap { request ->
                val changeCount = uzumAccountShopService.changeShopItemPriceOptions(
                    id,
                    shopItemId,
                    request.step,
                    request.minimumThreshold,
                    request.maximumThreshold,
                    request.discount
                )
                if (changeCount <= 0) {
                    ResponseEntity.notFound().build<KeAccountShopItem>().toMono()
                } else {
                    val keAccountShopItem = uzumAccountShopService.getKeAccountShopItem(principal.name, id, shopItemId)
                    val shopItem = conversionService.convert(keAccountShopItem, KeAccountShopItem::class.java)
                    ResponseEntity.ok(shopItem).toMono()
                }
            }
        }
    }
}
