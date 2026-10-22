package app.screenclock.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import app.screenclock.domain.time.ClockThemeId
import app.screenclock.domain.time.ColorwayId
import app.screenclock.domain.time.DatePosition
import app.screenclock.domain.time.DigitSize
import app.screenclock.domain.time.FaceTime
import app.screenclock.ui.theme.analog.AnalogClock
import app.screenclock.ui.theme.digital.DigitalClock
import app.screenclock.ui.theme.earth.EarthClock
import app.screenclock.ui.theme.fluid.FluidClock
import app.screenclock.ui.theme.flip.FlipClock
import app.screenclock.ui.theme.cartoon.CoupleClock
import app.screenclock.ui.theme.cartoon.BikiniClock
import app.screenclock.ui.theme.cartoon.FamilyClock
import app.screenclock.ui.theme.cartoon.KrabsClock
import app.screenclock.ui.theme.cartoon.SpongePatClock
import app.screenclock.ui.theme.minimal.MinimalClock
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
    datePosition: DatePosition = DatePosition.Above,
    digitSize: DigitSize = DigitSize.Regular,
    preview: Boolean = false,
    onOccasionClick: (() -> Unit)? = null,
) {
    CompositionLocalProvider(LocalClockLayout provides ClockLayoutPrefs(datePosition, digitSize)) {
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
        ClockThemeId.Minimal -> MinimalClock(
            face = face,
            dateLine = dateLine,
            weatherLine = weatherLine,
            occasionLine = occasionLine,
            expanded = expanded,
            modifier = modifier,
            preview = preview,
            onOccasionClick = onOccasionClick,
        )
        ClockThemeId.Couple -> CoupleClock(
            face = face,
            dateLine = dateLine,
            weatherLine = weatherLine,
            occasionLine = occasionLine,
            expanded = expanded,
            modifier = modifier,
            preview = preview,
            onOccasionClick = onOccasionClick,
        )
        ClockThemeId.Family -> FamilyClock(
            face = face,
            dateLine = dateLine,
            weatherLine = weatherLine,
            occasionLine = occasionLine,
            expanded = expanded,
            modifier = modifier,
            preview = preview,
            onOccasionClick = onOccasionClick,
        )
        ClockThemeId.SpongePat -> SpongePatClock(
            face = face,
            dateLine = dateLine,
            weatherLine = weatherLine,
            occasionLine = occasionLine,
            expanded = expanded,
            modifier = modifier,
            preview = preview,
            onOccasionClick = onOccasionClick,
        )
        ClockThemeId.Krabs -> KrabsClock(
            face = face,
            dateLine = dateLine,
            weatherLine = weatherLine,
            occasionLine = occasionLine,
            expanded = expanded,
            modifier = modifier,
            preview = preview,
            onOccasionClick = onOccasionClick,
        )
        ClockThemeId.Bikini -> BikiniClock(
            face = face,
            dateLine = dateLine,
            weatherLine = weatherLine,
            occasionLine = occasionLine,
            expanded = expanded,
            modifier = modifier,
            preview = preview,
            onOccasionClick = onOccasionClick,
        )
        }
    }
}
