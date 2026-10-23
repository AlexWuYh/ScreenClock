package app.screenclock.ui.ceremony

import androidx.compose.ui.graphics.Color
import app.screenclock.domain.occasion.CeremonyStyle

/** Every colour of the countdown page for one design in one tone. */
internal data class CeremonyPalette(
    val bg: Color,
    val glowA: Color,
    val glowB: Color,
    val glowC: Color,
    val text1: Color,
    val text2: Color,
    val text3: Color,
    val surface: Color,
    val surfaceBorder: Color,
    val card: Color,
    val cardBorder: Color,
    val well: Color,
    val wellBorder: Color,
    val divider: Color,
    val chip: Color,
    val chipBorder: Color,
    val accent: Color,
    val accentBg: Color,
    val dark: Boolean,
)

private fun ink(alpha: Float) = Color(0xFF1B2233).copy(alpha = alpha)

internal fun ceremonyPalette(style: CeremonyStyle, dark: Boolean): CeremonyPalette = when (style) {
    CeremonyStyle.Card -> if (dark) {
        CeremonyPalette(
            bg = Color(0xFF0F1117),
            glowA = Color(90, 120, 255).copy(alpha = .22f),
            glowB = Color(255, 150, 80).copy(alpha = .15f),
            glowC = Color(60, 70, 130).copy(alpha = .18f),
            text1 = Color.White,
            text2 = Color.White.copy(alpha = .65f),
            text3 = Color.White.copy(alpha = .5f),
            surface = Color(28, 32, 44).copy(alpha = .85f),
            surfaceBorder = Color.White.copy(alpha = .08f),
            card = Color.White.copy(alpha = .05f),
            cardBorder = Color.White.copy(alpha = .06f),
            well = Color.Black.copy(alpha = .2f),
            wellBorder = Color.White.copy(alpha = .03f),
            divider = Color.White.copy(alpha = .06f),
            chip = Color.White.copy(alpha = .08f),
            chipBorder = Color.White.copy(alpha = .1f),
            accent = Color(0xFFFFD966),
            accentBg = Color(255, 215, 0).copy(alpha = .12f),
            dark = true,
        )
    } else {
        CeremonyPalette(
            bg = Color(0xFFF6F8FD),
            glowA = Color(175, 200, 255).copy(alpha = .75f),
            glowB = Color(255, 215, 170).copy(alpha = .6f),
            glowC = Color(210, 220, 255).copy(alpha = .8f),
            text1 = Color(0xFF1B2233),
            text2 = ink(.66f),
            text3 = ink(.48f),
            surface = Color.White.copy(alpha = .8f),
            surfaceBorder = Color.Black.copy(alpha = .05f),
            card = ink(.035f),
            cardBorder = ink(.06f),
            well = Color.White.copy(alpha = .7f),
            wellBorder = ink(.05f),
            divider = ink(.07f),
            chip = ink(.05f),
            chipBorder = ink(.08f),
            accent = Color(0xFFA97C00),
            accentBg = Color(240, 180, 20).copy(alpha = .18f),
            dark = false,
        )
    }
    CeremonyStyle.Immersive -> if (dark) {
        CeremonyPalette(
            bg = Color(0xFF0A0C12),
            glowA = Color(90, 120, 255).copy(alpha = .15f),
            glowB = Color(255, 150, 80).copy(alpha = .10f),
            glowC = Color.Transparent,
            text1 = Color.White,
            text2 = Color.White.copy(alpha = .6f),
            text3 = Color.White.copy(alpha = .35f),
            surface = Color.Transparent,
            surfaceBorder = Color.Transparent,
            card = Color.Transparent,
            cardBorder = Color.Transparent,
            well = Color.Transparent,
            wellBorder = Color.Transparent,
            divider = Color.White.copy(alpha = .1f),
            chip = Color.White.copy(alpha = .05f),
            chipBorder = Color.White.copy(alpha = .1f),
            accent = Color(0xFFFFD966),
            accentBg = Color.Transparent,
            dark = true,
        )
    } else {
        val slate = Color(0xFF0F172A)
        CeremonyPalette(
            bg = Color(0xFFF5F7FB),
            glowA = Color(140, 180, 255).copy(alpha = .35f),
            glowB = Color(255, 200, 150).copy(alpha = .35f),
            glowC = Color.Transparent,
            text1 = slate,
            text2 = slate.copy(alpha = .65f),
            text3 = slate.copy(alpha = .4f),
            surface = Color.Transparent,
            surfaceBorder = Color.Transparent,
            card = Color.Transparent,
            cardBorder = Color.Transparent,
            well = Color.Transparent,
            wellBorder = Color.Transparent,
            divider = slate.copy(alpha = .1f),
            chip = slate.copy(alpha = .04f),
            chipBorder = slate.copy(alpha = .08f),
            accent = Color(0xFFD97706),
            accentBg = Color.Transparent,
            dark = false,
        )
    }
}
