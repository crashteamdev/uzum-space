package dev.crashteam.uzumspace.repository.postgre.mapper

import dev.crashteam.uzumspace.db.model.tables.Account.ACCOUNT
import dev.crashteam.uzumspace.db.model.tables.UzumAccount.UZUM_ACCOUNT
import dev.crashteam.uzumspace.repository.postgre.entity.UzumAccountEntity
import dev.crashteam.uzumspace.repository.postgre.entity.UzumAccountEntityJoinAccountEntity
import org.springframework.stereotype.Component
import org.jooq.Record

@Component
class RecordToUzumAccountEntityJoinAccountEntityMapper :
    RecordMapper<UzumAccountEntityJoinAccountEntity> {

    override fun convert(record: Record): UzumAccountEntityJoinAccountEntity {
        return UzumAccountEntityJoinAccountEntity(
            userId = record.getValue(ACCOUNT.USER_ID),
            uzumAccountEntity = UzumAccountEntity(
                id = record.getValue(UZUM_ACCOUNT.ID),
                accountId = record.getValue(UZUM_ACCOUNT.ACCOUNT_ID),
                externalAccountId = record.getValue(UZUM_ACCOUNT.EXTERNAL_ACCOUNT_ID),
                name = record.getValue(UZUM_ACCOUNT.NAME),
                login = record.getValue(UZUM_ACCOUNT.LOGIN),
                password = record.getValue(UZUM_ACCOUNT.PASSWORD),
                lastUpdate = record.getValue(UZUM_ACCOUNT.LAST_UPDATE),
                monitorState = record.getValue(UZUM_ACCOUNT.MONITOR_STATE),
                updateState = record.getValue(UZUM_ACCOUNT.UPDATE_STATE),
            )
        )
    }
}
