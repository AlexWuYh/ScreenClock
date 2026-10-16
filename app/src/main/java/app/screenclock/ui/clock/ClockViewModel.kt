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
import android.provider.Settings
import android.text.format.DateFormat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.screenclock.controller.BurnInController
import app.screenclock.controller.CeremonyExit
import app.screenclock.controller.ClockFaceController
import app.screenclock.controller.ClockTime
import app.screenclock.controller.ClockUiState
import app.screenclock.controller.FocusController
import app.screenclock.controller.FocusEvent
import app.screenclock.controller.OccasionController
import app.screenclock.controller.OccasionDraftStatus
import app.screenclock.controller.SystemClockTime
import app.screenclock.controller.WeatherController
import app.screenclock.controller.WeatherPanel
import app.screenclock.data.bedside.BedsideRepository
import app.screenclock.data.burnin.BurnInRepository
import app.screenclock.data.focus.FocusRepository
import app.screenclock.data.occasion.OccasionRepository
import app.screenclock.data.occasion.SixTailCalendar
import app.screenclock.data.settings.SettingsRepository
import app.screenclock.data.weather.AndroidCoarseLocator
import app.screenclock.data.weather.OpenMeteoClient
import app.screenclock.data.weather.WeatherRepository
import app.screenclock.domain.bedside.BedsidePreferences
import app.screenclock.domain.burnin.BurnInClock
import app.screenclock.domain.burnin.BurnInPreferences
import app.screenclock.domain.occasion.CeremonyFace
import app.screenclock.domain.occasion.OccasionDraft
import app.screenclock.domain.occasion.OccasionRow
import app.screenclock.domain.time.ClockThemeId
import app.screenclock.domain.time.ColorwayId
import app.screenclock.domain.time.HourMode
import app.screenclock.domain.weather.CityCandidate
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AppDestination {
    Clock,
    Settings,
    Focus,
    Occasions,
    Ceremony,
}

/**
 * The app shell. It owns navigation and the Android glue (system hour format, battery,
 * wall clock broadcasts) and delegates every feature to one controller:
 * [ClockFaceController], [WeatherController], [OccasionController], [FocusController]
 * and [BurnInController].
 */
class ClockViewModel(application: Application) : AndroidViewModel(application) {
    private val time: ClockTime = SystemClockTime
    private val settingsRepository = SettingsRepository(application)

    private val weatherController = WeatherController(
        scope = viewModelScope,
        store = WeatherRepository(application),
        source = OpenMeteoClient(),
        locator = AndroidCoarseLocator(application),
        time = time,
    )
    private val faceController = ClockFaceController(
        scope = viewModelScope,
        settings = settingsRepository,
        weather = WeatherRepository(application),
        time = time,
        systemIs24HourNow = { DateFormat.is24HourFormat(application) },
        onMinute = { weatherController.refreshIfNeeded() },
    )
    private val occasionController = OccasionController(
        scope = viewModelScope,
        store = OccasionRepository(application.filesDir),
        calendar = SixTailCalendar,
        minutes = faceController.minute,
    )
    private val focusController = FocusController(
        scope = viewModelScope,
        store = FocusRepository(application),
        time = time,
    )
    private val burnInController = BurnInController(
        scope = viewModelScope,
        burnInStore = BurnInRepository(application),
        bedsideStore = BedsideRepository(application),
        time = time,
    )

    private val chromeState = MutableStateFlow(ChromeState())
    val chrome: StateFlow<ChromeState> = chromeState.asStateFlow()

    private val destinationState = MutableStateFlow(AppDestination.Clock)
    val destination: StateFlow<AppDestination> = destinationState.asStateFlow()

    private val wallClockGenerationState = MutableStateFlow(0)
    val wallClockGeneration: StateFlow<Int> = wallClockGenerationState.asStateFlow()

    private var occasionsFromSettings = false

    val clock: StateFlow<ClockUiState?> = faceController.clock
    val minuteOfDay: StateFlow<Int> = faceController.minuteOfDay
    val weatherPanel: StateFlow<WeatherPanel> = weatherController.panel
    val burnInPrefs: StateFlow<BurnInPreferences> = burnInController.burnInPrefs
    val bedside: StateFlow<BedsidePreferences> = burnInController.bedside
    val plugged: StateFlow<Boolean> = burnInController.plugged
    val burnInSession: StateFlow<BurnInClock> = burnInController.burnInSession
    val focusEnd: StateFlow<Long?> = focusController.focusEnd
    val focusKnown: StateFlow<Boolean> = focusController.focusKnown
    val occasionRows: StateFlow<List<OccasionRow>> = occasionController.occasionRows
    val occasionLine: StateFlow<String?> = occasionController.occasionLine
    val leapMonths: StateFlow<Set<Int>> = occasionController.leapMonths
    val pinnedOccasionId: StateFlow<String?> = occasionController.pinnedOccasionId

    init {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                faceController.notifyTimeChanged()
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
        weatherController.refreshIfNeeded()
        viewModelScope.launch {
            // A restored focus session takes the screen unless a page is already open.
            focusController.focusEnd.collect { end ->
                if (end != null && destinationState.value in FOCUS_TAKES_SCREEN) {
                    destinationState.value = AppDestination.Focus
                    burnInController.setPagePaused(true)
                }
            }
        }
        viewModelScope.launch {
            focusController.events.collect { event ->
                if (event == FocusEvent.PersistFailed) {
                    leaveBlockingPage()
                }
            }
        }
    }

    private fun watchBattery(app: Application) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                burnInController.setPlugged(isPlugged(intent))
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val sticky = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            app.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            app.registerReceiver(receiver, filter)
        }
        burnInController.setPlugged(isPlugged(sticky))
        addCloseable { app.unregisterReceiver(receiver) }
    }

    private fun isPlugged(intent: Intent?): Boolean {
        if (intent == null) return false
        return intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
    }

    /**
     * A press on the clock. Returns true when the press ends a running screensaver,
     * so that same tap should not also open the control layer.
     */
    fun onBurnInContact(): Boolean = burnInController.onContact()

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
        burnInController.onContact()
        burnInController.setPagePaused(true)
        chromeState.value = ChromeState()
        destinationState.value = AppDestination.Settings
    }

    fun closeSettings() {
        leaveBlockingPage()
    }

    fun openFocus() {
        burnInController.onContact()
        burnInController.setPagePaused(true)
        chromeState.value = ChromeState()
        destinationState.value = AppDestination.Focus
    }

    /** Leaves the duration picker. A running session stays until it is abandoned. */
    fun closeFocusPicker() {
        if (focusEnd.value != null) return
        leaveBlockingPage()
    }

    fun startFocus(minutes: Int) {
        focusController.start(minutes)
        destinationState.value = AppDestination.Focus
        burnInController.setPagePaused(true)
    }

    /** Long-press cancel and the tap after zero both clear the stored end. */
    fun abandonFocus() {
        focusController.abandon()
        leaveBlockingPage()
    }

    fun openOccasions(fromSettings: Boolean) {
        burnInController.onContact()
        occasionsFromSettings = fromSettings
        burnInController.setPagePaused(true)
        chromeState.value = ChromeState()
        destinationState.value = AppDestination.Occasions
    }

    fun closeOccasions() {
        if (occasionsFromSettings) {
            destinationState.value = AppDestination.Settings
        } else {
            leaveBlockingPage()
        }
    }

    fun openCeremony(id: String, fromList: Boolean) {
        if (!occasionController.openCeremony(id, fromList)) return
        burnInController.setPagePaused(true)
        chromeState.value = ChromeState()
        destinationState.value = AppDestination.Ceremony
    }

    fun closeCeremony() {
        when (occasionController.closeCeremony()) {
            CeremonyExit.Occasions -> destinationState.value = AppDestination.Occasions
            CeremonyExit.Clock -> leaveBlockingPage()
        }
    }

    fun ceremonyNow(now: LocalDateTime): CeremonyFace? = occasionController.ceremonyNow(now)

    fun ceremonyDate(): LocalDate? = occasionController.ceremonyDate()

    fun occasionDraftStatus(draft: OccasionDraft): OccasionDraftStatus =
        occasionController.occasionDraftStatus(draft)

    fun addOccasion(draft: OccasionDraft): Boolean = occasionController.addOccasion(draft)

    fun deleteOccasion(id: String) = occasionController.deleteOccasion(id)

    fun pinOccasion(id: String) = occasionController.pinOccasion(id)

    fun unpinOccasion(id: String) = occasionController.unpinOccasion(id)

    fun setBurnInHostHidden(hidden: Boolean) = burnInController.setHostHidden(hidden)

    fun setBurnInEnabled(enabled: Boolean) = burnInController.setEnabled(enabled)

    fun setBurnInIdleMillis(idleMillis: Long) = burnInController.setIdleMillis(idleMillis)

    fun setBurnInRunMillis(runMillis: Long) = burnInController.setRunMillis(runMillis)

    fun setNightDimEnabled(enabled: Boolean) = burnInController.setNightDimEnabled(enabled)

    fun setNightStartHour(hour: Int) = burnInController.setNightStartHour(hour)

    fun setNightEndHour(hour: Int) = burnInController.setNightEndHour(hour)

    fun setNightWarmEnabled(enabled: Boolean) = burnInController.setNightWarmEnabled(enabled)

    fun setChargeKeepAwake(enabled: Boolean) = burnInController.setChargeKeepAwake(enabled)

    /** System clock changed, or the app returned to the front. Ceremony waits start over. */
    fun onTimeChanged() {
        faceController.notifyTimeChanged()
        wallClockGenerationState.value = wallClockGenerationState.value + 1
    }

    /** App is in front. Weather refresh stays off the minute loop. */
    fun onForeground() {
        onTimeChanged()
        weatherController.notifyForeground()
    }

    fun setHourMode(mode: HourMode) {
        viewModelScope.launch { settingsRepository.setHourMode(mode) }
    }

    fun setShowDate(show: Boolean) {
        viewModelScope.launch { settingsRepository.setShowDate(show) }
    }

    fun setShowYear(show: Boolean) {
        viewModelScope.launch { settingsRepository.setShowYear(show) }
    }

    fun setShowWeekday(show: Boolean) {
        viewModelScope.launch { settingsRepository.setShowWeekday(show) }
    }

    fun setShowLunarDate(show: Boolean) {
        viewModelScope.launch { settingsRepository.setShowLunarDate(show) }
    }

    fun setShowHoliday(show: Boolean) {
        viewModelScope.launch { settingsRepository.setShowHoliday(show) }
    }

    fun setTheme(theme: ClockThemeId) {
        viewModelScope.launch { settingsRepository.setTheme(theme) }
    }

    fun setColorway(colorway: ColorwayId) {
        viewModelScope.launch { settingsRepository.setColorway(colorway) }
    }

    fun setWeatherEnabled(enabled: Boolean) = weatherController.setEnabled(enabled)

    /** Turning this off keeps the saved city. Turning it on goes through [useCurrentLocation]. */
    fun setUseLocation(enabled: Boolean) = weatherController.setUseLocation(enabled)

    fun onLocationDenied() = weatherController.onLocationDenied()

    fun useCurrentLocation(hereLabel: String) = weatherController.useCurrentLocation(hereLabel)

    fun searchCity(raw: String) = weatherController.searchCity(raw)

    fun selectCity(city: CityCandidate) = weatherController.selectCity(city)

    /**
     * Back to the clock, or straight into a focus session that was restored while a
     * page was open. The burn-in hold follows the screen that is now showing.
     */
    private fun leaveBlockingPage() {
        if (focusController.focusEnd.value != null) {
            destinationState.value = AppDestination.Focus
            burnInController.setPagePaused(true)
        } else {
            destinationState.value = AppDestination.Clock
            burnInController.setPagePaused(false)
        }
    }

    private companion object {
        /** Only these destinations give way to a restored focus session. */
        val FOCUS_TAKES_SCREEN: Set<AppDestination> = setOf(AppDestination.Clock, AppDestination.Focus)
    }
}
