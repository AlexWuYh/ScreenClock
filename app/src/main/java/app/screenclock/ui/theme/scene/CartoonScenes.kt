package app.screenclock.ui.theme.scene

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale

/*
 * Two cartoon scenes, drawn from circles, ovals and a few paths. No bitmaps: the characters
 * are one shared `person` routine with a role, so a hand can be pointed at any spot and two
 * people can hold each other's hands.
 */

private val Ink = Color(0xFF4B3A48)
private val Blush = Color(0xFFFF9FB0)
private val Skin = Color(0xFFFFE0CC)

private fun DrawScope.heart(c: Offset, s: Float, color: Color) {
    val path = Path().apply {
        moveTo(c.x, c.y + s * 0.9f)
        cubicTo(c.x - s * 1.6f, c.y - s * 0.1f, c.x - s * 0.9f, c.y - s * 1.2f, c.x, c.y - s * 0.35f)
        cubicTo(c.x + s * 0.9f, c.y - s * 1.2f, c.x + s * 1.6f, c.y - s * 0.1f, c.x, c.y + s * 0.9f)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.cloud(c: Offset, s: Float, color: Color) {
    drawCircle(color, s * 0.55f, c + Offset(-s * 0.55f, s * 0.05f))
    drawCircle(color, s * 0.75f, c + Offset(0f, -s * 0.18f))
    drawCircle(color, s * 0.52f, c + Offset(s * 0.62f, s * 0.08f))
    drawRoundRect(color, c + Offset(-s * 1.05f, s * 0.05f), Size(s * 2.15f, s * 0.55f), CornerRadius(s * 0.27f))
}

private fun DrawScope.hills(top: Color, bottom: Color) {
    val w = size.width
    val h = size.height
    drawOval(top, Offset(-w * 0.25f, h * 0.90f), Size(w * 0.95f, h * 0.5f))
    drawOval(bottom, Offset(w * 0.30f, h * 0.93f), Size(w * 0.95f, h * 0.5f))
}

/** Two sweethearts under a cherry tree, a pink sky full of little hearts. */
@Composable
internal fun CoupleScene(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val wide = w > h
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color(0xFFFFD3E3),
                0.55f to Color(0xFFFFE6EE),
                1f to Color(0xFFFFF4E8),
            ),
        )
        val sun = Offset(w * 0.84f, h * 0.17f)
        drawCircle(
            brush = Brush.radialGradient(
                0f to Color(0xFFFFF3C4), 0.5f to Color(0xFFFFE9B8).copy(alpha = 0.5f), 1f to Color.Transparent,
                center = sun, radius = h * 0.2f,
            ),
            radius = h * 0.2f, center = sun,
        )
        drawCircle(Color(0xFFFFE8A8), h * 0.075f, sun)
        cloud(Offset(w * 0.18f, h * 0.2f), h * 0.075f, Color.White.copy(alpha = 0.85f))
        cloud(Offset(w * 0.62f, h * 0.1f), h * 0.055f, Color.White.copy(alpha = 0.75f))
        val floats = listOf(
            Triple(0.08f, 0.45f, 0.020f), Triple(0.30f, 0.12f, 0.016f), Triple(0.46f, 0.30f, 0.013f),
            Triple(0.72f, 0.28f, 0.022f), Triple(0.92f, 0.42f, 0.015f), Triple(0.56f, 0.06f, 0.012f),
            Triple(0.20f, 0.66f, 0.014f), Triple(0.78f, 0.62f, 0.018f),
        )
        floats.forEachIndexed { i, (x, y, s) ->
            val color = if (i % 2 == 0) Color(0xFFFF8FB0) else Color(0xFFFFB5C9)
            heart(Offset(w * x, h * y), h * s * 2f, color.copy(alpha = 0.55f))
        }
        hills(Color(0xFFCDEBCB), Color(0xFFB7E0BE))

        val u = if (wide) minOf(h * 0.052f, w * 0.04f) else w * 0.075f
        val base = h * 0.965f
        // The tree stands on the far side of the couple.
        val tx = w * 0.88f
        val t = u * 1.0f
        drawRoundRect(Color(0xFFB98A6B), Offset(tx - t * 0.3f, base - t * 3.6f), Size(t * 0.6f, t * 3.6f), CornerRadius(t * 0.2f))
        for ((dx, dy, r) in listOf(Triple(0f, -3.9f, 1.6f), Triple(-1.2f, -3.2f, 1.2f), Triple(1.2f, -3.2f, 1.2f), Triple(0f, -2.8f, 1.3f))) {
            drawCircle(Color(0xFFFFB6CB), t * r, Offset(tx + dx * t, base + dy * t))
        }
        for ((dx, dy) in listOf(-0.9f to -3.7f, 0.7f to -4.2f, 1.1f to -2.9f, -0.2f to -3.1f)) {
            heart(Offset(tx + dx * t, base + dy * t), t * 0.22f, Color(0xFFFF7FA8).copy(alpha = 0.8f))
        }

        // Hearts drift above the meadow where the couple used to stand.
        for ((fx, fy, sz) in listOf(Triple(0.16f, 0.80f, 0.55f), Triple(0.24f, 0.70f, 0.32f), Triple(0.09f, 0.68f, 0.26f), Triple(0.31f, 0.84f, 0.24f))) {
            heart(Offset(w * fx, h * fy), u * sz * 2f, Color(0xFFFF6F9B).copy(alpha = 0.9f))
        }
    }
}

/** A sunny meadow with a little house, a tree and flowers. No people. */
@Composable
internal fun FamilyScene(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val wide = w > h
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color(0xFFBFE3FF),
                0.6f to Color(0xFFE0F2FF),
                1f to Color(0xFFFFF6E2),
            ),
        )
        val sun = Offset(w * 0.12f, h * 0.17f)
        drawCircle(
            brush = Brush.radialGradient(
                0f to Color(0xFFFFF0B0), 0.5f to Color(0xFFFFE680).copy(alpha = 0.45f), 1f to Color.Transparent,
                center = sun, radius = h * 0.22f,
            ),
            radius = h * 0.22f, center = sun,
        )
        drawCircle(Color(0xFFFFD95E), h * 0.07f, sun)
        cloud(Offset(w * 0.40f, h * 0.15f), h * 0.07f, Color.White.copy(alpha = 0.9f))
        cloud(Offset(w * 0.78f, h * 0.22f), h * 0.06f, Color.White.copy(alpha = 0.85f))
        cloud(Offset(w * 0.60f, h * 0.06f), h * 0.04f, Color.White.copy(alpha = 0.7f))
        hills(Color(0xFFC5E8B8), Color(0xFFAEDCA3))

        val u = if (wide) minOf(h * 0.058f, w * 0.04f) else w * 0.062f
        val base = h * 0.965f
        // House, at 80% so it stays clear of the digits.
        val hx = if (wide) w * 0.085f else w * 0.15f
        scale(0.8f, 0.8f, pivot = Offset(hx, base)) {
            val hw = u * 4.6f
            val roofTop = base - u * 6.2f
            drawRoundRect(Color(0xFFFFF1D6), Offset(hx - hw / 2, base - u * 4.2f), Size(hw, u * 4.2f), CornerRadius(u * 0.3f))
            drawRoundRect(Color(0xFFE9A88C), Offset(hx + hw * 0.22f, roofTop + u * 0.4f), Size(u * 0.7f, u * 1.4f), CornerRadius(u * 0.1f))
            val roof = Path().apply {
                moveTo(hx - hw * 0.62f, base - u * 4.0f); lineTo(hx, roofTop); lineTo(hx + hw * 0.62f, base - u * 4.0f); close()
            }
            drawPath(roof, Color(0xFFF2877C))
            drawRoundRect(Color(0xFF9CC7F2), Offset(hx - u * 0.45f, base - u * 1.9f), Size(u * 0.9f, u * 1.9f), CornerRadius(u * 0.45f, u * 0.45f))
            drawCircle(Color(0xFFFFD95E), u * 0.07f, Offset(hx + u * 0.22f, base - u * 0.9f))
            for (side in listOf(-1f, 1f)) {
                val wc = Offset(hx + side * hw * 0.3f, base - u * 2.9f)
                drawRoundRect(Color(0xFFBFE3FF), wc - Offset(u * 0.38f, u * 0.38f), Size(u * 0.76f, u * 0.76f), CornerRadius(u * 0.12f))
                drawLine(Color.White, wc - Offset(0f, u * 0.38f), wc + Offset(0f, u * 0.38f), u * 0.07f)
                drawLine(Color.White, wc - Offset(u * 0.38f, 0f), wc + Offset(u * 0.38f, 0f), u * 0.07f)
            }
        }
        // Tree and flowers on the other side of the meadow.
        val tx = w * 0.9f
        drawRoundRect(Color(0xFFB98A6B), Offset(tx - u * 0.3f, base - u * 3.6f), Size(u * 0.6f, u * 3.6f), CornerRadius(u * 0.2f))
        for ((dx, dy, r) in listOf(Triple(0f, -3.9f, 1.5f), Triple(-1.1f, -3.2f, 1.1f), Triple(1.1f, -3.2f, 1.1f), Triple(0f, -2.8f, 1.2f))) {
            drawCircle(Color(0xFF8FD08A), u * r, Offset(tx + dx * u, base + dy * u))
        }
        for ((fx, c) in listOf(0.62f to Color(0xFFFF8FB0), 0.67f to Color(0xFFFFC857), 0.72f to Color(0xFFFF8FB0), 0.77f to Color(0xFFFFFFFF))) {
            val x = w * fx
            drawLine(Color(0xFF7FBF7A), Offset(x, base), Offset(x, base - u * 0.7f), u * 0.08f)
            drawCircle(c, u * 0.24f, Offset(x, base - u * 0.75f))
            drawCircle(Color(0xFFFFF3C4), u * 0.09f, Offset(x, base - u * 0.75f))
        }
    }
}
