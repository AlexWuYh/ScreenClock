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
    @Volatile private var foreground = false

    /** ON_START/ON_STOP visibility. Foreground return checks expiry immediately. */
    fun setForeground(visible: Boolean) {
        if (foreground == visible) return
        foreground = visible
        if (visible) {
            notifyForeground()
        } else {
            refreshJob?.cancel()
            searchJob?.cancel()
            panelState.value = panelState.value.copy(busy = false)
        }
    }

    /** Called once per minute and on foreground. Cheap while the cache is fresh. */
    fun refreshIfNeeded() {
        if (!foreground || refreshJob?.isActive == true) return
        refreshJob = scope.launch { fetchForecast(force = false) }
    }

    /** User-driven: picking a city or enabling the switch. Bypasses the attempt gate. */
    fun refreshNow() {
        if (!foreground) return
        refreshJob?.cancel()
        refreshJob = scope.launch { fetchForecast(force = true) }
    }

    /** Returning to the front may retry at once; only the minute loop is throttled. */
    fun notifyForeground() {
        if (!foreground) return
        weatherAttemptAtEpochMillis = null
        refreshIfNeeded()
    }

    fun setEnabled(enabled: Boolean) {
        searchJob?.cancel()
        panelState.value = WeatherPanel()
        scope.launchPersisting {
            store.setEnabled(enabled)
            if (enabled) refreshIfNeeded() else refreshJob?.cancel()
        }
    }

    /** Turning this off keeps the saved city. Turning it on goes through [useCurrentLocation]. */
    fun setUseLocation(enabled: Boolean) {
        if (enabled) return
        scope.launchPersisting { store.setUseLocation(false) }
    }

    fun onLocationDenied() {
        scope.launchPersisting { store.setUseLocation(false) }
        panelState.value = panelState.value.copy(busy = false, notice = WeatherNotice.NoPermission)
    }

    fun useCurrentLocation(hereLabel: String) {
        if (panelState.value.busy) return
        searchJob?.cancel()
        // Set before the first suspension so a second tap in the same frame is rejected.
        panelState.value = panelState.value.copy(
            busy = true,
            notice = WeatherNotice.None,
            results = emptyList(),
        )
        scope.launchPersisting {
            val enabled = store.weather.first().enabled
            if (!enabled) {
                panelState.value = WeatherPanel()
                return@launchPersisting
            }
            store.setUseLocation(true)
            val fix = locator.currentCoarse()
            val afterFix = store.weather.first()
            if (!afterFix.enabled) {
                store.setUseLocation(false)
                panelState.value = WeatherPanel()
                return@launchPersisting
            }
            // The switch may have been turned off while the fix was in flight.
            // That choice wins: no city is replaced behind the user's back.
            if (!afterFix.useLocation) {
                panelState.value = WeatherPanel()
                return@launchPersisting
            }
            val label = hereLabel.trim()
            if (fix == null || label.isEmpty() || !coordinateOk(fix.latitude, fix.longitude)) {
                store.setUseLocation(false)
                panelState.value = panelState.value.copy(busy = false, notice = WeatherNotice.NoLocation)
                return@launchPersisting
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
        if (!foreground) return
        val query = normalizeCityQuery(raw) ?: return
        if (panelState.value.busy) return
        searchJob?.cancel()
        searchJob = scope.launch {
            if (!foreground || !store.weather.first().enabled) return@launch
            panelState.value = panelState.value.copy(
                busy = true,
                results = emptyList(),
                notice = WeatherNotice.None,
            )
            try {
                val places = withContext(io) {
                    coroutineContext.ensureActive()
                    if (!foreground) return@withContext emptyList<CityCandidate>()
                    source.search(query)
                }
                coroutineContext.ensureActive()
                if (!foreground || !store.weather.first().enabled) return@launch
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
        scope.launchPersisting {
            if (!store.weather.first().enabled) return@launchPersisting
            store.replaceCity(
                SavedCity(city.label, city.latitude, city.longitude),
                useLocation = false,
            )
            panelState.value = WeatherPanel()
            refreshNow()
        }
    }

    private suspend fun fetchForecast(force: Boolean) {
        if (!foreground) return
        val prefs = store.weather.first()
        val city = prefs.city
        val nowMillis = time.nowMillis()
        val want = if (force) {
            prefs.enabled && city != null
        } else {
            shouldFetchWeather(prefs.enabled, city, prefs.cache, nowMillis) &&
                weatherAttemptAllowed(weatherAttemptAtEpochMillis, nowMillis)
        }
        if (!foreground || !want || city == null) return
        weatherAttemptAtEpochMillis = nowMillis
        try {
            val cache = withContext(io) {
                coroutineContext.ensureActive()
                if (!foreground) return@withContext null
                source.forecast(city.latitude, city.longitude, time.nowMillis())
            } ?: return
            coroutineContext.ensureActive()
            if (!foreground) return
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
