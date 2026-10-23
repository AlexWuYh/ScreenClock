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
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.screenclock.R
import app.screenclock.controller.BurnInController
import app.screenclock.controller.CeremonyExit
import app.screenclock.controller.ClockFaceController
import app.screenclock.controller.ClockTime
import app.screenclock.controller.ClockUiState
import app.screenclock.controller.FocusController
import app.screenclock.controller.FocusEvent
import app.screenclock.controller.FocusUi
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
import app.screenclock.data.settings.CeremonyLookRepository
import app.screenclock.data.settings.IntroHintRepository
import app.screenclock.data.settings.SettingsRepository
import app.screenclock.data.weather.AndroidCoarseLocator
import app.screenclock.data.weather.OpenMeteoClient
import app.screenclock.data.weather.WeatherRepository
import app.screenclock.domain.bedside.BedsidePreferences
import app.screenclock.domain.burnin.BurnInClock
import app.screenclock.domain.burnin.BurnInPreferences
import app.screenclock.domain.focus.FocusMode
import app.screenclock.domain.occasion.CeremonyFace
import app.screenclock.domain.occasion.CeremonyLook
import app.screenclock.domain.occasion.CeremonyStyle
import app.screenclock.domain.occasion.CeremonyTone
import app.screenclock.domain.occasion.OccasionDraft
import app.screenclock.domain.occasion.OccasionRow
import app.screenclock.domain.time.ClockThemeId
import app.screenclock.domain.time.ColorwayId
import app.screenclock.domain.time.DatePosition
import app.screenclock.domain.time.DigitSize
import app.screenclock.domain.time.HourMode
import app.screenclock.domain.time.nextShowcaseTheme
import app.screenclock.domain.weather.CityCandidate
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import app.screenclock.controller.launchPersisting

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
    private val introHintStore = IntroHintRepository(application)
    private val ceremonyLookStore = CeremonyLookRepository(application)

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

    private val introHintState = MutableStateFlow(false)

    /** True only on the first launch, while the control layer is still showing. */
    val introHint: StateFlow<Boolean> = introHintState.asStateFlow()

    private val destinationState = MutableStateFlow(AppDestination.Clock)
    val destination: StateFlow<AppDestination> = destinationState.asStateFlow()

    private val wallClockGenerationState = MutableStateFlow(0)
    val wallClockGeneration: StateFlow<Int> = wallClockGenerationState.asStateFlow()

    private var occasionsFromSettings = false
    private var pendingThemeSelection: ClockThemeId? = null

    val clock: StateFlow<ClockUiState?> = faceController.clock
    val minuteOfDay: StateFlow<Int> = faceController.minuteOfDay
    val weatherPanel: StateFlow<WeatherPanel> = weatherController.panel
    val burnInPrefs: StateFlow<BurnInPreferences> = burnInController.burnInPrefs
    val bedside: StateFlow<BedsidePreferences> = burnInController.bedside
    val plugged: StateFlow<Boolean> = burnInController.plugged
    val burnInSession: StateFlow<BurnInClock> = burnInController.burnInSession
    val focus: StateFlow<FocusUi> = focusController.state
    val occasionRows: StateFlow<List<OccasionRow>> = occasionController.occasionRows

    /** The design and light/dark tone of the countdown page. */
    val ceremonyLook: StateFlow<CeremonyLook> = ceremonyLookStore.look
        .stateIn(viewModelScope, SharingStarted.Eagerly, CeremonyLook())

    fun setCeremonyStyle(style: CeremonyStyle) {
        viewModelScope.launchPersisting { ceremonyLookStore.setStyle(style) }
    }

    fun setCeremonyTone(tone: CeremonyTone) {
        viewModelScope.launchPersisting { ceremonyLookStore.setTone(tone) }
    }
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
        // Forecast refresh waits until the Activity is started and visible.
        viewModelScope.launch {
            // A timer that was counting when the app closed takes the screen back, once.
            val restored = focusController.state.first { it.known }
            if (restored.session != null && destinationState.value == AppDestination.Clock) {
                destinationState.value = AppDestination.Focus
                burnInController.setPagePaused(true)
            }
        }
        viewModelScope.launch {
            faceController.clock.collect { shown ->
                if (shown != null && shown.theme == pendingThemeSelection) pendingThemeSelection = null
            }
        }
        viewModelScope.launch {
            // First launch: nothing on the face says the buttons exist, so show them once.
            if (introHintStore.seen.first()) return@launch
            faceController.clock.first { it != null }
            if (destinationState.value != AppDestination.Clock) return@launch
            introHintState.value = true
            chromeState.value = ChromeState(
                visible = true,
                hideAtEpochMillis = System.currentTimeMillis() + INTRO_HINT_MILLIS,
            )
            launchPersisting { introHintStore.markSeen() }
        }
        viewModelScope.launch {
            chromeState.collect { if (!it.visible) introHintState.value = false }
        }
        viewModelScope.launch {
            occasionController.saveFailed.collect {
                val app = getApplication<Application>()
                Toast.makeText(app, app.getString(R.string.occasion_save_failed), Toast.LENGTH_SHORT).show()
            }
        }
        viewModelScope.launch {
            focusController.events.collect { event ->
                when (event) {
                    FocusEvent.PersistFailed -> {
                        val app = getApplication<Application>()
                        Toast.makeText(app, app.getString(R.string.focus_persist_failed), Toast.LENGTH_SHORT).show()
                    }
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

    /**
     * The idle time before the buttons hide. Switch and TalkBack users get the system's
     * recommended time when it is longer than ours, because they cannot reach buttons in 8 s.
     */
    private fun chromeHideAfterMillis(): Long {
        val app = getApplication<Application>()
        val base = ChromeState.HIDE_AFTER_MILLIS
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return base
        val manager = app.getSystemService(AccessibilityManager::class.java) ?: return base
        val recommended = manager.getRecommendedTimeoutMillis(
            base.toInt(),
            AccessibilityManager.FLAG_CONTENT_CONTROLS,
        )
        return maxOf(base, recommended.toLong())
    }

    fun onBlankTap(nowEpochMillis: Long) {
        chromeState.value = chromeState.value.onBlankTap(nowEpochMillis, chromeHideAfterMillis())
    }

    /** TalkBack activates the clock without a pointer press. */
    fun onClockActivate(nowEpochMillis: Long) {
        val dismiss = onBurnInContact()
        if (!dismiss) onBlankTap(nowEpochMillis)
    }

    fun onChromeTick(nowEpochMillis: Long) {
        chromeState.value = chromeState.value.after(nowEpochMillis)
    }

    fun onChromeInteraction(nowEpochMillis: Long) {
        burnInController.onContact()
        chromeState.value = chromeState.value.onInteraction(nowEpochMillis, chromeHideAfterMillis())
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

    /** Back to the clock. A running timer keeps counting; the page shows it again on return. */
    fun closeFocus() {
        leaveBlockingPage()
    }

    fun selectFocusMode(mode: FocusMode) = focusController.selectMode(mode)

    fun setFocusCustomMinutes(minutes: Int) = focusController.setCustomMinutes(minutes)

    fun toggleFocus() = focusController.toggle()

    fun resetFocus() = focusController.reset()

    fun skipFocus() = focusController.skip()

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

    fun occasionDraftStatus(draft: OccasionDraft, editing: Boolean = false): OccasionDraftStatus =
        occasionController.occasionDraftStatus(draft, editing)

    fun occasionDraftOf(id: String): OccasionDraft? = occasionController.occasionDraftOf(id)

    fun updateOccasion(id: String, draft: OccasionDraft): Boolean = occasionController.updateOccasion(id, draft)

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
        focusController.refresh()
        wallClockGenerationState.value = wallClockGenerationState.value + 1
    }

    /** Network refresh is permitted only while the Activity is visible. */
    fun setWeatherForeground(visible: Boolean) = weatherController.setForeground(visible)

    /** App returned to the foreground; reconcile timers and retry stale weather. */
    fun onForeground() {
        onTimeChanged()
        weatherController.notifyForeground()
    }

    fun setHourMode(mode: HourMode) {
        viewModelScope.launchPersisting { settingsRepository.setHourMode(mode) }
    }

    fun setShowDate(show: Boolean) {
        viewModelScope.launchPersisting { settingsRepository.setShowDate(show) }
    }

    fun setShowYear(show: Boolean) {
        viewModelScope.launchPersisting { settingsRepository.setShowYear(show) }
    }

    fun setShowWeekday(show: Boolean) {
        viewModelScope.launchPersisting { settingsRepository.setShowWeekday(show) }
    }

    fun setShowLunarDate(show: Boolean) {
        viewModelScope.launchPersisting { settingsRepository.setShowLunarDate(show) }
    }

    fun setShowHoliday(show: Boolean) {
        viewModelScope.launchPersisting { settingsRepository.setShowHoliday(show) }
    }

    fun setTheme(theme: ClockThemeId) {
        pendingThemeSelection = theme
        viewModelScope.launch {
            try {
                settingsRepository.setTheme(theme)
            } catch (_: IOException) {
                if (pendingThemeSelection == theme) pendingThemeSelection = null
            }
        }
    }

    fun setColorway(colorway: ColorwayId) {
        viewModelScope.launchPersisting { settingsRepository.setColorway(colorway) }
    }

    fun setDatePosition(position: DatePosition) {
        viewModelScope.launchPersisting { settingsRepository.setDatePosition(position) }
    }

    fun setDigitSize(size: DigitSize) {
        viewModelScope.launchPersisting { settingsRepository.setDigitSize(size) }
    }

    /** Cycles through existing themes without leaving the full-screen clock. */
    fun cycleTheme() {
        val current = pendingThemeSelection ?: clock.value?.theme ?: ClockThemeId.Flip
        setTheme(nextShowcaseTheme(current))
    }

    fun setWeatherEnabled(enabled: Boolean) = weatherController.setEnabled(enabled)

    /** Turning this off keeps the saved city. Turning it on goes through [useCurrentLocation]. */
    fun setUseLocation(enabled: Boolean) = weatherController.setUseLocation(enabled)

    fun onLocationDenied() = weatherController.onLocationDenied()

    fun useCurrentLocation(hereLabel: String) = weatherController.useCurrentLocation(hereLabel)

    fun searchCity(raw: String) = weatherController.searchCity(raw)

    fun selectCity(city: CityCandidate) = weatherController.selectCity(city)

    /** Back to the clock. The burn-in hold follows the screen that is now showing. */
    private fun leaveBlockingPage() {
        destinationState.value = AppDestination.Clock
        burnInController.setPagePaused(false)
    }

    private companion object {
        /** The first-launch hint stays up longer than the normal idle hide. */
        const val INTRO_HINT_MILLIS: Long = 7_000L
    }
}
