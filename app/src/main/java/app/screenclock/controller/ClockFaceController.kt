package app.screenclock.controller

import app.screenclock.data.settings.SettingsStore
import app.screenclock.data.occasion.SixTailCalendar
import app.screenclock.data.occasion.statutoryHolidayName
import app.screenclock.data.weather.WeatherStore
import app.screenclock.domain.time.ClockPreferences
import app.screenclock.domain.time.ClockThemeId
import app.screenclock.domain.time.ColorwayId
import app.screenclock.domain.time.DateParts
import app.screenclock.domain.time.FaceTime
import app.screenclock.domain.time.HourMode
import app.screenclock.domain.time.displayMinute
import app.screenclock.domain.time.formatDateLine
import app.screenclock.domain.time.formatFaceTime
import app.screenclock.domain.time.millisUntilNextMinute
import app.screenclock.domain.weather.WeatherCache
import app.screenclock.domain.weather.WeatherPreferences
import java.time.LocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** What every theme draws: time, date, and the optional weather cache. */
data class ClockUiState(
    val face: FaceTime,
    val dateLine: String?,
    val hourMode: HourMode,
    val showDate: Boolean,
    val showYear: Boolean,
    val showWeekday: Boolean,
    val showLunarDate: Boolean,
    val showHoliday: Boolean,
    val theme: ClockThemeId,
    val colorway: ColorwayId,
    val weatherEnabled: Boolean = false,
    val useLocation: Boolean = false,
    val cityLabel: String? = null,
    val weather: WeatherCache? = null,
)

/**
 * The minute-aligned clock face. It wakes once per minute boundary, never per second,
 * and hands themes a ready [ClockUiState]. The weather slot only shows a cache the
 * weather controller left behind; failures simply keep the slot empty.
 */
class ClockFaceController(
    private val scope: CoroutineScope,
    private val settings: SettingsStore,
    private val weather: WeatherStore,
    private val time: ClockTime,
    private val systemIs24HourNow: () -> Boolean,
    private val onMinute: () -> Unit = {},
) {
    private val minuteState = MutableStateFlow(displayMinute(time.now()))
    val minute: StateFlow<LocalDateTime> = minuteState.asStateFlow()

    private val minuteOfDayState = MutableStateFlow(minuteOfDay(time.now()))
    val minuteOfDay: StateFlow<Int> = minuteOfDayState.asStateFlow()

    private val systemIs24Hour = MutableStateFlow(systemIs24HourNow())
    private val refresh = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val clockState = MutableStateFlow<ClockUiState?>(null)
    val clock: StateFlow<ClockUiState?> = clockState.asStateFlow()

    init {
        scope.launch {
            combine(
                settings.settings,
                weather.weather,
                minute,
                systemIs24Hour,
            ) { prefs, weatherPrefs, shown, is24 ->
                clockFace(prefs, weatherPrefs, shown, is24)
            }.collect { clockState.value = it }
        }
        scope.launch {
            while (isActive) {
                val now = time.now()
                minuteState.value = displayMinute(now)
                minuteOfDayState.value = minuteOfDay(now)
                onMinute()
                withTimeoutOrNull(millisUntilNextMinute(now)) {
                    refresh.first()
                }
            }
        }
    }

    /** System hour format changed, or the wall clock moved. Restarts the minute wait. */
    fun notifyTimeChanged() {
        systemIs24Hour.value = systemIs24HourNow()
        refresh.tryEmit(Unit)
    }

    private fun minuteOfDay(time: LocalDateTime): Int = time.hour * 60 + time.minute

    private fun clockFace(
        prefs: ClockPreferences,
        weatherPrefs: WeatherPreferences,
        shown: LocalDateTime,
        systemIs24Hour: Boolean,
    ): ClockUiState {
        return ClockUiState(
            face = formatFaceTime(shown.hour, shown.minute, prefs.hourMode, systemIs24Hour),
            dateLine = if (prefs.showDate) {
                val today = shown.toLocalDate()
                formatDateLine(
                    date = today,
                    lunarLabel = if (prefs.showLunarDate) SixTailCalendar.lunarDateLabel(today) else null,
                    holidayName = if (prefs.showHoliday) statutoryHolidayName(today) else null,
                    parts = DateParts(
                        year = prefs.showYear,
                        weekday = prefs.showWeekday,
                        lunar = prefs.showLunarDate,
                        holiday = prefs.showHoliday,
                    ),
                )
            } else {
                null
            },
            hourMode = prefs.hourMode,
            showDate = prefs.showDate,
            showYear = prefs.showYear,
            showWeekday = prefs.showWeekday,
            showLunarDate = prefs.showLunarDate,
            showHoliday = prefs.showHoliday,
            theme = prefs.theme,
            colorway = prefs.colorway,
            weatherEnabled = weatherPrefs.enabled,
            useLocation = weatherPrefs.useLocation,
            cityLabel = weatherPrefs.city?.label,
            weather = if (weatherPrefs.enabled) weatherPrefs.cache else null,
        )
    }
}
