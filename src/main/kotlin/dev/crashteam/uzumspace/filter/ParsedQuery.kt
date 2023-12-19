package dev.crashteam.uzumspace.filter

import dev.crashteam.uzumspace.repository.postgre.SortType
import org.jooq.Condition
import org.jooq.Record
import org.jooq.TableField

data class ParsedQuery(
    val filterCondition: Condition?,
    val sortFields: List<Pair<TableField<out Record, out Any?>, SortType>>?,
)
