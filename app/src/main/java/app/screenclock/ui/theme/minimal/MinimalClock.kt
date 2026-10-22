package app.screenclock.ui.theme.minimal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import app.screenclock.domain.time.FaceTime
import app.screenclock.ui.theme.ClockScaffold
import app.screenclock.ui.theme.SplitDigits
import app.screenclock.ui.theme.ThemeFont
import app.screenclock.ui.theme.digital.digitalStyle
import app.screenclock.ui.theme.rememberThemeFamily
import app.screenclock.ui.theme.scene.sceneDigitSize

/** 极简：hairline digits on pure black, nothing else. Black pixels are off pixels on OLED. */
@Composable
internal fun MinimalClock(
    face: FaceTime,
    dateLine: String?,
    weatherLine: String?,
    occasionLine: String?,
    expanded: Boolean,
    modifier: Modifier = Modifier,
    preview: Boolean = false,
    onOccasionClick: (() -> Unit)? = null,
) {
    val family = rememberThemeFamily(ThemeFont.Minimal)
    Box(modifier = modifier.background(Color.Black)) {
        ClockScaffold(
            face = face,
            dateLine = dateLine,
            weatherLine = weatherLine,
            occasionLine = occasionLine,
            expanded = expanded,
            modifier = Modifier.fillMaxSize(),
            preview = preview,
            meridiemBelow = true,
            infoColor = Color(0xFF8C8C8C),
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
                    color = Color.White,
                    family = family,
                )
            }
        }
    }
}
