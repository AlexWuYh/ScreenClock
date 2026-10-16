package app.screenclock.data.burnin

import app.screenclock.domain.burnin.BurnInPreferences
import kotlinx.coroutines.flow.Flow

/** Burn-in settings persistence. The running phase is never stored. */
interface BurnInStore {
    val preferences: Flow<BurnInPreferences>
    suspend fun setEnabled(enabled: Boolean)
    suspend fun setIdleMillis(idleMillis: Long)
    suspend fun setRunMillis(runMillis: Long)
}
