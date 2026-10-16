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
internal val GlassFill = Color.White.copy(alpha = 0.07f)
internal val GlassFillHigh = Color.White.copy(alpha = 0.12f)
internal val GlassBorder = Color.White.copy(alpha = 0.16f)

/** A frosted panel: translucent fill, top sheen, hairline border. */
internal fun Modifier.glassSurface(
    corner: Dp = 20.dp,
    fill: Color = GlassFill,
    border: Color = GlassBorder,
): Modifier {
    val shape = RoundedCornerShape(corner)
    return this
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

/**
 * Glassy press feedback: the surface scales down a touch and its edge brightens.
 * API 31+ also blurs [backdrop] behind the surface, which is drawn shifted so its
 * pixels line up with the screen behind.
 */
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
            indication = androidx.compose.foundation.LocalIndication.current,
            enabled = enabled,
            role = role,
            onClick = onClick,
        )
}

/** Glass button fill: brighter than a panel, still translucent. */
internal val GlassButtonFill = Color.White.copy(alpha = 0.12f)
internal val GlassButtonBorder = Color.White.copy(alpha = 0.22f)

internal fun Modifier.glassButtonSurface(): Modifier =
    glassSurface(corner = 16.dp, fill = GlassButtonFill, border = GlassButtonBorder)
