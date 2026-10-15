package app.screenclock.ui.settings

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.screenclock.R
import app.screenclock.domain.burnin.normalizeIdleMillis
import app.screenclock.domain.burnin.normalizeRunMillis
import app.screenclock.domain.time.ClockThemeId
import app.screenclock.domain.time.ColorwayId
import app.screenclock.domain.time.FaceTime
import app.screenclock.domain.time.HourMode
import app.screenclock.domain.weather.CityCandidate
import app.screenclock.domain.weather.WeatherCache
import app.screenclock.domain.weather.formatUpdatedAt
import app.screenclock.domain.weather.formatWeatherLine
import app.screenclock.ui.Amber
import app.screenclock.ui.clock.WeatherNotice
import app.screenclock.ui.clock.WeatherPanel
import app.screenclock.ui.ChromeBar
import app.screenclock.ui.Cream
import app.screenclock.ui.Info
import app.screenclock.ui.Ink
import app.screenclock.ui.theme.ThemedClock
import java.time.ZoneId

@Composable
fun SettingsScreen(
    face: FaceTime?,
    dateLine: String?,
    hourMode: HourMode,
    theme: ClockThemeId,
    colorway: ColorwayId,
    showDate: Boolean,
    onHourMode: (HourMode) -> Unit,
    onTheme: (ClockThemeId) -> Unit,
    onShowDate: (Boolean) -> Unit,
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
) {
    BackHandler(onBack = onBack)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 12.dp),
    ) {
        TextButton(onClick = onBack) {
            Text(
                text = stringResource(R.string.back),
                color = Cream,
                fontSize = 16.sp,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.settings),
            color = Cream,
            fontFamily = FontFamily.Serif,
            fontSize = 36.sp,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(modifier = Modifier.height(28.dp))
        Text(
            text = stringResource(R.string.section_display),
            color = Cream,
            fontSize = 14.sp,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.hour_mode),
            color = Info,
            fontSize = 14.sp,
        )
        Column(modifier = Modifier.selectableGroup()) {
            HourModeRow(
                label = stringResource(R.string.hour_mode_system),
                selected = hourMode == HourMode.FollowSystem,
                onClick = { onHourMode(HourMode.FollowSystem) },
            )
            HourModeRow(
                label = stringResource(R.string.hour_mode_12),
                selected = hourMode == HourMode.H12,
                onClick = { onHourMode(HourMode.H12) },
            )
            HourModeRow(
                label = stringResource(R.string.hour_mode_24),
                selected = hourMode == HourMode.H24,
                onClick = { onHourMode(HourMode.H24) },
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.theme),
            color = Info,
            fontSize = 14.sp,
        )
        Spacer(modifier = Modifier.height(12.dp))
        ThemePicker(
            face = face,
            dateLine = dateLine,
            theme = theme,
            colorway = colorway,
            weather = weather,
            onTheme = onTheme,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .toggleable(
                    value = showDate,
                    role = Role.Switch,
                    onValueChange = onShowDate,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.show_date),
                color = Cream,
                fontSize = 18.sp,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = showDate,
                onCheckedChange = null,
                modifier = Modifier.clearAndSetSemantics { },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Ink,
                    checkedTrackColor = Cream,
                    uncheckedThumbColor = Cream,
                    uncheckedTrackColor = ChromeBar,
                    uncheckedBorderColor = Info,
                ),
            )
        }
        Spacer(modifier = Modifier.height(28.dp))
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
        Spacer(modifier = Modifier.height(28.dp))
        BurnInSection(
            enabled = burnInEnabled,
            idleMillis = burnInIdleMillis,
            runMillis = burnInRunMillis,
            onEnabled = onBurnInEnabled,
            onIdleMillis = onBurnInIdleMillis,
            onRunMillis = onBurnInRunMillis,
        )
        Spacer(modifier = Modifier.height(28.dp))
        AboutCredit()
    }
}

@Composable
private fun ThemePicker(
    face: FaceTime?,
    dateLine: String?,
    theme: ClockThemeId,
    colorway: ColorwayId,
    weather: WeatherCache?,
    onTheme: (ClockThemeId) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val sideBySide = maxWidth >= 520.dp
        if (sideBySide) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ThemePreview(
                    label = stringResource(R.string.theme_flip),
                    selected = theme == ClockThemeId.Flip,
                    onClick = { onTheme(ClockThemeId.Flip) },
                    modifier = Modifier.weight(1f),
                ) {
                    ThemeFace(ClockThemeId.Flip, colorway, face, dateLine, weather, expanded = true)
                }
                ThemePreview(
                    label = stringResource(R.string.theme_tube),
                    selected = theme == ClockThemeId.Tube,
                    onClick = { onTheme(ClockThemeId.Tube) },
                    modifier = Modifier.weight(1f),
                ) {
                    ThemeFace(ClockThemeId.Tube, colorway, face, dateLine, weather, expanded = true)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ThemePreview(
                    label = stringResource(R.string.theme_flip),
                    selected = theme == ClockThemeId.Flip,
                    onClick = { onTheme(ClockThemeId.Flip) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ThemeFace(ClockThemeId.Flip, colorway, face, dateLine, weather, expanded = false)
                }
                ThemePreview(
                    label = stringResource(R.string.theme_tube),
                    selected = theme == ClockThemeId.Tube,
                    onClick = { onTheme(ClockThemeId.Tube) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ThemeFace(ClockThemeId.Tube, colorway, face, dateLine, weather, expanded = false)
                }
            }
        }
    }
}

@Composable
private fun ThemePreview(
    label: String,
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
                .height(176.dp)
                .clip(shape)
                .border(
                    width = if (selected) 1.5.dp else 1.dp,
                    color = if (selected) Amber else ChromeBar,
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
            color = if (selected) Amber else Info,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun ThemeFace(
    theme: ClockThemeId,
    colorway: ColorwayId,
    face: FaceTime?,
    dateLine: String?,
    weather: WeatherCache?,
    expanded: Boolean,
) {
    if (face == null) return
    ThemedClock(
        theme = theme,
        colorway = colorway,
        face = face,
        dateLine = dateLine,
        weatherLine = formatWeatherLine(weather, showRange = expanded),
        occasionLine = null,
        expanded = expanded,
        animate = false,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun HourModeRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            modifier = Modifier.clearAndSetSemantics { },
            colors = RadioButtonDefaults.colors(
                selectedColor = Cream,
                unselectedColor = Info,
            ),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = Cream,
            fontSize = 18.sp,
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
    Text(
        text = stringResource(R.string.section_weather),
        color = Cream,
        fontSize = 14.sp,
        modifier = Modifier.semantics { heading() },
    )
    Spacer(modifier = Modifier.height(4.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = enabled, role = Role.Switch, onValueChange = onEnabled),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.show_weather),
            color = Cream,
            fontSize = 18.sp,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = enabled,
            onCheckedChange = null,
            modifier = Modifier.clearAndSetSemantics { },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Ink,
                checkedTrackColor = Cream,
                uncheckedThumbColor = Cream,
                uncheckedTrackColor = ChromeBar,
                uncheckedBorderColor = Info,
            ),
        )
    }
    if (!enabled) {
        Text(
            text = stringResource(R.string.weather_off),
            color = Info,
            fontSize = 16.sp,
        )
        return
    }
    if (cityLabel != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = cityLabel, color = Cream, fontSize = 18.sp)
    }
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(
                value = useLocation,
                role = Role.Switch,
                onValueChange = { want ->
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
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.weather_use_location),
            color = Cream,
            fontSize = 18.sp,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = useLocation,
            onCheckedChange = null,
            modifier = Modifier.clearAndSetSemantics { },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Ink,
                checkedTrackColor = Cream,
                uncheckedThumbColor = Cream,
                uncheckedTrackColor = ChromeBar,
                uncheckedBorderColor = Info,
            ),
        )
    }
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
            textStyle = TextStyle(color = Cream, fontSize = 18.sp),
            placeholder = {
                Text(text = stringResource(R.string.weather_search_hint), color = Info, fontSize = 18.sp)
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Cream,
                unfocusedTextColor = Cream,
                focusedBorderColor = Cream,
                unfocusedBorderColor = Info,
                cursorColor = Cream,
            ),
        )
        Spacer(modifier = Modifier.width(8.dp))
        TextButton(
            onClick = { onSearch(query) },
            enabled = !panel.busy && query.isNotBlank(),
        ) {
            Text(
                text = stringResource(R.string.weather_search),
                color = if (!panel.busy && query.isNotBlank()) Cream else Info,
                fontSize = 18.sp,
            )
        }
    }
    panel.results.forEach { place ->
        key(place.label, place.latitude, place.longitude) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable { onSelectCity(place) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = place.label, color = Cream, fontSize = 18.sp)
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
        Text(text = status, color = Info, fontSize = 16.sp)
    }
    if (panel.notice == WeatherNotice.Unavailable && weather != null) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(
                R.string.weather_updated,
                formatUpdatedAt(weather.fetchedAtEpochMillis, ZoneId.systemDefault()),
            ),
            color = Info,
            fontSize = 16.sp,
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
    Text(
        text = stringResource(R.string.section_burnin),
        color = Cream,
        fontSize = 14.sp,
        modifier = Modifier.semantics { heading() },
    )
    Spacer(modifier = Modifier.height(4.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = enabled, role = Role.Switch, onValueChange = onEnabled),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.burnin_enable),
            color = Cream,
            fontSize = 18.sp,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = enabled,
            onCheckedChange = null,
            modifier = Modifier.clearAndSetSemantics { },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Ink,
                checkedTrackColor = Cream,
                uncheckedThumbColor = Cream,
                uncheckedTrackColor = ChromeBar,
                uncheckedBorderColor = Info,
            ),
        )
    }
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
        colors = SliderDefaults.colors(
            thumbColor = Cream,
            activeTrackColor = Cream,
            inactiveTrackColor = ChromeBar,
        ),
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
        colors = SliderDefaults.colors(
            thumbColor = Cream,
            activeTrackColor = Cream,
            inactiveTrackColor = ChromeBar,
        ),
    )
}

@Composable
private fun DurationRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = Info,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            color = Cream,
            fontSize = 16.sp,
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
private fun AboutCredit() {
    val context = LocalContext.current
    Text(
        text = stringResource(R.string.section_about),
        color = Cream,
        fontSize = 14.sp,
        modifier = Modifier.semantics { heading() },
    )
    Spacer(modifier = Modifier.height(4.dp))
    TextButton(
        onClick = {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://open-meteo.com/"))
            try {
                context.startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                // The credit stays on the page when the device has no browser.
            }
        },
    ) {
        Text(
            text = stringResource(R.string.weather_credit),
            color = Cream,
            fontSize = 16.sp,
        )
    }
}
