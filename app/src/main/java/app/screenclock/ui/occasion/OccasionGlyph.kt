package app.screenclock.ui.occasion

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.screenclock.domain.occasion.OccasionIcon
import app.screenclock.ui.circle
import app.screenclock.ui.dot
import app.screenclock.ui.line
import app.screenclock.ui.path

/** The occasion pictures, drawn as line art on a 24-unit grid like the rest of the app's icons. */
@Composable
internal fun OccasionGlyph(
    icon: OccasionIcon,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
) {
    Canvas(modifier.size(size)) {
        val unit = this.size.minDimension / 24f
        scale(scaleX = unit, scaleY = unit, pivot = Offset.Zero) {
            val stroke = Stroke(width = 1.9f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            when (icon) {
                OccasionIcon.Cake -> {
                    path(listOf(4f to 20f, 4f to 13f, 20f to 13f, 20f to 20f), tint, stroke, close = true)
                    path(listOf(4f to 16.5f, 8f to 18.2f, 12f to 16.5f, 16f to 18.2f, 20f to 16.5f), tint, stroke, curves = true)
                    for (x in listOf(8f, 12f, 16f)) {
                        line(x, 13f, x, 9.5f, tint, stroke)
                        dot(x, 7.4f, 1.3f, tint)
                    }
                    line(3f, 20f, 21f, 20f, tint, stroke)
                }
                OccasionIcon.Heart -> path(
                    listOf(12f to 20f, 4.2f to 12.4f, 4.6f to 7.6f, 8.4f to 5.4f, 12f to 8.4f, 15.6f to 5.4f, 19.4f to 7.6f, 19.8f to 12.4f),
                    tint, stroke, close = true, curves = true,
                )
                OccasionIcon.Gift -> {
                    path(listOf(4f to 11f, 20f to 11f, 20f to 20f, 4f to 20f), tint, stroke, close = true)
                    path(listOf(3f to 7.5f, 21f to 7.5f, 21f to 11f, 3f to 11f), tint, stroke, close = true)
                    line(12f, 7.5f, 12f, 20f, tint, stroke)
                    path(listOf(12f to 7.5f, 8.2f to 3.8f, 6.2f to 5.8f, 8.4f to 7.5f), tint, stroke)
                    path(listOf(12f to 7.5f, 15.8f to 3.8f, 17.8f to 5.8f, 15.6f to 7.5f), tint, stroke)
                }
                OccasionIcon.Plane -> {
                    path(listOf(21f to 3f, 3f to 10.5f, 10.5f to 13.5f, 13.5f to 21f), tint, stroke, close = true)
                    line(10.5f, 13.5f, 21f, 3f, tint, stroke)
                }
                OccasionIcon.Cap -> {
                    path(listOf(12f to 5f, 22f to 10f, 12f to 15f, 2f to 10f), tint, stroke, close = true)
                    path(listOf(6.5f to 12.4f, 6.5f to 16.6f, 12f to 19f, 17.5f to 16.6f, 17.5f to 12.4f), tint, stroke)
                    line(22f, 10f, 22f, 16f, tint, stroke)
                }
                OccasionIcon.Ring -> {
                    circle(12f, 15f, 5.6f, tint, stroke)
                    path(listOf(9f to 6.4f, 10.4f to 3.8f, 13.6f to 3.8f, 15f to 6.4f, 12f to 9.4f), tint, stroke, close = true)
                }
                OccasionIcon.Star -> path(
                    listOf(
                        12f to 3.4f, 14.6f to 9.1f, 20.8f to 9.7f, 16.1f to 13.8f, 17.5f to 19.9f,
                        12f to 16.7f, 6.5f to 19.9f, 7.9f to 13.8f, 3.2f to 9.7f, 9.4f to 9.1f,
                    ),
                    tint, stroke, close = true,
                )
                OccasionIcon.Flag -> {
                    line(5.5f, 3.5f, 5.5f, 21f, tint, stroke)
                    path(listOf(5.5f to 4.5f, 18.5f to 4.5f, 15.5f to 8.6f, 18.5f to 12.7f, 5.5f to 12.7f), tint, stroke, close = true)
                }
            }
        }
    }
}
