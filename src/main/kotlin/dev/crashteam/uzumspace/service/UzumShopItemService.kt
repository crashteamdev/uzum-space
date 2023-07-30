package dev.crashteam.uzumspace.service

import dev.brachtendorf.jimagehash.hashAlgorithms.AverageHash
import dev.brachtendorf.jimagehash.hashAlgorithms.HashingAlgorithm
import dev.brachtendorf.jimagehash.hashAlgorithms.PerceptiveHash
import dev.crashteam.uzumspace.client.uzum.UzumWebClient
import dev.crashteam.uzumspace.client.uzum.model.web.ProductData
import dev.crashteam.uzumspace.client.uzum.model.web.ProductPhoto
import dev.crashteam.uzumspace.repository.postgre.UzumShopItemRepository
import dev.crashteam.uzumspace.repository.postgre.entity.UzumShopItemEntity
import dev.crashteam.uzumspace.service.loader.RemoteImageLoader
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*
import javax.imageio.ImageIO

private val log = KotlinLogging.logger {}

@Service
class UzumShopItemService(
    private val uzumShopItemRepository: UzumShopItemRepository,
    private val remoteImageLoader: RemoteImageLoader,
    private val uzumWebClient: UzumWebClient,
) {

    private val avgHash = AverageHash(64)

    private val pHash = PerceptiveHash(64)

    @Transactional
    fun addShopItemFromUzumData(productData: ProductData) {
        val kazanExpressShopItemEntities = productData.skuList!!.mapNotNull { productSplit ->
            val photo: ProductPhoto = productSplit.characteristics.firstNotNullOfOrNull {
                val productCharacteristic = productData.characteristics[it.charIndex]
                val characteristicValue = productCharacteristic.values[it.valueIndex]
                val value = characteristicValue.value
                productData.photos.filter { photo -> photo.color != null }
                    .find { photo -> photo.color == value }
            } ?: productData.photos.firstOrNull() ?: return@mapNotNull null // Ignore empty photo item

            val url =
                "https://images.uzum.uz/${photo.photoKey}/t_product_240_high.jpg" // TODO: avoid static url
            val imageFingerprints = generateImageFingerprints(url)
            val characteristics = productSplit.characteristics.joinToString {
                val productCharacteristic = productData.characteristics[it.charIndex]
                productCharacteristic.values[it.valueIndex].title
            }
            val productTitle = productData.title + " " + characteristics
            UzumShopItemEntity(
                productId = productData.id,
                skuId = productSplit.id,
                categoryId = productData.category.id,
                name = productTitle,
                photoKey = photo.photoKey,
                avgHashFingerprint = imageFingerprints?.avgHash,
                pHashFingerprint = imageFingerprints?.pHash,
                price = productSplit.purchasePrice.movePointRight(2).toLong(),
                availableAmount = productSplit.availableAmount,
                lastUpdate = LocalDateTime.now()
            )
        }
        uzumShopItemRepository.saveBatch(kazanExpressShopItemEntities)
    }

    fun getRecentPrice(shopItemEntity: UzumShopItemEntity): BigDecimal? {
        return if (shopItemEntity.lastUpdate.isBefore(LocalDateTime.now().minusHours(4))) {
            log.info {
                "Product last update too old. Trying to get info from KE." +
                        " productId=${shopItemEntity.productId}; skuId=${shopItemEntity.skuId}"
            }
            val productInfo =
                uzumWebClient.getProductInfo(shopItemEntity.productId.toString())
            if (productInfo?.payload == null) {
                log.error {
                    "Error during try to change price case can't get product info from KE." +
                            " productId=${shopItemEntity.productId}; skuId=${shopItemEntity.skuId}"
                }
                return null
            }

            // Update recent price
            val keShopItemEntity = findShopItem(shopItemEntity.productId, shopItemEntity.skuId)
                ?: throw IllegalArgumentException("Unknown product. productId=${shopItemEntity.productId};skuId=${shopItemEntity.skuId}")
            val productSplit =
                productInfo.payload.data.skuList!!.find { it.id == shopItemEntity.skuId }!!
            uzumShopItemRepository.save(
                keShopItemEntity.copy(
                    price = productSplit.purchasePrice.movePointRight(2).toLong(),
                    lastUpdate = LocalDateTime.now()
                )
            )

            productSplit.purchasePrice
        } else {
            shopItemEntity.price.toBigDecimal().movePointLeft(2)
        }
    }

    fun findShopItem(productId: Long, skuId: Long): UzumShopItemEntity? {
        val keShopItemEntity = uzumShopItemRepository.findByProductIdAndSkuId(productId, skuId)
        if (keShopItemEntity == null) {
            val productInfo = uzumWebClient.getProductInfo(productId.toString())
            if (productInfo?.payload == null)
                throw IllegalStateException("Failed to get product info from KE")
            addShopItemFromUzumData(productInfo.payload.data)
        }
        return uzumShopItemRepository.findByProductIdAndSkuId(productId, skuId)
    }

    // TODO: Refactor and optimize
    fun findSimilarItems(
        shopItemId: UUID,
        productId: Long,
        skuId: Long,
        categoryId: Long,
        productName: String
    ): List<UzumShopItemEntity> {
        val kazanExpressShopItemEntity = uzumShopItemRepository.findByProductIdAndSkuId(productId, skuId)
        val similarItems = if (kazanExpressShopItemEntity?.pHashFingerprint != null) {
            uzumShopItemRepository.findSimilarItemsByNameAndHashAndCategoryId(
                shopItemId,
                productId,
                skuId,
                kazanExpressShopItemEntity.name,
                kazanExpressShopItemEntity.categoryId
            )
        } else {
            uzumShopItemRepository.findSimilarItemsByNameAndCategoryId(
                shopItemId,
                productId,
                skuId,
                productName,
                categoryId,
            )
        }

        return similarItems
    }

    fun findSimilarItemsByName(
        shopItemId: UUID,
        productId: Long,
        skuId: Long,
        name: String
    ): List<UzumShopItemEntity> {
        val targetShopItemEntity = uzumShopItemRepository.findByProductIdAndSkuId(productId, skuId) ?: return emptyList()
        return uzumShopItemRepository.findSimilarItemsByNameAndCategoryId(
            shopItemId,
            productId,
            skuId,
            name,
            targetShopItemEntity.categoryId
        )
    }

    fun findSimilarItemsByProductIdSkuId(productId: Long, skuId: Long): List<UzumShopItemEntity> {
        val targetShopItemEntity = uzumShopItemRepository.findByProductIdAndSkuId(productId, skuId) ?: return emptyList()
        return uzumShopItemRepository.findSimilarItemsByProductIdAndSkuId(
            productId,
            skuId,
            targetShopItemEntity.name,
            targetShopItemEntity.categoryId
        )
    }

    private fun generateImageFingerprints(url: String): ImageFingerprintHolder? {
        val imageByteArray = remoteImageLoader.loadResource(url)
        return try {
            val avgHashFingerprint = generateFingerprint(imageByteArray, avgHash)
            val pHashFingerprint = generateFingerprint(imageByteArray, pHash)

            ImageFingerprintHolder(avgHashFingerprint, pHashFingerprint)
        } catch (e: Exception) {
            log.warn(e) { "Failed to generate fingerprint from url=${url}; imageByteSize=${imageByteArray.size}" }
            null
        }
    }

    private fun generateFingerprint(byteArray: ByteArray, hashAlgorithm: HashingAlgorithm): String {
        val image = ImageIO.read(ByteArrayInputStream(byteArray))
        return hashAlgorithm.hash(image).hashValue.toString(16).uppercase()
    }

    data class ImageFingerprintHolder(
        val avgHash: String,
        val pHash: String,
    )

}
