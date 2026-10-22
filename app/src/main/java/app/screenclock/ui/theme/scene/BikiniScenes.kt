package app.screenclock.ui.theme.scene

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * Three undersea scenes. Every character is drawn here from circles, ovals and a few paths,
 * as an original cartoon rendition; no bitmaps are used. Sizes hang off one unit `u` so a
 * character scales with the screen.
 */

private val Line = Color(0xFF3A2A22)
private val SpongeYellow = Color(0xFFFFE23D)
private val SpongeSpot = Color(0xFFE9C71F)

private fun DrawScope.eye(c: Offset, r: Float, iris: Color, look: Float = 0f) {
    drawCircle(Color.White, r, c)
    drawCircle(Line.copy(alpha = 0.55f), r, c, style = Stroke(r * 0.08f))
    drawCircle(iris, r * 0.46f, c + Offset(look * r * 0.3f, r * 0.08f))
    drawCircle(Line, r * 0.22f, c + Offset(look * r * 0.3f, r * 0.08f))
    drawCircle(Color.White, r * 0.1f, c + Offset(look * r * 0.3f - r * 0.1f, -r * 0.1f))
}

private fun DrawScope.smile(c: Offset, w: Float, depth: Float, stroke: Float) {
    drawArc(Line, 10f, 160f, false, c - Offset(w / 2, depth * 0.3f), Size(w, depth), style = Stroke(stroke, cap = StrokeCap.Round))
}

private fun DrawScope.bubble(c: Offset, r: Float) {
    drawCircle(Color.White.copy(alpha = 0.16f), r, c)
    drawCircle(Color.White.copy(alpha = 0.55f), r, c, style = Stroke(r * 0.09f))
    drawCircle(Color.White.copy(alpha = 0.75f), r * 0.2f, c + Offset(-r * 0.4f, -r * 0.4f))
}

private fun DrawScope.jellyfish(c: Offset, r: Float, color: Color) {
    for (i in -2..2) {
        val x = c.x + i * r * 0.36f
        val path = Path().apply {
            moveTo(x, c.y)
            quadraticTo(x + r * 0.22f, c.y + r * 0.6f, x, c.y + r * 1.1f)
            quadraticTo(x - r * 0.22f, c.y + r * 1.5f, x + r * 0.05f, c.y + r * 1.8f)
        }
        drawPath(path, color.copy(alpha = 0.7f), style = Stroke(r * 0.07f, cap = StrokeCap.Round))
    }
    drawArc(color.copy(alpha = 0.78f), 180f, 180f, true, c - Offset(r, r), Size(r * 2, r * 2))
    drawArc(Color.White.copy(alpha = 0.4f), 200f, 60f, false, c - Offset(r * 0.8f, r * 0.8f), Size(r * 1.6f, r * 1.6f), style = Stroke(r * 0.1f, cap = StrokeCap.Round))
    for (spot in listOf(-0.4f to -0.45f, 0.05f to -0.7f, 0.45f to -0.35f)) {
        drawCircle(Color.White.copy(alpha = 0.45f), r * 0.1f, c + Offset(spot.first * r, spot.second * r))
    }
}

private fun DrawScope.seaweed(x: Float, base: Float, h: Float, color: Color) {
    val path = Path().apply {
        moveTo(x, base)
        cubicTo(x - h * 0.25f, base - h * 0.35f, x + h * 0.25f, base - h * 0.65f, x, base - h)
    }
    drawPath(path, color, style = Stroke(h * 0.09f, cap = StrokeCap.Round))
}

private fun DrawScope.sea(top: Color, bottom: Color, sand: Color, rays: Boolean = true) {
    val w = size.width
    val h = size.height
    drawRect(Brush.verticalGradient(0f to top, 1f to bottom))
    if (rays) {
        for (i in 0..3) {
            val x = w * (0.1f + i * 0.27f)
            val ray = Path().apply {
                moveTo(x, 0f); lineTo(x + w * 0.08f, 0f); lineTo(x + w * 0.2f, h * 0.9f); lineTo(x + w * 0.02f, h * 0.9f); close()
            }
            drawPath(ray, Color.White.copy(alpha = 0.06f))
        }
    }
    drawOval(sand, Offset(-w * 0.2f, h * 0.9f), Size(w * 0.9f, h * 0.32f))
    drawOval(sand.copy(alpha = 0.92f), Offset(w * 0.25f, h * 0.92f), Size(w * 0.95f, h * 0.32f))
}

private fun DrawScope.spongebob(cx: Float, base: Float, u: Float, wave: Boolean = true) {
    val legTop = base - 0.9f * u
    val top = legTop - 2.95f * u
    for (side in listOf(-1f, 1f)) {
        val x = cx + side * 0.55f * u
        drawLine(SpongeYellow, Offset(x, legTop), Offset(x, base - 0.45f * u), 0.22f * u)
        drawRect(Color.White, Offset(x - 0.17f * u, base - 0.62f * u), Size(0.34f * u, 0.2f * u))
        drawRect(Color(0xFFD9473F), Offset(x - 0.17f * u, base - 0.5f * u), Size(0.34f * u, 0.07f * u))
        drawOval(Line, Offset(x - 0.3f * u, base - 0.3f * u), Size(0.62f * u, 0.3f * u))
    }
    // Arms are drawn before the body so the sleeves stay thin.
    val shoulderY = top + 2.0f * u
    drawLine(SpongeYellow, Offset(cx - 1.25f * u, shoulderY), Offset(cx - 1.8f * u, shoulderY + 0.55f * u), 0.2f * u, StrokeCap.Round)
    drawCircle(SpongeYellow, 0.2f * u, Offset(cx - 1.82f * u, shoulderY + 0.6f * u))
    val handR = if (wave) Offset(cx + 1.85f * u, shoulderY - 1.1f * u) else Offset(cx + 1.8f * u, shoulderY + 0.6f * u)
    drawLine(SpongeYellow, Offset(cx + 1.25f * u, shoulderY), handR, 0.2f * u, StrokeCap.Round)
    drawCircle(SpongeYellow, 0.24f * u, handR)
    drawRoundRect(SpongeYellow, Offset(cx - 1.3f * u, top), Size(2.6f * u, 2.95f * u), CornerRadius(0.2f * u))
    for ((sx, sy, sr) in listOf(Triple(-0.95f, 0.3f, 0.14f), Triple(0.9f, 0.45f, 0.1f), Triple(-1.0f, 1.9f, 0.1f), Triple(0.95f, 1.75f, 0.15f), Triple(0.2f, 0.15f, 0.08f))) {
        drawCircle(SpongeSpot, sr * u, Offset(cx + sx * u, top + sy * u))
    }
    // Shirt, tie and trousers.
    drawRect(Color.White, Offset(cx - 1.3f * u, top + 2.0f * u), Size(2.6f * u, 0.2f * u))
    val tie = Path().apply {
        moveTo(cx - 0.1f * u, top + 2.0f * u); lineTo(cx + 0.1f * u, top + 2.0f * u)
        lineTo(cx + 0.16f * u, top + 2.35f * u); lineTo(cx, top + 2.45f * u); lineTo(cx - 0.16f * u, top + 2.35f * u); close()
    }
    drawPath(tie, Color(0xFFD9473F))
    drawRoundRect(Color(0xFFB07A3B), Offset(cx - 1.3f * u, top + 2.2f * u), Size(2.6f * u, 0.75f * u), CornerRadius(0.1f * u))
    drawRect(Line, Offset(cx - 1.3f * u, top + 2.2f * u), Size(2.6f * u, 0.1f * u))
    drawRect(Color(0xFFE9C45B), Offset(cx - 0.12f * u, top + 2.2f * u), Size(0.24f * u, 0.1f * u))
    // Face.
    for (side in listOf(-1f, 1f)) {
        val e = Offset(cx + side * 0.56f * u, top + 0.92f * u)
        eye(e, 0.5f * u, Color(0xFF4AA3DF))
        for (k in -1..1) {
            drawLine(Line, e + Offset(side * 0.15f * u + k * 0.14f * u, -0.5f * u), e + Offset(side * 0.15f * u + k * 0.24f * u, -0.78f * u), 0.07f * u, StrokeCap.Round)
        }
    }
    drawOval(Color(0xFFF2C400), Offset(cx - 0.1f * u, top + 1.35f * u), Size(0.22f * u, 0.3f * u))
    smile(Offset(cx, top + 1.82f * u), 1.4f * u, 0.7f * u, 0.07f * u)
    drawRect(Color.White, Offset(cx - 0.28f * u, top + 2.03f * u - 0.03f * u), Size(0.26f * u, 0.28f * u))
    drawRect(Color.White, Offset(cx + 0.02f * u, top + 2.03f * u - 0.03f * u), Size(0.26f * u, 0.28f * u))
    drawRect(Line.copy(alpha = 0.5f), Offset(cx - 0.28f * u, top + 2.03f * u - 0.03f * u), Size(0.56f * u, 0.28f * u), style = Stroke(0.03f * u))
    for (side in listOf(-1f, 1f)) drawCircle(Color(0xFFFF9A7A).copy(alpha = 0.55f), 0.2f * u, Offset(cx + side * 0.98f * u, top + 1.55f * u))
}

private fun DrawScope.patrick(cx: Float, base: Float, u: Float) {
    val R = 1.95f * u
    val r = 1.08f * u
    val cy = base - 1.8f * u
    val star = Path().apply {
        for (i in 0 until 10) {
            val ang = -PI / 2 + i * PI / 5
            val rad = if (i % 2 == 0) R else r
            val p = Offset(cx + (cos(ang) * rad).toFloat(), cy + (sin(ang) * rad).toFloat())
            if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
        }
        close()
    }
    val pink = Color(0xFFF7A1A4)
    drawPath(star, pink)
    drawPath(star, pink, style = Stroke(0.55f * u, join = StrokeJoin.Round))
    // Shorts with flowers.
    val shorts = Path().apply {
        moveTo(cx - 0.95f * u, cy + 0.2f * u); lineTo(cx + 0.95f * u, cy + 0.2f * u)
        lineTo(cx + 1.1f * u, cy + 1.4f * u); lineTo(cx + 0.1f * u, cy + 1.25f * u)
        lineTo(cx - 0.1f * u, cy + 1.25f * u); lineTo(cx - 1.1f * u, cy + 1.4f * u); close()
    }
    drawPath(shorts, Color(0xFF3FA74A))
    for ((fx, fy) in listOf(-0.55f to 0.5f, 0.5f to 0.55f, 0.0f to 0.95f, -0.8f to 1.1f, 0.8f to 1.12f)) {
        drawCircle(Color(0xFFB45FD6), 0.17f * u, Offset(cx + fx * u, cy + fy * u))
        drawCircle(Color(0xFFFFE680), 0.06f * u, Offset(cx + fx * u, cy + fy * u))
    }
    // Face, high on the star.
    for (side in listOf(-1f, 1f)) {
        val e = Offset(cx + side * 0.3f * u, cy - 0.7f * u)
        drawCircle(Color.White, 0.27f * u, e)
        drawCircle(Line, 0.11f * u, e + Offset(-side * 0.05f * u, 0.02f * u))
        drawCircle(Color.White, 0.04f * u, e + Offset(-side * 0.07f * u, -0.03f * u))
    }
    drawLine(Color(0xFFBF6F73), Offset(cx - 0.62f * u, cy - 1.08f * u), Offset(cx - 0.12f * u, cy - 1.0f * u), 0.07f * u, StrokeCap.Round)
    drawLine(Color(0xFFBF6F73), Offset(cx + 0.62f * u, cy - 1.08f * u), Offset(cx + 0.12f * u, cy - 1.0f * u), 0.07f * u, StrokeCap.Round)
    drawArc(Color(0xFFC0555C), 5f, 170f, true, Offset(cx - 0.4f * u, cy - 0.42f * u), Size(0.8f * u, 0.55f * u))
    drawArc(Color(0xFFFF8F98), 25f, 130f, true, Offset(cx - 0.2f * u, cy - 0.22f * u), Size(0.4f * u, 0.28f * u))
    for (side in listOf(-1f, 1f)) drawCircle(Color(0xFFFF7F8A).copy(alpha = 0.6f), 0.15f * u, Offset(cx + side * 0.62f * u, cy - 0.35f * u))
}

private fun DrawScope.krabs(cx: Float, base: Float, u: Float) {
    val red = Color(0xFFE5483B)
    val dark = Color(0xFFB92F27)
    for (side in listOf(-1f, 1f)) for (k in 0..2) {
        val x = cx + side * (0.6f + k * 0.45f) * u
        drawLine(dark, Offset(x, base - 0.8f * u), Offset(x + side * 0.15f * u, base - 0.05f * u), 0.2f * u, StrokeCap.Round)
    }
    // Claws on raised arms.
    for (side in listOf(-1f, 1f)) {
        val shoulder = Offset(cx + side * 1.5f * u, base - 1.9f * u)
        val claw = Offset(cx + side * 2.35f * u, base - 3.0f * u)
        drawLine(red, shoulder, claw, 0.4f * u, StrokeCap.Round)
        drawCircle(red, 0.9f * u, claw)
        val notch = Path().apply {
            moveTo(claw.x, claw.y); lineTo(claw.x + side * 0.9f * u, claw.y - 0.6f * u); lineTo(claw.x + side * 0.9f * u, claw.y - 0.05f * u); close()
        }
        drawPath(notch, Color(0xFF2E9FD0))
        drawCircle(dark, 0.9f * u, claw, style = Stroke(0.06f * u))
    }
    drawOval(red, Offset(cx - 1.75f * u, base - 2.85f * u), Size(3.5f * u, 2.3f * u))
    drawOval(Color(0xFFFF8A7A).copy(alpha = 0.45f), Offset(cx - 1.3f * u, base - 2.65f * u), Size(1.4f * u, 0.7f * u))
    // Eye stalks.
    for (side in listOf(-1f, 1f)) {
        val base0 = Offset(cx + side * 0.6f * u, base - 2.75f * u)
        val e = Offset(cx + side * 0.75f * u, base - 3.55f * u)
        drawLine(red, base0, e + Offset(0f, 0.25f * u), 0.28f * u, StrokeCap.Round)
        eye(e, 0.42f * u, Color(0xFF6A4A2A), look = -side)
    }
    drawLine(Line, Offset(cx - 1.1f * u, base - 4.05f * u), Offset(cx - 0.35f * u, base - 3.9f * u), 0.07f * u, StrokeCap.Round)
    drawLine(Line, Offset(cx + 1.1f * u, base - 4.05f * u), Offset(cx + 0.35f * u, base - 3.9f * u), 0.07f * u, StrokeCap.Round)
    drawArc(Color(0xFF7A1F18), 5f, 170f, true, Offset(cx - 0.8f * u, base - 2.55f * u), Size(1.6f * u, 0.95f * u))
    drawRect(Color.White, Offset(cx - 0.18f * u, base - 2.52f * u), Size(0.36f * u, 0.3f * u))
}

private fun DrawScope.plankton(cx: Float, base: Float, u: Float) {
    val green = Color(0xFF3F9F58)
    for (side in listOf(-1f, 1f)) {
        drawLine(green, Offset(cx + side * 0.25f * u, base - 0.7f * u), Offset(cx + side * 0.3f * u, base - 0.1f * u), 0.12f * u, StrokeCap.Round)
        drawLine(green, Offset(cx + side * 0.45f * u, base - 1.2f * u), Offset(cx + side * 0.8f * u, base - 0.8f * u), 0.12f * u, StrokeCap.Round)
        val tip = Offset(cx + side * 0.35f * u, base - 2.45f * u)
        drawLine(green, Offset(cx + side * 0.1f * u, base - 1.9f * u), tip, 0.08f * u, StrokeCap.Round)
        drawCircle(green, 0.1f * u, tip)
    }
    drawRoundRect(green, Offset(cx - 0.5f * u, base - 1.9f * u), Size(1.0f * u, 1.3f * u), CornerRadius(0.5f * u))
    drawCircle(Color(0xFF2E7E43), 0.5f * u, Offset(cx, base - 1.35f * u), style = Stroke(0.04f * u))
    val e = Offset(cx, base - 1.55f * u)
    drawCircle(Color.White, 0.36f * u, e)
    drawCircle(Color(0xFFE8742A), 0.19f * u, e)
    drawCircle(Line, 0.09f * u, e)
    drawLine(Line, e + Offset(-0.38f * u, -0.38f * u), e + Offset(0.3f * u, -0.18f * u), 0.07f * u, StrokeCap.Round)
    drawArc(Line, 200f, 130f, false, Offset(cx - 0.22f * u, base - 1.2f * u), Size(0.44f * u, 0.22f * u), style = Stroke(0.05f * u, cap = StrokeCap.Round))
}

/**
 * 路人鱼, in the spirit of the plush "residents of Bikini Bottom" look: a chubby egg body,
 * huge bulging eyes under heavy lids, thick pouty lips and stubby limbs, all with a deadpan
 * expression. [variant] picks the mood: 0 poker face, 1 tired office fish, 2 dazed,
 * 3 blank stare, 4 sardine.
 */
private fun DrawScope.fishFolk(
    cx: Float,
    base: Float,
    u: Float,
    body: Color,
    belly: Color,
    variant: Int,
) {
    val dark = Color(body.red * 0.72f, body.green * 0.72f, body.blue * 0.72f)
    val lid = Color(body.red * 0.88f, body.green * 0.88f, body.blue * 0.88f)
    val top = base - 3.7f * u
    // Feet and stubby arms sit behind the body.
    for (side in listOf(-1f, 1f)) {
        drawOval(dark, Offset(cx + side * 0.7f * u - 0.45f * u, base - 0.3f * u), Size(0.9f * u, 0.36f * u))
        drawLine(body, Offset(cx + side * 1.15f * u, base - 2.1f * u), Offset(cx + side * 1.6f * u, base - 1.1f * u), 0.42f * u, StrokeCap.Round)
        drawCircle(dark, 0.26f * u, Offset(cx + side * 1.6f * u, base - 1.05f * u))
    }
    // Dorsal fin and side fins.
    val dorsal = Path().apply {
        moveTo(cx - 0.7f * u, top + 0.5f * u)
        quadraticTo(cx - 0.1f * u, top - 0.75f * u, cx + 0.9f * u, top + 0.4f * u); close()
    }
    drawPath(dorsal, dark)
    for (side in listOf(-1f, 1f)) {
        val fin = Path().apply {
            moveTo(cx + side * 1.15f * u, base - 3.0f * u)
            lineTo(cx + side * 1.85f * u, base - 3.35f * u)
            lineTo(cx + side * 1.75f * u, base - 2.5f * u); close()
        }
        drawPath(fin, dark)
    }
    drawOval(body, Offset(cx - 1.3f * u, top), Size(2.6f * u, 3.4f * u))
    drawOval(belly, Offset(cx - 0.9f * u, base - 2.1f * u), Size(1.8f * u, 1.75f * u))
    if (variant == 4) for (k in 0..2) drawLine(dark.copy(alpha = 0.55f), Offset(cx - 1.0f * u + k * 0.1f * u, top + 0.8f * u + k * 0.45f * u), Offset(cx + 1.0f * u - k * 0.1f * u, top + 0.8f * u + k * 0.45f * u), 0.07f * u)
    if (variant == 1) {
        drawRect(Color.White, Offset(cx - 0.4f * u, base - 2.05f * u), Size(0.8f * u, 0.22f * u))
        val tie = Path().apply {
            moveTo(cx - 0.12f * u, base - 2.0f * u); lineTo(cx + 0.12f * u, base - 2.0f * u)
            lineTo(cx + 0.2f * u, base - 1.1f * u); lineTo(cx, base - 0.95f * u); lineTo(cx - 0.2f * u, base - 1.1f * u); close()
        }
        drawPath(tie, Color(0xFFD9473F))
    }
    // Huge bulging eyes.
    val eyeY = base - 2.8f * u
    val er = 0.66f * u
    for (side in listOf(-1f, 1f)) {
        val e = Offset(cx + side * 0.62f * u, eyeY)
        drawCircle(Color.White, er, e)
        drawCircle(Line.copy(alpha = 0.6f), er, e, style = Stroke(0.06f * u))
        val pupil = when (variant) {
            2 -> e + Offset(side * 0.22f * u, -0.22f * u)
            3 -> e
            else -> e + Offset(0f, 0.16f * u)
        }
        drawCircle(Line, if (variant == 3) 0.07f * u else 0.17f * u, pupil)
        if (variant != 3) drawCircle(Color.White, 0.05f * u, pupil + Offset(-0.05f * u, -0.05f * u))
        val lidFrac = when (variant) { 0 -> 0.48f; 1 -> 0.42f; 4 -> 0.4f; else -> 0f }
        if (lidFrac > 0f) {
            val lidPath = Path().apply {
                addArc(androidx.compose.ui.geometry.Rect(e, er), 180f, 180f)
                close()
            }
            clipPath(Path().apply { addOval(androidx.compose.ui.geometry.Rect(e, er)) }) {
                drawRect(lid, Offset(e.x - er, e.y - er), Size(er * 2f, er * 2f * lidFrac))
                drawLine(Line.copy(alpha = 0.7f), Offset(e.x - er, e.y - er + er * 2f * lidFrac), Offset(e.x + er, e.y - er + er * 2f * lidFrac), 0.07f * u)
            }
        }
        if (variant == 1) drawArc(dark.copy(alpha = 0.55f), 20f, 140f, false, e + Offset(-0.4f * u, 0.45f * u), Size(0.8f * u, 0.3f * u), style = Stroke(0.06f * u, cap = StrokeCap.Round))
    }
    // Thick pouty lips.
    val lipY = base - 1.9f * u
    val lipColor = Color(0xFFE86A7A)
    if (variant == 2) {
        drawOval(Color(0xFF8A2F3F), Offset(cx - 0.45f * u, lipY - 0.1f * u), Size(0.9f * u, 0.7f * u))
    }
    drawOval(lipColor, Offset(cx - 0.75f * u, lipY - 0.12f * u), Size(1.5f * u, 0.5f * u))
    drawOval(lipColor, Offset(cx - 0.68f * u, lipY + 0.2f * u), Size(1.36f * u, 0.46f * u))
    drawOval(Line.copy(alpha = 0.55f), Offset(cx - 0.75f * u, lipY - 0.12f * u), Size(1.5f * u, 0.5f * u), style = Stroke(0.04f * u))
    drawLine(Color(0xFFA33A4B), Offset(cx - 0.62f * u, lipY + 0.24f * u), Offset(cx + 0.62f * u, lipY + 0.24f * u), 0.05f * u, StrokeCap.Round)
    drawOval(Color.White.copy(alpha = 0.55f), Offset(cx - 0.32f * u, lipY - 0.04f * u), Size(0.34f * u, 0.12f * u))
}

private fun DrawScope.krabbyPatty(c: Offset, u: Float) {
    drawArc(Color(0xFFE0A24E), 180f, 180f, true, c - Offset(1.1f * u, 1.0f * u), Size(2.2f * u, 1.9f * u))
    for (s in listOf(-0.5f to -0.55f, 0.1f to -0.75f, 0.55f to -0.4f)) drawOval(Color(0xFFFFEBC2), Offset(c.x + s.first * u - 0.08f * u, c.y + s.second * u), Size(0.16f * u, 0.08f * u))
    drawRoundRect(Color(0xFF59B04B), Offset(c.x - 1.2f * u, c.y + 0.02f * u), Size(2.4f * u, 0.22f * u), CornerRadius(0.11f * u))
    drawRoundRect(Color(0xFFE5483B), Offset(c.x - 1.1f * u, c.y + 0.22f * u), Size(2.2f * u, 0.16f * u), CornerRadius(0.08f * u))
    drawRoundRect(Color(0xFF7B4A2A), Offset(c.x - 1.15f * u, c.y + 0.36f * u), Size(2.3f * u, 0.38f * u), CornerRadius(0.18f * u))
    drawRoundRect(Color(0xFFE0A24E), Offset(c.x - 1.1f * u, c.y + 0.72f * u), Size(2.2f * u, 0.32f * u), CornerRadius(0.16f * u))
}

private fun DrawScope.coin(c: Offset, r: Float) {
    drawCircle(Color(0xFFE9AE1F), r, c)
    drawCircle(Color(0xFFFFE27A), r * 0.78f, c)
    drawArc(Color(0xFFB87D10), 40f, 250f, false, c - Offset(r * 0.32f, r * 0.34f), Size(r * 0.64f, r * 0.68f), style = Stroke(r * 0.13f, cap = StrokeCap.Round))
    drawLine(Color(0xFFB87D10), c + Offset(0f, -r * 0.55f), c + Offset(0f, r * 0.55f), r * 0.1f)
}

private fun DrawScope.charUnit(w: Float, h: Float, wide: Boolean, wideFactor: Float, tallFactor: Float) =
    if (wide) minOf(h * wideFactor, w * 0.04f) else w * tallFactor

/** 海绵宝宝 and 派大星 waving beside each other in a sunny blue sea. */
@Composable
internal fun SpongePatScene(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val wide = w > h
        sea(Color(0xFF8FE0F5), Color(0xFF2E9FD0), Color(0xFFF2DC9E))
        jellyfish(Offset(w * 0.2f, h * 0.17f), h * 0.07f, Color(0xFFFF9EC9))
        jellyfish(Offset(w * 0.86f, h * 0.2f), h * 0.055f, Color(0xFFC9A2FF))
        for ((bx, by, br) in listOf(Triple(0.08f, 0.5f, 0.018f), Triple(0.5f, 0.12f, 0.014f), Triple(0.62f, 0.3f, 0.02f), Triple(0.92f, 0.55f, 0.016f), Triple(0.34f, 0.28f, 0.012f), Triple(0.74f, 0.1f, 0.012f))) {
            bubble(Offset(w * bx, h * by), h * br)
        }
        val u = charUnit(w, h, wide, 0.05f, 0.075f)
        val base = h * 0.955f
        seaweed(w * 0.03f, base, h * 0.22f, Color(0xFF3BAE6B))
        seaweed(w * 0.07f, base, h * 0.16f, Color(0xFF52C27E))
        seaweed(w * 0.96f, base, h * 0.2f, Color(0xFF3BAE6B))
        val cx = if (wide) w * 0.82f else w * 0.5f
        spongebob(cx - 2.0f * u, base, u)
        patrick(cx + 2.4f * u, base, u)
    }
}

/** 蟹老板 counting his coins while 皮老板 schemes beside a Krabby Patty. */
@Composable
internal fun KrabsScene(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val wide = w > h
        sea(Color(0xFF3FB0D8), Color(0xFF0E5F8F), Color(0xFFE8C98A))
        for ((bx, by, br) in listOf(Triple(0.1f, 0.4f, 0.02f), Triple(0.45f, 0.15f, 0.015f), Triple(0.7f, 0.27f, 0.018f), Triple(0.9f, 0.45f, 0.012f), Triple(0.28f, 0.22f, 0.012f))) {
            bubble(Offset(w * bx, h * by), h * br)
        }
        val u = charUnit(w, h, wide, 0.05f, 0.075f)
        val base = h * 0.955f
        val cx = if (wide) w * 0.8f else w * 0.5f
        // Coin piles in front of the pair.
        for ((dx, dy) in listOf(-4.9f to 0.0f, -4.35f to 0.1f, -4.65f to -0.45f, -3.95f to -0.1f)) {
            coin(Offset(cx + dx * u, base - 0.35f * u + dy * u), 0.42f * u)
        }
        plankton(cx - 3.0f * u, base, u * 0.9f)
        krabs(cx + 1.2f * u, base, u * 0.95f)
        krabbyPatty(Offset(w * (if (wide) 0.1f else 0.2f), base - 0.8f * u), u)
    }
}

/** A row of five plush-style 路人鱼 along the sea floor. */
@Composable
internal fun BikiniScene(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val wide = w > h
        sea(Color(0xFFA6EBF0), Color(0xFF3CB0C8), Color(0xFFF4E0A8))
        jellyfish(Offset(w * 0.12f, h * 0.16f), h * 0.06f, Color(0xFFFFB3D9))
        jellyfish(Offset(w * 0.9f, h * 0.18f), h * 0.07f, Color(0xFFB9C4FF))
        for ((bx, by, br) in listOf(Triple(0.3f, 0.2f, 0.015f), Triple(0.6f, 0.12f, 0.02f), Triple(0.05f, 0.5f, 0.012f), Triple(0.95f, 0.5f, 0.016f), Triple(0.78f, 0.3f, 0.012f))) {
            bubble(Offset(w * bx, h * by), h * br)
        }
        val u = if (wide) minOf(h * 0.042f, w * 0.026f) else w * 0.052f
        val base = h * 0.955f
        val step = if (wide) w * 0.145f else w * 0.19f
        val left = w * 0.5f - step * 2f
        seaweed(w * 0.02f, base, h * 0.2f, Color(0xFF3BAE6B))
        seaweed(w * 0.98f, base, h * 0.17f, Color(0xFF52C27E))
        fishFolk(left, base, u, Color(0xFFF0933F), Color(0xFFFFD8A6), 0)
        fishFolk(left + step, base, u, Color(0xFF6FB6D9), Color(0xFFCDE8F5), 1)
        fishFolk(left + step * 2f, base, u, Color(0xFF9DCB5A), Color(0xFFE0F0B8), 2)
        fishFolk(left + step * 3f, base, u, Color(0xFFE88AAE), Color(0xFFFFD3E2), 3)
        fishFolk(left + step * 4f, base, u, Color(0xFFB6C2CC), Color(0xFFEAF0F4), 4)
    }
}
