package app.screenclock.ui.clock

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.text.format.DateFormat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.screenclock.data.bedside.BedsideRepository
import app.screenclock.data.burnin.BurnInRepository
import app.screenclock.data.focus.FocusRepository
import app.screenclock.data.occasion.OccasionRepository
import app.screenclock.data.occasion.SixTailCalendar
import app.screenclock.data.occasion.statutoryHolidayName
import app.screenclock.data.settings.SettingsRepository
import app.screenclock.data.weather.AndroidCoarseLocator
import app.screenclock.data.weather.CoarseLocator
import app.screenclock.data.weather.OpenMeteoClient
import app.screenclock.data.weather.WeatherRepository
import app.screenclock.domain.bedside.BedsidePreferences
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
import app.screenclock.domain.occasion.CeremonyFace
import app.screenclock.domain.occasion.OCCASION_LIMIT
import app.screenclock.domain.occasion.Occasion
import app.screenclock.domain.occasion.OccasionDraft
import app.screenclock.domain.occasion.OccasionKind
import app.screenclock.domain.occasion.OccasionRow
import app.screenclock.domain.occasion.addOccasionItem
import app.screenclock.domain.occasion.canKeep
import app.screenclock.domain.occasion.canPin
import app.screenclock.domain.occasion.ceremonyFace
import app.screenclock.domain.occasion.deleteOccasionItem
import app.screenclock.domain.occasion.formatSolarDate
import app.screenclock.domain.occasion.nextOccurrence
import app.screenclock.domain.occasion.normalizeMessage
import app.screenclock.domain.occasion.normalizeTitle
import app.screenclock.domain.occasion.occasionRows
import app.screenclock.domain.occasion.pinOccasion as pinOneOccasion
import app.screenclock.domain.occasion.pinnedOccasionLine
import app.screenclock.domain.occasion.previewText
import app.screenclock.domain.occasion.refreshPins
import app.screenclock.domain.occasion.unpinOccasion as unpinOneOccasion
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

enum class AppDestination {
    Clock,
    Settings,
    Focus,
    Occasions,
    Ceremony,
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
    private val bedsideRepository = BedsideRepository(application)
    private val focusRepository = FocusRepository(application)
    private val occasionRepository = OccasionRepository(application.filesDir)
    private val occasionCalendar = SixTailCalendar
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

    private val bedsideState = MutableStateFlow(BedsidePreferences())
    val bedside: StateFlow<BedsidePreferences> = bedsideState.asStateFlow()

    private val minuteOfDayState = MutableStateFlow(minuteOfDay(LocalDateTime.now()))
    val minuteOfDay: StateFlow<Int> = minuteOfDayState.asStateFlow()

    private val pluggedState = MutableStateFlow(false)
    val plugged: StateFlow<Boolean> = pluggedState.asStateFlow()

    private val burnInState = MutableStateFlow(freshBurnInClock(enabled = false, nowMillis = 0L))
    val burnInSession: StateFlow<BurnInClock> = burnInState.asStateFlow()

    private val focusEndState = MutableStateFlow<Long?>(null)
    val focusEnd: StateFlow<Long?> = focusEndState.asStateFlow()

    private val focusKnownState = MutableStateFlow(false)
    val focusKnown: StateFlow<Boolean> = focusKnownState.asStateFlow()

    private val occasionLock = Mutex()
    private val occasionsState = MutableStateFlow<List<Occasion>>(emptyList())
    private val occasionRowsState = MutableStateFlow<List<OccasionRow>>(emptyList())
    val occasionRows: StateFlow<List<OccasionRow>> = occasionRowsState.asStateFlow()
    private val occasionLineState = MutableStateFlow<String?>(null)
    val occasionLine: StateFlow<String?> = occasionLineState.asStateFlow()
    private val pinnedOccasionIdState = MutableStateFlow<String?>(null)
    val pinnedOccasionId: StateFlow<String?> = pinnedOccasionIdState.asStateFlow()

    private var refreshJob: Job? = null
    private var searchJob: Job? = null
    private var burnInJob: Job? = null
    private var burnInPlaced = false
    private var pendingFocusEnd: Long? = null
    private var awaitFocusClear = false
    @Volatile private var occasionsReady = false
    private var occasionsFromSettings = false
    private var ceremony: CeremonySession? = null

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
        watchBattery(application)
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
                minuteOfDayState.value = minuteOfDay(now)
                withTimeoutOrNull(millisUntilNextMinute(now)) {
                    refresh.first()
                }
            }
        }
        refreshWeatherIfNeeded()
        viewModelScope.launch(Dispatchers.IO) {
            occasionCalendar.leapMonthEver(1)
        }
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) { occasionRepository.load() }
            occasionLock.withLock {
                val now = minute.value
                val refreshed = refreshPins(loaded, now.toLocalDate(), occasionCalendar)
                occasionsState.value = refreshed
                publishOccasions(now)
                if (refreshed != loaded) persistOccasions(refreshed)
                occasionsReady = true
            }
        }
        viewModelScope.launch {
            minute.collect { shown ->
                occasionLock.withLock {
                    if (!occasionsReady) return@withLock
                    val refreshed = refreshPins(occasionsState.value, shown.toLocalDate(), occasionCalendar)
                    val changed = refreshed != occasionsState.value
                    if (changed) occasionsState.value = refreshed
                    publishOccasions(shown)
                    if (changed) persistOccasions(refreshed)
                }
            }
        }
        viewModelScope.launch {
            burnInRepository.preferences.collect { prefs -> placeBurnIn(prefs) }
        }
        viewModelScope.launch {
            focusRepository.endEpochMillis.collect { stored -> placeFocus(stored) }
        }
        viewModelScope.launch {
            bedsideRepository.preferences.collect { bedsideState.value = it }
        }
    }

    private fun watchBattery(app: Application) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                pluggedState.value = isPlugged(intent)
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val sticky = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            app.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            app.registerReceiver(receiver, filter)
        }
        pluggedState.value = isPlugged(sticky)
        addCloseable { app.unregisterReceiver(receiver) }
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

    fun openOccasions(fromSettings: Boolean) {
        onBurnInContact()
        occasionsFromSettings = fromSettings
        setBurnInPagePaused(true)
        chromeState.value = ChromeState()
        destinationState.value = AppDestination.Occasions
    }

    fun closeOccasions() {
        if (occasionsFromSettings) {
            destinationState.value = AppDestination.Settings
        } else {
            destinationState.value = AppDestination.Clock
            setBurnInPagePaused(false)
        }
    }

    /**
     * Opens the fullscreen countdown for a future or today's date.
     * The occurrence date stays fixed until the user leaves, so a finished yearly date does not jump a year.
     */
    fun openCeremony(id: String, fromList: Boolean) {
        val item = occasionsState.value.find { it.id == id } ?: return
        val next = nextOccurrence(minute.value.toLocalDate(), item, occasionCalendar)
        val date = next.date ?: return
        if (!canPin(next.status)) return
        ceremony = CeremonySession(id = id, occurrence = date, returnToList = fromList)
        setBurnInPagePaused(true)
        chromeState.value = ChromeState()
        destinationState.value = AppDestination.Ceremony
    }

    fun closeCeremony() {
        val session = ceremony
        if (session == null) {
            if (destinationState.value == AppDestination.Ceremony) {
                destinationState.value = AppDestination.Clock
                setBurnInPagePaused(false)
            }
            return
        }
        ceremony = null
        if (session.returnToList) {
            destinationState.value = AppDestination.Occasions
        } else {
            destinationState.value = AppDestination.Clock
            setBurnInPagePaused(false)
        }
    }

    fun ceremonyNow(now: LocalDateTime): CeremonyFace? {
        val session = ceremony ?: return null
        val item = occasionsState.value.find { it.id == session.id } ?: return null
        return ceremonyFace(
            title = item.title,
            message = item.message,
            solarLabel = formatSolarDate(session.occurrence),
            now = now,
            occurrence = session.occurrence,
        )
    }

    fun ceremonyDate(): LocalDate? {
        val session = ceremony ?: return null
        if (occasionsState.value.none { it.id == session.id }) return null
        return session.occurrence
    }

    fun occasionPreview(draft: OccasionDraft): String {
        return previewText(nextOccurrence(minute.value.toLocalDate(), draft, occasionCalendar))
    }

    fun occasionCanSave(draft: OccasionDraft): Boolean {
        if (!occasionsReady) return false
        if (normalizeTitle(draft.title) == null) return false
        if (occasionsState.value.size >= OCCASION_LIMIT) return false
        return canKeep(nextOccurrence(minute.value.toLocalDate(), draft, occasionCalendar).status)
    }

    fun occasionLeapAllowed(month: Int): Boolean = occasionCalendar.leapMonthEver(month)

    /** Returns false when the title, the date, or the 30-item cap rejects the draft. */
    fun addOccasion(draft: OccasionDraft): Boolean {
        if (!occasionCanSave(draft)) return false
        val title = normalizeTitle(draft.title) ?: return false
        val item = Occasion(
            id = UUID.randomUUID().toString(),
            title = title,
            kind = draft.kind,
            year = if (draft.kind == OccasionKind.SolarOnce) draft.year else null,
            month = draft.month,
            day = draft.day,
            leap = draft.kind == OccasionKind.LunarYearly && draft.leap,
            message = normalizeMessage(draft.message),
            pinned = false,
        )
        viewModelScope.launch {
            occasionLock.withLock {
                if (!occasionsReady) return@withLock
                if (!canKeep(nextOccurrence(minute.value.toLocalDate(), item, occasionCalendar).status)) {
                    return@withLock
                }
                val added = addOccasionItem(occasionsState.value, item) ?: return@withLock
                occasionsState.value = added
                publishOccasions(minute.value)
                persistOccasions(added)
            }
        }
        return true
    }

    fun deleteOccasion(id: String) {
        viewModelScope.launch {
            occasionLock.withLock {
                if (!occasionsReady) return@withLock
                val next = deleteOccasionItem(occasionsState.value, id)
                if (next == occasionsState.value) return@withLock
                occasionsState.value = next
                publishOccasions(minute.value)
                persistOccasions(next)
            }
        }
    }

    fun pinOccasion(id: String) {
        viewModelScope.launch {
            occasionLock.withLock {
                if (!occasionsReady) return@withLock
                val now = minute.value
                val next = pinOneOccasion(occasionsState.value, id, now.toLocalDate(), occasionCalendar)
                if (next == occasionsState.value) return@withLock
                occasionsState.value = next
                publishOccasions(now)
                persistOccasions(next)
            }
        }
    }

    fun unpinOccasion(id: String) {
        viewModelScope.launch {
            occasionLock.withLock {
                if (!occasionsReady) return@withLock
                val next = unpinOneOccasion(occasionsState.value, id)
                if (next == occasionsState.value) return@withLock
                occasionsState.value = next
                publishOccasions(minute.value)
                persistOccasions(next)
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

    /** Settings, focus, the occasion list, and ceremony share this hold. */
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

    fun setNightDimEnabled(enabled: Boolean) {
        viewModelScope.launch { bedsideRepository.setDimEnabled(enabled) }
    }

    fun setNightStartHour(hour: Int) {
        viewModelScope.launch { bedsideRepository.setStartHour(hour) }
    }

    fun setNightEndHour(hour: Int) {
        viewModelScope.launch { bedsideRepository.setEndHour(hour) }
    }

    fun setNightWarmEnabled(enabled: Boolean) {
        viewModelScope.launch { bedsideRepository.setWarmEnabled(enabled) }
    }

    fun setChargeKeepAwake(enabled: Boolean) {
        viewModelScope.launch { bedsideRepository.setChargeKeepAwake(enabled) }
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
        val blocked = destinationState.value == AppDestination.Settings ||
            destinationState.value == AppDestination.Occasions ||
            destinationState.value == AppDestination.Ceremony
        if (incoming != null && !blocked) {
            destinationState.value = AppDestination.Focus
            setBurnInPagePaused(true)
        }
        focusKnownState.value = true
    }

    private fun minuteOfDay(time: LocalDateTime): Int = time.hour * 60 + time.minute

    private fun isPlugged(intent: Intent?): Boolean {
        if (intent == null) return false
        return intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
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
            dateLine = if (prefs.showDate) {
                val today = shown.toLocalDate()
                formatDateLine(today, statutoryHolidayName(today))
            } else {
                null
            },
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

    private fun publishOccasions(now: LocalDateTime) {
        val items = occasionsState.value
        val today = now.toLocalDate()
        occasionRowsState.value = occasionRows(items, today, occasionCalendar)
        occasionLineState.value = pinnedOccasionLine(items, now, occasionCalendar)
        pinnedOccasionIdState.value = items.firstOrNull { it.pinned }?.id
    }

    /** A failed write keeps the list in memory. The next edit tries the file again. */
    private suspend fun persistOccasions(items: List<Occasion>) {
        try {
            withContext(Dispatchers.IO) { occasionRepository.save(items) }
        } catch (_: IOException) {
            // The in-memory list stays. The next edit writes the file again.
        }
    }

    private data class CeremonySession(
        val id: String,
        val occurrence: LocalDate,
        val returnToList: Boolean,
    )
}
