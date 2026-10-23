package app.screenclock.ui.settings

import android.Manifest
import androidx.compose.material3.TextButton
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import app.screenclock.ui.glassPressable
import androidx.compose.ui.semantics.LiveRegionMode
import android.provider.Settings
import android.net.Uri
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import app.screenclock.domain.occasion.CeremonyLook
import app.screenclock.domain.occasion.CeremonyStyle
import app.screenclock.domain.occasion.CeremonyTone
import app.screenclock.domain.time.showcasedThemes
import app.screenclock.domain.weather.CityCandidate
import app.screenclock.domain.weather.WeatherCache
import app.screenclock.domain.weather.formatUpdatedAt
import app.screenclock.domain.weather.formatWeatherLine
import app.screenclock.ui.UiAccent
import app.screenclock.ui.UiAccentDeep
import app.screenclock.ui.AppIcon
import app.screenclock.ui.AppIconKind
import app.screenclock.ui.Cream
import app.screenclock.ui.FieldLabel
import app.screenclock.ui.GlassBorder
import app.screenclock.ui.GlassFill
import app.screenclock.ui.GlassFillHigh
import app.screenclock.ui.glassBackdrop
import app.screenclock.ui.glassSurface
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

private val SettingsMaxWidth = 780.dp

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
    ceremonyLook: CeremonyLook,
    onCeremonyStyle: (CeremonyStyle) -> Unit,
    onCeremonyTone: (CeremonyTone) -> Unit,
) {
    BackHandler(onBack = onBack)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .glassBackdrop()
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        // The back action stays reachable while only the form below it scrolls.
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().glassSurface(corner = 0.dp, fill = GlassFill),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier
                    .width(minOf(maxWidth, SettingsMaxWidth))
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconBox(
                    icon = AppIconKind.Back,
                    contentDescription = stringResource(R.string.back),
                    onClick = onBack,
                )
                Spacer(modifier = Modifier.width(14.dp))
                PageTitle(stringResource(R.string.settings))
            }
        }
        BoxWithConstraints(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .width(minOf(maxWidth, SettingsMaxWidth))
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
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
                    Spacer(modifier = Modifier.height(14.dp))
                    FieldLabel(stringResource(R.string.ceremony_style))
                    Spacer(modifier = Modifier.height(8.dp))
                    SegmentedRow(
                        listOf(
                            SegmentOption(
                                label = stringResource(R.string.ceremony_style_card),
                                selected = ceremonyLook.style == CeremonyStyle.Card,
                                onClick = { onCeremonyStyle(CeremonyStyle.Card) },
                            ),
                            SegmentOption(
                                label = stringResource(R.string.ceremony_style_immersive),
                                selected = ceremonyLook.style == CeremonyStyle.Immersive,
                                onClick = { onCeremonyStyle(CeremonyStyle.Immersive) },
                            ),
                        ),
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    FieldLabel(stringResource(R.string.ceremony_tone))
                    Spacer(modifier = Modifier.height(8.dp))
                    SegmentedRow(
                        listOf(
                            SegmentOption(
                                label = stringResource(R.string.ceremony_tone_system),
                                selected = ceremonyLook.tone == CeremonyTone.FollowSystem,
                                onClick = { onCeremonyTone(CeremonyTone.FollowSystem) },
                            ),
                            SegmentOption(
                                label = stringResource(R.string.ceremony_tone_light),
                                selected = ceremonyLook.tone == CeremonyTone.Light,
                                onClick = { onCeremonyTone(CeremonyTone.Light) },
                            ),
                            SegmentOption(
                                label = stringResource(R.string.ceremony_tone_dark),
                                selected = ceremonyLook.tone == CeremonyTone.Dark,
                                onClick = { onCeremonyTone(CeremonyTone.Dark) },
                            ),
                        ),
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
    }
}

/** Keep real clock previews proportionate instead of stretching them into wide strips. */
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
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val gridWidth = minOf(maxWidth, 680.dp)
        val columns = themePreviewColumns(gridWidth.value)
        Column(
            modifier = Modifier
                .width(gridWidth)
                .align(Alignment.TopCenter),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            showcasedThemes.chunked(columns).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (columns == 2 && rowItems.size == 1) Spacer(Modifier.weight(0.5f))
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
                    if (columns == 2 && rowItems.size == 1) Spacer(Modifier.weight(0.5f))
                }
            }
            if (theme == ClockThemeId.Flip) {
                FieldLabel(stringResource(R.string.colorway_name))
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
    }
}

/** Mobile gets one legible preview per row; larger screens can show two. */
internal fun themePreviewColumns(widthDp: Float): Int = if (widthDp < 360f) 1 else 2

/** Preserve an image-like ratio while keeping both small and tablet cards useful. */
internal fun themePreviewHeight(widthDp: Float): Float = (widthDp * 0.62f).coerceIn(132f, 220f)

private fun themeSubtitle(id: ClockThemeId): Int = when (id) {
    ClockThemeId.Flip -> R.string.theme_flip_desc
    ClockThemeId.Tube -> R.string.theme_tube_desc
    ClockThemeId.Digital -> R.string.theme_digital_desc
    ClockThemeId.Analog -> R.string.theme_analog_desc
    ClockThemeId.Earth -> R.string.theme_earth_desc
    ClockThemeId.Fluid -> R.string.theme_fluid_desc
    ClockThemeId.Neon -> R.string.theme_neon_desc
    ClockThemeId.Couple -> R.string.theme_couple_desc
    ClockThemeId.Minimal -> R.string.theme_minimal_desc
    ClockThemeId.Family -> R.string.theme_family_desc
    ClockThemeId.SpongePat -> R.string.theme_spongepat_desc
    ClockThemeId.Krabs -> R.string.theme_krabs_desc
    ClockThemeId.Bikini -> R.string.theme_bikini_desc
}

internal fun themeLabel(id: ClockThemeId): Int {
    return when (id) {
        ClockThemeId.Flip -> R.string.theme_flip
        ClockThemeId.Tube -> R.string.theme_tube
        ClockThemeId.Digital -> R.string.theme_digital
        ClockThemeId.Analog -> R.string.theme_analog
        ClockThemeId.Earth -> R.string.theme_earth
        ClockThemeId.Fluid -> R.string.theme_fluid
        ClockThemeId.Neon -> R.string.theme_neon
        ClockThemeId.Couple -> R.string.theme_couple
        ClockThemeId.Minimal -> R.string.theme_minimal
        ClockThemeId.Family -> R.string.theme_family
        ClockThemeId.SpongePat -> R.string.theme_spongepat
        ClockThemeId.Krabs -> R.string.theme_krabs
        ClockThemeId.Bikini -> R.string.theme_bikini
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
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(themePreviewHeight(maxWidth.value).dp)
                    .clip(shape)
                    .border(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) UiAccent else GlassBorder,
                        shape = shape,
                    )
                    .background(Ink),
            ) {
                Box(Modifier.fillMaxSize().clearAndSetSemantics { }) {
                    face()
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            color = if (selected) UiAccentDeep else Cream,
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
                focusedBorderColor = UiAccent,
                unfocusedBorderColor = Hairline,
                cursorColor = UiAccent,
            ),
        )
        Spacer(modifier = Modifier.width(10.dp))
        IconBox(
            icon = AppIconKind.Search,
            contentDescription = stringResource(R.string.weather_search),
            onClick = { onSearch(query) },
            enabled = !panel.busy,
        )
    }
    if (panel.busy) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.weather_busy),
            color = Muted,
            fontSize = 14.sp,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
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
        Text(
            text = status,
            color = Muted,
            fontSize = 14.sp,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
    if (panel.notice == WeatherNotice.NoPermission) {
        // After two refusals the system stops asking, so offer the page where it can be turned on.
        TextButton(
            onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.fromParts("package", context.packageName, null))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            },
        ) {
            Text(text = stringResource(R.string.weather_open_settings), color = UiAccentDeep)
        }
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
    Text(
        text = stringResource(R.string.burnin_hint),
        color = Muted,
        fontSize = 13.sp,
    )
    Spacer(modifier = Modifier.height(6.dp))
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
    val version = remember(context) {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
        } catch (_: Exception) {
            ""
        }
    }
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = stringResource(R.string.app_name),
            color = Cream,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
        )
        if (version.isNotEmpty()) {
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.about_version, version),
                color = Muted,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
    }
    Spacer(modifier = Modifier.height(6.dp))
    Text(text = stringResource(R.string.about_intro), color = Info, fontSize = 14.sp)
    Spacer(modifier = Modifier.height(10.dp))
    NavRow(
        icon = AppIconKind.Link,
        label = stringResource(R.string.about_github),
        trailing = AppIconKind.Link,
        onClick = { openLink(context, "https://github.com/AlexWuYh/ScreenClock") },
    )

    AboutDivider()
    Text(
        text = stringResource(R.string.about_license_title),
        color = Cream,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.semantics { heading() },
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(text = stringResource(R.string.about_license_mit), color = Info, fontSize = 14.sp)

    AboutDivider()
    Text(
        text = stringResource(R.string.about_third_party),
        color = Cream,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.semantics { heading() },
    )
    Spacer(modifier = Modifier.height(6.dp))
    CreditRow(R.string.credit_lunar_name, R.string.credit_lunar_detail)
    CreditRow(
        R.string.credit_meteo_name,
        R.string.credit_meteo_detail,
        onClick = { openLink(context, "https://open-meteo.com/") },
    )
    CreditRow(R.string.credit_fonts_name, R.string.credit_fonts_detail)
    CreditRow(R.string.credit_nasa_name, R.string.credit_nasa_detail)
}

@Composable
private fun AboutDivider() {
    Spacer(modifier = Modifier.height(14.dp))
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Hairline))
    Spacer(modifier = Modifier.height(14.dp))
}

/** One credited work: its name, then what it is used for and its licence beneath. */
@Composable
private fun CreditRow(name: Int, detail: Int, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(if (onClick != null) Modifier.clip(RoundedCornerShape(12.dp)).glassPressable(onClick = onClick) else Modifier)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(name), color = Cream, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = stringResource(detail), color = Muted, fontSize = 13.sp)
        }
        if (onClick != null) AppIcon(kind = AppIconKind.Link, tint = Muted, size = 18.dp)
    }
}

private fun openLink(context: android.content.Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    } catch (_: ActivityNotFoundException) {
        // The text stays on the page when the device has no browser.
    }
}
