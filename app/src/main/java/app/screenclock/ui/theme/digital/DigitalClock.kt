package app.screenclock.ui.theme.digital

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import app.screenclock.domain.time.FaceTime
import app.screenclock.ui.Amber
import app.screenclock.ui.Cream
import app.screenclock.ui.theme.ClockScaffold

/**
 * One bold digital line. The colon is quiet and never blinks; the home screen
 * stays still between minutes.
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
    ClockScaffold(
        face = face,
        dateLine = dateLine,
        weatherLine = weatherLine,
        occasionLine = occasionLine,
        expanded = expanded,
        modifier = modifier,
        preview = preview,
        onOccasionClick = onOccasionClick,
    ) { stacked, cardWidth, cardHeight, gap ->
        val areaWidth = if (stacked) cardWidth else cardWidth * 2 + gap
        val areaHeight = if (stacked) cardHeight * 2 + gap else cardHeight
        val density = LocalDensity.current
        val digitSize = with(density) { areaHeight.toSp() } * 0.72f
        val colonSize = with(density) { areaHeight.toSp() } * 0.5f
        Row(
            modifier = Modifier.size(areaWidth, areaHeight),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = face.hourDigits,
                color = Cream,
                fontSize = digitSize,
                fontWeight = FontWeight.Bold,
                style = digitStyle(),
            )
            Text(
                text = ":",
                color = Amber,
                fontSize = colonSize,
                fontWeight = FontWeight.Bold,
                style = digitStyle(),
            )
            Text(
                text = face.minuteDigits,
                color = Cream,
                fontSize = digitSize,
                fontWeight = FontWeight.Bold,
                style = digitStyle(),
            )
        }
    }
}

internal fun digitStyle() = androidx.compose.ui.text.TextStyle(
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    ),
)
