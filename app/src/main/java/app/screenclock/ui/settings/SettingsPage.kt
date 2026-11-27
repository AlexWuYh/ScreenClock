package app.screenclock.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import app.screenclock.ui.clock.ClockViewModel

/** Reads the settings snapshot out of the view model and hands it to the shell. */
@Composable
fun SettingsPage(viewModel: ClockViewModel) {
    val clock by viewModel.clock.collectAsStateWithLifecycle()
    val weatherPanel by viewModel.weatherPanel.collectAsStateWithLifecycle()
    val burnIn by viewModel.burnInPrefs.collectAsStateWithLifecycle()
    val bedside by viewModel.bedside.collectAsStateWithLifecycle()
    val ceremonyLook by viewModel.ceremonyLook.collectAsStateWithLifecycle()
    val appearanceTone by viewModel.appearanceTone.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val version = remember(context) {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
        } catch (_: Exception) {
            ""
        }
    }
    val defaults = SettingsUi()
    val ui = SettingsUi(
        face = clock?.face,
        dateLine = clock?.dateLine,
        hourMode = clock?.hourMode ?: defaults.hourMode,
        theme = clock?.theme ?: defaults.theme,
        colorway = clock?.colorway ?: defaults.colorway,
        datePosition = clock?.datePosition ?: defaults.datePosition,
        digitSize = clock?.digitSize ?: defaults.digitSize,
        showDate = clock?.showDate ?: defaults.showDate,
        showYear = clock?.showYear ?: defaults.showYear,
        showWeekday = clock?.showWeekday ?: defaults.showWeekday,
        showLunarDate = clock?.showLunarDate ?: defaults.showLunarDate,
        showHoliday = clock?.showHoliday ?: defaults.showHoliday,
        weatherEnabled = clock?.weatherEnabled ?: false,
        useLocation = clock?.useLocation ?: false,
        cityLabel = clock?.cityLabel,
        weather = clock?.weather,
        weatherPanel = weatherPanel,
        burnInEnabled = burnIn.enabled,
        burnInTheme = burnIn.theme,
        burnInIdleMillis = burnIn.idleMillis,
        burnInRunMillis = burnIn.runMillis,
        nightDimEnabled = bedside.dimEnabled,
        nightStartHour = bedside.startHour,
        nightEndHour = bedside.endHour,
        nightWarmEnabled = bedside.warmEnabled,
        chargeKeepAwake = bedside.chargeKeepAwake,
        ceremonyLook = ceremonyLook,
        appearanceTone = appearanceTone,
        version = version,
    )
    val actions = SettingsActions(
        onBack = viewModel::closeSettings,
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
        onWeatherEnabled = viewModel::setWeatherEnabled,
        onUseLocation = viewModel::setUseLocation,
        onLocate = viewModel::useCurrentLocation,
        onLocationDenied = viewModel::onLocationDenied,
        onSearch = viewModel::searchCity,
        onSelectCity = viewModel::selectCity,
        onBurnInEnabled = viewModel::setBurnInEnabled,
        onBurnInTheme = viewModel::setBurnInTheme,
        onBurnInIdleMillis = viewModel::setBurnInIdleMillis,
        onBurnInRunMillis = viewModel::setBurnInRunMillis,
        onNightDimEnabled = viewModel::setNightDimEnabled,
        onNightStartHour = viewModel::setNightStartHour,
        onNightEndHour = viewModel::setNightEndHour,
        onNightWarmEnabled = viewModel::setNightWarmEnabled,
        onChargeKeepAwake = viewModel::setChargeKeepAwake,
        onCeremonyStyle = viewModel::setCeremonyStyle,
        onAutoPlaySeconds = viewModel::setAutoPlaySeconds,
        onAppearanceTone = viewModel::setAppearanceTone,
        onOpenOccasions = { viewModel.openOccasions(fromSettings = true) },
    )
    SettingsShell(ui = ui, actions = actions)
}
