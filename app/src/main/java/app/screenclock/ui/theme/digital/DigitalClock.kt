package app.screenclock.ui.theme.digital

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import app.screenclock.domain.time.FaceTime
import app.screenclock.ui.Cream
import app.screenclock.ui.theme.ClockScaffold
import app.screenclock.ui.theme.scene.DuskLakeScene
import app.screenclock.ui.theme.scene.sceneDigitSize

private val DigitTint = Color(0xFFF4F0EA)

/**
 * 简约数字：thin digits over a drawn dusk lake. The meridiem mark sits under
 * the time, and nothing moves between minutes.
 */
@Composable
internal fun DigitalClock(
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
    Box(modifier = modifier) {
        DuskLakeScene(modifier = Modifier.fillMaxSize())
        ClockScaffold(
            face = face,
            dateLine = dateLine,
            weatherLine = weatherLine,
            occasionLine = occasionLine,
            expanded = expanded,
            modifier = Modifier.fillMaxSize(),
            preview = preview,
            meridiemBelow = true,
            infoColor = Cream.copy(alpha = 0.88f),
            onOccasionClick = onOccasionClick,
        ) { stacked, cardWidth, cardHeight, gap ->
            val areaWidth = if (stacked) cardWidth else cardWidth * 2 + gap
            val areaHeight = if (stacked) cardHeight * 2 + gap else cardHeight
            Row(
                modifier = Modifier.size(areaWidth, areaHeight),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${face.hourDigits}:${face.minuteDigits}",
                    color = DigitTint,
                    fontSize = sceneDigitSize(areaWidth, areaHeight),
                    fontWeight = FontWeight.Light,
                    style = digitalStyle(),
                )
            }
        }
    }
}

internal fun digitalStyle() = androidx.compose.ui.text.TextStyle(
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    ),
)
