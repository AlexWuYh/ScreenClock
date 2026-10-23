package app.screenclock.data.focus

import app.screenclock.domain.focus.FocusPrefs
import app.screenclock.domain.focus.FocusSession
import app.screenclock.domain.focus.FocusStats
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/** Everything the focus page remembers between launches. */
data class FocusData(
    val prefs: FocusPrefs = FocusPrefs(),
    val session: FocusSession? = null,
    val stats: FocusStats = FocusStats(),
)

interface FocusStore {
    val data: Flow<FocusData>

    suspend fun savePrefs(prefs: FocusPrefs)

    /** Replaces the running or paused session. Null clears it. */
    suspend fun saveSession(session: FocusSession?)

    /** Replaces the session and adds [creditMillis] to [day]'s total in one write. */
    suspend fun saveSessionAndCredit(session: FocusSession?, creditMillis: Long, day: LocalDate)
}
