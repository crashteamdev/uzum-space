package dev.crashteam.uzumspace.filter

import dev.crashteam.uzumspace.controller.ViewFieldToTableFieldMapper
import dev.crashteam.uzumspace.db.model.tables.records.UzumAccountShopItemRecord

interface FilterRecordMapper {

    fun recordMapper(): Map<String, ViewFieldToTableFieldMapper<UzumAccountShopItemRecord, out Comparable<*>>>

}
