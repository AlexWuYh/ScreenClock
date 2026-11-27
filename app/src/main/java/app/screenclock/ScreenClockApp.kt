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
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import app.screenclock.ui.ChromeBar
import app.screenclock.ui.Cream
import app.screenclock.ui.Info
import app.screenclock.ui.UiAccentDeep
import app.screenclock.domain.focus.focusKeepsScreen
import app.screenclock.domain.appearance.appearanceIsDark
import app.screenclock.ui.DarkUiPalette
import app.screenclock.ui.Ink
import app.screenclock.ui.LightUiPalette
import app.screenclock.ui.LocalUiPalette
import app.screenclock.ui.Paper
import app.screenclock.ui.NIGHT_DIM_BRIGHTNESS
import app.screenclock.ui.ceremony.CeremonyScreen
import app.screenclock.ui.clock.AppDestination
import app.screenclock.ui.clock.ClockShell
import app.screenclock.ui.clock.ClockViewModel
import app.screenclock.ui.focus.FocusScreen
import app.screenclock.ui.nightWarmTint
import app.screenclock.ui.occasion.OccasionScreen
import app.screenclock.ui.settings.SettingsPage

@Composable
fun ScreenClockApp(viewModel: ClockViewModel = viewModel()) {
    val destination by viewModel.destination.collectAsStateWithLifecycle()
    val ceremonyLook by viewModel.ceremonyLook.collectAsStateWithLifecycle()
    val appearanceTone by viewModel.appearanceTone.collectAsStateWithLifecycle()
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
    // The clock face keeps its own dark colours, so it always wants light bar icons.
    val darkPages = appearanceIsDark(appearanceTone, systemDark)
    val lightPage = !darkPages && focus.known && destination != AppDestination.Clock
    val barView = LocalView.current
    SideEffect {
        val window = (barView.context as? android.app.Activity)?.window ?: return@SideEffect
        val controller = androidx.core.view.WindowCompat.getInsetsController(window, barView)
        controller.isAppearanceLightStatusBars = lightPage
        controller.isAppearanceLightNavigationBars = lightPage
    }
    val palette = if (darkPages) DarkUiPalette else LightUiPalette
    val scheme = if (darkPages) {
        darkColorScheme(
            background = palette.backdropMid,
            surface = palette.tile,
            primary = palette.accent,
            onPrimary = palette.backdropMid,
            onBackground = palette.on,
            onSurface = palette.on,
            onSurfaceVariant = palette.on2,
            outline = palette.outline,
            error = palette.alert,
        )
    } else {
        lightColorScheme(
            background = Paper,
            surface = ChromeBar,
            primary = UiAccentDeep,
            onPrimary = Paper,
            onBackground = Cream,
            onSurface = Cream,
            onSurfaceVariant = Info,
            outline = palette.outline,
            error = palette.alert,
        )
    }
    CompositionLocalProvider(LocalUiPalette provides palette) {
        MaterialTheme(colorScheme = scheme) {
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
                    AppDestination.Settings -> SettingsPage(viewModel)
                }
            }
        }
    }
}
