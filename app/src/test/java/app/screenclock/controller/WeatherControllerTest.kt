package app.screenclock.controller

import app.screenclock.data.weather.CoarseLocator
import app.screenclock.data.weather.LatLon
import app.screenclock.data.weather.WeatherSource
import app.screenclock.data.weather.WeatherStore
import app.screenclock.domain.weather.CityCandidate
import app.screenclock.domain.weather.SavedCity
import app.screenclock.domain.weather.WeatherCache
import app.screenclock.domain.weather.WeatherPreferences
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeWeatherStore : WeatherStore {
    val state = MutableStateFlow(WeatherPreferences())
    override val weather: Flow<WeatherPreferences> = state

    override suspend fun setEnabled(enabled: Boolean) {
        state.value = state.value.copy(enabled = enabled)
    }

    override suspend fun setUseLocation(useLocation: Boolean) {
        state.value = state.value.copy(useLocation = useLocation)
    }

    override suspend fun replaceCity(city: SavedCity, useLocation: Boolean) {
        state.value = state.value.copy(city = city, useLocation = useLocation, cache = null)
    }

    override suspend fun saveCache(cache: WeatherCache) {
        state.value = state.value.copy(cache = cache)
    }
}

private class FakeWeatherSource : WeatherSource {
    val searches = mutableListOf<String>()
    val forecasts = mutableListOf<Pair<Double, Double>>()
    var searchResult: List<CityCandidate> = emptyList()
    var fail = false

    override fun search(rawQuery: String): List<CityCandidate> {
        searches += rawQuery
        if (fail) throw IOException("net")
        return searchResult
    }

    override fun forecast(latitude: Double, longitude: Double, fetchedAtEpochMillis: Long): WeatherCache {
        forecasts += latitude to longitude
        if (fail) throw IOException("net")
        return WeatherCache(temperatureC = 18, code = 0, highC = null, lowC = null, fetchedAtEpochMillis = fetchedAtEpochMillis)
    }
}

private class FakeLocator : CoarseLocator {
    var fix: LatLon? = null
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun currentCoarse(): LatLon? {
        gate?.await()
        return fix
    }
}

class WeatherControllerTest {
    private fun controller(
        store: FakeWeatherStore,
        source: FakeWeatherSource = FakeWeatherSource(),
        locator: FakeLocator = FakeLocator(),
        time: FakeClockTime = FakeClockTime(),
        foreground: Boolean = true,
    ): WeatherController {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        return WeatherController(scope, store, source, locator, time, io = Dispatchers.Unconfined)
            .also { if (foreground) it.setForeground(true) }
    }

    private val hangzhou = SavedCity("杭州 · 浙江", 30.27, 120.15)

    @Test
    fun aLocationFixLandingAfterTheSwitchIsOffKeepsTheCity() {
        val store = FakeWeatherStore()
        store.state.value = WeatherPreferences(enabled = true, city = hangzhou)
        val locator = FakeLocator().apply {
            fix = LatLon(31.2, 121.4)
            gate = CompletableDeferred()
        }
        val controller = controller(store, locator = locator)
        controller.useCurrentLocation("当前位置")
        controller.setUseLocation(false) // the user turns the switch off mid-fix
        locator.gate!!.complete(Unit)
        assertEquals(hangzhou, store.state.value.city)
        assertFalse(store.state.value.useLocation)
    }

    @Test
    fun aFixWhileTheSwitchStaysOnReplacesTheCity() {
        val store = FakeWeatherStore()
        store.state.value = WeatherPreferences(enabled = true, city = hangzhou)
        val locator = FakeLocator().apply { fix = LatLon(31.2, 121.4) }
        val source = FakeWeatherSource()
        val controller = controller(store, source = source, locator = locator)
        controller.useCurrentLocation("当前位置")
        assertEquals(SavedCity("当前位置", 31.2, 121.4), store.state.value.city)
        assertTrue(store.state.value.useLocation)
        assertTrue(source.forecasts.isNotEmpty())
    }

    @Test
    fun aSearchWithNoResultsSaysSoAndKeepsTheClockUntouched() {
        val store = FakeWeatherStore()
        store.state.value = WeatherPreferences(enabled = true, city = hangzhou)
        val source = FakeWeatherSource().apply { searchResult = emptyList() }
        val controller = controller(store, source = source)
        controller.searchCity("杭州")
        assertEquals(WeatherNotice.NotFound, controller.panel.value.notice)
        assertEquals(hangzhou, store.state.value.city)
    }

    @Test
    fun aFailingSearchSaysUnavailable() {
        val store = FakeWeatherStore()
        store.state.value = WeatherPreferences(enabled = true, city = hangzhou)
        val source = FakeWeatherSource().apply { fail = true }
        val controller = controller(store, source = source)
        controller.searchCity("杭州")
        assertEquals(WeatherNotice.Unavailable, controller.panel.value.notice)
    }

    @Test
    fun nothingIsRequestedWhileTheSwitchIsOff() {
        val store = FakeWeatherStore()
        val source = FakeWeatherSource()
        val controller = controller(store, source = source)
        controller.searchCity("杭州")
        controller.refreshIfNeeded()
        assertTrue(source.searches.isEmpty())
        assertTrue(source.forecasts.isEmpty())
    }

    @Test
    fun aFailedRefreshRetriesOnlyAfterOneCacheWindow() {
        val store = FakeWeatherStore()
        store.state.value = WeatherPreferences(enabled = true, city = hangzhou)
        val source = FakeWeatherSource().apply { fail = true }
        val time = FakeClockTime(nowMillis = 1_000L)
        val controller = controller(store, source = source, time = time)
        controller.refreshIfNeeded()
        assertEquals(1, source.forecasts.size)
        controller.refreshIfNeeded() // throttled: the attempt gate is still closed
        assertEquals(1, source.forecasts.size)
        time.nowMillis += 30L * 60_000L
        controller.refreshIfNeeded()
        assertEquals(2, source.forecasts.size)
    }

    @Test
    fun backgroundCrossingTtlMakesNoRequestsUntilVisibleAgain() {
        val store = FakeWeatherStore()
        val time = FakeClockTime(nowMillis = 1_000L)
        store.state.value = WeatherPreferences(
            enabled = true,
            city = hangzhou,
            cache = WeatherCache(18, 0, null, null, time.nowMillis),
        )
        val source = FakeWeatherSource()
        val controller = controller(store, source = source, time = time)
        assertTrue(source.forecasts.isEmpty())
        controller.setForeground(false)
        time.nowMillis += 31L * 60_000L
        controller.refreshIfNeeded()
        controller.refreshNow()
        controller.searchCity("杭州")
        assertTrue(source.forecasts.isEmpty())
        assertTrue(source.searches.isEmpty())
        controller.setForeground(true)
        assertEquals(1, source.forecasts.size)
        controller.refreshIfNeeded()
        assertEquals(1, source.forecasts.size)
    }

    @Test
    fun foregroundReturnRetriesAFailedRequestBeforeItsAttemptTtl() {
        val store = FakeWeatherStore()
        store.state.value = WeatherPreferences(enabled = true, city = hangzhou)
        val source = FakeWeatherSource().apply { fail = true }
        val controller = controller(store, source = source)
        assertEquals(1, source.forecasts.size)
        controller.setForeground(false)
        controller.setForeground(true)
        assertEquals(2, source.forecasts.size)
    }

    @Test
    fun comingBackToTheFrontMayRetryAtOnce() {
        val store = FakeWeatherStore()
        store.state.value = WeatherPreferences(enabled = true, city = hangzhou)
        val source = FakeWeatherSource().apply { fail = true }
        val controller = controller(store, source = source)
        controller.refreshIfNeeded()
        assertEquals(1, source.forecasts.size)
        controller.notifyForeground()
        assertEquals(2, source.forecasts.size)
    }
}
