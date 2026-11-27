package app.screenclock.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.screenclock.R
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import app.screenclock.domain.settings.SettingsHit
import app.screenclock.domain.settings.searchSettings
import app.screenclock.domain.settings.settingsIndex
import app.screenclock.domain.settings.SettingsGroups
import app.screenclock.domain.settings.SettingsPage
import app.screenclock.domain.settings.SettingsSummaryInput
import app.screenclock.domain.settings.aboutSummary
import app.screenclock.domain.settings.burnInSummary
import app.screenclock.domain.settings.displaySummary
import app.screenclock.domain.settings.layoutSummary
import app.screenclock.domain.settings.nightSummary
import app.screenclock.domain.settings.occasionSettingsSummary
import app.screenclock.domain.settings.themeSummary
import app.screenclock.domain.settings.weatherSummary
import app.screenclock.domain.burnin.normalizeIdleMillis
import app.screenclock.ui.AppIcon
import app.screenclock.ui.AppIconKind
import app.screenclock.ui.UiPalette
import app.screenclock.ui.glassPressable

/** One entry's visual identity: icon, chip colour and the summary it carries. */
internal data class Entry(
    val page: SettingsPage,
    val title: Int,
    val icon: AppIconKind,
    val color: Color,
)

private val EntryTheme = Entry(SettingsPage.Theme, R.string.theme, AppIconKind.Palette, Color(0xFF6B7DF7))
private val EntryDisplay = Entry(SettingsPage.Display, R.string.section_display, AppIconKind.Timer, Color(0xFF2F94EE))
private val EntryLayout = Entry(SettingsPage.Layout, R.string.section_layout, AppIconKind.Sliders, Color(0xFF16A394))
private val EntryWeather = Entry(SettingsPage.Weather, R.string.section_weather, AppIconKind.Location, Color(0xFFE8920A))
private val EntryBurnIn = Entry(SettingsPage.BurnIn, R.string.section_burnin, AppIconKind.Timer, Color(0xFFEC5A49))
private val EntryNight = Entry(SettingsPage.Night, R.string.section_night, AppIconKind.Moon, Color(0xFF9467E6))
private val EntryOccasion = Entry(SettingsPage.Occasion, R.string.section_occasion, AppIconKind.Heart, Color(0xFFE45A90))
private val EntryAbout = Entry(SettingsPage.About, R.string.section_about, AppIconKind.Link, Color(0xFF7B95A3))

internal fun entryFor(page: SettingsPage): Entry = when (page) {
    SettingsPage.Theme -> EntryTheme
    SettingsPage.Display -> EntryDisplay
    SettingsPage.Layout -> EntryLayout
    SettingsPage.Weather -> EntryWeather
    SettingsPage.BurnIn -> EntryBurnIn
    SettingsPage.Night -> EntryNight
    SettingsPage.Occasion -> EntryOccasion
    SettingsPage.About -> EntryAbout
    SettingsPage.Home -> EntryTheme
}

internal fun groupTitle(page: SettingsPage): Int = when (page) {
    SettingsPage.Theme, SettingsPage.Display, SettingsPage.Layout, SettingsPage.Weather -> R.string.settings_group_face
    SettingsPage.BurnIn, SettingsPage.Night -> R.string.settings_group_protect
    else -> R.string.settings_group_other
}

/** The home index: a live face preview, then the grouped entries. */
@Composable
internal fun HomeIndex(
    ui: SettingsUi,
    actions: SettingsActions,
    palette: UiPalette,
    selected: SettingsPage?,
    onOpen: (SettingsPage) -> Unit,
    onToggleTone: () -> Unit,
    toneLabel: String,
    showHero: Boolean,
    wide: Boolean,
    onSearchHit: (SettingsHit) -> Unit = {},
) {
    val scroll = rememberLazyListState()
    var query by rememberSaveable { mutableStateOf("") }
    val index = buildSettingsIndex(ui)
    val hits = remember(index, query) { searchSettings(index, query) }
    val searching = query.isNotBlank()
    // Back clears the query first; leaving the page is the next press's job.
    BackHandler(enabled = searching) { query = "" }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIconButton(palette, AppIconKind.Back, stringResource(R.string.back), actions.onBack)
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.settings),
                color = palette.on,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            RoundIconButton(
                palette,
                if (palette.dark) AppIconKind.Sun else AppIconKind.Moon,
                toneLabel,
                onToggleTone,
            )
        }
        SearchField(
            palette = palette,
            query = query,
            onQuery = { query = it },
        )
        // A screen reader hears how many settings matched without reading the list.
        if (searching) {
            Text(
                text = if (hits.isEmpty()) {
                    stringResource(R.string.settings_search_empty, query.trim())
                } else {
                    stringResource(R.string.settings_search_count, hits.size)
                },
                color = palette.on3,
                fontSize = 12.5.sp,
                modifier = Modifier
                    .padding(start = 28.dp, end = 28.dp, top = 2.dp, bottom = 4.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        LazyColumn(
            state = scroll,
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp),
        ) {
            if (searching) {
                items(hits, key = { it.page.name + it.key }) { hit ->
                    SearchHitRow(
                        palette = palette,
                        hit = hit,
                        first = hit === hits.first(),
                        last = hit === hits.last(),
                        onClick = { onSearchHit(hit) },
                    )
                }
                return@LazyColumn
            }
            if (showHero && !wide) {
                item {
                    HeroCard(
                        ui = ui,
                        palette = palette,
                        onClick = { onOpen(SettingsPage.Theme) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }
            SettingsGroups.forEach { (heading, pages) ->
                item(key = "head-$heading") {
                    Text(
                        text = stringResource(
                            when (heading) {
                                "钟面" -> R.string.settings_group_face
                                "保护与夜间" -> R.string.settings_group_protect
                                else -> R.string.settings_group_other
                            },
                        ),
                        color = palette.accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 16.dp, bottom = 8.dp),
                    )
                }
                items(pages, key = { it.name }) { page ->
                    GroupEntryRow(
                        ui = ui,
                        palette = palette,
                        page = page,
                        first = page == pages.first(),
                        last = page == pages.last(),
                        selected = selected == page,
                        onClick = { onOpen(page) },
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupEntryRow(
    ui: SettingsUi,
    palette: UiPalette,
    page: SettingsPage,
    first: Boolean,
    last: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val entry = entryFor(page)
    EntryTile(
        palette = palette,
        icon = {
            CategoryChip(entry.color) {
                AppIcon(kind = entry.icon, tint = entry.color, size = 20.dp)
            }
        },
        title = stringResource(entry.title),
        summary = entrySummary(page, ui),
        onClick = onClick,
        first = first,
        last = last,
        selected = selected,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 1.dp),
    )
}

/** The current face, tapping it opens the theme page. */
@Composable
private fun HeroCard(
    ui: SettingsUi,
    palette: UiPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(26.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 520.dp)
            .clip(shape)
            .background(palette.tile)
            .glassPressable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .background(Color.Black),
        ) {
            ClockFace(ui, modifier = Modifier.fillMaxSize())
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(themeLabel(ui.theme)),
                    color = palette.on,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.settings_current_face),
                    color = palette.on3,
                    fontSize = 13.sp,
                )
            }
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .clip(CircleShape)
                    .background(palette.accentSoft)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.settings_change_theme),
                    color = palette.onAccentSoft,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/** A small summary line for each entry, built from the pure functions in `domain`. */
@Composable
internal fun entrySummary(page: SettingsPage, ui: SettingsUi): String {
    val input = summaryInput(ui)
    return when (page) {
        SettingsPage.Theme -> themeSummary(input)
        SettingsPage.Display -> displaySummary(input)
        SettingsPage.Layout -> layoutSummary(input)
        SettingsPage.Weather -> weatherSummary(input)
        SettingsPage.BurnIn -> burnInSummary(input)
        SettingsPage.Night -> nightSummary(input)
        SettingsPage.Occasion -> occasionSettingsSummary(input)
        SettingsPage.About -> aboutSummary(input)
        SettingsPage.Home -> ""
    }
}

@Composable
internal fun summaryInput(ui: SettingsUi): SettingsSummaryInput = SettingsSummaryInput(
    themeName = stringResource(themeLabel(ui.theme)),
    showsColorway = ui.theme == app.screenclock.domain.time.ClockThemeId.Flip,
    colorwayName = stringResource(
        when (ui.colorway) {
            app.screenclock.domain.time.ColorwayId.InkBlack -> R.string.colorway_ink
            app.screenclock.domain.time.ColorwayId.SnowWhite -> R.string.colorway_snow
            app.screenclock.domain.time.ColorwayId.Graphite -> R.string.colorway_graphite
        },
    ),
    hourModeName = stringResource(
        when (ui.hourMode) {
            app.screenclock.domain.time.HourMode.FollowSystem -> R.string.hour_mode_system
            app.screenclock.domain.time.HourMode.H12 -> R.string.hour_mode_12
            app.screenclock.domain.time.HourMode.H24 -> R.string.hour_mode_24
        },
    ),
    showsDate = ui.showDate,
    visibleDateParts = listOf(ui.showYear, ui.showLunarDate, ui.showWeekday, ui.showHoliday).count { it },
    datePositionAbove = ui.datePosition == app.screenclock.domain.time.DatePosition.Above,
    digitSizeName = stringResource(
        when (ui.digitSize) {
            app.screenclock.domain.time.DigitSize.Compact -> R.string.digit_compact
            app.screenclock.domain.time.DigitSize.Regular -> R.string.digit_regular
            app.screenclock.domain.time.DigitSize.Large -> R.string.digit_large
        },
    ),
    weatherEnabled = ui.weatherEnabled,
    weatherPlace = ui.cityLabel,
    burnInEnabled = ui.burnInEnabled,
    screensaverName = stringResource(screensaverThemeLabel(ui.burnInTheme)),
    idleMinutes = (normalizeIdleMillis(ui.burnInIdleMillis) / 60_000L).toInt(),
    nightDimEnabled = ui.nightDimEnabled,
    nightStartHour = ui.nightStartHour,
    nightEndHour = ui.nightEndHour,
    nightWarmEnabled = ui.nightWarmEnabled,
    chargeKeepAwake = ui.chargeKeepAwake,
    ceremonyStyle = ui.ceremonyLook.style,
    autoPlaySeconds = ui.ceremonyLook.autoPlaySeconds,
    version = ui.version,
)

/** Builds the search index from the strings the pages already show. */
@Composable
internal fun buildSettingsIndex(ui: SettingsUi): List<SettingsHit> = settingsIndex(
    themeTitle = stringResource(R.string.theme),
    colorwayTitle = stringResource(R.string.colorway_name),
    hourTitle = stringResource(R.string.hour_mode),
    dateToggles = listOf(
        stringResource(R.string.show_date),
        stringResource(R.string.show_year),
        stringResource(R.string.show_weekday),
        stringResource(R.string.show_lunar),
        stringResource(R.string.show_holiday),
    ),
    datePositionTitle = stringResource(R.string.date_position),
    digitSizeTitle = stringResource(R.string.digit_size),
    weatherTitle = stringResource(R.string.show_weather),
    weatherLocationTitle = stringResource(R.string.weather_use_location),
    weatherSearchTitle = stringResource(R.string.weather_search),
    burnInTitle = stringResource(R.string.burnin_enable),
    screensaverTitle = stringResource(R.string.burnin_theme),
    burnInIdleTitle = stringResource(R.string.burnin_idle_short),
    burnInRunTitle = stringResource(R.string.burnin_run_short),
    nightDimTitle = stringResource(R.string.night_dim),
    nightStartTitle = stringResource(R.string.night_start),
    nightEndTitle = stringResource(R.string.night_end),
    nightWarmTitle = stringResource(R.string.night_warm),
    chargeTitle = stringResource(R.string.charge_keep_awake),
    occasionTitle = stringResource(R.string.occasion),
    ceremonyStyleTitle = stringResource(R.string.ceremony_style),
    autoPlayTitle = stringResource(R.string.auto_play_title),
    autoPlayDwellTitle = stringResource(R.string.auto_play_dwell),
    updateTitle = stringResource(R.string.update_check),
    aboutTitles = listOf(
        stringResource(R.string.about_github),
        stringResource(R.string.about_author_name),
        stringResource(R.string.about_license_title),
        stringResource(R.string.about_third_party),
    ),
)

/** The home search box: a quiet field that hides the grouped index while it has text. */
@Composable
private fun SearchField(
    palette: UiPalette,
    query: String,
    onQuery: (String) -> Unit,
) {
    val shape = RoundedCornerShape(25.dp)
    val clearLabel = stringResource(R.string.settings_search_clear)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .height(50.dp)
            .clip(shape)
            .background(palette.field)
            .padding(start = 16.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(kind = AppIconKind.Search, tint = palette.on2, size = 18.dp)
        Spacer(Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(text = stringResource(R.string.settings_search_hint), color = palette.on3, fontSize = 15.sp)
            }
            androidx.compose.foundation.text.BasicTextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = palette.on,
                    fontSize = 15.sp,
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(palette.accent),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .semantics { contentDescription = clearLabel }
                    .glassPressable(onClick = { onQuery("") }),
                contentAlignment = Alignment.Center,
            ) {
                AppIcon(kind = AppIconKind.Plus, tint = palette.on3, size = 16.dp, rotationDegrees = 45f)
            }
        }
    }
}

/** One search result: the setting's name, the page it lives on, and where it goes. */
@Composable
private fun SearchHitRow(
    palette: UiPalette,
    hit: SettingsHit,
    first: Boolean,
    last: Boolean,
    onClick: () -> Unit,
) {
    val entry = entryFor(hit.page)
    Tile(palette = palette, first = first, last = last, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryChip(entry.color) {
                AppIcon(kind = entry.icon, tint = entry.color, size = 20.dp)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = hit.title, color = palette.on, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(text = stringResource(entry.title), color = palette.on3, fontSize = 13.sp)
            }
            AppIcon(kind = AppIconKind.ChevronRight, tint = palette.on3, size = 16.dp)
        }
    }
}
