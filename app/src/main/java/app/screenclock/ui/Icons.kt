package app.screenclock.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The small line-icon set. Drawn here, on a 24 unit grid, so nothing is bundled. */
internal enum class AppIconKind {
    Back,
    Sliders,
    Palette,
    Timer,
    Moon,
    Heart,
    Plus,
    Minus,
    Trash,
    Pin,
    Search,
    Location,
    Check,
    ChevronLeft,
    ChevronRight,
    Link,
    Edit,
}

@Composable
internal fun AppIcon(
    kind: AppIconKind,
    modifier: Modifier = Modifier,
    tint: Color = Cream,
    size: Dp = 22.dp,
) {
    Canvas(modifier.size(size)) {
        val unit = this.size.minDimension / 24f
        scale(scaleX = unit, scaleY = unit, pivot = Offset.Zero) {
            val line = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            when (kind) {
                AppIconKind.Edit -> {
                    path(listOf(5f to 19f, 5.8f to 15.2f, 16.2f to 4.8f, 19.2f to 7.8f, 8.8f to 18.2f), tint, line, close = true)
                    line(14f, 7f, 17f, 10f, tint, line)
                }
                AppIconKind.Back -> {
                    line(5f, 12f, 19f, 12f, tint, line)
                    path(listOf(11f to 6f, 5f to 12f, 11f to 18f), tint, line)
                }
                AppIconKind.ChevronLeft -> path(listOf(14f to 6f, 8f to 12f, 14f to 18f), tint, line)
                AppIconKind.ChevronRight -> path(listOf(10f to 6f, 16f to 12f, 10f to 18f), tint, line)
                AppIconKind.Plus -> {
                    line(12f, 5f, 12f, 19f, tint, line)
                    line(5f, 12f, 19f, 12f, tint, line)
                }
                AppIconKind.Minus -> line(5f, 12f, 19f, 12f, tint, line)
                AppIconKind.Check -> path(listOf(5f to 12.5f, 10f to 17.5f, 19f to 7f), tint, line)
                AppIconKind.Sliders -> {
                    line(4f, 6.5f, 20f, 6.5f, tint, line)
                    line(4f, 12f, 20f, 12f, tint, line)
                    line(4f, 17.5f, 20f, 17.5f, tint, line)
                    dot(15f, 6.5f, 2.4f, tint)
                    dot(9f, 12f, 2.4f, tint)
                    dot(15.5f, 17.5f, 2.4f, tint)
                }
                AppIconKind.Palette -> {
                    circle(12f, 12f, 8.6f, tint, line)
                    dot(8f, 8.5f, 1.3f, tint)
                    dot(13f, 6.9f, 1.3f, tint)
                    dot(17f, 10.5f, 1.3f, tint)
                    dot(8f, 15.4f, 1.3f, tint)
                }
                AppIconKind.Timer -> {
                    circle(12f, 14f, 6.5f, tint, line)
                    line(12f, 4.2f, 12f, 7.5f, tint, line)
                    line(9.8f, 4.2f, 14.2f, 4.2f, tint, line)
                    line(12f, 14f, 12f, 10.4f, tint, line)
                }
                AppIconKind.Moon -> path(
                    listOf(
                        20.2f to 14.8f,
                        19f to 18.2f,
                        15.8f to 20.6f,
                        12f to 20.6f,
                        7.2f to 20.6f,
                        3.4f to 16.8f,
                        3.4f to 12f,
                        3.4f to 8.2f,
                        5.8f to 4.9f,
                        9.2f to 4f,
                        8.4f to 5.4f,
                        7.9f to 7f,
                        7.9f to 8.6f,
                        7.9f to 12.6f,
                        11.1f to 15.8f,
                        15.1f to 15.8f,
                        16.7f to 15.8f,
                        18.3f to 15.3f,
                        20.2f to 14.8f,
                    ),
                    tint,
                    line,
                    close = true,
                    curves = true,
                )
                AppIconKind.Search -> {
                    circle(10.8f, 10.8f, 6.2f, tint, line)
                    line(15.4f, 15.4f, 20f, 20f, tint, line)
                }
                AppIconKind.Location -> {
                    path(
                        listOf(
                            12f to 20.8f,
                            5.2f to 14.6f,
                            5.2f to 9.6f,
                            8.2f to 3f,
                            15.8f to 3f,
                            18.8f to 9.6f,
                            18.8f to 14.6f,
                            12f to 20.8f,
                        ),
                        tint,
                        line,
                        close = true,
                        curves = true,
                    )
                    circle(12f, 9.6f, 2.5f, tint, line)
                }
                AppIconKind.Heart -> path(
                    listOf(
                        12f to 20.2f,
                        3.6f to 15f,
                        3.6f to 9.2f,
                        5.8f to 4.3f,
                        8.4f to 4.3f,
                        10f to 5.2f,
                        12f to 6.4f,
                        14f to 5.2f,
                        15.6f to 4.3f,
                        18.2f to 4.3f,
                        20.4f to 9.2f,
                        20.4f to 15f,
                        12f to 20.2f,
                    ),
                    tint,
                    line,
                    close = true,
                    curves = true,
                )
                AppIconKind.Trash -> {
                    line(4.2f, 6.8f, 19.8f, 6.8f, tint, line)
                    path(
                        listOf(6.6f to 6.8f, 7.5f to 19.2f, 16.5f to 19.2f, 17.4f to 6.8f),
                        tint,
                        line,
                    )
                    path(
                        listOf(9.8f to 6.8f, 9.8f to 5.2f, 11.2f to 3.8f, 12.8f to 3.8f, 14.2f to 5.2f, 14.2f to 6.8f),
                        tint,
                        line,
                    )
                }
                AppIconKind.Pin -> {
                    path(
                        listOf(9f to 4.5f, 15f to 4.5f, 14f to 10f, 18f to 13.5f, 6f to 13.5f, 10f to 10f),
                        tint,
                        line,
                        close = true,
                    )
                    line(12f, 13.5f, 12f, 20f, tint, line)
                }
                AppIconKind.Link -> {
                    path(
                        listOf(
                            18f to 13.5f,
                            18f to 18.2f,
                            16.2f to 20f,
                            5.8f to 20f,
                            4f to 18.2f,
                            4f to 7.8f,
                            5.8f to 6f,
                            10.5f to 6f,
                        ),
                        tint,
                        line,
                    )
                    line(14.5f, 4f, 20f, 4f, tint, line)
                    line(20f, 4f, 20f, 9.5f, tint, line)
                    line(20f, 4f, 12.5f, 11.5f, tint, line)
                }
            }
        }
    }
}

internal fun DrawScope.line(x1: Float, y1: Float, x2: Float, y2: Float, color: Color, stroke: Stroke) {
    drawPath(
        path = Path().apply {
            moveTo(x1, y1)
            lineTo(x2, y2)
        },
        color = color,
        style = stroke,
    )
}

internal fun DrawScope.circle(x: Float, y: Float, radius: Float, color: Color, stroke: Stroke) {
    drawCircle(color = color, radius = radius, center = Offset(x, y), style = stroke)
}

internal fun DrawScope.dot(x: Float, y: Float, radius: Float, color: Color) {
    drawCircle(color = color, radius = radius, center = Offset(x, y))
}

internal fun DrawScope.path(
    points: List<Pair<Float, Float>>,
    color: Color,
    stroke: Stroke,
    close: Boolean = false,
    curves: Boolean = false,
) {
    val path = Path()
    path.moveTo(points.first().first, points.first().second)
    if (curves) {
        // Smooth each corner so hearts and pins stay soft instead of faceted.
        var previous = points.first()
        for (point in points.drop(1)) {
            val mid = Offset((previous.first + point.first) / 2f, (previous.second + point.second) / 2f)
            path.quadraticTo(previous.first, previous.second, mid.x, mid.y)
            previous = point
        }
        path.lineTo(previous.first, previous.second)
    } else {
        for (point in points.drop(1)) {
            path.lineTo(point.first, point.second)
        }
    }
    if (close) path.close()
    drawPath(path = path, color = color, style = stroke)
}
