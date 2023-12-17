package dev.crashteam.uzumspace.service

import dev.crashteam.openapi.space.model.AddStrategyRequest
import dev.crashteam.openapi.space.model.PatchStrategy
import dev.crashteam.uzumspace.repository.postgre.UzumAccountShopItemRepository
import dev.crashteam.uzumspace.repository.postgre.UzumAccountShopItemStrategyRepository
import dev.crashteam.uzumspace.repository.postgre.entity.strategy.UzumAccountShopItemStrategyEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class UzumShopItemStrategyService(
    private val strategyRepository: UzumAccountShopItemStrategyRepository) {

    @Transactional
    fun saveStrategy(addStrategyRequest: AddStrategyRequest): Long {
        return strategyRepository.save(addStrategyRequest)
    }

    fun findStrategy(shopItemId: UUID): UzumAccountShopItemStrategyEntity? {
        return strategyRepository.findById(shopItemId)
    }

    @Transactional
    fun updateStrategy(shopItemId: UUID, patchStrategy: PatchStrategy): Long {
        return strategyRepository.update(shopItemId, patchStrategy)
    }

    @Transactional
    fun deleteStrategy(shopItemId: UUID): Int? {
        return strategyRepository.deleteById(shopItemId)
    }
}