package dev.crashteam.uzumspace.repository.postgre.mapper

import dev.crashteam.uzumspace.db.model.tables.Account.ACCOUNT
import dev.crashteam.uzumspace.db.model.tables.KeAccount.KE_ACCOUNT
import dev.crashteam.uzumspace.repository.postgre.entity.UzumAccountEntity
import dev.crashteam.uzumspace.repository.postgre.entity.UzumAccountEntityJoinAccountEntity
import org.springframework.stereotype.Component

@Component
class RecordToUzumAccountEntityJoinAccountEntityMapper :
    RecordMapper<UzumAccountEntityJoinAccountEntity> {

    override fun convert(record: Record): UzumAccountEntityJoinAccountEntity {
        return UzumAccountEntityJoinAccountEntity(
            userId = record.getValue(ACCOUNT.USER_ID),
            keAccountEntity = UzumAccountEntity(
                id = record.getValue(KE_ACCOUNT.ID),
                accountId = record.getValue(KE_ACCOUNT.ACCOUNT_ID),
                externalAccountId = record.getValue(KE_ACCOUNT.EXTERNAL_ACCOUNT_ID),
                name = record.getValue(KE_ACCOUNT.NAME),
                login = record.getValue(KE_ACCOUNT.LOGIN),
                password = record.getValue(KE_ACCOUNT.PASSWORD),
                lastUpdate = record.getValue(KE_ACCOUNT.LAST_UPDATE),
                monitorState = record.getValue(KE_ACCOUNT.MONITOR_STATE),
                updateState = record.getValue(KE_ACCOUNT.UPDATE_STATE),
            )
        )
    }
}
