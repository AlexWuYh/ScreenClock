package app.screenclock.data.burnin

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import app.screenclock.data.settings.clockSettingsStore
import app.screenclock.domain.burnin.BurnInPreferences
import app.screenclock.domain.burnin.burnInPreferences
import app.screenclock.domain.burnin.normalizeIdleMillis
import app.screenclock.domain.burnin.normalizeRunMillis
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val EnabledKey = booleanPreferencesKey("burnin_enabled")
private val IdleKey = longPreferencesKey("burnin_idle_millis")
private val RunKey = longPreferencesKey("burnin_run_millis")

class BurnInRepository(private val context: Context) : BurnInStore {
    override val preferences: Flow<BurnInPreferences> = context.clockSettingsStore.data
        .map { prefs -> prefs.toBurnInPreferences() }
        .distinctUntilChanged()
        .catch { error ->
            if (error is IOException) emit(BurnInPreferences()) else throw error
        }

    override suspend fun setEnabled(enabled: Boolean) {
        context.clockSettingsStore.edit { prefs -> prefs[EnabledKey] = enabled }
    }

    override suspend fun setIdleMillis(idleMillis: Long) {
        val stored = normalizeIdleMillis(idleMillis)
        context.clockSettingsStore.edit { prefs -> prefs[IdleKey] = stored }
    }

    override suspend fun setRunMillis(runMillis: Long) {
        val stored = normalizeRunMillis(runMillis)
        context.clockSettingsStore.edit { prefs -> prefs[RunKey] = stored }
    }
}

private fun Preferences.toBurnInPreferences(): BurnInPreferences {
    return burnInPreferences(
        enabled = this[EnabledKey] ?: false,
        idleMillis = this[IdleKey] ?: BurnInPreferences().idleMillis,
        runMillis = this[RunKey] ?: BurnInPreferences().runMillis,
    )
}
