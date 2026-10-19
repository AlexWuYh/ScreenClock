package app.screenclock.controller

import app.screenclock.data.occasion.SixTailCalendar
import app.screenclock.data.settings.SettingsStore
import app.screenclock.data.weather.WeatherStore
import app.screenclock.domain.time.ClockPreferences
import app.screenclock.domain.time.ClockThemeId
import app.screenclock.domain.time.ColorwayId
import app.screenclock.domain.time.DatePosition
import app.screenclock.domain.time.DigitSize
import app.screenclock.domain.time.HourMode
import app.screenclock.domain.weather.SavedCity
import app.screenclock.domain.weather.WeatherCache
import java.time.LocalDate
import app.screenclock.domain.weather.WeatherPreferences
import java.time.LocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeSettingsStore : SettingsStore {
    val state = MutableStateFlow(ClockPreferences())
    override val settings: Flow<ClockPreferences> = state

    override suspend fun setHourMode(mode: HourMode) {
        state.value = state.value.copy(hourMode = mode)
    }

    override suspend fun setShowDate(show: Boolean) {
        state.value = state.value.copy(showDate = show)
    }

    override suspend fun setShowYear(show: Boolean) {
        state.value = state.value.copy(showYear = show)
    }

    override suspend fun setShowWeekday(show: Boolean) {
        state.value = state.value.copy(showWeekday = show)
    }

    override suspend fun setShowLunarDate(show: Boolean) {
        state.value = state.value.copy(showLunarDate = show)
    }

    override suspend fun setShowHoliday(show: Boolean) {
        state.value = state.value.copy(showHoliday = show)
    }

    override suspend fun setTheme(theme: ClockThemeId) {
        state.value = state.value.copy(theme = theme)
    }

    override suspend fun setColorway(colorway: ColorwayId) {
        state.value = state.value.copy(colorway = colorway)
    }

    override suspend fun setDatePosition(position: DatePosition) {
        state.value = state.value.copy(datePosition = position)
    }

    override suspend fun setDigitSize(size: DigitSize) {
        state.value = state.value.copy(digitSize = size)
    }
}

private class FaceWeatherStore : WeatherStore {
    val state = MutableStateFlow(WeatherPreferences())
    override val weather: Flow<WeatherPreferences> = state

    override suspend fun setEnabled(enabled: Boolean) {
        state.value = state.value.copy(enabled = enabled)
    }

    override suspend fun setUseLocation(useLocation: Boolean) {
        state.value = state.value.copy(useLocation = useLocation)
    }

    override suspend fun replaceCity(city: SavedCity, useLocation: Boolean) {
        state.value = state.value.copy(city = city, useLocation = useLocation)
    }

    override suspend fun saveCache(cache: WeatherCache) {
        state.value = state.value.copy(cache = cache)
    }
}

class ClockFaceControllerTest {
    private val morning = LocalDateTime.of(2026, 10, 16, 14, 30)

    private fun controller(
        settings: FakeSettingsStore = FakeSettingsStore(),
        weather: FaceWeatherStore = FaceWeatherStore(),
        onMinute: () -> Unit = {},
    ): ClockFaceController {
        return ClockFaceController(
            scope = CoroutineScope(Dispatchers.Unconfined),
            settings = settings,
            weather = weather,
            time = FakeClockTime(now = morning),
            systemIs24HourNow = { true },
            onMinute = onMinute,
        )
    }

    @Test
    fun theFaceFollowsSettingsAndTheMinute() {
        val controller = controller()
        val state = requireNotNull(controller.clock.value)
        assertEquals("14", state.face.hourDigits)
        assertEquals("30", state.face.minuteDigits)
        assertNotNull(state.dateLine)
        assertEquals(morning, controller.minute.value)
    }

    @Test
    fun layoutChoicesReachTheClockFace() {
        val settings = FakeSettingsStore().apply {
            state.value = ClockPreferences(datePosition = DatePosition.Below, digitSize = DigitSize.Large)
        }
        val state = requireNotNull(controller(settings = settings).clock.value)
        assertEquals(DatePosition.Below, state.datePosition)
        assertEquals(DigitSize.Large, state.digitSize)
    }

    @Test
    fun theDateLineHidesWhenTurnedOff() {
        val settings = FakeSettingsStore().apply { state.value = ClockPreferences(showDate = false) }
        assertNull(controller(settings = settings).clock.value?.dateLine)
    }

    @Test
    fun theDateLinePartsFollowTheirSwitches() {
        val bare = FakeSettingsStore().apply {
            state.value = ClockPreferences(
                showDate = true,
                showWeekday = false,
                showLunarDate = false,
                showHoliday = false,
            )
        }
        assertEquals("2026年10月16日", controller(settings = bare).clock.value?.dateLine)

        val full = FakeSettingsStore()
        val lunar = SixTailCalendar.lunarDateLabel(LocalDate.of(2026, 10, 16))
        assertEquals("2026年10月16日 $lunar 星期五", controller(settings = full).clock.value?.dateLine)
    }

    @Test
    fun theWeatherSlotOnlyComesFromAnEnabledCache() {
        val weather = FaceWeatherStore()
        val cache = WeatherCache(temperatureC = 18, code = 0, highC = null, lowC = null, fetchedAtEpochMillis = 1L)
        weather.state.value = WeatherPreferences(enabled = true, city = SavedCity("杭州", 30.0, 120.0), cache = cache)
        val controller = controller(weather = weather)
        assertEquals(cache, controller.clock.value?.weather)
        weather.state.value = weather.state.value.copy(enabled = false)
        assertNull(controller.clock.value?.weather)
    }

    @Test
    fun theMinuteLoopHandsControlBackEachMinute() {
        var ticks = 0
        controller(onMinute = { ticks++ })
        // The loop runs its first pass at once, then waits for the minute boundary.
        assertTrue(ticks >= 1)
    }
}
