package app.screenclock.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.screenclock.domain.time.ClockThemeId
import app.screenclock.domain.time.ColorwayId
import app.screenclock.domain.time.FaceTime
import app.screenclock.ui.theme.flip.FlipClock
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
        )
        ClockThemeId.Tube -> TubeClock(
            face = face,
            dateLine = dateLine,
            weatherLine = weatherLine,
            occasionLine = occasionLine,
            expanded = expanded,
            animate = animate,
            modifier = modifier,
        )
    }
}
