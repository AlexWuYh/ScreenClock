package app.screenclock.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.screenclock.domain.time.ClockThemeId
import app.screenclock.domain.time.ColorwayId
import app.screenclock.domain.time.FaceTime
import app.screenclock.ui.theme.analog.AnalogClock
import app.screenclock.ui.theme.digital.DigitalClock
import app.screenclock.ui.theme.earth.EarthClock
import app.screenclock.ui.theme.fluid.FluidClock
import app.screenclock.ui.theme.flip.FlipClock
import app.screenclock.ui.theme.neon.NeonClock
import app.screenclock.ui.theme.tube.TubeClock

@Composable
internal fun ThemedClock(
    theme: ClockThemeId,
    colorway: ColorwayId,
    face: FaceTime,
    dateLine: String?,
    weatherLine: String?,
    occasionLine: String?,
    expanded: Boolean,
    animate: Boolean,
    modifier: Modifier = Modifier,
    preview: Boolean = false,
    onOccasionClick: (() -> Unit)? = null,
) {
    when (theme) {
        ClockThemeId.Flip -> FlipClock(
            face = face,
            dateLine = dateLine,
            weatherLine = weatherLine,
            occasionLine = occasionLine,
            colorway = colorway,
            expanded = expanded,
            animate = animate,
            modifier = modifier,
            preview = preview,
            onOccasionClick = onOccasionClick,
        )
        ClockThemeId.Tube -> TubeClock(
            face = face,
            dateLine = dateLine,
            weatherLine = weatherLine,
            occasionLine = occasionLine,
            expanded = expanded,
            animate = animate,
            modifier = modifier,
            preview = preview,
            onOccasionClick = onOccasionClick,
        )
        ClockThemeId.Digital -> DigitalClock(
            face = face,
            dateLine = dateLine,
            weatherLine = weatherLine,
            occasionLine = occasionLine,
            expanded = expanded,
            animate = animate,
            modifier = modifier,
            preview = preview,
            onOccasionClick = onOccasionClick,
        )
        ClockThemeId.Analog -> AnalogClock(
            face = face,
            dateLine = dateLine,
            weatherLine = weatherLine,
            occasionLine = occasionLine,
            expanded = expanded,
            animate = animate,
            modifier = modifier,
            preview = preview,
            onOccasionClick = onOccasionClick,
        )
        ClockThemeId.Earth -> EarthClock(
            face = face,
            dateLine = dateLine,
            weatherLine = weatherLine,
            occasionLine = occasionLine,
            expanded = expanded,
            animate = animate,
            modifier = modifier,
            preview = preview,
            onOccasionClick = onOccasionClick,
        )
        ClockThemeId.Fluid -> FluidClock(
            face = face,
            dateLine = dateLine,
            weatherLine = weatherLine,
            occasionLine = occasionLine,
            expanded = expanded,
            animate = animate,
            modifier = modifier,
            preview = preview,
            onOccasionClick = onOccasionClick,
        )
        ClockThemeId.Neon -> NeonClock(
            face = face,
            dateLine = dateLine,
            weatherLine = weatherLine,
            occasionLine = occasionLine,
            expanded = expanded,
            animate = animate,
            modifier = modifier,
            preview = preview,
            onOccasionClick = onOccasionClick,
        )
    }
}
