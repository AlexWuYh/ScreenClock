package app.screenclock.ui.settings

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import app.screenclock.R
import app.screenclock.controller.WeatherNotice
import app.screenclock.controller.WeatherPanel
import app.screenclock.domain.burnin.normalizeIdleMillis
import app.screenclock.domain.burnin.normalizeRunMillis
import app.screenclock.domain.time.ClockThemeId
import app.screenclock.domain.time.ColorwayId
import app.screenclock.domain.time.DatePosition
import app.screenclock.domain.time.DigitSize
import app.screenclock.domain.time.FaceTime
import app.screenclock.domain.time.HourMode
import app.screenclock.domain.time.showcasedThemes
import app.screenclock.domain.weather.CityCandidate
import app.screenclock.domain.weather.WeatherCache
import app.screenclock.domain.weather.formatUpdatedAt
import app.screenclock.domain.weather.formatWeatherLine
import app.screenclock.ui.Amber
import app.screenclock.ui.AmberDeep
import app.screenclock.ui.AppIcon
import app.screenclock.ui.AppIconKind
import app.screenclock.ui.Cream
import app.screenclock.ui.FieldLabel
import app.screenclock.ui.GlassBorder
import app.screenclock.ui.Hairline
import app.screenclock.ui.IconBox
import app.screenclock.ui.Info
import app.screenclock.ui.Ink
import app.screenclock.ui.Paper
import app.screenclock.ui.Muted
import app.screenclock.ui.NavRow
import app.screenclock.ui.PageTitle
import app.screenclock.ui.SectionCard
import app.screenclock.ui.SectionHeader
import app.screenclock.ui.SegmentOption
import app.screenclock.ui.SegmentedRow
import app.screenclock.ui.ToggleRow
import app.screenclock.ui.appSliderColors
import app.screenclock.ui.theme.ThemedClock
import java.time.ZoneId

@Composable
fun SettingsScreen(
    face: FaceTime?,
    dateLine: String?,
    hourMode: HourMode,
    theme: ClockThemeId,
    colorway: ColorwayId,
    datePosition: DatePosition,
    digitSize: DigitSize,
    showDate: Boolean,
    showYear: Boolean,
    showWeekday: Boolean,
    showLunarDate: Boolean,
    showHoliday: Boolean,
    onHourMode: (HourMode) -> Unit,
    onTheme: (ClockThemeId) -> Unit,
    onColorway: (ColorwayId) -> Unit,
    onDatePosition: (DatePosition) -> Unit,
    onDigitSize: (DigitSize) -> Unit,
    onShowDate: (Boolean) -> Unit,
    onShowYear: (Boolean) -> Unit,
    onShowWeekday: (Boolean) -> Unit,
    onShowLunarDate: (Boolean) -> Unit,
    onShowHoliday: (Boolean) -> Unit,
    onBack: () -> Unit,
    weatherEnabled: Boolean,
    useLocation: Boolean,
    cityLabel: String?,
    weather: WeatherCache?,
    panel: WeatherPanel,
    onWeatherEnabled: (Boolean) -> Unit,
    onUseLocation: (Boolean) -> Unit,
    onLocate: (String) -> Unit,
    onLocationDenied: () -> Unit,
    onSearch: (String) -> Unit,
    onSelectCity: (CityCandidate) -> Unit,
    burnInEnabled: Boolean,
    burnInIdleMillis: Long,
    burnInRunMillis: Long,
    onBurnInEnabled: (Boolean) -> Unit,
    onBurnInIdleMillis: (Long) -> Unit,
    onBurnInRunMillis: (Long) -> Unit,
    nightDimEnabled: Boolean,
    nightStartHour: Int,
    nightEndHour: Int,
    nightWarmEnabled: Boolean,
    chargeKeepAwake: Boolean,
    onNightDimEnabled: (Boolean) -> Unit,
    onNightStartHour: (Int) -> Unit,
    onNightEndHour: (Int) -> Unit,
    onNightWarmEnabled: (Boolean) -> Unit,
    onChargeKeepAwake: (Boolean) -> Unit,
    onOpenOccasions: () -> Unit,
) {
    BackHandler(onBack = onBack)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        IconBox(
            icon = AppIconKind.Back,
            contentDescription = stringResource(R.string.back),
            onClick = onBack,
        )
        Spacer(modifier = Modifier.height(14.dp))
        PageTitle(stringResource(R.string.settings))
        Spacer(modifier = Modifier.height(22.dp))
        SectionCard {
            SectionHeader(icon = AppIconKind.Sliders, text = stringResource(R.string.section_display))
            FieldLabel(stringResource(R.string.hour_mode))
            Spacer(modifier = Modifier.height(8.dp))
            SegmentedRow(
                listOf(
                    SegmentOption(
                        label = stringResource(R.string.hour_mode_system),
                        selected = hourMode == HourMode.FollowSystem,
                        onClick = { onHourMode(HourMode.FollowSystem) },
                    ),
                    SegmentOption(
                        label = stringResource(R.string.hour_mode_12),
                        selected = hourMode == HourMode.H12,
                        onClick = { onHourMode(HourMode.H12) },
                    ),
                    SegmentOption(
                        label = stringResource(R.string.hour_mode_24),
                        selected = hourMode == HourMode.H24,
                        onClick = { onHourMode(HourMode.H24) },
                    ),
                ),
            )
            Spacer(modifier = Modifier.height(18.dp))
            FieldLabel(stringResource(R.string.theme))
            Spacer(modifier = Modifier.height(8.dp))
            ThemePicker(
                face = face,
                dateLine = dateLine,
                theme = theme,
                colorway = colorway,
                datePosition = datePosition,
                digitSize = digitSize,
                weather = weather,
                onTheme = onTheme,
                onColorway = onColorway,
            )
            Spacer(modifier = Modifier.height(10.dp))
            ToggleRow(
                label = stringResource(R.string.show_date),
                checked = showDate,
                onCheckedChange = onShowDate,
            )
            ToggleRow(
                label = stringResource(R.string.show_year),
                checked = showYear,
                enabled = showDate,
                onCheckedChange = onShowYear,
            )
            ToggleRow(
                label = stringResource(R.string.show_weekday),
                checked = showWeekday,
                enabled = showDate,
                onCheckedChange = onShowWeekday,
            )
            ToggleRow(
                label = stringResource(R.string.show_lunar),
                checked = showLunarDate,
                enabled = showDate,
                onCheckedChange = onShowLunarDate,
            )
            ToggleRow(
                label = stringResource(R.string.show_holiday),
                checked = showHoliday,
                enabled = showDate,
                onCheckedChange = onShowHoliday,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        SectionCard {
            SectionHeader(icon = AppIconKind.Sliders, text = stringResource(R.string.section_layout))
            FieldLabel(stringResource(R.string.date_position))
            Spacer(modifier = Modifier.height(8.dp))
            SegmentedRow(listOf(
                SegmentOption(stringResource(R.string.position_above), datePosition == DatePosition.Above) { onDatePosition(DatePosition.Above) },
                SegmentOption(stringResource(R.string.position_below), datePosition == DatePosition.Below) { onDatePosition(DatePosition.Below) },
            ))
            Spacer(modifier = Modifier.height(16.dp))
            FieldLabel(stringResource(R.string.digit_size))
            Spacer(modifier = Modifier.height(8.dp))
            SegmentedRow(listOf(
                SegmentOption(stringResource(R.string.digit_compact), digitSize == DigitSize.Compact) { onDigitSize(DigitSize.Compact) },
                SegmentOption(stringResource(R.string.digit_regular), digitSize == DigitSize.Regular) { onDigitSize(DigitSize.Regular) },
                SegmentOption(stringResource(R.string.digit_large), digitSize == DigitSize.Large) { onDigitSize(DigitSize.Large) },
            ))
        }
        Spacer(modifier = Modifier.height(16.dp))
        SectionCard {
            SectionHeader(icon = AppIconKind.Location, text = stringResource(R.string.section_weather))
            WeatherSection(
                enabled = weatherEnabled,
                useLocation = useLocation,
                cityLabel = cityLabel,
                weather = weather,
                panel = panel,
                onEnabled = onWeatherEnabled,
                onUseLocation = onUseLocation,
                onLocate = onLocate,
                onLocationDenied = onLocationDenied,
                onSearch = onSearch,
                onSelectCity = onSelectCity,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        SectionCard {
            SectionHeader(icon = AppIconKind.Timer, text = stringResource(R.string.section_burnin))
            BurnInSection(
                enabled = burnInEnabled,
                idleMillis = burnInIdleMillis,
                runMillis = burnInRunMillis,
                onEnabled = onBurnInEnabled,
                onIdleMillis = onBurnInIdleMillis,
                onRunMillis = onBurnInRunMillis,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        SectionCard {
            SectionHeader(icon = AppIconKind.Moon, text = stringResource(R.string.section_night))
            NightSection(
                dimEnabled = nightDimEnabled,
                startHour = nightStartHour,
                endHour = nightEndHour,
                warmEnabled = nightWarmEnabled,
                chargeKeepAwake = chargeKeepAwake,
                onDimEnabled = onNightDimEnabled,
                onStartHour = onNightStartHour,
                onEndHour = onNightEndHour,
                onWarmEnabled = onNightWarmEnabled,
                onChargeKeepAwake = onChargeKeepAwake,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        SectionCard {
            SectionHeader(icon = AppIconKind.Heart, text = stringResource(R.string.section_occasion))
            NavRow(
                icon = AppIconKind.Heart,
                label = stringResource(R.string.occasion),
                onClick = onOpenOccasions,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        SectionCard {
            SectionHeader(icon = AppIconKind.Link, text = stringResource(R.string.section_about))
            AboutCredit()
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ThemePicker(
    face: FaceTime?,
    dateLine: String?,
    theme: ClockThemeId,
    colorway: ColorwayId,
    datePosition: DatePosition,
    digitSize: DigitSize,
    weather: WeatherCache?,
    onTheme: (ClockThemeId) -> Unit,
    onColorway: (ColorwayId) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        showcasedThemes.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowItems.forEach { id ->
                    ThemePreview(
                        label = "${showcasedThemes.indexOf(id) + 1}".padStart(2, '0') + " · " + stringResource(themeLabel(id)),
                        subtitle = stringResource(themeSubtitle(id)),
                        selected = theme == id,
                        onClick = { onTheme(id) },
                        modifier = Modifier.weight(1f),
                    ) {
                        ThemeFace(id, colorway, datePosition, digitSize, face, dateLine, weather)
                    }
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
    if (theme == ClockThemeId.Flip) {
        Spacer(modifier = Modifier.height(14.dp))
        FieldLabel(stringResource(R.string.colorway_name))
        Spacer(modifier = Modifier.height(8.dp))
        SegmentedRow(
            listOf(
                SegmentOption(
                    label = stringResource(R.string.colorway_ink),
                    selected = colorway == ColorwayId.InkBlack,
                    onClick = { onColorway(ColorwayId.InkBlack) },
                ),
                SegmentOption(
                    label = stringResource(R.string.colorway_snow),
                    selected = colorway == ColorwayId.SnowWhite,
                    onClick = { onColorway(ColorwayId.SnowWhite) },
                ),
                SegmentOption(
                    label = stringResource(R.string.colorway_graphite),
                    selected = colorway == ColorwayId.Graphite,
                    onClick = { onColorway(ColorwayId.Graphite) },
                ),
            ),
        )
    }
}

private fun themeSubtitle(id: ClockThemeId): Int = when (id) {
    ClockThemeId.Flip -> R.string.theme_flip_desc
    ClockThemeId.Tube -> R.string.theme_tube_desc
    ClockThemeId.Digital -> R.string.theme_digital_desc
    ClockThemeId.Analog -> R.string.theme_analog_desc
    ClockThemeId.Earth -> R.string.theme_earth_desc
    ClockThemeId.Fluid -> R.string.theme_fluid_desc
    ClockThemeId.Neon -> R.string.theme_neon_desc
}

private fun themeLabel(id: ClockThemeId): Int {
    return when (id) {
        ClockThemeId.Flip -> R.string.theme_flip
        ClockThemeId.Tube -> R.string.theme_tube
        ClockThemeId.Digital -> R.string.theme_digital
        ClockThemeId.Analog -> R.string.theme_analog
        ClockThemeId.Earth -> R.string.theme_earth
        ClockThemeId.Fluid -> R.string.theme_fluid
        ClockThemeId.Neon -> R.string.theme_neon
    }
}

@Composable
private fun ThemePreview(
    label: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    face: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier.selectable(
            selected = selected,
            role = Role.RadioButton,
            onClick = onClick,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
                .clip(shape)
                .border(
                    width = if (selected) 1.5.dp else 1.dp,
                    color = if (selected) Amber else GlassBorder,
                    shape = shape,
                )
                .background(Ink),
        ) {
            Box(Modifier.fillMaxSize().clearAndSetSemantics { }) {
                face()
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            color = if (selected) AmberDeep else Cream,
            fontSize = 14.sp,
        )
        Text(
            text = subtitle,
            color = Muted,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ThemeFace(
    theme: ClockThemeId,
    colorway: ColorwayId,
    datePosition: DatePosition,
    digitSize: DigitSize,
    face: FaceTime?,
    dateLine: String?,
    weather: WeatherCache?,
) {
    if (face == null) return
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val expanded = maxWidth >= 240.dp
        ThemedClock(
            theme = theme,
            colorway = colorway,
            face = face,
            dateLine = dateLine,
            weatherLine = formatWeatherLine(weather, showRange = expanded),
            occasionLine = null,
            expanded = expanded,
            animate = false,
            datePosition = datePosition,
            digitSize = digitSize,
            modifier = Modifier.fillMaxSize(),
            preview = true,
        )
    }
}

@Composable
private fun WeatherSection(
    enabled: Boolean,
    useLocation: Boolean,
    cityLabel: String?,
    weather: WeatherCache?,
    panel: WeatherPanel,
    onEnabled: (Boolean) -> Unit,
    onUseLocation: (Boolean) -> Unit,
    onLocate: (String) -> Unit,
    onLocationDenied: () -> Unit,
    onSearch: (String) -> Unit,
    onSelectCity: (CityCandidate) -> Unit,
) {
    val context = LocalContext.current
    val here = stringResource(R.string.weather_here)
    var query by rememberSaveable { mutableStateOf("") }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onLocate(here) else onLocationDenied()
    }
    ToggleRow(
        label = stringResource(R.string.show_weather),
        checked = enabled,
        onCheckedChange = onEnabled,
    )
    if (!enabled) {
        Text(
            text = stringResource(R.string.weather_off),
            color = Muted,
            fontSize = 14.sp,
        )
        return
    }
    if (cityLabel != null) {
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(kind = AppIconKind.Location, tint = Info, size = 16.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = cityLabel, color = Cream, fontSize = 16.sp)
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    ToggleRow(
        label = stringResource(R.string.weather_use_location),
        checked = useLocation,
        onCheckedChange = { want ->
            if (!want) {
                onUseLocation(false)
            } else if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                onLocate(here)
            } else {
                permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        },
    )
    Spacer(modifier = Modifier.height(12.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it.take(40) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = TextStyle(color = Cream, fontSize = 16.sp),
            shape = RoundedCornerShape(14.dp),
            placeholder = {
                Text(text = stringResource(R.string.weather_search_hint), color = Muted, fontSize = 16.sp)
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Cream,
                unfocusedTextColor = Cream,
                focusedBorderColor = Amber,
                unfocusedBorderColor = Hairline,
                cursorColor = Amber,
            ),
        )
        Spacer(modifier = Modifier.width(10.dp))
        IconBox(
            icon = AppIconKind.Search,
            contentDescription = stringResource(R.string.weather_search),
            onClick = { onSearch(query) },
        )
    }
    panel.results.forEach { place ->
        key(place.label, place.latitude, place.longitude) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelectCity(place) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcon(kind = AppIconKind.Location, tint = Muted, size = 16.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = place.label, color = Cream, fontSize = 16.sp)
            }
        }
    }
    val status = when (panel.notice) {
        WeatherNotice.NotFound -> stringResource(R.string.weather_not_found)
        WeatherNotice.Unavailable -> stringResource(R.string.weather_unavailable)
        WeatherNotice.NoPermission -> stringResource(R.string.weather_no_permission)
        WeatherNotice.NoLocation -> stringResource(R.string.weather_no_location)
        WeatherNotice.None -> null
    }
    if (status != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = status, color = Muted, fontSize = 14.sp)
    }
    if (panel.notice == WeatherNotice.Unavailable && weather != null) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(
                R.string.weather_updated,
                formatUpdatedAt(weather.fetchedAtEpochMillis, ZoneId.systemDefault()),
            ),
            color = Muted,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun BurnInSection(
    enabled: Boolean,
    idleMillis: Long,
    runMillis: Long,
    onEnabled: (Boolean) -> Unit,
    onIdleMillis: (Long) -> Unit,
    onRunMillis: (Long) -> Unit,
) {
    ToggleRow(
        label = stringResource(R.string.burnin_enable),
        checked = enabled,
        onCheckedChange = onEnabled,
    )
    if (!enabled) return
    val idleMinutes = (normalizeIdleMillis(idleMillis) / 60_000L).toFloat()
    var idleValue by remember(idleMinutes) { mutableFloatStateOf(idleMinutes) }
    DurationRow(
        label = stringResource(R.string.burnin_idle),
        value = stringResource(R.string.burnin_minutes, idleValue.toInt()),
    )
    Slider(
        value = idleValue,
        onValueChange = { idleValue = it },
        onValueChangeFinished = { onIdleMillis(idleValue.toLong() * 60_000L) },
        valueRange = 1f..60f,
        steps = 58,
        colors = appSliderColors(),
    )
    val runSeconds = (normalizeRunMillis(runMillis) / 1_000L).toFloat()
    var runValue by remember(runSeconds) { mutableFloatStateOf(runSeconds) }
    DurationRow(
        label = stringResource(R.string.burnin_run),
        value = runDurationText(runValue.toLong() * 1_000L),
    )
    Slider(
        value = runValue,
        onValueChange = { runValue = it },
        onValueChangeFinished = { onRunMillis(runValue.toLong() * 1_000L) },
        valueRange = 10f..1_800f,
        steps = 178,
        colors = appSliderColors(),
    )
}

@Composable
private fun DurationRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FieldLabel(label)
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value,
            color = Cream,
            fontSize = 15.sp,
        )
    }
}

@Composable
private fun runDurationText(millis: Long): String {
    val totalSeconds = (normalizeRunMillis(millis) / 1_000L).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return when {
        minutes > 0 && seconds == 0 -> stringResource(R.string.burnin_minutes, minutes)
        minutes > 0 -> stringResource(R.string.burnin_minutes_seconds, minutes, seconds)
        else -> stringResource(R.string.burnin_seconds, seconds)
    }
}

@Composable
private fun NightSection(
    dimEnabled: Boolean,
    startHour: Int,
    endHour: Int,
    warmEnabled: Boolean,
    chargeKeepAwake: Boolean,
    onDimEnabled: (Boolean) -> Unit,
    onStartHour: (Int) -> Unit,
    onEndHour: (Int) -> Unit,
    onWarmEnabled: (Boolean) -> Unit,
    onChargeKeepAwake: (Boolean) -> Unit,
) {
    ToggleRow(
        label = stringResource(R.string.night_dim),
        checked = dimEnabled,
        onCheckedChange = onDimEnabled,
    )
    if (dimEnabled) {
        Text(
            text = stringResource(R.string.night_hint),
            color = Muted,
            fontSize = 13.sp,
        )
        Spacer(modifier = Modifier.height(6.dp))
        HourStep(
            label = stringResource(R.string.night_start),
            hour = startHour,
            onHour = onStartHour,
        )
        HourStep(
            label = stringResource(R.string.night_end),
            hour = endHour,
            onHour = onEndHour,
        )
    }
    ToggleRow(
        label = stringResource(R.string.night_warm),
        checked = warmEnabled,
        onCheckedChange = onWarmEnabled,
    )
    ToggleRow(
        label = stringResource(R.string.charge_keep_awake),
        checked = chargeKeepAwake,
        onCheckedChange = onChargeKeepAwake,
    )
}

@Composable
private fun HourStep(label: String, hour: Int, onHour: (Int) -> Unit) {
    val shown = ((hour % 24) + 24) % 24
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FieldLabel(label)
        Spacer(modifier = Modifier.weight(1f))
        IconBox(
            icon = AppIconKind.ChevronLeft,
            contentDescription = stringResource(R.string.night_earlier),
            onClick = { onHour((shown + 23) % 24) },
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.night_hour, shown),
            color = Cream,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(58.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        IconBox(
            icon = AppIconKind.ChevronRight,
            contentDescription = stringResource(R.string.night_later),
            onClick = { onHour((shown + 1) % 24) },
        )
    }
}

@Composable
private fun AboutCredit() {
    val context = LocalContext.current
    Text(
        text = stringResource(R.string.about_intro),
        color = Info,
        fontSize = 14.sp,
    )
    Spacer(modifier = Modifier.height(12.dp))
    NavRow(
        icon = AppIconKind.Link,
        label = stringResource(R.string.about_github),
        trailing = AppIconKind.Link,
        onClick = { openLink(context, "https://github.com/AlexWuYh/ScreenClock") },
    )
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 44.dp)) {
        AppIcon(kind = AppIconKind.Heart, tint = Amber, size = 18.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = stringResource(R.string.about_author), color = Cream, fontSize = 16.sp)
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text(text = stringResource(R.string.about_license), color = Cream, fontSize = 16.sp)
    Spacer(modifier = Modifier.height(2.dp))
    Text(
        text = stringResource(R.string.about_license_detail),
        color = Muted,
        fontSize = 13.sp,
    )
    Spacer(modifier = Modifier.height(4.dp))
    NavRow(
        icon = AppIconKind.Link,
        label = stringResource(R.string.weather_credit),
        trailing = AppIconKind.Link,
        onClick = { openLink(context, "https://open-meteo.com/") },
    )
}

private fun openLink(context: android.content.Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    } catch (_: ActivityNotFoundException) {
        // The text stays on the page when the device has no browser.
    }
}
