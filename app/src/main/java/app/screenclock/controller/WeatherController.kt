package app.screenclock.controller

import app.screenclock.data.weather.CoarseLocator
import app.screenclock.data.weather.WeatherSource
import app.screenclock.data.weather.WeatherStore
import app.screenclock.domain.weather.CityCandidate
import app.screenclock.domain.weather.SavedCity
import app.screenclock.domain.weather.coordinateOk
import app.screenclock.domain.weather.normalizeCityQuery
import app.screenclock.domain.weather.shouldFetchWeather
import app.screenclock.domain.weather.weatherAttemptAllowed
import java.io.IOException
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class WeatherNotice {
    None,
    NotFound,
    Unavailable,
    NoPermission,
    NoLocation,
}

data class WeatherPanel(
    val notice: WeatherNotice = WeatherNotice.None,
    val results: List<CityCandidate> = emptyList(),
    val busy: Boolean = false,
)

/**
 * One city, one forecast. The minute loop notices a stale cache, because a bedside
 * clock may stay in front for hours; failed attempts retry no faster than once per
 * cache window so a dead network is not polled every minute.
 */
class WeatherController(
    private val scope: CoroutineScope,
    private val store: WeatherStore,
    private val source: WeatherSource,
    private val locator: CoarseLocator,
    private val time: ClockTime,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    private val panelState = MutableStateFlow(WeatherPanel())
    val panel: StateFlow<WeatherPanel> = panelState.asStateFlow()

    private var refreshJob: Job? = null
    private var searchJob: Job? = null
    private var weatherAttemptAtEpochMillis: Long? = null

    /** Called once per minute and on foreground. Cheap while the cache is fresh. */
    fun refreshIfNeeded() {
        if (refreshJob?.isActive == true) return
        refreshJob = scope.launch { fetchForecast(force = false) }
    }

    /** User-driven: picking a city or enabling the switch. Bypasses the attempt gate. */
    fun refreshNow() {
        refreshJob?.cancel()
        refreshJob = scope.launch { fetchForecast(force = true) }
    }

    /** Returning to the front may retry at once; only the minute loop is throttled. */
    fun notifyForeground() {
        weatherAttemptAtEpochMillis = null
        refreshIfNeeded()
    }

    fun setEnabled(enabled: Boolean) {
        searchJob?.cancel()
        panelState.value = WeatherPanel()
        scope.launch {
            store.setEnabled(enabled)
            if (enabled) refreshIfNeeded() else refreshJob?.cancel()
        }
    }

    /** Turning this off keeps the saved city. Turning it on goes through [useCurrentLocation]. */
    fun setUseLocation(enabled: Boolean) {
        if (enabled) return
        scope.launch { store.setUseLocation(false) }
    }

    fun onLocationDenied() {
        scope.launch { store.setUseLocation(false) }
        panelState.value = panelState.value.copy(busy = false, notice = WeatherNotice.NoPermission)
    }

    fun useCurrentLocation(hereLabel: String) {
        if (panelState.value.busy) return
        searchJob?.cancel()
        scope.launch {
            val enabled = store.weather.first().enabled
            if (!enabled) return@launch
            store.setUseLocation(true)
            panelState.value = panelState.value.copy(
                busy = true,
                notice = WeatherNotice.None,
                results = emptyList(),
            )
            val fix = locator.currentCoarse()
            val afterFix = store.weather.first()
            if (!afterFix.enabled) {
                store.setUseLocation(false)
                panelState.value = WeatherPanel()
                return@launch
            }
            // The switch may have been turned off while the fix was in flight.
            // That choice wins: no city is replaced behind the user's back.
            if (!afterFix.useLocation) {
                panelState.value = WeatherPanel()
                return@launch
            }
            val label = hereLabel.trim()
            if (fix == null || label.isEmpty() || !coordinateOk(fix.latitude, fix.longitude)) {
                store.setUseLocation(false)
                panelState.value = panelState.value.copy(busy = false, notice = WeatherNotice.NoLocation)
                return@launch
            }
            store.replaceCity(
                SavedCity(label, fix.latitude, fix.longitude),
                useLocation = true,
            )
            panelState.value = WeatherPanel()
            refreshNow()
        }
    }

    fun searchCity(raw: String) {
        val query = normalizeCityQuery(raw) ?: return
        if (panelState.value.busy) return
        searchJob?.cancel()
        searchJob = scope.launch {
            if (!store.weather.first().enabled) return@launch
            panelState.value = panelState.value.copy(
                busy = true,
                results = emptyList(),
                notice = WeatherNotice.None,
            )
            try {
                val places = withContext(io) { source.search(query) }
                coroutineContext.ensureActive()
                if (!store.weather.first().enabled) return@launch
                panelState.value = if (places.isEmpty()) {
                    WeatherPanel(notice = WeatherNotice.NotFound)
                } else {
                    WeatherPanel(results = places)
                }
            } catch (_: IOException) {
                if (!store.weather.first().enabled) return@launch
                panelState.value = panelState.value.copy(busy = false, notice = WeatherNotice.Unavailable)
            }
        }
    }

    fun selectCity(city: CityCandidate) {
        if (!coordinateOk(city.latitude, city.longitude)) return
        searchJob?.cancel()
        scope.launch {
            if (!store.weather.first().enabled) return@launch
            store.replaceCity(
                SavedCity(city.label, city.latitude, city.longitude),
                useLocation = false,
            )
            panelState.value = WeatherPanel()
            refreshNow()
        }
    }

    private suspend fun fetchForecast(force: Boolean) {
        val prefs = store.weather.first()
        val city = prefs.city
        val nowMillis = time.nowMillis()
        val want = if (force) {
            prefs.enabled && city != null
        } else {
            shouldFetchWeather(prefs.enabled, city, prefs.cache, nowMillis) &&
                weatherAttemptAllowed(weatherAttemptAtEpochMillis, nowMillis)
        }
        if (!want || city == null) return
        weatherAttemptAtEpochMillis = nowMillis
        try {
            val cache = withContext(io) {
                source.forecast(city.latitude, city.longitude, time.nowMillis())
            }
            coroutineContext.ensureActive()
            val latest = store.weather.first()
            if (!latest.enabled || latest.city != city) return
            store.saveCache(cache)
            if (panelState.value.notice == WeatherNotice.Unavailable) {
                panelState.value = panelState.value.copy(notice = WeatherNotice.None)
            }
        } catch (_: IOException) {
            val latest = store.weather.first()
            if (!latest.enabled) return
            panelState.value = panelState.value.copy(busy = false, notice = WeatherNotice.Unavailable)
        }
    }
}
