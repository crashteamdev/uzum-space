package dev.crashteam.uzumspace.repository.postgre.mapper

import dev.crashteam.uzumspace.db.model.tables.KeAccount.KE_ACCOUNT
import dev.crashteam.uzumspace.repository.postgre.entity.UzumAccountEntity
import org.springframework.stereotype.Component

@Component
class RecordToUzumAccountEntityMapper : RecordMapper<UzumAccountEntity> {

    override fun convert(record: Record): UzumAccountEntity {
        return UzumAccountEntity(
            id = record.getValue(KE_ACCOUNT.ID),
            accountId = record.getValue(KE_ACCOUNT.ACCOUNT_ID),
            externalAccountId = record.getValue(KE_ACCOUNT.EXTERNAL_ACCOUNT_ID),
            name = record.getValue(KE_ACCOUNT.NAME),
            email = record.getValue(KE_ACCOUNT.EMAIL),
            login = record.getValue(KE_ACCOUNT.LOGIN),
            password = record.getValue(KE_ACCOUNT.PASSWORD),
            lastUpdate = record.getValue(KE_ACCOUNT.LAST_UPDATE),
            monitorState = record.getValue(KE_ACCOUNT.MONITOR_STATE),
            updateState = record.getValue(KE_ACCOUNT.UPDATE_STATE),
            initializeState = record.getValue(KE_ACCOUNT.INITIALIZE_STATE)
        )
    }
}
