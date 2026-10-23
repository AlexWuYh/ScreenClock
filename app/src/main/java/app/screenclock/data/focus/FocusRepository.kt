package app.screenclock.data.focus

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.screenclock.data.settings.clockSettingsStore
import app.screenclock.domain.focus.FocusMode
import app.screenclock.domain.focus.FocusPrefs
import app.screenclock.domain.focus.FocusRun
import app.screenclock.domain.focus.FocusSession
import app.screenclock.domain.focus.FocusStats
import app.screenclock.domain.focus.addFocusMillis
import app.screenclock.domain.focus.normalizeFocusMinutes
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val ModeKey = stringPreferencesKey("focus_mode")
private val CustomKey = intPreferencesKey("focus_custom_minutes")
private val RunKey = stringPreferencesKey("focus_run")
private val SessionModeKey = stringPreferencesKey("focus_session_mode")
private val TotalKey = longPreferencesKey("focus_total_millis")
private val EndKey = longPreferencesKey("focus_end_millis")
private val LeftKey = longPreferencesKey("focus_left_millis")
private val StatsDateKey = stringPreferencesKey("focus_stats_date")
private val StatsMillisKey = longPreferencesKey("focus_stats_millis")

// The first design kept a bare end moment here. It means nothing to the new model.
private val LegacyEndKey = longPreferencesKey("focus_end_epoch_millis")

class FocusRepository(private val context: Context) : FocusStore {
    override val data: Flow<FocusData> = context.clockSettingsStore.data
        .map { prefs -> prefs.toFocusData() }
        .distinctUntilChanged()
        .catch { error -> if (error is IOException) emit(FocusData()) else throw error }

    override suspend fun savePrefs(prefs: FocusPrefs) {
        context.clockSettingsStore.edit { stored ->
            stored[ModeKey] = prefs.mode.name
            stored[CustomKey] = normalizeFocusMinutes(prefs.customMinutes)
        }
    }

    override suspend fun saveSession(session: FocusSession?) {
        context.clockSettingsStore.edit { stored -> stored.writeSession(session) }
    }

    override suspend fun saveSessionAndCredit(session: FocusSession?, creditMillis: Long, day: LocalDate) {
        context.clockSettingsStore.edit { stored ->
            stored.writeSession(session)
            if (creditMillis > 0L) {
                val next = addFocusMillis(stored.readStats(), day, creditMillis)
                stored[StatsDateKey] = next.date.toString()
                stored[StatsMillisKey] = next.millis
            }
        }
    }
}

private fun MutablePreferences.writeSession(session: FocusSession?) {
    remove(LegacyEndKey)
    remove(EndKey)
    remove(LeftKey)
    if (session == null) {
        remove(RunKey)
        remove(SessionModeKey)
        remove(TotalKey)
        return
    }
    this[SessionModeKey] = session.mode.name
    this[TotalKey] = session.totalMillis
    when (val run = session.run) {
        is FocusRun.Running -> {
            this[RunKey] = "running"
            this[EndKey] = run.endEpochMillis
        }
        is FocusRun.Paused -> {
            this[RunKey] = "paused"
            this[LeftKey] = run.leftMillis
        }
        FocusRun.Done -> this[RunKey] = "done"
    }
}

private fun Preferences.readStats(): FocusStats {
    val date = get(StatsDateKey)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    return FocusStats(date, get(StatsMillisKey) ?: 0L)
}

private fun Preferences.toFocusData(): FocusData {
    val prefs = FocusPrefs(
        mode = FocusMode.fromStored(get(ModeKey)),
        customMinutes = normalizeFocusMinutes(get(CustomKey) ?: 30),
    )
    val total = get(TotalKey)
    val run = when (get(RunKey)) {
        "running" -> get(EndKey)?.takeIf { it > 0L }?.let { FocusRun.Running(it) }
        "paused" -> get(LeftKey)?.takeIf { it >= 0L }?.let { FocusRun.Paused(it) }
        "done" -> FocusRun.Done
        else -> null
    }
    // A half-written session is dropped rather than guessed at.
    val session = if (run != null && total != null && total > 0L) {
        FocusSession(FocusMode.fromStored(get(SessionModeKey)), total, run)
    } else {
        null
    }
    return FocusData(prefs, session, readStats())
}
