package app.screenclock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.screenclock.domain.bedside.keepScreenOn
import app.screenclock.domain.bedside.nightDimActive
import app.screenclock.domain.bedside.warmActive
import app.screenclock.domain.time.ClockThemeId
import app.screenclock.domain.time.ColorwayId
import app.screenclock.domain.time.DatePosition
import app.screenclock.domain.time.DigitSize
import app.screenclock.domain.time.HourMode
import app.screenclock.ui.ChromeBar
import app.screenclock.ui.Cream
import app.screenclock.ui.UiAccentDeep
import app.screenclock.domain.focus.focusKeepsScreen
import app.screenclock.domain.occasion.ceremonyIsDark
import app.screenclock.ui.Ink
import app.screenclock.ui.Paper
import app.screenclock.ui.NIGHT_DIM_BRIGHTNESS
import app.screenclock.ui.ceremony.CeremonyScreen
import app.screenclock.ui.clock.AppDestination
import app.screenclock.ui.clock.ClockShell
import app.screenclock.ui.clock.ClockViewModel
import app.screenclock.ui.focus.FocusScreen
import app.screenclock.ui.nightWarmTint
import app.screenclock.ui.occasion.OccasionScreen
import app.screenclock.ui.settings.SettingsScreen

@Composable
fun ScreenClockApp(viewModel: ClockViewModel = viewModel()) {
    val destination by viewModel.destination.collectAsStateWithLifecycle()
    val ceremonyLook by viewModel.ceremonyLook.collectAsStateWithLifecycle()
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val clock by viewModel.clock.collectAsStateWithLifecycle()
    val weatherPanel by viewModel.weatherPanel.collectAsStateWithLifecycle()
    val burnIn by viewModel.burnInPrefs.collectAsStateWithLifecycle()
    val bedside by viewModel.bedside.collectAsStateWithLifecycle()
    val minuteOfDay by viewModel.minuteOfDay.collectAsStateWithLifecycle()
    val plugged by viewModel.plugged.collectAsStateWithLifecycle()
    val focus by viewModel.focus.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = LocalActivity.current
    val context = LocalContext.current
    val resumeTick = remember { mutableIntStateOf(0) }
    DisposableEffect(context, viewModel) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                viewModel.onTimeChanged()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        onDispose { context.unregisterReceiver(receiver) }
    }
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    viewModel.setWeatherForeground(true)
                    viewModel.setBurnInHostHidden(false)
                }
                Lifecycle.Event.ON_STOP -> {
                    viewModel.setWeatherForeground(false)
                    viewModel.setBurnInHostHidden(true)
                }
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.onForeground()
                    resumeTick.intValue = resumeTick.intValue + 1
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            viewModel.setWeatherForeground(false)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    val nightOn = nightDimActive(
        bedside.dimEnabled,
        minuteOfDay,
        bedside.startHour,
        bedside.endHour,
    )
    val warmOn = warmActive(nightOn, bedside.warmEnabled)
    val screenOn = keepScreenOn(
        focusHolding = destination == AppDestination.Focus && focusKeepsScreen(focus.session),
        chargeEnabled = bedside.chargeKeepAwake,
        plugged = plugged,
    )
    val resumeGeneration = resumeTick.intValue
    DisposableEffect(activity, nightOn, resumeGeneration) {
        val window = activity?.window
        if (window != null) {
            val params = window.attributes
            params.screenBrightness = if (nightOn) {
                NIGHT_DIM_BRIGHTNESS
            } else {
                WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }
            window.attributes = params
        }
        // The next effect writes the brightness. Clearing here would flash on resume.
        onDispose { }
    }
    DisposableEffect(activity, screenOn) {
        val window = activity?.window
        if (window != null) {
            if (screenOn) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    // Bars are hidden, but a swipe reveals them briefly: icons must read on the page behind.
    val lightPage = destination == AppDestination.Settings ||
        (destination == AppDestination.Occasions) ||
        (destination == AppDestination.Focus && !ceremonyIsDark(ceremonyLook.tone, systemDark)) ||
        (destination == AppDestination.Ceremony && !ceremonyIsDark(ceremonyLook.tone, systemDark))
    val barView = LocalView.current
    SideEffect {
        val window = (barView.context as? android.app.Activity)?.window ?: return@SideEffect
        val controller = androidx.core.view.WindowCompat.getInsetsController(window, barView)
        controller.isAppearanceLightStatusBars = lightPage
        controller.isAppearanceLightNavigationBars = lightPage
    }
    MaterialTheme(
        colorScheme = lightColorScheme(
            background = Paper,
            surface = ChromeBar,
            primary = UiAccentDeep,
            onPrimary = Paper,
            onBackground = Cream,
            onSurface = Cream,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (warmOn) Modifier.nightWarmTint() else Modifier),
        ) {
            if (!focus.known) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Ink),
                )
            } else when (destination) {
                AppDestination.Clock -> ClockShell(viewModel)
                AppDestination.Focus -> FocusScreen(viewModel)
                AppDestination.Occasions -> OccasionScreen(viewModel)
                AppDestination.Ceremony -> CeremonyScreen(viewModel)
                AppDestination.Settings -> SettingsScreen(
                    face = clock?.face,
                    dateLine = clock?.dateLine,
                    hourMode = clock?.hourMode ?: HourMode.FollowSystem,
                    theme = clock?.theme ?: ClockThemeId.Flip,
                    colorway = clock?.colorway ?: ColorwayId.InkBlack,
                    datePosition = clock?.datePosition ?: DatePosition.Above,
                    digitSize = clock?.digitSize ?: DigitSize.Regular,
                    showDate = clock?.showDate ?: true,
                    showYear = clock?.showYear ?: true,
                    showWeekday = clock?.showWeekday ?: true,
                    showLunarDate = clock?.showLunarDate ?: true,
                    showHoliday = clock?.showHoliday ?: true,
                    onHourMode = viewModel::setHourMode,
                    onTheme = viewModel::setTheme,
                    onColorway = viewModel::setColorway,
                    onDatePosition = viewModel::setDatePosition,
                    onDigitSize = viewModel::setDigitSize,
                    onShowDate = viewModel::setShowDate,
                    onShowYear = viewModel::setShowYear,
                    onShowWeekday = viewModel::setShowWeekday,
                    onShowLunarDate = viewModel::setShowLunarDate,
                    onShowHoliday = viewModel::setShowHoliday,
                    onBack = viewModel::closeSettings,
                    weatherEnabled = clock?.weatherEnabled ?: false,
                    useLocation = clock?.useLocation ?: false,
                    cityLabel = clock?.cityLabel,
                    weather = clock?.weather,
                    panel = weatherPanel,
                    onWeatherEnabled = viewModel::setWeatherEnabled,
                    onUseLocation = viewModel::setUseLocation,
                    onLocate = viewModel::useCurrentLocation,
                    onLocationDenied = viewModel::onLocationDenied,
                    onSearch = viewModel::searchCity,
                    onSelectCity = viewModel::selectCity,
                    burnInEnabled = burnIn.enabled,
                    burnInIdleMillis = burnIn.idleMillis,
                    burnInRunMillis = burnIn.runMillis,
                    onBurnInEnabled = viewModel::setBurnInEnabled,
                    onBurnInIdleMillis = viewModel::setBurnInIdleMillis,
                    onBurnInRunMillis = viewModel::setBurnInRunMillis,
                    nightDimEnabled = bedside.dimEnabled,
                    nightStartHour = bedside.startHour,
                    nightEndHour = bedside.endHour,
                    nightWarmEnabled = bedside.warmEnabled,
                    chargeKeepAwake = bedside.chargeKeepAwake,
                    onNightDimEnabled = viewModel::setNightDimEnabled,
                    onNightStartHour = viewModel::setNightStartHour,
                    onNightEndHour = viewModel::setNightEndHour,
                    onNightWarmEnabled = viewModel::setNightWarmEnabled,
                    onChargeKeepAwake = viewModel::setChargeKeepAwake,
                    ceremonyLook = ceremonyLook,
                    onCeremonyStyle = viewModel::setCeremonyStyle,
                    onCeremonyTone = viewModel::setCeremonyTone,
                    onOpenOccasions = { viewModel.openOccasions(fromSettings = true) },
                )
            }
        }
    }
}
