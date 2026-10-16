package app.screenclock.data.settings

import app.screenclock.domain.time.ClockPreferences
import app.screenclock.domain.time.ClockThemeId
import app.screenclock.domain.time.HourMode
import kotlinx.coroutines.flow.Flow

/** Display settings persistence. Controllers depend on this, so tests can fake it. */
interface SettingsStore {
    val settings: Flow<ClockPreferences>
    suspend fun setHourMode(mode: HourMode)
    suspend fun setShowDate(show: Boolean)
    suspend fun setTheme(theme: ClockThemeId)
}
