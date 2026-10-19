package app.screenclock.data.settings

import app.screenclock.domain.time.ClockPreferences
import app.screenclock.domain.time.ClockThemeId
import app.screenclock.domain.time.ColorwayId
import app.screenclock.domain.time.DatePosition
import app.screenclock.domain.time.DigitSize
import app.screenclock.domain.time.HourMode
import kotlinx.coroutines.flow.Flow

/** Display settings persistence. Controllers depend on this, so tests can fake it. */
interface SettingsStore {
    val settings: Flow<ClockPreferences>
    suspend fun setHourMode(mode: HourMode)
    suspend fun setShowDate(show: Boolean)
    suspend fun setShowYear(show: Boolean)
    suspend fun setShowWeekday(show: Boolean)
    suspend fun setShowLunarDate(show: Boolean)
    suspend fun setShowHoliday(show: Boolean)
    suspend fun setTheme(theme: ClockThemeId)
    suspend fun setColorway(colorway: ColorwayId)
    suspend fun setDatePosition(position: DatePosition)
    suspend fun setDigitSize(size: DigitSize)
}
