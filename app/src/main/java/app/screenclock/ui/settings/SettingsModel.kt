package app.screenclock.ui.settings

import app.screenclock.controller.WeatherPanel
import app.screenclock.domain.appearance.AppearanceTone
import app.screenclock.domain.burnin.ScreensaverTheme
import app.screenclock.domain.occasion.CeremonyLook
import app.screenclock.domain.occasion.CeremonyStyle
import app.screenclock.domain.time.ClockThemeId
import app.screenclock.domain.time.ColorwayId
import app.screenclock.domain.time.DatePosition
import app.screenclock.domain.time.DigitSize
import app.screenclock.domain.time.FaceTime
import app.screenclock.domain.time.HourMode
import app.screenclock.domain.weather.CityCandidate
import app.screenclock.domain.weather.WeatherCache

/**
 * Everything the settings shell reads. Hoisted into one value so the home list, the
 * sub-pages and the live previews all see the same snapshot.
 */
internal data class SettingsUi(
    val face: FaceTime? = null,
    val dateLine: String? = null,
    val hourMode: HourMode = HourMode.FollowSystem,
    val theme: ClockThemeId = ClockThemeId.Flip,
    val colorway: ColorwayId = ColorwayId.InkBlack,
    val datePosition: DatePosition = DatePosition.Above,
    val digitSize: DigitSize = DigitSize.Regular,
    val showDate: Boolean = true,
    val showYear: Boolean = true,
    val showWeekday: Boolean = true,
    val showLunarDate: Boolean = true,
    val showHoliday: Boolean = true,
    val weatherEnabled: Boolean = false,
    val useLocation: Boolean = false,
    val cityLabel: String? = null,
    val weather: WeatherCache? = null,
    val weatherPanel: WeatherPanel = WeatherPanel(),
    val burnInEnabled: Boolean = false,
    val burnInTheme: ScreensaverTheme = ScreensaverTheme.Drift,
    val burnInIdleMillis: Long = 0L,
    val burnInRunMillis: Long = 0L,
    val nightDimEnabled: Boolean = false,
    val nightStartHour: Int = 22,
    val nightEndHour: Int = 7,
    val nightWarmEnabled: Boolean = false,
    val chargeKeepAwake: Boolean = false,
    val ceremonyLook: CeremonyLook = CeremonyLook(),
    val appearanceTone: AppearanceTone = AppearanceTone.FollowSystem,
    val version: String = "",
)

/** Every write the settings shell can ask for. */
internal class SettingsActions(
    val onBack: () -> Unit,
    val onHourMode: (HourMode) -> Unit,
    val onTheme: (ClockThemeId) -> Unit,
    val onColorway: (ColorwayId) -> Unit,
    val onDatePosition: (DatePosition) -> Unit,
    val onDigitSize: (DigitSize) -> Unit,
    val onShowDate: (Boolean) -> Unit,
    val onShowYear: (Boolean) -> Unit,
    val onShowWeekday: (Boolean) -> Unit,
    val onShowLunarDate: (Boolean) -> Unit,
    val onShowHoliday: (Boolean) -> Unit,
    val onWeatherEnabled: (Boolean) -> Unit,
    val onUseLocation: (Boolean) -> Unit,
    val onLocate: (String) -> Unit,
    val onLocationDenied: () -> Unit,
    val onSearch: (String) -> Unit,
    val onSelectCity: (CityCandidate) -> Unit,
    val onBurnInEnabled: (Boolean) -> Unit,
    val onBurnInTheme: (ScreensaverTheme) -> Unit,
    val onBurnInIdleMillis: (Long) -> Unit,
    val onBurnInRunMillis: (Long) -> Unit,
    val onNightDimEnabled: (Boolean) -> Unit,
    val onNightStartHour: (Int) -> Unit,
    val onNightEndHour: (Int) -> Unit,
    val onNightWarmEnabled: (Boolean) -> Unit,
    val onChargeKeepAwake: (Boolean) -> Unit,
    val onCeremonyStyle: (CeremonyStyle) -> Unit,
    val onAutoPlaySeconds: (Int) -> Unit,
    val onAppearanceTone: (AppearanceTone) -> Unit,
    val onOpenOccasions: () -> Unit,
)
