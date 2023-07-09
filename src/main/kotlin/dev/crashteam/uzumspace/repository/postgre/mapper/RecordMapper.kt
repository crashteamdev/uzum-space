package dev.crashteam.uzumspace.repository.postgre.mapper
import org.jooq.Record

interface RecordMapper<T> {
    fun convert(record: Record): T
}
