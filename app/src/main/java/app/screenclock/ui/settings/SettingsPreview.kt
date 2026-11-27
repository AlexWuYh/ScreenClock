package app.screenclock.ui.settings

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.screenclock.R
import app.screenclock.domain.burnin.ScreensaverTheme
import app.screenclock.domain.occasion.CeremonyStyle
import app.screenclock.domain.time.ClockThemeId
import app.screenclock.domain.settings.settingsPreviewHeight
import app.screenclock.domain.settings.settingsThemeColumns
import app.screenclock.domain.time.showcasedThemes
import app.screenclock.domain.weather.formatWeatherLine
import app.screenclock.ui.UiPalette
import app.screenclock.ui.ceremony.CeremonyPalette
import app.screenclock.ui.ceremony.ceremonyPalette
import app.screenclock.ui.clock.ScreensaverVideo
import app.screenclock.ui.theme.ThemedClock
import androidx.compose.ui.res.stringResource

/** A real, non-animating clock face in a rounded frame; the top preview of a page. */
@Composable
internal fun ClockPreview(
    ui: SettingsUi,
    modifier: Modifier = Modifier,
    tall: Boolean = false,
) {
    val shape = RoundedCornerShape(24.dp)
    val height = if (tall) 196.dp else 150.dp
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(Color.Black)
            .border(1.dp, Color.Black.copy(alpha = 0.2f), shape),
    ) {
        Box(Modifier.fillMaxSize().clearAndSetSemantics { }) {
            ClockFace(ui, modifier = Modifier.fillMaxSize())
        }
    }
}

/**
 * The shared live rendering used by the page preview and the theme grid. These are always
 * previews: a settings page shows a scaled-down face, so the scaffold must shrink its date
 * and footer lines too, or the digits are squeezed into whatever height is left.
 */
@Composable
internal fun ClockFace(
    ui: SettingsUi,
    modifier: Modifier = Modifier,
    theme: ClockThemeId = ui.theme,
) {
    val face = ui.face ?: return
    BoxWithConstraints(modifier = modifier) {
        val expanded = maxWidth >= 240.dp
        ThemedClock(
            theme = theme,
            colorway = ui.colorway,
            face = face,
            dateLine = ui.dateLine,
            weatherLine = formatWeatherLine(ui.weather, showRange = expanded),
            occasionLine = null,
            expanded = expanded,
            animate = false,
            datePosition = ui.datePosition,
            digitSize = ui.digitSize,
            modifier = Modifier.fillMaxSize(),
            preview = true,
        )
    }
}

/**
 * The theme grid. Previews appear one per frame so opening the page never waits on
 * drawing all of them at once.
 */
@Composable
internal fun ThemeGrid(
    ui: SettingsUi,
    palette: UiPalette,
    onTheme: (ClockThemeId) -> Unit,
    readyCount: Int,
    short: Boolean,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columns = settingsThemeColumns(maxWidth.value)
        val gap = 12.dp
        val tileWidth = (maxWidth.value - gap.value * (columns - 1)) / columns
        val height = settingsPreviewHeight(tileWidth, short)
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            showcasedThemes.chunked(columns).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                ) {
                    val filler = if (columns > 1) (columns - rowItems.size).coerceAtLeast(0) else 0
                    rowItems.forEach { id ->
                        ThemeTile(
                            ui = ui,
                            palette = palette,
                            id = id,
                            heightDp = height,
                            selected = ui.theme == id,
                            ready = showcasedThemes.indexOf(id) < readyCount,
                            onClick = { onTheme(id) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(filler) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun ThemeTile(
    ui: SettingsUi,
    palette: UiPalette,
    id: ClockThemeId,
    heightDp: Float,
    selected: Boolean,
    ready: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(heightDp.dp)
                .clip(shape)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) palette.accent else palette.tileBorder,
                    shape = shape,
                )
                .background(Color.Black),
        ) {
            if (ready) {
                Box(Modifier.fillMaxSize().clearAndSetSemantics { }) {
                    ClockFace(ui, modifier = Modifier.fillMaxSize(), theme = id)
                }
            }
        }
        Spacer(modifier = Modifier.height(7.dp))
        Text(
            text = "${showcasedThemes.indexOf(id) + 1}".padStart(2, '0') + " · " + stringResource(themeLabel(id)),
            color = if (selected) palette.accent else palette.on,
            fontSize = 13.5.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(themeSubtitle(id)),
            color = palette.on3,
            fontSize = 11.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Mobile gets one legible preview per row; larger screens can show two. */
internal fun themePreviewColumns(widthDp: Float): Int = if (widthDp < 360f) 1 else 2

/** Preserve an image-like ratio while keeping both small and tablet cards useful. */
internal fun themePreviewHeight(widthDp: Float): Float = (widthDp * 0.62f).coerceIn(132f, 220f)

internal fun themeSubtitle(id: ClockThemeId): Int = when (id) {
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

internal fun themeLabel(id: ClockThemeId): Int = when (id) {
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

/**
 * The 16:9 screensaver preview. Video themes play for real; the drift theme shows a small
 * clock wandering, so the difference between them is visible before it runs.
 */
@Composable
internal fun ScreensaverPreview(theme: ScreensaverTheme, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .widthIn(max = 360.dp)
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(shape)
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        if (theme.isVideo) {
            key(theme) { ScreensaverVideo(theme, Modifier.fillMaxSize()) }
        } else {
            val drift = rememberInfiniteTransition(label = "driftPreview")
            val dx by drift.animateFloat(
                initialValue = -14f,
                targetValue = 14f,
                animationSpec = infiniteRepeatable(tween(4_000, easing = LinearEasing), RepeatMode.Reverse),
                label = "dx",
            )
            Text(
                text = "12:34",
                color = Color.White,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.offset(x = dx.dp, y = (dx / 2).dp),
            )
        }
    }
}

/**
 * The two countdown designs as small previews, drawn with the real palette of the chosen
 * tone so the choice reads before opening the page. Card gives each number its own little
 * chip on a soft background; immersive puts the numbers straight on the gradient.
 */
@Composable
internal fun CeremonyStylePreview(
    style: CeremonyStyle,
    dark: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = remember(style, dark) { ceremonyPalette(style, dark) }
    val shape = RoundedCornerShape(18.dp)
    val numberColor = if (style == CeremonyStyle.Card) palette.text1 else palette.text1
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(112.dp)
            .clip(shape)
            .background(palette.bg)
            .drawBehind {
                // The real page glows from two corners; the thumbnail keeps that cue.
                drawRect(
                    brush = Brush.radialGradient(
                        listOf(palette.glowA, Color.Transparent),
                        center = Offset(size.width * 0.2f, 0f),
                        radius = maxOf(size.width, size.height) * 0.8f,
                    ),
                )
                drawRect(
                    brush = Brush.radialGradient(
                        listOf(palette.glowB, Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height),
                        radius = maxOf(size.width, size.height) * 0.8f,
                    ),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "妈妈生日", color = palette.text3, fontSize = 10.5.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                MiniPart(style, palette, "86", "天", numberColor)
                MiniPart(style, palette, "04", "时", numberColor)
                MiniPart(style, palette, "12", "分", numberColor)
                MiniPart(style, palette, "09", "秒", numberColor)
            }
        }
    }
}

@Composable
private fun MiniPart(
    style: CeremonyStyle,
    palette: CeremonyPalette,
    number: String,
    unit: String,
    text: Color,
) {
    if (style == CeremonyStyle.Card) {
        Column(
            modifier = Modifier
                .width(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(
                    if (palette.surface.alpha == 0f) Color.White.copy(alpha = 0.08f) else palette.surface,
                )
                .border(1.dp, palette.surfaceBorder, RoundedCornerShape(9.dp))
                .padding(vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = number, color = text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(text = unit, color = text.copy(alpha = 0.6f), fontSize = 8.5.sp)
        }
    } else {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(text = number, color = text, fontSize = 17.sp, fontWeight = FontWeight.Light)
            Text(
                text = unit,
                color = text.copy(alpha = 0.55f),
                fontSize = 8.5.sp,
                modifier = Modifier.padding(start = 1.dp, bottom = 1.dp),
            )
        }
    }
}
