package app.screenclock.ui.theme.scene

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.screenclock.domain.time.ClockThemeId
import app.screenclock.ui.Ink

/**
 * The drawn backdrop of a theme, without its digits. Glass surfaces sample this
 * so a blurred copy can sit behind them and line up with the screen.
 */
@Composable
internal fun ThemeScene(theme: ClockThemeId, modifier: Modifier = Modifier) {
    when (theme) {
        ClockThemeId.Digital -> DuskLakeScene(modifier)
        ClockThemeId.Analog -> MountainScene(modifier)
        ClockThemeId.Earth -> EarthScene(modifier)
        ClockThemeId.Fluid -> FluidScene(modifier)
        ClockThemeId.Neon -> NeonScene(modifier)
        ClockThemeId.Flip, ClockThemeId.Tube -> Box(modifier.background(Ink))
    }
}
