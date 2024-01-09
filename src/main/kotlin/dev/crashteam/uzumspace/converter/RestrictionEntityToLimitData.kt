package dev.crashteam.repricer.converter

import dev.crashteam.openapi.space.model.LimitData
import dev.crashteam.repricer.repository.postgre.entity.RestrictionEntity
import dev.crashteam.uzumspace.converter.DataConverter
import org.springframework.stereotype.Component

@Component
class RestrictionEntityToLimitData : DataConverter<RestrictionEntity, LimitData> {
    override fun convert(source: RestrictionEntity): LimitData? {
        return LimitData().apply {
            userId = source.userId
            accountLimit = source.accountLimit.toLong()
            accountLimitCurrent = source.accountLimitCurrent.toLong()
            itemPoolLimit = source.itemPoolLimit.toLong()
            itemPoolLimitCurrent = source.itemPoolLimitCurrent.toLong()
        }
    }
}