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
internal val GlassFill = Color.White.copy(alpha = 0.55f)
internal val GlassFillHigh = Color.White.copy(alpha = 0.72f)
internal val GlassBorder = Color.White.copy(alpha = 0.85f)
// The same glass read against a dark clock face.
internal val GlassDarkFill = Color.White.copy(alpha = 0.10f)
internal val GlassDarkBorder = Color.White.copy(alpha = 0.18f)

/** A frosted panel: translucent fill, top sheen, hairline border. */
internal fun Modifier.glassSurface(
    corner: Dp = 20.dp,
    fill: Color = GlassFill,
    border: Color = GlassBorder,
): Modifier {
    val shape = RoundedCornerShape(corner)
    return this
        .shadow(elevation = 6.dp, shape = shape, clip = false, ambientColor = Color(0x14000000), spotColor = Color(0x14000000))
        .clip(shape)
        .background(fill)
        .drawBehind {
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.10f),
                    0.55f to Color.Transparent,
                    startY = 0f,
                    endY = size.height * 0.55f,
                ),
            )
        }
        .border(1.dp, border, shape)
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
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
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
internal val GlassButtonFill = Color.White.copy(alpha = 0.65f)
internal val GlassButtonBorder = Color.White.copy(alpha = 0.85f)

internal fun Modifier.glassButtonSurface(): Modifier =
    glassSurface(corner = 16.dp, fill = GlassButtonFill, border = GlassButtonBorder)
