package app.screenclock.ui.theme.flip

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Shared flap row for focus and the ceremony clock. The home clock keeps its own two flaps. */
@Composable
internal fun FlipFlaps(
    groups: List<String>,
    animate: Boolean,
    modifier: Modifier = Modifier,
    keyOf: (index: Int) -> Any = { index -> index },
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        val count = groups.size.coerceAtLeast(1)
        val stacked = clockStacked(maxWidth.value, maxHeight.value)
        val layout = flapLayout(
            width = maxWidth.value.coerceAtLeast(1f),
            height = maxHeight.value.coerceAtLeast(1f),
            stacked = stacked,
            reserveSide = 0f,
            count = count,
        )
        val card = Modifier.size(layout.cardWidth.dp, layout.cardHeight.dp)
        if (stacked) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                groups.forEachIndexed { index, digits ->
                    if (index > 0) Spacer(modifier = Modifier.height(layout.gap.dp))
                    key(keyOf(index)) {
                        FlipCard(digits, animate, card)
                    }
                }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                groups.forEachIndexed { index, digits ->
                    if (index > 0) Spacer(modifier = Modifier.width(layout.gap.dp))
                    key(keyOf(index)) {
                        FlipCard(digits, animate, card)
                    }
                }
            }
        }
    }
}

/** Keeps the minute and second flaps when a focus countdown drops from three groups to two. */
internal fun focusFlapKey(count: Int, index: Int): String {
    val names = if (count >= 3) listOf("h", "m", "s") else listOf("m", "s")
    return names.getOrElse(index) { index.toString() }
}
