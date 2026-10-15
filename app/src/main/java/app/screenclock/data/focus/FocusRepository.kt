package app.screenclock.data.focus

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import app.screenclock.data.settings.clockSettingsStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val EndKey = longPreferencesKey("focus_end_epoch_millis")

class FocusRepository(private val context: Context) {
    val endEpochMillis: Flow<Long?> = context.clockSettingsStore.data
        .map { prefs -> prefs[EndKey] }
        .catch { error ->
            if (error is IOException) emit(null) else throw error
        }

    suspend fun setEnd(endEpochMillis: Long) {
        context.clockSettingsStore.edit { prefs -> prefs[EndKey] = endEpochMillis }
    }

    suspend fun clear() {
        context.clockSettingsStore.edit { prefs -> prefs.remove(EndKey) }
    }
}
