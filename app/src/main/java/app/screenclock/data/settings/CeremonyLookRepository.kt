package app.screenclock.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.screenclock.domain.occasion.CeremonyLook
import app.screenclock.domain.occasion.CeremonyStyle
import app.screenclock.domain.occasion.CeremonyTone
import app.screenclock.domain.occasion.normalizeAutoPlaySeconds
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val StyleKey = stringPreferencesKey("ceremony_style")
private val ToneKey = stringPreferencesKey("ceremony_tone")
private val AutoPlayKey = intPreferencesKey("ceremony_auto_play_seconds")

/** Which design and which light/dark tone the countdown page uses. */
class CeremonyLookRepository(private val context: Context) {
    val look: Flow<CeremonyLook> = context.clockSettingsStore.data
        .map { prefs ->
            CeremonyLook(
                style = CeremonyStyle.fromStored(prefs[StyleKey]),
                tone = CeremonyTone.fromStored(prefs[ToneKey]),
                autoPlaySeconds = normalizeAutoPlaySeconds(prefs[AutoPlayKey]),
            )
        }
        .distinctUntilChanged()
        .catch { error -> if (error is IOException) emit(CeremonyLook()) else throw error }

    suspend fun setStyle(style: CeremonyStyle) {
        context.clockSettingsStore.edit { prefs -> prefs[StyleKey] = style.name }
    }

    suspend fun setAutoPlaySeconds(seconds: Int) {
        val safe = normalizeAutoPlaySeconds(seconds)
        context.clockSettingsStore.edit { prefs -> prefs[AutoPlayKey] = safe }
    }

    suspend fun setTone(tone: CeremonyTone) {
        context.clockSettingsStore.edit { prefs -> prefs[ToneKey] = tone.name }
    }
}
