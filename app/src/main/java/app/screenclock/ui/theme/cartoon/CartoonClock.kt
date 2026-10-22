package app.screenclock.ui.theme.cartoon

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import app.screenclock.domain.time.FaceTime
import app.screenclock.ui.theme.ClockScaffold
import app.screenclock.ui.theme.SplitDigits
import app.screenclock.ui.theme.ThemeFont
import app.screenclock.ui.theme.digital.digitalStyle
import app.screenclock.ui.theme.rememberThemeFamily
import app.screenclock.ui.theme.scene.BikiniScene
import app.screenclock.ui.theme.scene.CoupleScene
import app.screenclock.ui.theme.scene.FamilyScene
import app.screenclock.ui.theme.scene.KrabsScene
import app.screenclock.ui.theme.scene.SpongePatScene
import app.screenclock.ui.theme.scene.sceneDigitSize

/** Digit and info colours of one cartoon theme. [outline] draws a thick border around the digits. */
private data class CartoonLook(
    val fill: Color,
    val info: Color,
    val font: ThemeFont,
    val outline: Color? = null,
)

private val CoupleLook = CartoonLook(Color(0xFF8A3F5E), Color(0xFF8A3F5E), ThemeFont.Couple)
private val FamilyLook = CartoonLook(Color(0xFF3F5F8E), Color(0xFF3F5F8E), ThemeFont.Family)
private val SpongePatLook = CartoonLook(Color(0xFFFFE84A), Color.White, ThemeFont.SpongePat, Color(0xFF0F4C75))
private val KrabsLook = CartoonLook(Color(0xFFFFD54A), Color.White, ThemeFont.Krabs, Color(0xFF6B1A12))
private val BikiniLook = CartoonLook(Color(0xFFFFF6B8), Color(0xFF0B4F73), ThemeFont.Bikini, Color(0xFF0B5C8A))

/** 情侣：rose digits over a pink sky and drifting hearts. */
@Composable
internal fun CoupleClock(
    face: FaceTime, dateLine: String?, weatherLine: String?, occasionLine: String?, expanded: Boolean,
    modifier: Modifier = Modifier, preview: Boolean = false, onOccasionClick: (() -> Unit)? = null,
) = CartoonClock({ CoupleScene(Modifier.fillMaxSize()) }, CoupleLook, face, dateLine, weatherLine, occasionLine, expanded, modifier, preview, onOccasionClick)

/** 温馨小屋：navy digits over a sunny meadow with a little house. */
@Composable
internal fun FamilyClock(
    face: FaceTime, dateLine: String?, weatherLine: String?, occasionLine: String?, expanded: Boolean,
    modifier: Modifier = Modifier, preview: Boolean = false, onOccasionClick: (() -> Unit)? = null,
) = CartoonClock({ FamilyScene(Modifier.fillMaxSize()) }, FamilyLook, face, dateLine, weatherLine, occasionLine, expanded, modifier, preview, onOccasionClick)

/** 海绵宝宝&派大星. */
@Composable
internal fun SpongePatClock(
    face: FaceTime, dateLine: String?, weatherLine: String?, occasionLine: String?, expanded: Boolean,
    modifier: Modifier = Modifier, preview: Boolean = false, onOccasionClick: (() -> Unit)? = null,
) = CartoonClock({ SpongePatScene(Modifier.fillMaxSize()) }, SpongePatLook, face, dateLine, weatherLine, occasionLine, expanded, modifier, preview, onOccasionClick)

/** 皮老板&蟹老板. */
@Composable
internal fun KrabsClock(
    face: FaceTime, dateLine: String?, weatherLine: String?, occasionLine: String?, expanded: Boolean,
    modifier: Modifier = Modifier, preview: Boolean = false, onOccasionClick: (() -> Unit)? = null,
) = CartoonClock({ KrabsScene(Modifier.fillMaxSize()) }, KrabsLook, face, dateLine, weatherLine, occasionLine, expanded, modifier, preview, onOccasionClick)

/** 比奇堡居民: the fish townsfolk. */
@Composable
internal fun BikiniClock(
    face: FaceTime, dateLine: String?, weatherLine: String?, occasionLine: String?, expanded: Boolean,
    modifier: Modifier = Modifier, preview: Boolean = false, onOccasionClick: (() -> Unit)? = null,
) = CartoonClock({ BikiniScene(Modifier.fillMaxSize()) }, BikiniLook, face, dateLine, weatherLine, occasionLine, expanded, modifier, preview, onOccasionClick)

@Composable
private fun CartoonClock(
    scene: @Composable () -> Unit,
    look: CartoonLook,
    face: FaceTime,
    dateLine: String?,
    weatherLine: String?,
    occasionLine: String?,
    expanded: Boolean,
    modifier: Modifier,
    preview: Boolean,
    onOccasionClick: (() -> Unit)?,
) {
    val family = rememberThemeFamily(look.font)
    Box(modifier = modifier) {
        scene()
        ClockScaffold(
            face = face,
            dateLine = dateLine,
            weatherLine = weatherLine,
            occasionLine = occasionLine,
            expanded = expanded,
            modifier = Modifier.fillMaxSize(),
            preview = preview,
            meridiemBelow = true,
            infoColor = look.info.copy(alpha = 0.92f),
            onOccasionClick = onOccasionClick,
        ) { stacked, cardWidth, cardHeight, gap ->
            val areaWidth = if (stacked) cardWidth else cardWidth * 2 + gap
            val areaHeight = if (stacked) cardHeight * 2 + gap else cardHeight
            val size = sceneDigitSize(areaWidth, areaHeight)
            val text = "${face.hourDigits}:${face.minuteDigits}"
            Row(
                modifier = Modifier.requiredSize(areaWidth, areaHeight),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SplitDigits(
                    hour = face.hourDigits,
                    minute = face.minuteDigits,
                    fontSize = size,
                    color = look.fill,
                    family = family,
                    outline = look.outline,
                    style = digitalStyle().copy(
                        shadow = if (look.outline == null) {
                            Shadow(color = Color.White.copy(alpha = 0.85f), offset = Offset(0f, 0f), blurRadius = 18f)
                        } else {
                            null
                        },
                    ),
                    gapFactor = if (look.outline != null) 0.27f else 0.2f,
                )
            }
        }
    }
}
