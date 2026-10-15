package app.screenclock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.screenclock.domain.time.ClockThemeId
import app.screenclock.domain.time.ColorwayId
import app.screenclock.domain.time.HourMode
import app.screenclock.ui.ChromeBar
import app.screenclock.ui.Cream
import app.screenclock.ui.Ink
import app.screenclock.ui.clock.AppDestination
import app.screenclock.ui.clock.ClockShell
import app.screenclock.ui.clock.ClockViewModel
import app.screenclock.ui.focus.FocusScreen
import app.screenclock.ui.settings.SettingsScreen

@Composable
fun ScreenClockApp(viewModel: ClockViewModel = viewModel()) {
    val destination by viewModel.destination.collectAsStateWithLifecycle()
    val focusKnown by viewModel.focusKnown.collectAsStateWithLifecycle()
    val clock by viewModel.clock.collectAsStateWithLifecycle()
    val weatherPanel by viewModel.weatherPanel.collectAsStateWithLifecycle()
    val burnIn by viewModel.burnInPrefs.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.setBurnInHostHidden(false)
                Lifecycle.Event.ON_STOP -> viewModel.setBurnInHostHidden(true)
                Lifecycle.Event.ON_RESUME -> viewModel.onForeground()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Ink,
            surface = ChromeBar,
            primary = Cream,
            onPrimary = Ink,
            onBackground = Cream,
            onSurface = Cream,
        ),
    ) {
        if (!focusKnown) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Ink),
            )
        } else when (destination) {
            AppDestination.Clock -> ClockShell(viewModel)
            AppDestination.Focus -> FocusScreen(viewModel)
            AppDestination.Settings -> SettingsScreen(
                face = clock?.face,
                dateLine = clock?.dateLine,
                hourMode = clock?.hourMode ?: HourMode.FollowSystem,
                theme = clock?.theme ?: ClockThemeId.Flip,
                colorway = clock?.colorway ?: ColorwayId.InkBlack,
                showDate = clock?.showDate ?: true,
                onHourMode = viewModel::setHourMode,
                onTheme = viewModel::setTheme,
                onShowDate = viewModel::setShowDate,
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
            )
        }
    }
}
