package app.screenclock.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.screenclock.domain.time.ClockPreferences
import app.screenclock.domain.time.ClockThemeId
import app.screenclock.domain.time.ColorwayId
import app.screenclock.domain.time.HourMode
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

internal val Context.clockSettingsStore by preferencesDataStore(name = "clock_settings")

private val HourModeKey = stringPreferencesKey("hour_mode")
private val ShowDateKey = booleanPreferencesKey("show_date")
private val ShowYearKey = booleanPreferencesKey("show_year")
private val ShowWeekdayKey = booleanPreferencesKey("show_weekday")
private val ShowLunarKey = booleanPreferencesKey("show_lunar_date")
private val ShowHolidayKey = booleanPreferencesKey("show_holiday")
private val ThemeKey = stringPreferencesKey("theme")
private val ColorwayKey = stringPreferencesKey("colorway")

class SettingsRepository(private val context: Context) : SettingsStore {
    override val settings: Flow<ClockPreferences> = context.clockSettingsStore.data
        .map { prefs -> prefs.toClockPreferences() }
        .catch { error ->
            if (error is IOException) emit(ClockPreferences()) else throw error
        }

    override suspend fun setHourMode(mode: HourMode) {
        context.clockSettingsStore.edit { prefs -> prefs[HourModeKey] = mode.name }
    }

    override suspend fun setShowDate(show: Boolean) {
        context.clockSettingsStore.edit { prefs -> prefs[ShowDateKey] = show }
    }

    override suspend fun setShowYear(show: Boolean) {
        context.clockSettingsStore.edit { prefs -> prefs[ShowYearKey] = show }
    }

    override suspend fun setShowWeekday(show: Boolean) {
        context.clockSettingsStore.edit { prefs -> prefs[ShowWeekdayKey] = show }
    }

    override suspend fun setShowLunarDate(show: Boolean) {
        context.clockSettingsStore.edit { prefs -> prefs[ShowLunarKey] = show }
    }

    override suspend fun setShowHoliday(show: Boolean) {
        context.clockSettingsStore.edit { prefs -> prefs[ShowHolidayKey] = show }
    }

    override suspend fun setTheme(theme: ClockThemeId) {
        context.clockSettingsStore.edit { prefs ->
            prefs[ThemeKey] = theme.name
            if (prefs[ColorwayKey] == null) {
                prefs[ColorwayKey] = ColorwayId.InkBlack.name
            }
        }
    }

    override suspend fun setColorway(colorway: ColorwayId) {
        context.clockSettingsStore.edit { prefs -> prefs[ColorwayKey] = colorway.name }
    }
}

private fun Preferences.toClockPreferences(): ClockPreferences {
    return ClockPreferences(
        hourMode = HourMode.fromStored(this[HourModeKey]),
        showDate = this[ShowDateKey] ?: true,
        showYear = this[ShowYearKey] ?: true,
        showWeekday = this[ShowWeekdayKey] ?: true,
        showLunarDate = this[ShowLunarKey] ?: true,
        showHoliday = this[ShowHolidayKey] ?: true,
        theme = ClockThemeId.fromStored(this[ThemeKey]),
        colorway = ColorwayId.fromStored(this[ColorwayKey]),
    )
}
