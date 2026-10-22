package app.screenclock.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The glass look: translucent fills, a hairline light border, a soft top sheen,
 * and press feedback that brightens instead of rippling.
 */
internal val GlassFill = Color.White.copy(alpha = 0.62f)
internal val GlassFillHigh = Color.White.copy(alpha = 0.78f)
internal val GlassBorder = Color.White.copy(alpha = 0.90f)
// The same glass read against a dark clock face.
internal val GlassDarkFill = Color.White.copy(alpha = 0.10f)
internal val GlassDarkBorder = Color.White.copy(alpha = 0.18f)

/** A frosted panel: translucent fill, top sheen, a rim light that fades downward. */
internal fun Modifier.glassSurface(
    corner: Dp = 20.dp,
    fill: Color = GlassFill,
    border: Color = GlassBorder,
): Modifier {
    val shape = RoundedCornerShape(corner)
    val dark = fill == GlassDarkFill
    val ambient = if (dark) Color(0x14000000) else Color(0x1F3A5A8C)
    val spot = if (dark) Color(0x14000000) else Color(0x163A5A8C)
    // Light glass catches light along its top edge and lets go of it toward the bottom.
    val rim = if (dark) {
        Brush.verticalGradient(listOf(border, border))
    } else {
        Brush.verticalGradient(listOf(border, border.copy(alpha = border.alpha * 0.40f)))
    }
    val sheenTop = if (dark) 0.10f else 0.34f
    return this
        .shadow(elevation = 8.dp, shape = shape, clip = false, ambientColor = ambient, spotColor = spot)
        .clip(shape)
        .background(fill)
        .drawBehind {
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.White.copy(alpha = sheenTop),
                    0.55f to Color.Transparent,
                    startY = 0f,
                    endY = size.height * 0.55f,
                ),
            )
        }
        .border(1.dp, rim, shape)
}

/**
 * The page behind light glass. Flat pale colour gives frosted panels nothing to frost, so
 * a few soft colour pools sit behind the content; the warm one echoes the clock face's amber.
 */
internal fun Modifier.glassBackdrop(): Modifier = drawBehind {
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color(0xFFF5F8FD),
            0.5f to Color(0xFFF8FAFE),
            1f to Color(0xFFF2F7FD),
        ),
    )
    fun pool(color: Color, cx: Float, cy: Float, radius: Float) {
        val center = Offset(size.width * cx, size.height * cy)
        val r = size.width * radius
        drawCircle(
            brush = Brush.radialGradient(
                0f to color,
                1f to Color.Transparent,
                center = center,
                radius = r,
            ),
            radius = r,
            center = center,
        )
    }
    pool(Color(0xFFA9C8F0).copy(alpha = 0.20f), 0.10f, 0.06f, 0.55f)
    pool(Color(0xFFC4D8F5).copy(alpha = 0.24f), 0.95f, 0.38f, 0.50f)
    pool(Color(0xFFD3E2F8).copy(alpha = 0.26f), 0.12f, 0.78f, 0.48f)
    pool(Color(0xFFBBD3F3).copy(alpha = 0.18f), 0.92f, 1.00f, 0.52f)
}

/** Frosted press feedback scales and highlights the pressed surface, without a ripple. */
@Composable
internal fun Modifier.glassPressable(
    enabled: Boolean = true,
    role: Role = Role.Button,
    onClick: () -> Unit,
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
    )
    return this
        .graphicsLayer {
            val scale = 1f - 0.03f * press
            scaleX = scale
            scaleY = scale
        }
        .drawWithContent {
            drawContent()
            if (press > 0f) {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.06f * press),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                        this.size.minDimension * 0.08f,
                    ),
                )
            }
        }
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            role = role,
            onClick = onClick,
        )
}

/** Glass button fill: brighter than a panel, still translucent. */
internal val GlassButtonFill = Color.White.copy(alpha = 0.70f)
internal val GlassButtonBorder = Color.White.copy(alpha = 0.90f)

internal fun Modifier.glassButtonSurface(): Modifier =
    glassSurface(corner = 16.dp, fill = GlassButtonFill, border = GlassButtonBorder)
