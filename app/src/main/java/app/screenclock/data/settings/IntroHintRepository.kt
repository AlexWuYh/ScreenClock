package app.screenclock.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val IntroSeenKey = booleanPreferencesKey("intro_hint_seen")

/** Remembers that the "tap to bring back the buttons" hint has already been shown once. */
class IntroHintRepository(private val context: Context) {
    val seen: Flow<Boolean> = context.clockSettingsStore.data
        .map { prefs -> prefs[IntroSeenKey] ?: false }
        .distinctUntilChanged()
        .catch { error ->
            // An unreadable store counts as seen: a hint must never nag on every launch.
            if (error is IOException) emit(true) else throw error
        }

    suspend fun markSeen() {
        context.clockSettingsStore.edit { prefs -> prefs[IntroSeenKey] = true }
    }
}
