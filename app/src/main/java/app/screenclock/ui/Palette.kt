package app.screenclock.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Clock art keeps its own palette, separate from the management pages.
internal val Ink = Color(0xFF000000)
// The default flip finish deliberately has a slightly darker flap than its backdrop.
internal val FlipBackdrop = Color(0xFF22211F)
internal val FlipDarkFlap = Color(0xFF171716)
// Focus and ceremony keep the shared graphite flap independent from the home finish.
internal val Flap = Color(0xFF333333)
internal val GraphiteFlap = Color(0xFF484A4D)
internal val Digit = Color(0xFFFFFFFF)
internal val Amber = Color(0xFFE6A15C)
internal val SceneLight = Color(0xFFF2EDE4)
internal val TubeGlass = Color(0xFF100E0C)
internal val SnowFlap = Color(0xFFF1EDE4)
internal val SnowDigit = Color(0xFF1B1A17)
internal val FluidInk = Color(0xFF2E3A66)

// The light palette of the management pages: cool porcelain and calm blue; face Amber stays separate.
internal val Paper = Color(0xFFF4F8FD)
internal val Cream = Color(0xFF222F44)
internal val Info = Color(0xFF4F647E)
internal val Muted = Color(0xFF4E637D)
internal val Panel = Color(0x00FFFFFF)
internal val PanelHigh = Color(0x00FFFFFF)
internal val Hairline = Color(0x264A6C9A)
internal val ChromeBar = Color(0xFFE6EDF8)
internal val Alert = Color(0xFFBA4950)
internal val UiAccent = Color(0xFF9DBFEA)
internal val UiAccentDeep = Color(0xFF4A78B8)
// Text on the dark clock faces themselves.
internal val FaceBright = Color(0xFFF3EDE2)
internal val FaceText = Color(0xFFA39E96)

/**
 * Every colour a management page needs for one tone. The settings, occasion, countdown
 * and focus pages read the same instance through [LocalUiPalette], so the light/dark
 * choice applies everywhere at once. Clock themes never read it.
 */
@Immutable
internal data class UiPalette(
    val dark: Boolean,
    /** Page backdrop: a three-stop vertical gradient. */
    val backdropTop: Color,
    val backdropMid: Color,
    val backdropBottom: Color,
    /** Soft colour pools behind the content, so panels and tiles have something to sit on. */
    val pools: List<Color>,
    /** A tappable tile or card. */
    val tile: Color,
    /** The same tile when it is the selected one, and its text. */
    val tileSelected: Color,
    val onTileSelected: Color,
    /** The well a grouped tile sits in, and the raised row inside it. */
    val field: Color,
    val tileBorder: Color,
    /** Text: primary, secondary, tertiary. */
    val on: Color,
    val on2: Color,
    val on3: Color,
    /** Interaction colour and its soft companion. */
    val accent: Color,
    val accentSoft: Color,
    val onAccentSoft: Color,
    val outline: Color,
    val line: Color,
    val alert: Color,
    /** A note that asks the user to do something. */
    val warn: Color,
    val warnBg: Color,
    /** Press feedback on a tile. */
    val hit: Color,
    /** Frosted glass, still used by the occasion pages and dialogs. */
    val glassFill: Color,
    val glassFillHigh: Color,
    val glassBorder: Color,
)

internal val LightUiPalette = UiPalette(
    dark = false,
    backdropTop = Color(0xFFF5F8FD),
    backdropMid = Color(0xFFF8FAFE),
    backdropBottom = Color(0xFFF2F7FD),
    pools = listOf(
        Color(0xFFA9C8F0).copy(alpha = 0.20f),
        Color(0xFFC4D8F5).copy(alpha = 0.24f),
        Color(0xFFD3E2F8).copy(alpha = 0.26f),
        Color(0xFFBBD3F3).copy(alpha = 0.18f),
    ),
    tile = Color(0xE6FFFFFF),
    tileSelected = Color(0xFFDBE9FC),
    onTileSelected = Color(0xFF14365F),
    field = Color(0xFFE7EDF8),
    tileBorder = Color(0x1F3A5A8C),
    on = Cream,
    on2 = Info,
    on3 = Muted,
    accent = UiAccentDeep,
    accentSoft = Color(0xFF9DBFEA).copy(alpha = 0.24f),
    onAccentSoft = Color(0xFF17395F),
    outline = Color(0xFF74777F),
    line = Hairline,
    alert = Alert,
    warn = Color(0xFFB3541E),
    warnBg = Color(0xFFFFF1E0),
    hit = Color(0xFFEAEEFA),
    glassFill = GlassFill,
    glassFillHigh = GlassFillHigh,
    glassBorder = GlassBorder,
)

/** The dark twin of [LightUiPalette]; the clock faces are unaffected either way. */
internal val DarkUiPalette = UiPalette(
    dark = true,
    backdropTop = Color(0xFF101319),
    backdropMid = Color(0xFF12161D),
    backdropBottom = Color(0xFF0E1117),
    pools = listOf(
        Color(0xFF2B4B86).copy(alpha = 0.32f),
        Color(0xFF1E3A66).copy(alpha = 0.26f),
        Color(0xFF27405F).copy(alpha = 0.22f),
        Color(0xFF203A5C).copy(alpha = 0.18f),
    ),
    tile = Color(0xFF1B1F27),
    tileSelected = Color(0xFF2C4478),
    onTileSelected = Color(0xFFDBE3FF),
    field = Color(0xFF252932),
    tileBorder = Color(0x14FFFFFF),
    on = Color(0xFFE4E2E9),
    on2 = Color(0xFFC4C6D0),
    on3 = Color(0xFF8E919A),
    accent = Color(0xFFAEC6FF),
    accentSoft = Color(0xFF2C4478),
    onAccentSoft = Color(0xFFDBE3FF),
    outline = Color(0xFF8E919A),
    line = Color(0x1FFFFFFF),
    alert = Color(0xFFFF8A90),
    warn = Color(0xFFFFB77C),
    warnBg = Color(0xFF3A2A18),
    hit = Color(0xFF272B35),
    glassFill = GlassDarkPanelFill,
    glassFillHigh = GlassDarkPanelFillHigh,
    glassBorder = GlassDarkBorder,
)

internal fun uiPalette(dark: Boolean): UiPalette = if (dark) DarkUiPalette else LightUiPalette

/** The palette of the page being composed. Set once at the app root. */
internal val LocalUiPalette = staticCompositionLocalOf { LightUiPalette }
