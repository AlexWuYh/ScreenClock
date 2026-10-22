package app.screenclock.ui.theme

import android.content.Context
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily

/**
 * The digit face of each non-flip theme. All are SIL OFL variable fonts shipped in
 * `assets/fonts`; [weight] is set on the `wght` axis so one file serves every weight.
 */
internal enum class ThemeFont(val file: String, val weight: Int) {
    Digital("Outfit.ttf", 300),
    Minimal("Jost.ttf", 600),
    Analog("CormorantGaramond.ttf", 600),
    Earth("Exo2.ttf", 200),
    Fluid("Quicksand.ttf", 600),
    Neon("Orbitron.ttf", 600),
    Couple("Fredoka.ttf", 600),
    Family("Nunito.ttf", 800),
    SpongePat("LilitaOne.ttf", 400),
    Krabs("TitanOne.ttf", 400),
    Bikini("Fredoka.ttf", 700),
}

private val cache = HashMap<ThemeFont, Typeface>()

/** Falls back to the platform sans if the asset cannot be opened. */
internal fun themeTypeface(context: Context, font: ThemeFont): Typeface = synchronized(cache) {
    cache.getOrPut(font) {
        try {
            Typeface.Builder(context.assets, "fonts/${font.file}")
                .setFontVariationSettings("'wght' ${font.weight}")
                .build()
        } catch (_: Exception) {
            null
        } ?: Typeface.SANS_SERIF
    }
}

@Composable
internal fun rememberThemeTypeface(font: ThemeFont): Typeface {
    val context = LocalContext.current
    return remember(font) { themeTypeface(context, font) }
}

@Composable
internal fun rememberThemeFamily(font: ThemeFont): FontFamily {
    val face = rememberThemeTypeface(font)
    return remember(face) { FontFamily(face) }
}
