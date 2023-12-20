package dev.crashteam.uzumspace.filter

import dev.crashteam.uzumspace.controller.FilterOperation
import dev.crashteam.uzumspace.controller.SortOperation
import org.springframework.stereotype.Component

@Component
class QueryFilterParser {

    fun parseFilter(filter: String?, sort: List<String>? = null, filterRecordMapper: FilterRecordMapper): ParsedQuery {
        val recordFilter = filterRecordMapper.recordMapper()
        val filterCondition = filter?.let {
            FilterOperation.parse(filter, recordFilter)
        }
        val sortFields = if (sort != null) {
            SortOperation.parse(sort, recordFilter)
        } else null

        return ParsedQuery(filterCondition, sortFields)
    }

}
