package dev.crashteam.uzumspace.service

import dev.crashteam.openapi.kerepricer.model.AddStrategyRequest
import dev.crashteam.openapi.kerepricer.model.PatchStrategy
import dev.crashteam.uzumspace.repository.postgre.UzumAccountShopItemRepository
import dev.crashteam.uzumspace.repository.postgre.UzumAccountShopItemStrategyRepository
import dev.crashteam.uzumspace.repository.postgre.entity.strategy.UzumAccountShopItemStrategyEntity
import org.springframework.stereotype.Service

@Service
class UzumShopItemStrategyService(
    private val strategyRepository: UzumAccountShopItemStrategyRepository,
    private val uzumAccountShopItemRepository: UzumAccountShopItemRepository
) {

    fun saveStrategy(addStrategyRequest: AddStrategyRequest): Long {
        return uzumAccountShopItemRepository.saveStrategy(addStrategyRequest)
    }

    fun findStrategy(id: Long): UzumAccountShopItemStrategyEntity? {
        return strategyRepository.findById(id)
    }

    fun updateStrategy(shopItemStrategyId: Long, patchStrategy: PatchStrategy): Int {
        return strategyRepository.update(shopItemStrategyId, patchStrategy)
    }

    fun deleteStrategy(id: Long): Int? {
        return strategyRepository.deleteById(id)
    }
}