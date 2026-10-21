package app.screenclock.ui.theme.scene

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin

/**
 * Drawn backdrops for the scene themes. Everything here is shapes and gradients
 * painted by the app itself: no photos, no bundled images.
 */

/** Warm dusk over a lake: gradient sky, low sun, two ridges, shimmering water. */
@Composable
internal fun DuskLakeScene(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val horizon = h * 0.56f
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color(0xFF28406E),
                0.26f to Color(0xFF6A587E),
                0.54f to Color(0xFFB57A7E),
                0.82f to Color(0xFFE7A679),
                1f to Color(0xFFD99A6C),
                startY = 0f,
                endY = horizon,
            ),
            topLeft = Offset(0f, 0f),
            size = Size(w, horizon),
        )
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color(0xFFD99A6C),
                0.23f to Color(0xFFB98B7F),
                0.68f to Color(0xFF82617A),
                1f to Color(0xFF5E4B70),
                startY = horizon,
                endY = h,
            ),
            topLeft = Offset(0f, horizon),
            size = Size(w, h - horizon),
        )
        val sun = Offset(w * 0.54f, horizon * 0.99f)
        drawCircle(
            brush = Brush.radialGradient(
                0f to Color(0xFFFFD9A0).copy(alpha = 0.85f),
                0.55f to Color(0xFFFFB470).copy(alpha = 0.35f),
                1f to Color.Transparent,
                center = sun,
                radius = h * 0.30f,
            ),
            radius = h * 0.30f,
            center = sun,
        )
        drawPath(
            path = Path().apply {
                moveTo(0f, horizon)
                lineTo(w * 0.12f, horizon - h * 0.050f)
                lineTo(w * 0.22f, horizon - h * 0.018f)
                lineTo(w * 0.34f, horizon - h * 0.085f)
                lineTo(w * 0.46f, horizon - h * 0.028f)
                lineTo(w * 0.62f, horizon - h * 0.070f)
                lineTo(w * 0.78f, horizon - h * 0.024f)
                lineTo(w * 0.92f, horizon - h * 0.058f)
                lineTo(w, horizon - h * 0.020f)
                lineTo(w, horizon)
                close()
            },
            color = Color(0xFF4B3F63).copy(alpha = 0.9f),
        )
        drawPath(
            path = Path().apply {
                moveTo(0f, horizon)
                lineTo(w * 0.18f, horizon - h * 0.028f)
                lineTo(w * 0.38f, horizon - h * 0.006f)
                lineTo(w * 0.55f, horizon - h * 0.036f)
                lineTo(w * 0.72f, horizon - h * 0.010f)
                lineTo(w * 0.88f, horizon - h * 0.030f)
                lineTo(w, horizon - h * 0.008f)
                lineTo(w, horizon)
                close()
            },
            color = Color(0xFF3A3050),
        )
        for (band in 0 until 7) {
            val y = horizon + (h - horizon) * (0.12f + band * 0.115f)
            drawLine(
                color = Color(0xFFFFE2B8).copy(alpha = 0.16f - band * 0.018f),
                start = Offset(w * (0.08f + band * 0.03f), y),
                end = Offset(w * (0.92f - band * 0.02f), y),
                strokeWidth = (h * 0.004f).coerceAtLeast(1f),
                cap = StrokeCap.Round,
            )
        }
    }
}

/** Cool blue sky over layered snow ridges, with a soft mist band. */
@Composable
internal fun MountainScene(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val horizon = h * 0.62f
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color(0xFF17304F),
                0.55f to Color(0xFF4F74A6),
                1f to Color(0xFFB9CBDD),
                startY = 0f,
                endY = horizon,
            ),
            topLeft = Offset(0f, 0f),
            size = Size(w, horizon),
        )
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color(0xFFB9CBDD),
                0.24f to Color(0xFF9FB6CE),
                1f to Color(0xFF22375A),
                startY = horizon,
                endY = h,
            ),
            topLeft = Offset(0f, horizon),
            size = Size(w, h - horizon),
        )
        drawCircle(
            brush = Brush.radialGradient(
                0f to Color(0xFFFFE8C8).copy(alpha = 0.35f),
                1f to Color.Transparent,
                center = Offset(w * 0.30f, h * 0.30f),
                radius = h * 0.42f,
            ),
            radius = h * 0.42f,
            center = Offset(w * 0.30f, h * 0.30f),
        )
        drawPath(
            path = Path().apply {
                moveTo(0f, horizon * 0.98f)
                lineTo(w * 0.16f, horizon * 0.62f)
                lineTo(w * 0.30f, horizon * 0.86f)
                lineTo(w * 0.47f, horizon * 0.55f)
                lineTo(w * 0.62f, horizon * 0.82f)
                lineTo(w * 0.80f, horizon * 0.60f)
                lineTo(w, horizon * 0.90f)
                lineTo(w, horizon)
                lineTo(0f, horizon)
                close()
            },
            color = Color(0xFF6E8FB8).copy(alpha = 0.85f),
        )
        drawPath(
            path = Path().apply {
                moveTo(0f, horizon)
                lineTo(w * 0.10f, h * 0.46f)
                lineTo(w * 0.22f, h * 0.58f)
                lineTo(w * 0.38f, h * 0.40f)
                lineTo(w * 0.52f, h * 0.56f)
                lineTo(w * 0.70f, h * 0.42f)
                lineTo(w * 0.86f, h * 0.58f)
                lineTo(w, h * 0.50f)
                lineTo(w, horizon + h * 0.12f)
                lineTo(0f, horizon + h * 0.12f)
                close()
            },
            color = Color(0xFF2C4568),
        )
        drawPath(
            path = Path().apply {
                moveTo(w * 0.38f, h * 0.40f)
                lineTo(w * 0.34f, h * 0.455f)
                lineTo(w * 0.41f, h * 0.44f)
                close()
                moveTo(w * 0.70f, h * 0.42f)
                lineTo(w * 0.67f, h * 0.465f)
                lineTo(w * 0.73f, h * 0.455f)
                close()
            },
            color = Color(0xFFE8F0F8).copy(alpha = 0.9f),
        )
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color(0xFFDCE8F4).copy(alpha = 0.32f),
                1f to Color.Transparent,
                startY = horizon - h * 0.10f,
                endY = horizon + h * 0.05f,
            ),
            topLeft = Offset(0f, horizon - h * 0.10f),
            size = Size(w, h * 0.15f),
        )
    }
}

/** Night earth from orbit: stars, the dark limb, an atmosphere rim and a sunrise. */
@Composable
internal fun EarthScene(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color(0xFF04060E),
                0.55f to Color(0xFF0A1224),
                1f to Color(0xFF12203C),
            ),
        )
        for (star in 0 until 70) {
            val x = ((star * 137.5f) % 100f) / 100f * w
            val y = ((star * 73.3f) % 100f) / 100f * h * 0.66f
            val bright = 0.25f + ((star * 29.7f) % 60f) / 100f
            drawCircle(
                color = Color.White.copy(alpha = bright),
                radius = (h * 0.0016f) * (1f + (star % 3) * 0.5f),
                center = Offset(x, y),
            )
        }
        val earthCenter = Offset(w * 0.5f, h * 1.52f)
        val earthRadius = h * 1.06f
        drawCircle(
            brush = Brush.radialGradient(
                0f to Color(0xFF16294A),
                1f to Color(0xFF0A1730),
                center = earthCenter,
                radius = earthRadius,
            ),
            radius = earthRadius,
            center = earthCenter,
        )
        drawCircle(
            color = Color(0xFF3E7BD8).copy(alpha = 0.30f),
            radius = earthRadius * 1.012f,
            center = earthCenter,
            style = Stroke(width = h * 0.014f),
        )
        drawCircle(
            color = Color(0xFF7FB0FF).copy(alpha = 0.14f),
            radius = earthRadius * 1.05f,
            center = earthCenter,
            style = Stroke(width = h * 0.030f),
        )
        val dawn = Offset(w * 0.80f, h * 0.72f)
        drawCircle(
            brush = Brush.radialGradient(
                0f to Color(0xFFFFB56B).copy(alpha = 0.65f),
                0.5f to Color(0xFFFF8C4A).copy(alpha = 0.22f),
                1f to Color.Transparent,
                center = dawn,
                radius = h * 0.55f,
            ),
            radius = h * 0.55f,
            center = dawn,
        )
        for (light in 0 until 26) {
            val angle = Math.toRadians((18.0 + light * 2.9))
            val radius = earthRadius * (0.985f + (light % 3) * 0.004f)
            val x = earthCenter.x + (sin(angle) * radius).toFloat()
            val y = earthCenter.y - (cos(angle) * radius).toFloat()
            if (y > h * 0.62f) {
                drawCircle(
                    color = Color(0xFFFFC98A).copy(alpha = 0.35f + (light % 4) * 0.14f),
                    radius = h * 0.0026f * (1f + (light % 3) * 0.4f),
                    center = Offset(x, y),
                )
            }
        }
    }
}

/** Light pastel waves: a calm surface of soft flowing bands. */
@Composable
internal fun FluidScene(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color(0xFFE7EDF9),
                1f to Color(0xFFF6EEE6),
            ),
        )
        val bands = listOf(
            Triple(Color(0xFFB8D5ED), 0.25f, 0.34f),
            Triple(Color(0xFFB7C2E6), 0.37f, 0.49f),
            Triple(Color(0xFFC9B6DF), 0.53f, 0.63f),
            Triple(Color(0xFFE8CFC8), 0.68f, 0.76f),
        )
        for ((color, leftY, rightY) in bands) {
            val path = Path().apply {
                moveTo(0f, h * leftY)
                cubicTo(w * 0.25f, h * (leftY - 0.12f), w * 0.55f, h * (rightY + 0.12f), w, h * rightY)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    0f to lerp(color, Color.White, 0.22f).copy(alpha = 0.85f),
                    0.38f to color.copy(alpha = 0.92f),
                    1f to color.copy(alpha = 0.96f),
                    startY = h * (minOf(leftY, rightY) - 0.12f),
                    endY = h * (maxOf(leftY, rightY) + 0.35f),
                ),
            )
        }
        for ((index, curve) in bands.withIndex()) {
            val (color, leftY, rightY) = curve
            drawPath(
                path = Path().apply {
                    moveTo(0f, h * leftY)
                    cubicTo(w * 0.25f, h * (leftY - 0.12f), w * 0.55f, h * (rightY + 0.12f), w, h * rightY)
                },
                color = color.copy(alpha = 0.55f - index * 0.08f),
                style = Stroke(width = h * 0.0025f),
            )
        }
    }
}

/** Synthwave: a purple gradient, a glowing horizon and a perspective grid. */
@Composable
internal fun NeonScene(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val horizon = h * 0.60f
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color(0xFF140923),
                0.7f to Color(0xFF2B0F45),
                1f to Color(0xFF3B1557),
                startY = 0f,
                endY = horizon,
            ),
            topLeft = Offset(0f, 0f),
            size = Size(w, horizon),
        )
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color(0xFF2A0E3F),
                1f to Color(0xFF12081E),
                startY = horizon,
                endY = h,
            ),
            topLeft = Offset(0f, horizon),
            size = Size(w, h - horizon),
        )
        drawCircle(
            brush = Brush.radialGradient(
                0f to Color(0xFFFF3E9E).copy(alpha = 0.30f),
                1f to Color.Transparent,
                center = Offset(w * 0.5f, horizon),
                radius = h * 0.55f,
            ),
            radius = h * 0.55f,
            center = Offset(w * 0.5f, horizon),
        )
        drawCircle(
            brush = Brush.radialGradient(
                0f to Color(0xFF7A4BFF).copy(alpha = 0.28f),
                1f to Color.Transparent,
                center = Offset(w * 0.5f, horizon),
                radius = h * 0.75f,
            ),
            radius = h * 0.75f,
            center = Offset(w * 0.5f, horizon),
        )
        // A striped sunset and mountain silhouettes give the grid a horizon.
        val sun = Offset(w * 0.52f, horizon - h * 0.11f)
        val sunRadius = h * 0.17f
        drawCircle(
            brush = Brush.verticalGradient(
                0f to Color(0xFFFFB65F),
                1f to Color(0xFFE9438A),
                startY = sun.y - sunRadius,
                endY = sun.y + sunRadius,
            ),
            radius = sunRadius,
            center = sun,
        )
        for (stripe in 0..3) {
            val fraction = 0.22f + stripe * 0.17f
            val halfWidth = sunRadius * kotlin.math.sqrt(1f - fraction * fraction)
            drawRect(
                color = Color(0xFF311143),
                topLeft = Offset(sun.x - halfWidth, sun.y + sunRadius * fraction),
                size = Size(halfWidth * 2f, h * (0.003f + stripe * 0.002f)),
            )
        }
        drawPath(
            path = Path().apply {
                moveTo(0f, horizon)
                lineTo(w * 0.12f, horizon - h * 0.10f)
                lineTo(w * 0.25f, horizon - h * 0.012f)
                lineTo(w * 0.38f, horizon - h * 0.06f)
                lineTo(w * 0.53f, horizon)
                lineTo(w * 0.7f, horizon - h * 0.075f)
                lineTo(w * 0.85f, horizon - h * 0.012f)
                lineTo(w, horizon - h * 0.095f)
                lineTo(w, horizon + h * 0.025f)
                lineTo(0f, horizon + h * 0.025f)
                close()
            },
            color = Color(0xFF170C2D),
        )
        val grid = Color(0xFFC05BFF).copy(alpha = 0.30f)
        for (i in -7..7) {
            drawLine(
                color = grid,
                start = Offset(w * 0.5f + i * w * 0.045f, horizon),
                end = Offset(w * 0.5f + i * w * 0.36f, h),
                strokeWidth = (h * 0.0016f).coerceAtLeast(1f),
            )
        }
        for (step in 1..7) {
            val t = (step / 7f) * (step / 7f)
            val y = horizon + (h - horizon) * t
            drawLine(
                color = grid,
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = (h * 0.0016f).coerceAtLeast(1f),
            )
        }
    }
}
