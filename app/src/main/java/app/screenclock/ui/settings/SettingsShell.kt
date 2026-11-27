package app.screenclock.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.screenclock.R
import app.screenclock.domain.appearance.AppearanceTone
import app.screenclock.domain.appearance.nextAppearanceTone
import app.screenclock.domain.settings.SETTINGS_DETAIL_MAX_DP
import app.screenclock.domain.settings.SettingsPage
import app.screenclock.domain.settings.SettingsWidth
import app.screenclock.domain.settings.settingsIsShort
import app.screenclock.domain.settings.settingsListWidth
import app.screenclock.domain.settings.settingsWidth
import app.screenclock.ui.AppIcon
import app.screenclock.ui.AppIconKind
import app.screenclock.ui.LocalUiPalette
import app.screenclock.ui.UiPalette
import app.screenclock.ui.glassPressable
import app.screenclock.ui.rememberAnimatorEnabled

/**
 * The settings shell. A grouped index, eight short sub-pages, and one app-wide light/dark
 * button. Narrow windows push a page over the index; wide windows show the index beside the
 * selected page, so choosing another entry just swaps the right column.
 */
@Composable
internal fun SettingsShell(ui: SettingsUi, actions: SettingsActions) {
    val palette = LocalUiPalette.current
    val configuration = LocalConfiguration.current
    val widthDp = configuration.screenWidthDp.toFloat()
    val heightDp = configuration.screenHeightDp.toFloat()
    val wide = settingsWidth(widthDp) != SettingsWidth.Compact
    // Rotation, a fold and a resize all keep the page the user was looking at.
    var page by rememberSaveable { mutableStateOf(SettingsPage.Home) }
    // A note can send the user to a specific switch on another page; it flashes for a moment.
    var highlight by rememberSaveable { mutableStateOf<String?>(null) }
    val jumpTo: (SettingsPage, String) -> Unit = { target, key ->
        page = target
        highlight = key.ifBlank { null }
    }
    val animator = rememberAnimatorEnabled()

    val toneLabel = stringResource(
        R.string.appearance_a11y,
        stringResource(
            when (ui.appearanceTone) {
                AppearanceTone.FollowSystem -> R.string.appearance_system
                AppearanceTone.Light -> R.string.appearance_light
                AppearanceTone.Dark -> R.string.appearance_dark
            },
        ),
    )
    val toggleTone = { actions.onAppearanceTone(nextAppearanceTone(ui.appearanceTone)) }
    val showHero = !settingsIsShort(heightDp)

    BackHandler {
        if (page != SettingsPage.Home) page = SettingsPage.Home else actions.onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(palette.backdropTop, palette.backdropMid, palette.backdropBottom),
                ),
            )
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        if (wide) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .width(settingsListWidth(widthDp).dp)
                        .fillMaxHeight(),
                ) {
                    HomeIndex(
                        ui = ui,
                        actions = actions,
                        palette = palette,
                        selected = page,
                        onOpen = { page = it },
                        onToggleTone = toggleTone,
                        toneLabel = toneLabel,
                        showHero = false,
                        wide = true,
                        onSearchHit = { hit -> jumpTo(hit.page, hit.key) },
                    )
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(palette.line),
                )
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    SubPageTransition(
                        page = if (page == SettingsPage.Home) SettingsPage.Theme else page,
                        ui = ui,
                        actions = actions,
                        onJump = jumpTo,
                        palette = palette,
                        onBack = null,
                        onToggleTone = toggleTone,
                        toneLabel = toneLabel,
                        maxWidthDp = SETTINGS_DETAIL_MAX_DP,
                        highlight = highlight,
                        onHighlightShown = { highlight = null },
                    )
                }
            }
        } else {
            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    val towardHome = targetState == SettingsPage.Home
                    val duration = if (animator) 320 else 0
                    (fadeIn(tween(if (animator) 200 else 0)) +
                        slideInHorizontally(tween(duration)) { if (towardHome) it / 6 else it })
                        .togetherWith(fadeOut(tween(if (animator) 140 else 0)))
                },
                label = "settingsPage",
            ) { shown ->
                if (shown == SettingsPage.Home) {
                    HomeIndex(
                        ui = ui,
                        actions = actions,
                        palette = palette,
                        selected = null,
                        onOpen = { page = it },
                        onToggleTone = toggleTone,
                        toneLabel = toneLabel,
                        showHero = showHero,
                        wide = false,
                        onSearchHit = { hit -> jumpTo(hit.page, hit.key) },
                    )
                } else {
                    SubPageTransition(
                        page = shown,
                        ui = ui,
                        actions = actions,
                        onJump = jumpTo,
                        palette = palette,
                        onBack = { page = SettingsPage.Home },
                        onToggleTone = toggleTone,
                        toneLabel = toneLabel,
                        maxWidthDp = 780f,
                        highlight = highlight,
                        onHighlightShown = { highlight = null },
                    )
                }
            }
        }
    }
}

/** A sub-page with its fixed bar. The content below it scrolls on its own. */
@Composable
private fun SubPageTransition(
    page: SettingsPage,
    ui: SettingsUi,
    actions: SettingsActions,
    palette: UiPalette,
    onBack: (() -> Unit)?,
    onToggleTone: () -> Unit,
    toneLabel: String,
    maxWidthDp: Float,
    highlight: String?,
    onHighlightShown: () -> Unit,
    onJump: (SettingsPage, String) -> Unit = { _, _ -> },
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        PageBar(
            palette = palette,
            title = stringResource(pageTitle(page)),
            onBack = onBack,
            onToggleTone = onToggleTone,
            toneLabel = toneLabel,
        )
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            SubPage(
                page = page,
                ui = ui,
                actions = actions,
                palette = palette,
                maxWidthDp = maxWidthDp,
                highlight = highlight,
                onHighlightShown = onHighlightShown,
                onJump = onJump,
            )
        }
    }
}

internal fun pageTitle(page: SettingsPage): Int = when (page) {
    SettingsPage.Home -> R.string.settings
    SettingsPage.Theme -> R.string.theme
    SettingsPage.Display -> R.string.section_display
    SettingsPage.Layout -> R.string.section_layout
    SettingsPage.Weather -> R.string.section_weather
    SettingsPage.BurnIn -> R.string.section_burnin
    SettingsPage.Night -> R.string.section_night
    SettingsPage.Occasion -> R.string.section_occasion
    SettingsPage.About -> R.string.section_about
}

internal fun pageDescription(page: SettingsPage): Int? = when (page) {
    SettingsPage.Theme -> R.string.page_theme_desc
    SettingsPage.Display -> R.string.page_display_desc
    SettingsPage.Layout -> R.string.page_layout_desc
    SettingsPage.Weather -> R.string.page_weather_desc
    SettingsPage.BurnIn -> R.string.page_burnin_desc
    SettingsPage.Night -> R.string.page_night_desc
    SettingsPage.Occasion -> R.string.page_occasion_desc
    else -> null
}

/** The fixed bar of a page: back on the left, title, tone button on the right. */
@Composable
private fun PageBar(
    palette: UiPalette,
    title: String,
    onBack: (() -> Unit)?,
    onToggleTone: () -> Unit,
    toneLabel: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(palette.backdropTop.copy(alpha = 0.97f))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            RoundIconButton(palette, AppIconKind.Back, stringResource(R.string.back), onBack)
            Spacer(Modifier.width(4.dp))
        } else {
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = title,
            color = palette.on,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        RoundIconButton(palette, toneIcon(palette), toneLabel, onToggleTone)
    }
}

private fun toneIcon(palette: UiPalette): AppIconKind =
    if (palette.dark) AppIconKind.Sun else AppIconKind.Moon

/** A 48dp round button with a hairline press highlight. */
@Composable
internal fun RoundIconButton(
    palette: UiPalette,
    icon: AppIconKind,
    label: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .semantics { contentDescription = label }
            .glassPressable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(kind = icon, tint = palette.on, size = 21.dp)
    }
}
