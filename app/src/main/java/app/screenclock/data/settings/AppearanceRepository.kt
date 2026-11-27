package app.screenclock.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.screenclock.domain.appearance.AppearanceTone
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** The key the countdown page already used for its tone; reusing it avoids a migration. */
private val ToneKey = stringPreferencesKey("ceremony_tone")

/** The app-wide light/dark choice. */
class AppearanceRepository(private val context: Context) {
    val tone: Flow<AppearanceTone> = context.clockSettingsStore.data
        .map { prefs -> AppearanceTone.fromStored(prefs[ToneKey]) }
        .distinctUntilChanged()
        .catch { error -> if (error is IOException) emit(AppearanceTone.FollowSystem) else throw error }

    suspend fun setTone(tone: AppearanceTone) {
        context.clockSettingsStore.edit { prefs -> prefs[ToneKey] = tone.name }
    }
}
