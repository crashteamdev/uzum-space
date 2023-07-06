package dev.crashteam.uzumspace.repository.postgre

import dev.crashteam.uzumspace.db.model.enums.InitializeState
import dev.crashteam.uzumspace.db.model.enums.MonitorState
import dev.crashteam.uzumspace.db.model.enums.UpdateState
import dev.crashteam.uzumspace.db.model.tables.Account.ACCOUNT
import dev.crashteam.uzumspace.db.model.tables.KeAccount.KE_ACCOUNT
import dev.crashteam.uzumspace.repository.postgre.entity.UzumAccountEntity
import dev.crashteam.uzumspace.repository.postgre.entity.UzumAccountEntityJoinAccountEntity
import dev.crashteam.uzumspace.repository.postgre.mapper.RecordToUzumAccountEntityJoinAccountEntityMapper
import dev.crashteam.uzumspace.repository.postgre.mapper.RecordToUzumAccountEntityMapper
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

@Repository
class UzumAccountRepository(
    private val dsl: DSLContext,
    private val recordToUzumAccountEntityMapper: RecordToUzumAccountEntityMapper,
    private val recordToUzumAccountEntityJoinAccountEntityMapper: RecordToUzumAccountEntityJoinAccountEntityMapper
) {

    fun save(keAccountEntity: UzumAccountEntity): UUID? {
        val k = KE_ACCOUNT
        return dsl.insertInto(
            k,
            k.ID,
            k.ACCOUNT_ID,
            k.EXTERNAL_ACCOUNT_ID,
            k.NAME,
            k.EMAIL,
            k.LOGIN,
            k.PASSWORD,
            k.LAST_UPDATE
        )
            .values(
                keAccountEntity.id,
                keAccountEntity.accountId,
                keAccountEntity.externalAccountId,
                keAccountEntity.name,
                keAccountEntity.email,
                keAccountEntity.login,
                keAccountEntity.password,
                keAccountEntity.lastUpdate
            )
            .onDuplicateKeyUpdate()
            .set(
                mapOf(
                    k.ACCOUNT_ID to keAccountEntity.accountId,
                    k.EXTERNAL_ACCOUNT_ID to keAccountEntity.externalAccountId,
                    k.NAME to keAccountEntity.name,
                    k.EMAIL to keAccountEntity.email,
                    k.LOGIN to keAccountEntity.login,
                    k.PASSWORD to keAccountEntity.password,
                    k.LAST_UPDATE to keAccountEntity.lastUpdate,
                )
            )
            .returningResult(k.ID)
            .fetchOne()!!.getValue(k.ID)
    }

    fun getUzumAccounts(userId: String): List<UzumAccountEntity> {
        val a = ACCOUNT
        val k = KE_ACCOUNT
        val records = dsl.select()
            .from(k.join(a).on(a.ID.eq(k.ACCOUNT_ID)))
            .where(a.USER_ID.eq(userId))
            .fetch()
        return records.map { recordToUzumAccountEntityMapper.convert(it) }.toList()
    }

    fun getUzumAccountsCount(userId: String): Int {
        val a = ACCOUNT
        val k = KE_ACCOUNT
        return dsl.selectCount()
            .from(k.join(a).on(a.ID.eq(k.ACCOUNT_ID)))
            .where(a.USER_ID.eq(userId))
            .fetchOne(0, Int::class.java) ?: 0
    }

    fun getUzumAccount(userId: String, keAccountId: UUID): UzumAccountEntity? {
        val a = ACCOUNT
        val k = KE_ACCOUNT
        val record = dsl.select()
            .from(k.join(a).on(a.ID.eq(k.ACCOUNT_ID)))
            .where(a.USER_ID.eq(userId).and(k.ID.eq(keAccountId)))
            .fetchOne() ?: return null
        return recordToUzumAccountEntityMapper.convert(record)
    }

    fun getUzumAccount(keAccountId: UUID): UzumAccountEntityJoinAccountEntity? {
        val a = ACCOUNT
        val k = KE_ACCOUNT
        val record = dsl.select()
            .from(k.join(a).on(a.ID.eq(k.ACCOUNT_ID)))
            .where(k.ID.eq(keAccountId))
            .fetchOne() ?: return null
        return recordToUzumAccountEntityJoinAccountEntityMapper.convert(record)
    }

    fun changeUpdateState(
        userId: String,
        keAccountId: UUID,
        updateState: UpdateState,
        lastUpdate: LocalDateTime? = null
    ): Int {
        val a = ACCOUNT
        val k = KE_ACCOUNT
        val updateStep = dsl.update(k)
            .set(
                mapOf(
                    k.UPDATE_STATE to updateState,
                    k.UPDATE_STATE_LAST_UPDATE to LocalDateTime.now()
                )
            )
        if (lastUpdate != null) {
            updateStep.set(k.LAST_UPDATE, lastUpdate)
        }
        return updateStep.from(a).where(a.USER_ID.eq(userId).and(k.ID.eq(keAccountId))).execute()
    }

    fun changeMonitorState(
        userId: String,
        keAccountId: UUID,
        monitorState: MonitorState,
    ): Int {
        val a = ACCOUNT
        val k = KE_ACCOUNT
        return dsl.update(k)
            .set(k.MONITOR_STATE, monitorState)
            .from(a)
            .where(a.USER_ID.eq(userId).and(k.ID.eq(keAccountId))).execute()
    }

    fun changeInitializeState(
        userId: String,
        keAccountId: UUID,
        initializeState: InitializeState
    ): Int {
        val a = ACCOUNT
        val k = KE_ACCOUNT
        return dsl.update(k)
            .set(
                mapOf(
                    k.INITIALIZE_STATE to initializeState,
                    k.INITIALIZE_STATE_LAST_UPDATE to LocalDateTime.now()
                )
            )
            .from(a)
            .where(a.USER_ID.eq(userId).and(k.ID.eq(keAccountId))).execute()
    }

    fun removeUzumAccount(userId: String, keAccountId: UUID): Int {
        val a = ACCOUNT
        val k = KE_ACCOUNT
        return dsl.deleteFrom(k)
            .using(a)
            .where(
                k.ID.eq(keAccountId).and(a.USER_ID.eq(userId))
            )
            .execute()
    }

    fun findAccountUpdateNotInProgress(
        lastUpdate: LocalDateTime
    ): MutableList<UzumAccountEntityJoinAccountEntity> {
        val a = ACCOUNT
        val k = KE_ACCOUNT
        val records = dsl.selectFrom(k.join(a).on(a.ID.eq(k.ACCOUNT_ID)))
            .where(
                k.INITIALIZE_STATE.eq(InitializeState.finished)
                    .and(k.UPDATE_STATE.notEqual(UpdateState.in_progress))
                    .and(k.LAST_UPDATE.lessThan(lastUpdate))
                    .and(a.SUBSCRIPTION_VALID_UNTIL.greaterThan(LocalDateTime.now()))
            )
            .fetch()
        return records.map { recordToUzumAccountEntityJoinAccountEntityMapper.convert(it) }
    }

    fun findAccountUpdateInProgressCount(): Int {
        val k = KE_ACCOUNT
        return dsl.selectCount()
            .from(k)
            .where(k.UPDATE_STATE.eq(UpdateState.in_progress))
            .fetchOne(0, Int::class.java) ?: 0
    }

    fun findAccountByUpdateStateInProgressAndLastUpdateLessThan(
        updateStateLastUpdate: LocalDateTime
    ): List<UzumAccountEntityJoinAccountEntity> {
        val a = ACCOUNT
        val k = KE_ACCOUNT
        val records = dsl.selectFrom(k.join(a).on(a.ID.eq(k.ACCOUNT_ID)))
            .where(
                k.UPDATE_STATE.eq(UpdateState.in_progress)
                    .and(k.UPDATE_STATE_LAST_UPDATE.lessThan(updateStateLastUpdate))
            )
            .fetch()
        return records.map { recordToUzumAccountEntityJoinAccountEntityMapper.convert(it) }
    }

    fun findAccountWhereMonitorActiveWithValidSubscription(): List<UzumAccountEntity> {
        val a = ACCOUNT
        val k = KE_ACCOUNT
        val records = dsl.selectFrom(k.join(a).on(a.ID.eq(k.ACCOUNT_ID)))
            .where(
                k.MONITOR_STATE.eq(MonitorState.active).and(a.SUBSCRIPTION_VALID_UNTIL.greaterThan(LocalDateTime.now()))
            )
            .fetch()

        return records.map { recordToUzumAccountEntityMapper.convert(it) }
    }

    fun findNotInitializedAccount(): List<UzumAccountEntityJoinAccountEntity> {
        val a = ACCOUNT
        val k = KE_ACCOUNT
        val records = dsl.selectFrom(k.join(a).on(a.ID.eq(k.ACCOUNT_ID)))
            .where(k.INITIALIZE_STATE.eq(InitializeState.not_started))
            .fetch()
        return records.map { recordToUzumAccountEntityJoinAccountEntityMapper.convert(it) }
    }

    fun findAccountByInitializeStateInProgressAndLastUpdateLessThan(
        initializeStateLastUpdate: LocalDateTime
    ): List<UzumAccountEntityJoinAccountEntity> {
        val a = ACCOUNT
        val k = KE_ACCOUNT
        val records = dsl.selectFrom(k.join(a).on(a.ID.eq(k.ACCOUNT_ID)))
            .where(
                k.INITIALIZE_STATE.eq(InitializeState.in_progress)
                    .and(k.INITIALIZE_STATE_LAST_UPDATE.lessThan(initializeStateLastUpdate))
            )
            .fetch()
        return records.map { recordToUzumAccountEntityJoinAccountEntityMapper.convert(it) }
    }

}
