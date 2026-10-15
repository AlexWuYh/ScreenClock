package app.screenclock.ui.clock

import android.app.Application
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.text.format.DateFormat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.screenclock.data.burnin.BurnInRepository
import app.screenclock.data.focus.FocusRepository
import app.screenclock.data.settings.SettingsRepository
import app.screenclock.data.weather.AndroidCoarseLocator
import app.screenclock.data.weather.CoarseLocator
import app.screenclock.data.weather.OpenMeteoClient
import app.screenclock.data.weather.WeatherRepository
import app.screenclock.domain.burnin.BurnInClock
import app.screenclock.domain.burnin.BurnInPhase
import app.screenclock.domain.burnin.BurnInPreferences
import app.screenclock.domain.burnin.burnInSetEnabled
import app.screenclock.domain.burnin.burnInSetHostHidden
import app.screenclock.domain.burnin.burnInSetPagePaused
import app.screenclock.domain.burnin.burnInTick
import app.screenclock.domain.burnin.burnInTouch
import app.screenclock.domain.burnin.freshBurnInClock
import app.screenclock.domain.burnin.millisUntilBurnInEdge
import app.screenclock.domain.focus.focusEndEpochMillis
import app.screenclock.domain.focus.storedFocusEnd
import app.screenclock.domain.time.ClockPreferences
import app.screenclock.domain.time.ClockThemeId
import app.screenclock.domain.time.ColorwayId
import app.screenclock.domain.time.FaceTime
import app.screenclock.domain.time.HourMode
import app.screenclock.domain.time.displayMinute
import app.screenclock.domain.time.formatDateLine
import app.screenclock.domain.time.formatFaceTime
import app.screenclock.domain.time.millisUntilNextMinute
import app.screenclock.domain.weather.CityCandidate
import app.screenclock.domain.weather.SavedCity
import app.screenclock.domain.weather.WeatherCache
import app.screenclock.domain.weather.WeatherPreferences
import app.screenclock.domain.weather.coordinateOk
import app.screenclock.domain.weather.normalizeCityQuery
import app.screenclock.domain.weather.shouldFetchWeather
import java.io.IOException
import java.time.LocalDateTime
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

enum class AppDestination {
    Clock,
    Settings,
    Focus,
}

data class ClockUiState(
    val face: FaceTime,
    val dateLine: String?,
    val hourMode: HourMode,
    val showDate: Boolean,
    val theme: ClockThemeId,
    val colorway: ColorwayId,
    val weatherEnabled: Boolean = false,
    val useLocation: Boolean = false,
    val cityLabel: String? = null,
    val weather: WeatherCache? = null,
)

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

class ClockViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application)
    private val weatherRepository = WeatherRepository(application)
    private val burnInRepository = BurnInRepository(application)
    private val focusRepository = FocusRepository(application)
    private val weatherClient = OpenMeteoClient()
    private val locator: CoarseLocator = AndroidCoarseLocator(application)

    private val chromeState = MutableStateFlow(ChromeState())
    val chrome: StateFlow<ChromeState> = chromeState.asStateFlow()

    private val destinationState = MutableStateFlow(AppDestination.Clock)
    val destination: StateFlow<AppDestination> = destinationState.asStateFlow()

    private val minute = MutableStateFlow(displayMinute(LocalDateTime.now()))
    private val systemIs24Hour = MutableStateFlow(DateFormat.is24HourFormat(application))
    private val refresh = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val clockState = MutableStateFlow<ClockUiState?>(null)
    val clock: StateFlow<ClockUiState?> = clockState.asStateFlow()

    private val panelState = MutableStateFlow(WeatherPanel())
    val weatherPanel: StateFlow<WeatherPanel> = panelState.asStateFlow()

    private val burnInPrefsState = MutableStateFlow(BurnInPreferences())
    val burnInPrefs: StateFlow<BurnInPreferences> = burnInPrefsState.asStateFlow()

    private val burnInState = MutableStateFlow(freshBurnInClock(enabled = false, nowMillis = 0L))
    val burnInSession: StateFlow<BurnInClock> = burnInState.asStateFlow()

    private val focusEndState = MutableStateFlow<Long?>(null)
    val focusEnd: StateFlow<Long?> = focusEndState.asStateFlow()

    private val focusKnownState = MutableStateFlow(false)
    val focusKnown: StateFlow<Boolean> = focusKnownState.asStateFlow()

    private var refreshJob: Job? = null
    private var searchJob: Job? = null
    private var burnInJob: Job? = null
    private var burnInPlaced = false
    private var pendingFocusEnd: Long? = null
    private var awaitFocusClear = false

    init {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                publishSystemHourFormat()
                refresh.tryEmit(Unit)
            }
        }
        application.contentResolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.TIME_12_24),
            false,
            observer,
        )
        addCloseable {
            application.contentResolver.unregisterContentObserver(observer)
        }
        viewModelScope.launch {
            combine(
                settingsRepository.settings,
                weatherRepository.weather,
                minute,
                systemIs24Hour,
            ) { prefs, weather, shown, is24 ->
                clockFace(prefs, weather, shown, is24)
            }.collect { clockState.value = it }
        }
        viewModelScope.launch {
            while (isActive) {
                val now = LocalDateTime.now()
                minute.value = displayMinute(now)
                withTimeoutOrNull(millisUntilNextMinute(now)) {
                    refresh.first()
                }
            }
        }
        refreshWeatherIfNeeded()
        viewModelScope.launch {
            burnInRepository.preferences.collect { prefs -> placeBurnIn(prefs) }
        }
        viewModelScope.launch {
            focusRepository.endEpochMillis.collect { stored -> placeFocus(stored) }
        }
    }

    /**
     * A press on the clock. Returns true when the press ends a running screensaver,
     * so that same tap should not also open the control layer.
     */
    fun onBurnInContact(): Boolean {
        val state = burnInState.value
        val dismiss = state.phase == BurnInPhase.Running && !state.paused
        val next = burnInTouch(state, elapsedNow())
        if (next != state) {
            burnInState.value = next
            rescheduleBurnIn()
        }
        return dismiss
    }

    fun onBlankTap(nowEpochMillis: Long) {
        chromeState.value = chromeState.value.onBlankTap(nowEpochMillis)
    }

    /** TalkBack activates the clock without a pointer press. */
    fun onClockActivate(nowEpochMillis: Long) {
        val dismiss = onBurnInContact()
        if (!dismiss) onBlankTap(nowEpochMillis)
    }

    fun onChromeTick(nowEpochMillis: Long) {
        chromeState.value = chromeState.value.after(nowEpochMillis)
    }

    fun openSettings() {
        onBurnInContact()
        setBurnInPagePaused(true)
        chromeState.value = ChromeState()
        destinationState.value = AppDestination.Settings
    }

    fun closeSettings() {
        destinationState.value = AppDestination.Clock
        setBurnInPagePaused(false)
    }

    fun openFocus() {
        onBurnInContact()
        setBurnInPagePaused(true)
        chromeState.value = ChromeState()
        destinationState.value = AppDestination.Focus
    }

    /** Leaves the duration picker. A running session stays until it is abandoned. */
    fun closeFocusPicker() {
        if (focusEndState.value != null) return
        destinationState.value = AppDestination.Clock
        setBurnInPagePaused(false)
    }

    fun startFocus(minutes: Int) {
        val end = focusEndEpochMillis(System.currentTimeMillis(), minutes)
        pendingFocusEnd = end
        awaitFocusClear = false
        focusEndState.value = end
        destinationState.value = AppDestination.Focus
        setBurnInPagePaused(true)
        viewModelScope.launch {
            try {
                focusRepository.setEnd(end)
            } catch (_: IOException) {
                if (pendingFocusEnd != end) return@launch
                pendingFocusEnd = null
                focusEndState.value = null
                destinationState.value = AppDestination.Clock
                setBurnInPagePaused(false)
            }
        }
    }

    /** Long-press cancel and the tap after zero both clear the stored end. */
    fun abandonFocus() {
        pendingFocusEnd = null
        awaitFocusClear = true
        focusEndState.value = null
        destinationState.value = AppDestination.Clock
        setBurnInPagePaused(false)
        viewModelScope.launch { focusRepository.clear() }
    }

    /** Settings and focus share this hold. Ceremony will use it too. */
    fun setBurnInPagePaused(paused: Boolean) {
        val next = burnInSetPagePaused(burnInState.value, paused, elapsedNow())
        if (next == burnInState.value) return
        burnInState.value = next
        rescheduleBurnIn()
    }

    /** The activity left the screen, or came back. */
    fun setBurnInHostHidden(hidden: Boolean) {
        val next = burnInSetHostHidden(burnInState.value, hidden, elapsedNow())
        if (next == burnInState.value) return
        burnInState.value = next
        rescheduleBurnIn()
    }

    fun setBurnInEnabled(enabled: Boolean) {
        viewModelScope.launch { burnInRepository.setEnabled(enabled) }
    }

    fun setBurnInIdleMillis(idleMillis: Long) {
        viewModelScope.launch { burnInRepository.setIdleMillis(idleMillis) }
    }

    fun setBurnInRunMillis(runMillis: Long) {
        viewModelScope.launch { burnInRepository.setRunMillis(runMillis) }
    }

    /** System clock or 12/24 setting changed. The minute loop reads the clock again. */
    fun onTimeChanged() {
        publishSystemHourFormat()
        refresh.tryEmit(Unit)
    }

    /** App is in front. Weather refresh stays off the minute loop. */
    fun onForeground() {
        onTimeChanged()
        refreshWeatherIfNeeded()
    }

    fun setHourMode(mode: HourMode) {
        viewModelScope.launch { settingsRepository.setHourMode(mode) }
    }

    fun setShowDate(show: Boolean) {
        viewModelScope.launch { settingsRepository.setShowDate(show) }
    }

    fun setTheme(theme: ClockThemeId) {
        viewModelScope.launch { settingsRepository.setTheme(theme) }
    }

    fun setWeatherEnabled(enabled: Boolean) {
        searchJob?.cancel()
        panelState.value = WeatherPanel()
        viewModelScope.launch {
            weatherRepository.setEnabled(enabled)
            if (enabled) refreshWeatherIfNeeded() else refreshJob?.cancel()
        }
    }

    /** Turning this off keeps the saved city. Turning it on goes through [useCurrentLocation]. */
    fun setUseLocation(enabled: Boolean) {
        if (enabled) return
        viewModelScope.launch { weatherRepository.setUseLocation(false) }
    }

    fun onLocationDenied() {
        viewModelScope.launch { weatherRepository.setUseLocation(false) }
        panelState.value = panelState.value.copy(busy = false, notice = WeatherNotice.NoPermission)
    }

    fun useCurrentLocation(hereLabel: String) {
        if (panelState.value.busy) return
        searchJob?.cancel()
        viewModelScope.launch {
            val enabled = weatherRepository.weather.first().enabled
            if (!enabled) return@launch
            weatherRepository.setUseLocation(true)
            panelState.value = panelState.value.copy(
                busy = true,
                notice = WeatherNotice.None,
                results = emptyList(),
            )
            val fix = locator.currentCoarse()
            if (!weatherRepository.weather.first().enabled) {
                weatherRepository.setUseLocation(false)
                panelState.value = WeatherPanel()
                return@launch
            }
            val label = hereLabel.trim()
            if (fix == null || label.isEmpty() || !coordinateOk(fix.latitude, fix.longitude)) {
                weatherRepository.setUseLocation(false)
                panelState.value = panelState.value.copy(busy = false, notice = WeatherNotice.NoLocation)
                return@launch
            }
            weatherRepository.replaceCity(
                SavedCity(label, fix.latitude, fix.longitude),
                useLocation = true,
            )
            panelState.value = WeatherPanel()
            refreshWeatherNow()
        }
    }

    fun searchCity(raw: String) {
        val query = normalizeCityQuery(raw) ?: return
        if (panelState.value.busy) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (!weatherRepository.weather.first().enabled) return@launch
            panelState.value = panelState.value.copy(
                busy = true,
                results = emptyList(),
                notice = WeatherNotice.None,
            )
            try {
                val places = withContext(Dispatchers.IO) { weatherClient.search(query) }
                coroutineContext.ensureActive()
                if (!weatherRepository.weather.first().enabled) return@launch
                panelState.value = if (places.isEmpty()) {
                    WeatherPanel(notice = WeatherNotice.NotFound)
                } else {
                    WeatherPanel(results = places)
                }
            } catch (_: IOException) {
                if (!weatherRepository.weather.first().enabled) return@launch
                panelState.value = panelState.value.copy(busy = false, notice = WeatherNotice.Unavailable)
            }
        }
    }

    fun selectCity(city: CityCandidate) {
        if (!coordinateOk(city.latitude, city.longitude)) return
        searchJob?.cancel()
        viewModelScope.launch {
            if (!weatherRepository.weather.first().enabled) return@launch
            weatherRepository.replaceCity(
                SavedCity(city.label, city.latitude, city.longitude),
                useLocation = false,
            )
            panelState.value = WeatherPanel()
            refreshWeatherNow()
        }
    }

    private fun placeBurnIn(prefs: BurnInPreferences) {
        val now = elapsedNow()
        val current = burnInState.value
        val next = if (!burnInPlaced) {
            burnInPlaced = true
            freshBurnInClock(prefs.enabled, now, current.pagePaused, current.hostHidden)
        } else if ((current.phase != BurnInPhase.Off) != prefs.enabled) {
            burnInSetEnabled(current, prefs.enabled, now)
        } else {
            current
        }
        burnInPrefsState.value = prefs
        burnInState.value = next
        rescheduleBurnIn()
    }

    private fun rescheduleBurnIn() {
        burnInJob?.cancel()
        burnInJob = viewModelScope.launch {
            while (isActive) {
                val prefs = burnInPrefsState.value
                val wait = millisUntilBurnInEdge(
                    burnInState.value,
                    prefs.idleMillis,
                    prefs.runMillis,
                    elapsedNow(),
                ) ?: return@launch
                delay(wait)
                val latest = burnInPrefsState.value
                val updated = burnInTick(
                    burnInState.value,
                    latest.idleMillis,
                    latest.runMillis,
                    elapsedNow(),
                )
                if (updated == burnInState.value) return@launch
                burnInState.value = updated
            }
        }
    }

    private fun placeFocus(stored: Long?) {
        val incoming = storedFocusEnd(stored)
        val pending = pendingFocusEnd
        if (pending != null) {
            if (incoming != pending) {
                focusKnownState.value = true
                return
            }
            pendingFocusEnd = null
        }
        if (awaitFocusClear) {
            if (incoming != null) {
                focusKnownState.value = true
                return
            }
            awaitFocusClear = false
        }
        focusEndState.value = incoming
        if (incoming != null && destinationState.value != AppDestination.Settings) {
            destinationState.value = AppDestination.Focus
            setBurnInPagePaused(true)
        }
        focusKnownState.value = true
    }

    private fun elapsedNow(): Long = SystemClock.elapsedRealtime()

    private fun publishSystemHourFormat() {
        systemIs24Hour.value = DateFormat.is24HourFormat(getApplication())
    }

    private fun refreshWeatherIfNeeded() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch { fetchForecast(force = false) }
    }

    private fun refreshWeatherNow() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch { fetchForecast(force = true) }
    }

    private suspend fun fetchForecast(force: Boolean) {
        val prefs = weatherRepository.weather.first()
        val city = prefs.city
        val want = if (force) {
            prefs.enabled && city != null
        } else {
            shouldFetchWeather(prefs.enabled, city, prefs.cache, System.currentTimeMillis())
        }
        if (!want || city == null) return
        try {
            val cache = withContext(Dispatchers.IO) {
                weatherClient.forecast(city.latitude, city.longitude, System.currentTimeMillis())
            }
            coroutineContext.ensureActive()
            val latest = weatherRepository.weather.first()
            if (!latest.enabled || latest.city != city) return
            weatherRepository.saveCache(cache)
            if (panelState.value.notice == WeatherNotice.Unavailable) {
                panelState.value = panelState.value.copy(notice = WeatherNotice.None)
            }
        } catch (_: IOException) {
            val latest = weatherRepository.weather.first()
            if (!latest.enabled) return
            panelState.value = panelState.value.copy(busy = false, notice = WeatherNotice.Unavailable)
        }
    }

    private fun clockFace(
        prefs: ClockPreferences,
        weather: WeatherPreferences,
        shown: LocalDateTime,
        systemIs24Hour: Boolean,
    ): ClockUiState {
        return ClockUiState(
            face = formatFaceTime(shown.hour, shown.minute, prefs.hourMode, systemIs24Hour),
            dateLine = if (prefs.showDate) formatDateLine(shown.toLocalDate()) else null,
            hourMode = prefs.hourMode,
            showDate = prefs.showDate,
            theme = prefs.theme,
            colorway = prefs.colorway,
            weatherEnabled = weather.enabled,
            useLocation = weather.useLocation,
            cityLabel = weather.city?.label,
            weather = if (weather.enabled) weather.cache else null,
        )
    }
}
