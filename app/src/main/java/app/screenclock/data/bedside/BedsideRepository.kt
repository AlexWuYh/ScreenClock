package app.screenclock.data.bedside

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import app.screenclock.data.settings.clockSettingsStore
import app.screenclock.domain.bedside.BedsidePreferences
import app.screenclock.domain.bedside.NIGHT_END_DEFAULT
import app.screenclock.domain.bedside.NIGHT_START_DEFAULT
import app.screenclock.domain.bedside.bedsidePreferences
import app.screenclock.domain.bedside.normalizeHour
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val DimKey = booleanPreferencesKey("night_dim_enabled")
private val StartKey = intPreferencesKey("night_dim_start_hour")
private val EndKey = intPreferencesKey("night_dim_end_hour")
private val WarmKey = booleanPreferencesKey("night_warm")
private val ChargeKey = booleanPreferencesKey("charge_keep_awake")

class BedsideRepository(private val context: Context) {
    val preferences: Flow<BedsidePreferences> = context.clockSettingsStore.data
        .map { prefs -> prefs.toBedsidePreferences() }
        .catch { error ->
            if (error is IOException) emit(BedsidePreferences()) else throw error
        }

    suspend fun setDimEnabled(enabled: Boolean) {
        context.clockSettingsStore.edit { prefs -> prefs[DimKey] = enabled }
    }

    suspend fun setStartHour(hour: Int) {
        val stored = normalizeHour(hour, NIGHT_START_DEFAULT)
        context.clockSettingsStore.edit { prefs -> prefs[StartKey] = stored }
    }

    suspend fun setEndHour(hour: Int) {
        val stored = normalizeHour(hour, NIGHT_END_DEFAULT)
        context.clockSettingsStore.edit { prefs -> prefs[EndKey] = stored }
    }

    suspend fun setWarmEnabled(enabled: Boolean) {
        context.clockSettingsStore.edit { prefs -> prefs[WarmKey] = enabled }
    }

    suspend fun setChargeKeepAwake(enabled: Boolean) {
        context.clockSettingsStore.edit { prefs -> prefs[ChargeKey] = enabled }
    }
}

private fun Preferences.toBedsidePreferences(): BedsidePreferences {
    return bedsidePreferences(
        dimEnabled = this[DimKey] ?: false,
        startHour = this[StartKey] ?: NIGHT_START_DEFAULT,
        endHour = this[EndKey] ?: NIGHT_END_DEFAULT,
        warmEnabled = this[WarmKey] ?: false,
        chargeKeepAwake = this[ChargeKey] ?: false,
    )
}
