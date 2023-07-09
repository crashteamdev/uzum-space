package dev.crashteam.uzumspace.converter

import dev.crashteam.openapi.space.model.UzumAccount
import dev.crashteam.uzumspace.db.model.enums.InitializeState
import dev.crashteam.uzumspace.db.model.enums.MonitorState
import dev.crashteam.uzumspace.db.model.enums.UpdateState
import dev.crashteam.uzumspace.repository.postgre.entity.UzumAccountEntity
import org.springframework.stereotype.Component
import java.time.ZoneOffset

@Component
class UzumAccountEntityToViewConverter : DataConverter<UzumAccountEntity, UzumAccount> {
    override fun convert(source: UzumAccountEntity): UzumAccount {
        return UzumAccount().apply {
            this.id = source.id
            this.name = source.name
            this.email = source.email
            this.login = source.login
            this.lastUpdate = source.lastUpdate?.atOffset(ZoneOffset.UTC)
            this.monitorState = when (source.monitorState) {
                MonitorState.active -> dev.crashteam.openapi.space.model.MonitorState.ACTIVE
                MonitorState.suspended -> dev.crashteam.openapi.space.model.MonitorState.SUSPENDED
                else -> dev.crashteam.openapi.space.model.MonitorState.SUSPENDED
            }
            this.updateState = when (source.updateState) {
                UpdateState.not_started -> dev.crashteam.openapi.space.model.UpdateState.NOT_STARTED
                UpdateState.in_progress -> dev.crashteam.openapi.space.model.UpdateState.IN_PROGRESS
                UpdateState.finished -> dev.crashteam.openapi.space.model.UpdateState.FINISHED
                UpdateState.error -> dev.crashteam.openapi.space.model.UpdateState.ERROR
                null -> dev.crashteam.openapi.space.model.UpdateState.NOT_STARTED
            }
            this.initializeState = when (source.initializeState) {
                InitializeState.not_started -> dev.crashteam.openapi.space.model.InitializeState.NOT_STARTED
                InitializeState.in_progress -> dev.crashteam.openapi.space.model.InitializeState.IN_PROGRESS
                InitializeState.finished -> dev.crashteam.openapi.space.model.InitializeState.FINISHED
                InitializeState.error -> dev.crashteam.openapi.space.model.InitializeState.ERROR
                null -> dev.crashteam.openapi.space.model.InitializeState.NOT_STARTED
            }
        }
    }
}
