package app.screenclock.ui.theme.fluid

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import app.screenclock.domain.time.FaceTime
import app.screenclock.ui.FluidInk
import app.screenclock.ui.theme.ClockScaffold
import app.screenclock.ui.theme.SplitDigits
import app.screenclock.ui.theme.ThemeFont
import app.screenclock.ui.theme.rememberThemeFamily
import app.screenclock.ui.theme.digital.digitalStyle
import app.screenclock.ui.theme.scene.FluidScene
import app.screenclock.ui.theme.scene.sceneDigitSize

/**
 * 流体光影：soft digits on flowing pastel waves. Calm colours, no motion
 * between minutes.
 */
@Composable
internal fun FluidClock(
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
    val themeFont = rememberThemeFamily(ThemeFont.Fluid)
    Box(modifier = modifier) {
        FluidScene(modifier = Modifier.fillMaxSize())
        ClockScaffold(
            face = face,
            dateLine = dateLine,
            weatherLine = weatherLine,
            occasionLine = occasionLine,
            expanded = expanded,
            modifier = Modifier.fillMaxSize(),
            preview = preview,
            meridiemBelow = true,
            infoColor = FluidInk.copy(alpha = 0.8f),
            onOccasionClick = onOccasionClick,
        ) { stacked, cardWidth, cardHeight, gap ->
            val areaWidth = if (stacked) cardWidth else cardWidth * 2 + gap
            val areaHeight = if (stacked) cardHeight * 2 + gap else cardHeight
            Row(
                modifier = Modifier.requiredSize(areaWidth, areaHeight),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SplitDigits(
                    hour = face.hourDigits,
                    minute = face.minuteDigits,
                    fontSize = sceneDigitSize(areaWidth, areaHeight),
                    color = FluidInk,
                    family = themeFont,
                )
            }
        }
    }
}
