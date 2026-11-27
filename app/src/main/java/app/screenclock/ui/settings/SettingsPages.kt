package app.screenclock.ui.settings

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.screenclock.R
import app.screenclock.controller.WeatherNotice
import app.screenclock.data.update.UpdateChecker
import app.screenclock.domain.burnin.IDLE_MAX_MILLIS
import app.screenclock.domain.burnin.IDLE_MIN_MILLIS
import app.screenclock.domain.burnin.RUN_MAX_MILLIS
import app.screenclock.domain.burnin.RUN_MIN_MILLIS
import app.screenclock.domain.burnin.ScreensaverTheme
import app.screenclock.domain.burnin.normalizeIdleMillis
import app.screenclock.domain.burnin.normalizeRunMillis
import app.screenclock.domain.occasion.AUTO_PLAY_SECONDS_CHOICES
import app.screenclock.domain.occasion.CeremonyStyle
import app.screenclock.domain.settings.IDLE_PRESET_MINUTES
import app.screenclock.domain.settings.RUN_PRESET_SECONDS
import app.screenclock.domain.settings.idleFromMinutes
import app.screenclock.domain.settings.settingsIsShort
import app.screenclock.domain.settings.nightBarSpans
import app.screenclock.domain.settings.rhythmWeights
import app.screenclock.domain.settings.runFromSeconds
import app.screenclock.domain.settings.stepIdleMillis
import app.screenclock.domain.settings.stepRunMillis
import app.screenclock.domain.time.ClockThemeId
import app.screenclock.domain.time.ColorwayId
import app.screenclock.domain.time.DatePosition
import app.screenclock.domain.time.DigitSize
import app.screenclock.domain.time.HourMode
import app.screenclock.domain.time.showcasedThemes
import app.screenclock.domain.update.UpdateResult
import app.screenclock.domain.update.releasePageUrl
import app.screenclock.domain.weather.formatUpdatedAt
import app.screenclock.ui.AppIcon
import app.screenclock.ui.AppIconKind
import app.screenclock.ui.UiPalette
import app.screenclock.ui.glassPressable
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The scrolling body of one sub-page. */
@Composable
internal fun SubPage(
    page: app.screenclock.domain.settings.SettingsPage,
    ui: SettingsUi,
    actions: SettingsActions,
    palette: UiPalette,
    maxWidthDp: Float,
    highlight: String? = null,
    onHighlightShown: () -> Unit = {},
    onJump: (app.screenclock.domain.settings.SettingsPage, String) -> Unit = { _, _ -> },
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(modifier = Modifier.widthIn(max = maxWidthDp.dp).fillMaxWidth()) {
            pageDescription(page)?.let { desc ->
                Text(
                    text = stringResource(desc),
                    color = palette.on3,
                    fontSize = 13.5.sp,
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 2.dp, bottom = 10.dp),
                )
            }
            when (page) {
                app.screenclock.domain.settings.SettingsPage.Theme -> ThemePage(ui, actions, palette, highlight, onHighlightShown)
                app.screenclock.domain.settings.SettingsPage.Display -> DisplayPage(ui, actions, palette, highlight, onHighlightShown)
                app.screenclock.domain.settings.SettingsPage.Layout -> LayoutPage(ui, actions, palette, highlight, onHighlightShown)
                app.screenclock.domain.settings.SettingsPage.Weather -> WeatherPage(ui, actions, palette, highlight, onHighlightShown)
                app.screenclock.domain.settings.SettingsPage.BurnIn -> BurnInPage(ui, actions, palette, onJump, highlight, onHighlightShown)
                app.screenclock.domain.settings.SettingsPage.Night -> NightPage(
                    ui = ui,
                    actions = actions,
                    palette = palette,
                    highlight = highlight,
                    onHighlightShown = onHighlightShown,
                )
                app.screenclock.domain.settings.SettingsPage.Occasion -> OccasionSettingsPage(
                    ui = ui,
                    actions = actions,
                    palette = palette,
                    highlight = highlight,
                    onHighlightShown = onHighlightShown,
                )
                app.screenclock.domain.settings.SettingsPage.About -> AboutPage(ui, palette)
                app.screenclock.domain.settings.SettingsPage.Home -> Unit
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

// ───────────────────────── theme ─────────────────────────

@Composable
private fun ThemePage(
    ui: SettingsUi,
    actions: SettingsActions,
    palette: UiPalette,
    highlight: String?,
    onHighlightShown: () -> Unit,
) {
    val previews = rememberThemePreviewStage()
    ClockPreview(ui, tall = true)
    Spacer(Modifier.height(14.dp))
    ThemeGrid(
        ui = ui,
        palette = palette,
        readyCount = previews,
        onTheme = { actions.onTheme(it) },
        short = settingsIsShort(LocalConfiguration.current.screenHeightDp.toFloat()),
    )
    if (ui.theme == ClockThemeId.Flip) {
        Spacer(Modifier.height(16.dp))
        HighlightTarget(palette, "colorway", highlight, onHighlightShown) {
        SegmentedTile(
            palette = palette,
            label = stringResource(R.string.colorway_name),
            options = listOf(
                stringResource(R.string.colorway_ink) to (ui.colorway == ColorwayId.InkBlack),
                stringResource(R.string.colorway_snow) to (ui.colorway == ColorwayId.SnowWhite),
                stringResource(R.string.colorway_graphite) to (ui.colorway == ColorwayId.Graphite),
            ),
            onSelect = { index ->
                actions.onColorway(
                    when (index) {
                        0 -> ColorwayId.InkBlack
                        1 -> ColorwayId.SnowWhite
                        else -> ColorwayId.Graphite
                    },
                )
            },
            first = true,
            last = true,
        )
        }
    }
}

/**
 * One preview at a time: the page draws its frame first, then the grid fills in.
 * A frame wait stalls when nothing else is invalidating, so this steps on a timer instead;
 * it still spreads the work over frames rather than composing every preview at once.
 */
@Composable
internal fun rememberThemePreviewStage(): Int {
    var ready by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        while (ready < showcasedThemes.size) {
            kotlinx.coroutines.delay(16)
            ready += 1
        }
    }
    return ready
}

// ───────────────────────── display ─────────────────────────

@Composable
private fun DisplayPage(
    ui: SettingsUi,
    actions: SettingsActions,
    palette: UiPalette,
    highlight: String?,
    onHighlightShown: () -> Unit,
) {
    ClockPreview(ui)
    Spacer(Modifier.height(14.dp))
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        HighlightTarget(palette, "hour", highlight, onHighlightShown) {
        SegmentedTile(
            palette = palette,
            label = stringResource(R.string.hour_mode),
            options = listOf(
                stringResource(R.string.hour_mode_system) to (ui.hourMode == HourMode.FollowSystem),
                stringResource(R.string.hour_mode_12) to (ui.hourMode == HourMode.H12),
                stringResource(R.string.hour_mode_24) to (ui.hourMode == HourMode.H24),
            ),
            onSelect = { index ->
                actions.onHourMode(
                    when (index) {
                        0 -> HourMode.FollowSystem
                        1 -> HourMode.H12
                        else -> HourMode.H24
                    },
                )
            },
            first = true,
            last = false,
        )
        }
        HighlightTarget(palette, "date", highlight, onHighlightShown) {
            SwitchTile(palette, stringResource(R.string.show_date), ui.showDate, actions.onShowDate, false, false)
        }
        HighlightTarget(palette, "year", highlight, onHighlightShown) {
            SwitchTile(palette, stringResource(R.string.show_year), ui.showYear, actions.onShowYear, false, false, enabled = ui.showDate)
        }
        HighlightTarget(palette, "weekday", highlight, onHighlightShown) {
            SwitchTile(palette, stringResource(R.string.show_weekday), ui.showWeekday, actions.onShowWeekday, false, false, enabled = ui.showDate)
        }
        HighlightTarget(palette, "lunar", highlight, onHighlightShown) {
            SwitchTile(palette, stringResource(R.string.show_lunar), ui.showLunarDate, actions.onShowLunarDate, false, false, enabled = ui.showDate)
        }
        HighlightTarget(palette, "holiday", highlight, onHighlightShown) {
            SwitchTile(palette, stringResource(R.string.show_holiday), ui.showHoliday, actions.onShowHoliday, false, true, enabled = ui.showDate)
        }
    }
}

// ───────────────────────── layout ─────────────────────────

@Composable
private fun LayoutPage(
    ui: SettingsUi,
    actions: SettingsActions,
    palette: UiPalette,
    highlight: String?,
    onHighlightShown: () -> Unit,
) {
    ClockPreview(ui, tall = true)
    Spacer(Modifier.height(14.dp))
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        HighlightTarget(palette, "datePosition", highlight, onHighlightShown) {
        SegmentedTile(
            palette = palette,
            label = stringResource(R.string.date_position),
            options = listOf(
                stringResource(R.string.position_above) to (ui.datePosition == DatePosition.Above),
                stringResource(R.string.position_below) to (ui.datePosition == DatePosition.Below),
            ),
            onSelect = { index -> actions.onDatePosition(if (index == 0) DatePosition.Above else DatePosition.Below) },
            first = true,
            last = false,
        )
        }
        HighlightTarget(palette, "digit", highlight, onHighlightShown) {
        SegmentedTile(
            palette = palette,
            label = stringResource(R.string.digit_size),
            options = listOf(
                stringResource(R.string.digit_compact) to (ui.digitSize == DigitSize.Compact),
                stringResource(R.string.digit_regular) to (ui.digitSize == DigitSize.Regular),
                stringResource(R.string.digit_large) to (ui.digitSize == DigitSize.Large),
            ),
            onSelect = { index ->
                actions.onDigitSize(
                    when (index) {
                        0 -> DigitSize.Compact
                        1 -> DigitSize.Regular
                        else -> DigitSize.Large
                    },
                )
            },
            first = false,
            last = true,
        )
        }
    }
}

// ───────────────────────── weather ─────────────────────────

@Composable
private fun WeatherPage(
    ui: SettingsUi,
    actions: SettingsActions,
    palette: UiPalette,
    highlight: String?,
    onHighlightShown: () -> Unit,
) {
    ClockPreview(ui)
    Spacer(Modifier.height(14.dp))
    WeatherBody(ui, actions, palette, highlight, onHighlightShown)
}

@Composable
private fun WeatherBody(
    ui: SettingsUi,
    actions: SettingsActions,
    palette: UiPalette,
    highlight: String?,
    onHighlightShown: () -> Unit,
) {
    val context = LocalContext.current
    val here = stringResource(R.string.weather_here)
    var query by rememberSaveable { mutableStateOf("") }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) actions.onLocate(here) else actions.onLocationDenied()
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        HighlightTarget(palette, "weather", highlight, onHighlightShown) {
            SwitchTile(
                palette,
                stringResource(R.string.show_weather),
                ui.weatherEnabled,
                actions.onWeatherEnabled,
                true,
                !ui.weatherEnabled,
            )
        }
        if (!ui.weatherEnabled) {
            NoteTile(
                palette = palette,
                title = stringResource(R.string.weather_off),
                note = stringResource(R.string.page_weather_desc),
                first = false,
                last = true,
            )
            return
        }
        ui.cityLabel?.let { label ->
            Tile(palette = palette, first = false, last = false, minHeight = 52.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(kind = AppIconKind.Location, tint = palette.on2, size = 16.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(text = label, color = palette.on, fontSize = 16.sp)
                }
            }
        }
        HighlightTarget(palette, "location", highlight, onHighlightShown) {
        SwitchTile(
            palette,
            stringResource(R.string.weather_use_location),
            ui.useLocation,
            { want ->
                if (!want) {
                    actions.onUseLocation(false)
                } else if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    actions.onLocate(here)
                } else {
                    permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
            },
            false,
            false,
        )
        }
        HighlightTarget(palette, "city", highlight, onHighlightShown) {
        Tile(palette = palette, first = false, last = true, minHeight = 0.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val shape = RoundedCornerShape(14.dp)
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(40) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = TextStyle(color = palette.on, fontSize = 16.sp),
                    shape = shape,
                    placeholder = {
                        Text(text = stringResource(R.string.weather_search_hint), color = palette.on3, fontSize = 16.sp)
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { actions.onSearch(query) }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = palette.on,
                        unfocusedTextColor = palette.on,
                        focusedBorderColor = palette.accent,
                        unfocusedBorderColor = palette.outline.copy(alpha = 0.5f),
                        cursorColor = palette.accent,
                    ),
                )
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.accentSoft)
                        .glassPressable(onClick = { actions.onSearch(query) }),
                    contentAlignment = Alignment.Center,
                ) {
                    AppIcon(kind = AppIconKind.Search, tint = palette.onAccentSoft, size = 21.dp)
                }
            }
            WeatherStatus(ui, actions, palette)
        }
        }
    }
}

@Composable
private fun WeatherStatus(ui: SettingsUi, actions: SettingsActions, palette: UiPalette) {
    val context = LocalContext.current
    val panel = ui.weatherPanel
    if (panel.busy) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.weather_busy),
            color = palette.on3,
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
                    .clickable { actions.onSelectCity(place) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcon(kind = AppIconKind.Location, tint = palette.on3, size = 16.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = place.label,
                    color = palette.on,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
        Spacer(Modifier.height(8.dp))
        Text(
            text = status,
            color = palette.on3,
            fontSize = 14.sp,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
    if (panel.notice == WeatherNotice.NoPermission) {
        TextButton(
            onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.fromParts("package", context.packageName, null))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            },
        ) {
            Text(text = stringResource(R.string.weather_open_settings), color = palette.accent)
        }
    }
    if (panel.notice == WeatherNotice.Unavailable && ui.weather != null) {
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(
                R.string.weather_updated,
                formatUpdatedAt(ui.weather.fetchedAtEpochMillis, ZoneId.systemDefault()),
            ),
            color = palette.on3,
            fontSize = 14.sp,
        )
    }
}

// ───────────────────────── burn-in ─────────────────────────

@Composable
private fun BurnInPage(
    ui: SettingsUi,
    actions: SettingsActions,
    palette: UiPalette,
    onJump: (app.screenclock.domain.settings.SettingsPage, String) -> Unit,
    highlight: String?,
    onHighlightShown: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        HighlightTarget(palette, "burnin", highlight, onHighlightShown) {
            SwitchTile(
                palette,
                stringResource(R.string.burnin_enable),
                ui.burnInEnabled,
                actions.onBurnInEnabled,
                true,
                !ui.burnInEnabled && !ui.chargeKeepAwake,
                note = stringResource(R.string.burnin_hint),
            )
        }
        if (ui.burnInEnabled && !ui.chargeKeepAwake) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(palette.warnBg)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(R.string.burnin_charge_warn_title), color = palette.warn, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(text = stringResource(R.string.burnin_charge_warn_note), color = palette.warn.copy(alpha = 0.85f), fontSize = 12.5.sp)
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(CircleShape)
                        .background(palette.warn)
                        .clickable { onJump(app.screenclock.domain.settings.SettingsPage.Night, "charge") }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = stringResource(R.string.burnin_go_enable), color = palette.backdropTop, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        if (!ui.burnInEnabled) {
            NoteTile(
                palette = palette,
                title = stringResource(R.string.section_burnin),
                note = stringResource(R.string.page_burnin_desc),
                first = false,
                last = true,
            )
            return@Column
        }
        HighlightTarget(palette, "screensaver", highlight, onHighlightShown) {
        SegmentedTile(
            palette = palette,
            label = stringResource(R.string.burnin_theme),
            options = ScreensaverTheme.entries.map { stringResource(screensaverThemeLabel(it)) to (ui.burnInTheme == it) },
            onSelect = { index -> actions.onBurnInTheme(ScreensaverTheme.entries[index]) },
            first = false,
            last = false,
        )
        }
        Tile(palette = palette, first = false, last = false, minHeight = 0.dp) {
            ScreensaverPreview(ui.burnInTheme)
            if (ui.burnInTheme.isVideo) {
                Spacer(Modifier.height(8.dp))
                Text(text = stringResource(R.string.burnin_theme_video_hint), color = palette.on3, fontSize = 13.sp)
            }
        }
        RhythmBar(ui, palette, first = false, last = false)
        HighlightTarget(palette, "idle", highlight, onHighlightShown) {
            IdleDial(ui, actions, palette)
        }
        HighlightTarget(palette, "run", highlight, onHighlightShown) {
            RunDial(ui, actions, palette, last = true)
        }
    }
}

/** "One round": idle, then the screensaver, then the clock — widths follow the settings. */
@Composable
private fun RhythmBar(ui: SettingsUi, palette: UiPalette, first: Boolean, last: Boolean) {
    val idleMinutes = (normalizeIdleMillis(ui.burnInIdleMillis) / 60_000L).toInt()
    val runMinutes = normalizeRunMillis(ui.burnInRunMillis) / 60_000f
    val (idleWeight, runWeight) = rhythmWeights(idleMinutes, runMinutes)
    Tile(palette = palette, first = first, last = last, minHeight = 0.dp) {
        Text(text = stringResource(R.string.burnin_rhythm), color = palette.on, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth().height(58.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            PhaseBox(
                modifier = Modifier.weight(idleWeight),
                title = stringResource(R.string.burnin_rhythm_idle),
                value = stringResource(R.string.burnin_minutes, idleMinutes),
                fill = palette.field,
                text = palette.on2,
            )
            PhaseBox(
                modifier = Modifier.weight(runWeight),
                title = stringResource(R.string.burnin_rhythm_run),
                value = runDurationText(ui.burnInRunMillis),
                fill = palette.accent,
                text = palette.backdropTop,
            )
            PhaseBox(
                modifier = Modifier.weight(1.3f),
                title = stringResource(R.string.burnin_rhythm_after),
                value = stringResource(R.string.burnin_back_to_clock),
                fill = Color.Transparent,
                text = palette.on3,
                dashed = true,
                small = true,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(text = stringResource(R.string.burnin_rhythm_caption), color = palette.on3, fontSize = 12.5.sp)
    }
}

@Composable
private fun PhaseBox(
    modifier: Modifier,
    title: String,
    value: String,
    fill: Color,
    text: Color,
    dashed: Boolean = false,
    small: Boolean = false,
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .height(58.dp)
            .clip(shape)
            .background(fill)
            .then(if (dashed) Modifier.border(1.dp, text.copy(alpha = 0.5f), shape) else Modifier)
            .padding(horizontal = 9.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = title, color = text.copy(alpha = 0.8f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            text = value,
            color = text,
            fontSize = if (small) 12.sp else 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun IdleDial(ui: SettingsUi, actions: SettingsActions, palette: UiPalette) {
    val current = normalizeIdleMillis(ui.burnInIdleMillis)
    val minutes = (current / 60_000L).toInt()
    var slider by remember(current) { mutableFloatStateOf(current / 60_000f) }
    val canDown = current > IDLE_MIN_MILLIS
    val canUp = current < IDLE_MAX_MILLIS
    Tile(palette = palette, first = false, last = false, minHeight = 0.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(palette.outline))
            Spacer(Modifier.width(8.dp))
            Text(text = stringResource(R.string.burnin_idle_short), color = palette.on, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(6.dp))
        DialRow(
            palette = palette,
            value = {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(text = "$minutes", color = palette.on, fontSize = 34.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = stringResource(R.string.unit_minute),
                        color = palette.on3,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 5.dp),
                    )
                }
            },
            decreaseLabel = stringResource(R.string.burnin_decrease),
            increaseLabel = stringResource(R.string.burnin_increase),
            onDecrease = { actions.onBurnInIdleMillis(stepIdleMillis(current, -1)) },
            onIncrease = { actions.onBurnInIdleMillis(stepIdleMillis(current, 1)) },
            canDecrease = canDown,
            canIncrease = canUp,
        )
        SliderBlock(
            palette = palette,
            value = slider,
            range = (IDLE_MIN_MILLIS / 60_000f)..(IDLE_MAX_MILLIS / 60_000f),
            steps = 58,
            onChange = { slider = it },
            onFinished = { actions.onBurnInIdleMillis(idleFromMinutes(it.toInt())) },
            startLabel = stringResource(R.string.burnin_minutes, 1),
            middleLabel = stringResource(R.string.burnin_minutes, 30),
            endLabel = stringResource(R.string.burnin_minutes, 60),
        )
        PresetRow(
            palette = palette,
            labels = IDLE_PRESET_MINUTES.map { stringResource(R.string.burnin_minutes, it) },
            selectedIndex = IDLE_PRESET_MINUTES.indexOf(minutes),
            onPick = { actions.onBurnInIdleMillis(idleFromMinutes(IDLE_PRESET_MINUTES[it])) },
        )
    }
}

@Composable
private fun RunDial(ui: SettingsUi, actions: SettingsActions, palette: UiPalette, last: Boolean) {
    val current = normalizeRunMillis(ui.burnInRunMillis)
    var slider by remember(current) { mutableFloatStateOf(current / 1_000f) }
    val canDown = current > RUN_MIN_MILLIS
    val canUp = current < RUN_MAX_MILLIS
    val seconds = (current / 1_000L).toInt()
    Tile(palette = palette, first = false, last = last, minHeight = 0.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(palette.accent))
            Spacer(Modifier.width(8.dp))
            Text(text = stringResource(R.string.burnin_run_short), color = palette.on, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(6.dp))
        DialRow(
            palette = palette,
            value = { RunValueText(current) },
            decreaseLabel = stringResource(R.string.burnin_decrease),
            increaseLabel = stringResource(R.string.burnin_increase),
            onDecrease = { actions.onBurnInRunMillis(stepRunMillis(current, -1)) },
            onIncrease = { actions.onBurnInRunMillis(stepRunMillis(current, 1)) },
            canDecrease = canDown,
            canIncrease = canUp,
        )
        SliderBlock(
            palette = palette,
            value = slider,
            range = (RUN_MIN_MILLIS / 1_000f)..(RUN_MAX_MILLIS / 1_000f),
            steps = 178,
            onChange = { slider = it },
            onFinished = { actions.onBurnInRunMillis(runFromSeconds(it.toInt())) },
            startLabel = stringResource(R.string.burnin_seconds, 10),
            middleLabel = stringResource(R.string.burnin_minutes, 15),
            endLabel = stringResource(R.string.burnin_minutes, 30),
        )
        PresetRow(
            palette = palette,
            labels = RUN_PRESET_SECONDS.map { secondsLabel(it) },
            selectedIndex = RUN_PRESET_SECONDS.indexOf(seconds),
            onPick = { actions.onBurnInRunMillis(runFromSeconds(RUN_PRESET_SECONDS[it])) },
        )
    }
}

/** The dial value: whole seconds below a minute, otherwise minutes and leftover seconds. */
@Composable
private fun RunValueText(millis: Long) {
    val total = (normalizeRunMillis(millis) / 1_000L).toInt()
    val minutes = total / 60
    val seconds = total % 60
    val palette = app.screenclock.ui.LocalUiPalette.current
    Row(verticalAlignment = Alignment.Bottom) {
        if (minutes > 0) {
            Text(text = "$minutes", color = palette.on, fontSize = 34.sp, fontWeight = FontWeight.SemiBold)
            Text(text = stringResource(R.string.unit_minute), color = palette.on3, fontSize = 15.sp, modifier = Modifier.padding(start = 2.dp, end = 4.dp, bottom = 5.dp))
        }
        if (seconds > 0) {
            Text(text = "$seconds", color = palette.on, fontSize = if (minutes > 0) 22.sp else 34.sp, fontWeight = FontWeight.SemiBold)
            Text(text = stringResource(R.string.unit_second), color = palette.on3, fontSize = 15.sp, modifier = Modifier.padding(start = 2.dp, bottom = 5.dp))
        }
    }
}

@Composable
private fun secondsLabel(seconds: Int): String = when {
    seconds < 60 -> stringResource(R.string.burnin_seconds, seconds)
    seconds % 60 == 0 -> stringResource(R.string.burnin_minutes, seconds / 60)
    else -> stringResource(R.string.burnin_minutes_seconds, seconds / 60, seconds % 60)
}

@Composable
private fun runDurationText(millis: Long): String {
    val total = (normalizeRunMillis(millis) / 1_000L).toInt()
    val minutes = total / 60
    val seconds = total % 60
    return when {
        minutes > 0 && seconds == 0 -> stringResource(R.string.burnin_minutes, minutes)
        minutes > 0 -> stringResource(R.string.burnin_minutes_seconds, minutes, seconds)
        else -> stringResource(R.string.burnin_seconds, seconds)
    }
}

internal fun screensaverThemeLabel(theme: ScreensaverTheme): Int = when (theme) {
    ScreensaverTheme.Drift -> R.string.burnin_theme_drift
    ScreensaverTheme.Glitch -> R.string.burnin_theme_glitch
    ScreensaverTheme.Tunnel -> R.string.burnin_theme_tunnel
    ScreensaverTheme.Portrait -> R.string.burnin_theme_portrait
}

// ───────────────────────── night ─────────────────────────

@Composable
private fun NightPage(
    ui: SettingsUi,
    actions: SettingsActions,
    palette: UiPalette,
    highlight: String?,
    onHighlightShown: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        HighlightTarget(palette, "night", highlight, onHighlightShown) {
            SwitchTile(
                palette,
                stringResource(R.string.night_dim),
                ui.nightDimEnabled,
                actions.onNightDimEnabled,
                true,
                !ui.nightDimEnabled,
                note = stringResource(R.string.night_hint),
            )
        }
        if (ui.nightDimEnabled) {
            NightBar(ui, palette)
            HighlightTarget(palette, "start", highlight, onHighlightShown) {
                HourSetter(palette, stringResource(R.string.night_start), ui.nightStartHour, actions.onNightStartHour)
            }
            HighlightTarget(palette, "end", highlight, onHighlightShown) {
                HourSetter(palette, stringResource(R.string.night_end), ui.nightEndHour, actions.onNightEndHour, last = false)
            }
        }
        HighlightTarget(palette, "warm", highlight, onHighlightShown) {
            SwitchTile(palette, stringResource(R.string.night_warm), ui.nightWarmEnabled, actions.onNightWarmEnabled, false, false)
        }
        // "去开启" on the burn-in page lands here, so the keep-awake switch is last.
        HighlightTarget(palette, "charge", highlight, onHighlightShown) {
            SwitchTile(palette, stringResource(R.string.charge_keep_awake), ui.chargeKeepAwake, actions.onChargeKeepAwake, false, true)
        }
    }
}

/** The 24-hour strip; a window that crosses midnight is drawn as two dark pieces. */
@Composable
private fun NightBar(ui: SettingsUi, palette: UiPalette) {
    Tile(palette = palette, first = false, last = false, minHeight = 0.dp) {
        val spans = nightBarSpans(ui.nightStartHour, ui.nightEndHour)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFFFBE3B4),
                            Color(0xFFCDE2F6),
                            Color(0xFFCDE2F6),
                            Color(0xFF6B80B4),
                            Color(0xFF242C4C),
                        ),
                    ),
                ),
        ) {
            // The darkened hours are painted over the day/night gradient, as fractions of the
            // full strip, so 22:00–07:00 reads as two pieces without any layout guesswork.
            Canvas(modifier = Modifier.fillMaxSize()) {
                spans.forEach { span ->
                    drawRect(
                        color = NightBarShade,
                        topLeft = Offset(size.width * span.start, 0f),
                        size = Size(size.width * (span.endInclusive - span.start), size.height),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(horizontal = 8.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                listOf("0", "6", "12", "18", "24").forEach {
                    Text(text = it, color = NightBarLabel, fontSize = 9.5.sp)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.night_hint),
            color = palette.on3,
            fontSize = 12.5.sp,
        )
    }
}

private val NightBarShade = Color(0x8C0E1226)
private val NightBarLabel = Color(0xAA142032)

@Composable
private fun HourSetter(
    palette: UiPalette,
    label: String,
    hour: Int,
    onHour: (Int) -> Unit,
    last: Boolean = false,
) {
    val shown = ((hour % 24) + 24) % 24
    val shape = RoundedCornerShape(
        topStart = TileSmallRadius,
        topEnd = TileSmallRadius,
        bottomStart = if (last) TileLargeRadius else TileSmallRadius,
        bottomEnd = if (last) TileLargeRadius else TileSmallRadius,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clip(shape)
            .background(palette.tile)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, color = palette.on, fontSize = 16.sp, modifier = Modifier.weight(1f))
        RoundIconButton(palette, AppIconKind.Minus, stringResource(R.string.night_earlier)) { onHour((shown + 23) % 24) }
        Text(
            text = stringResource(R.string.night_hour, shown),
            color = palette.on,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(66.dp),
        )
        RoundIconButton(palette, AppIconKind.Plus, stringResource(R.string.night_later)) { onHour((shown + 1) % 24) }
    }
}

// ───────────────────────── occasion ─────────────────────────

@Composable
private fun OccasionSettingsPage(
    ui: SettingsUi,
    actions: SettingsActions,
    palette: UiPalette,
    highlight: String?,
    onHighlightShown: () -> Unit,
) {
    val dark = palette.dark
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Tile(
            palette = palette,
            first = true,
            last = false,
            onClick = actions.onOpenOccasions,
            minHeight = 64.dp,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryChip(Color(0xFFE45A90)) {
                    AppIcon(kind = AppIconKind.Heart, tint = Color(0xFFE45A90), size = 20.dp)
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    text = stringResource(R.string.section_occasion),
                    color = palette.on,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                AppIcon(kind = AppIconKind.ChevronRight, tint = palette.on3, size = 16.dp)
            }
        }
        HighlightTarget(palette, "style", highlight, onHighlightShown) {
        Tile(palette = palette, first = false, last = false, minHeight = 0.dp) {
            Text(text = stringResource(R.string.ceremony_style), color = palette.on, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                CeremonyStyleOption(
                    palette = palette,
                    style = CeremonyStyle.Card,
                    selected = ui.ceremonyLook.style == CeremonyStyle.Card,
                    dark = dark,
                    label = stringResource(R.string.ceremony_style_card),
                    note = stringResource(R.string.ceremony_style_card_desc),
                    onClick = { actions.onCeremonyStyle(CeremonyStyle.Card) },
                    modifier = Modifier.weight(1f),
                )
                CeremonyStyleOption(
                    palette = palette,
                    style = CeremonyStyle.Immersive,
                    selected = ui.ceremonyLook.style == CeremonyStyle.Immersive,
                    dark = dark,
                    label = stringResource(R.string.ceremony_style_immersive),
                    note = stringResource(R.string.ceremony_style_immersive_desc),
                    onClick = { actions.onCeremonyStyle(CeremonyStyle.Immersive) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        }
        HighlightTarget(palette, "autoplay", highlight, onHighlightShown) {
        NoteTile(
            palette = palette,
            title = stringResource(R.string.auto_play_title),
            note = stringResource(R.string.auto_play_help),
            first = false,
            last = false,
        )
        }
        HighlightTarget(palette, "dwell", highlight, onHighlightShown) {
        SegmentedTile(
            palette = palette,
            label = stringResource(R.string.auto_play_dwell),
            options = AUTO_PLAY_SECONDS_CHOICES.map { stringResource(R.string.auto_play_seconds, it) to (ui.ceremonyLook.autoPlaySeconds == it) },
            onSelect = { index -> actions.onAutoPlaySeconds(AUTO_PLAY_SECONDS_CHOICES[index]) },
            first = false,
            last = true,
        )
        }
    }
}

@Composable
private fun CeremonyStyleOption(
    palette: UiPalette,
    style: CeremonyStyle,
    selected: Boolean,
    dark: Boolean,
    label: String,
    note: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .semantics { this.selected = selected },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .then(
                    if (selected) Modifier.border(2.dp, palette.accent, RoundedCornerShape(18.dp)) else Modifier,
                ),
        ) {
            CeremonyStylePreview(style = style, dark = dark, modifier = Modifier.clip(RoundedCornerShape(18.dp)))
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
            if (selected) {
                AppIcon(kind = AppIconKind.Check, tint = palette.accent, size = 14.dp)
                Spacer(Modifier.width(4.dp))
            }
            Text(text = label, color = palette.on, fontSize = 14.5.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(
            text = note,
            color = palette.on3,
            fontSize = 11.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

// ───────────────────────── about ─────────────────────────

@Composable
private fun AboutPage(ui: SettingsUi, palette: UiPalette) {
    val context = LocalContext.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF080A0C)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(width = 40.dp, height = 46.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF17171A))
                    .border(1.dp, Color(0xFF3A3A3E), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "18", color = Color(0xFFF1EDE4), fontSize = 21.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(text = stringResource(R.string.app_name), color = palette.on, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        if (ui.version.isNotEmpty()) {
            Text(text = stringResource(R.string.about_version, ui.version), color = palette.on3, fontSize = 14.sp)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.about_intro),
            color = palette.on2,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(20.dp))
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        UpdateCheckTile(ui.version, palette, first = true, last = false)
        LinkTile(palette, stringResource(R.string.about_github)) {
            openLink(context, "https://github.com/AlexWuYh/ScreenClock")
        }
        Tile(
            palette = palette,
            first = false,
            last = true,
            onClick = { openLink(context, "https://github.com/AlexWuYh") },
            minHeight = 64.dp,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.author_avatar),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(40.dp).clip(CircleShape),
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(R.string.about_author_name), color = palette.on, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text(text = stringResource(R.string.about_author_link), color = palette.on3, fontSize = 13.sp)
                }
                AppIcon(kind = AppIconKind.Link, tint = palette.on3, size = 17.dp)
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    Text(
        text = stringResource(R.string.about_license_title),
        color = palette.on,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 8.dp, bottom = 6.dp).semantics { heading() },
    )
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Tile(palette = palette, first = true, last = true, minHeight = 0.dp) {
            Text(text = stringResource(R.string.about_license_mit), color = palette.on2, fontSize = 14.sp)
        }
    }
    Spacer(Modifier.height(16.dp))
    Text(
        text = stringResource(R.string.about_third_party),
        color = palette.on,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 8.dp, bottom = 6.dp).semantics { heading() },
    )
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        val credits = listOf(
            R.string.credit_lunar_name to R.string.credit_lunar_detail,
            R.string.credit_meteo_name to R.string.credit_meteo_detail,
            R.string.credit_fonts_name to R.string.credit_fonts_detail,
            R.string.credit_nasa_name to R.string.credit_nasa_detail,
        )
        credits.forEachIndexed { index, (name, detail) ->
            val meteo = name == R.string.credit_meteo_name
            CreditTile(
                palette = palette,
                name = stringResource(name),
                detail = stringResource(detail),
                first = index == 0,
                last = index == credits.lastIndex,
                onClick = if (meteo) {
                    { openLink(context, "https://open-meteo.com/") }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
private fun LinkTile(palette: UiPalette, label: String, onClick: () -> Unit) {
    Tile(palette = palette, first = false, last = false, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = label, color = palette.on, fontSize = 16.sp, modifier = Modifier.weight(1f))
            AppIcon(kind = AppIconKind.ChevronRight, tint = palette.on3, size = 17.dp)
        }
    }
}

@Composable
private fun CreditTile(
    palette: UiPalette,
    name: String,
    detail: String,
    first: Boolean,
    last: Boolean,
    onClick: (() -> Unit)?,
) {
    Tile(palette = palette, first = first, last = last, minHeight = 0.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, color = palette.on, fontSize = 15.sp)
                Spacer(Modifier.height(2.dp))
                Text(text = detail, color = palette.on3, fontSize = 13.sp)
            }
            if (onClick != null) AppIcon(kind = AppIconKind.Link, tint = palette.on3, size = 17.dp)
        }
    }
}

/** Manual only: nothing is requested until the row is tapped. */
@Composable
private fun UpdateCheckTile(currentVersion: String, palette: UiPalette, first: Boolean, last: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<UpdateResult?>(null) }
    val status = when {
        checking -> stringResource(R.string.update_checking)
        result is UpdateResult.UpToDate -> stringResource(R.string.update_latest)
        result is UpdateResult.Available -> stringResource(R.string.update_available, (result as UpdateResult.Available).version)
        result is UpdateResult.Failed -> stringResource(R.string.update_failed)
        else -> null
    }
    Tile(
        palette = palette,
        first = first,
        last = last,
        onClick = {
            val found = result
            if (found is UpdateResult.Available) {
                openLink(context, releasePageUrl(found.version))
            } else if (!checking) {
                checking = true
                scope.launch {
                    result = withContext(Dispatchers.IO) { UpdateChecker().check(currentVersion) }
                    checking = false
                }
            }
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.update_check), color = palette.on, fontSize = 16.sp)
                if (status != null) {
                    Text(
                        text = status,
                        color = if (result is UpdateResult.Available) palette.accent else palette.on3,
                        fontSize = 13.sp,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
            AppIcon(
                kind = if (result is UpdateResult.Available) AppIconKind.Link else AppIconKind.Reset,
                tint = palette.on3,
                size = 17.dp,
            )
        }
    }
}

private fun openLink(context: android.content.Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: ActivityNotFoundException) {
        // The text stays on the page when the device has no browser.
    }
}
