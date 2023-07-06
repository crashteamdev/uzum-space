package dev.crashteam.uzumspace.repository.postgre.mapper

interface RecordMapper<T> {
    fun convert(record: org.jooq.Record): T
}
