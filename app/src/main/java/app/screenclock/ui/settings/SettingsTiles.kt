package app.screenclock.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.screenclock.ui.UiPalette
import app.screenclock.ui.glassPressable

/** Tile corner radii: the first and last of a group are round, the joins are tight. */
internal val TileLargeRadius = 24.dp
internal val TileSmallRadius = 5.dp

/**
 * One row of a grouped list. [first] and [last] decide which corners are large, so a run of
 * tiles reads as one card while each row stays its own touch target.
 */
@Composable
internal fun Tile(
    palette: UiPalette,
    first: Boolean,
    last: Boolean,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    minHeight: androidx.compose.ui.unit.Dp = 60.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val radius = RoundedCornerShape(
        topStart = if (first) TileLargeRadius else TileSmallRadius,
        topEnd = if (first) TileLargeRadius else TileSmallRadius,
        bottomStart = if (last) TileLargeRadius else TileSmallRadius,
        bottomEnd = if (last) TileLargeRadius else TileSmallRadius,
    )
    val fill = if (selected) palette.tileSelected else palette.tile
    val base = Modifier
        .fillMaxWidth()
        .heightIn(min = minHeight)
        .clip(radius)
        .background(fill)
        .then(if (palette.dark) Modifier.border(1.dp, palette.tileBorder, radius) else Modifier)
        .then(
            if (onClick != null) {
                Modifier.glassPressable(role = Role.Button, onClick = onClick)
            } else {
                Modifier
            },
        )
    Column(
        modifier = modifier.then(base).padding(horizontal = 16.dp, vertical = 12.dp),
        content = content,
    )
}

/** A tile that only holds a control, without its own label. */
@Composable
internal fun ControlTile(
    palette: UiPalette,
    first: Boolean,
    last: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    Tile(palette = palette, first = first, last = last, minHeight = 0.dp, content = content)
}

/** A tile with an optional note under the title, and a switch at the end. */
@Composable
internal fun SwitchTile(
    palette: UiPalette,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    first: Boolean,
    last: Boolean,
    note: String? = null,
    enabled: Boolean = true,
) {
    val rowModifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 60.dp)
        .clip(
            RoundedCornerShape(
                topStart = if (first) TileLargeRadius else TileSmallRadius,
                topEnd = if (first) TileLargeRadius else TileSmallRadius,
                bottomStart = if (last) TileLargeRadius else TileSmallRadius,
                bottomEnd = if (last) TileLargeRadius else TileSmallRadius,
            ),
        )
        .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
    Row(
        modifier = rowModifier
            .background(palette.tile)
            .then(
                if (palette.dark) {
                    Modifier.border(
                        1.dp,
                        palette.tileBorder,
                        RoundedCornerShape(
                            topStart = if (first) TileLargeRadius else TileSmallRadius,
                            topEnd = if (first) TileLargeRadius else TileSmallRadius,
                            bottomStart = if (last) TileLargeRadius else TileSmallRadius,
                            bottomEnd = if (last) TileLargeRadius else TileSmallRadius,
                        ),
                    )
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).alpha(if (enabled) 1f else 0.55f)) {
            Text(text = label, color = palette.on, fontSize = 16.sp)
            if (note != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = note, color = palette.on3, fontSize = 13.sp)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            modifier = Modifier.clearAndSetSemantics { },
            colors = tileSwitchColors(palette),
        )
    }
}

@Composable
internal fun tileSwitchColors(palette: UiPalette) = SwitchDefaults.colors(
    checkedThumbColor = if (palette.dark) palette.onAccentSoft else palette.accent,
    checkedTrackColor = palette.accent.copy(alpha = if (palette.dark) 0.55f else 0.30f),
    checkedBorderColor = palette.accent,
    uncheckedThumbColor = palette.on3,
    uncheckedTrackColor = palette.field,
    uncheckedBorderColor = palette.outline,
)

/** A tile holding a label and a radio row, as the design's segmented control. */
@Composable
internal fun SegmentedTile(
    palette: UiPalette,
    label: String,
    options: List<Pair<String, Boolean>>,
    onSelect: (Int) -> Unit,
    first: Boolean,
    last: Boolean,
) {
    Tile(palette = palette, first = first, last = last, minHeight = 0.dp) {
        Text(text = label, color = palette.on, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(10.dp))
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
        ) {
            options.forEachIndexed { index, (text, selected) ->
                val shape = RoundedCornerShape(20.dp)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .clip(shape)
                        .background(if (selected) palette.accentSoft else Color.Transparent)
                        .then(
                            if (selected) Modifier.border(1.dp, palette.accent, shape) else Modifier.border(1.dp, palette.outline.copy(alpha = 0.4f), shape),
                        )
                        .selectable(selected = selected, role = Role.RadioButton) { onSelect(index) }
                        .semantics { this.selected = selected },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = text,
                        color = if (selected) palette.on else palette.on2,
                        fontSize = 13.5.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** A tile that opens another page: icon, title, short value, chevron. */
@Composable
internal fun EntryTile(
    palette: UiPalette,
    icon: @Composable () -> Unit,
    title: String,
    summary: String,
    onClick: () -> Unit,
    first: Boolean,
    last: Boolean,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    Tile(
        palette = palette,
        first = first,
        last = last,
        modifier = modifier,
        selected = selected,
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon()
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (selected) palette.onTileSelected else palette.on,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = summary,
                    color = if (selected) palette.onTileSelected.copy(alpha = 0.8f) else palette.on3,
                    fontSize = 13.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            appIconChevron(palette, selected)
        }
    }
}

@Composable
private fun appIconChevron(palette: UiPalette, selected: Boolean) {
    app.screenclock.ui.AppIcon(
        kind = app.screenclock.ui.AppIconKind.ChevronRight,
        tint = if (selected) palette.onTileSelected.copy(alpha = 0.75f) else palette.on3,
        size = 16.dp,
    )
}

/** The coloured round chip that opens each home entry. */
@Composable
internal fun CategoryChip(color: Color, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(color.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** A longer explanation that has no control of its own. */
@Composable
internal fun NoteTile(
    palette: UiPalette,
    title: String,
    note: String,
    first: Boolean,
    last: Boolean,
) {
    Tile(palette = palette, first = first, last = last, minHeight = 0.dp) {
        Text(text = title, color = palette.on, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = note, color = palette.on3, fontSize = 13.5.sp)
    }
}

/** The thin slider used by the timing dials: a plain track and a bar thumb. */
@Composable
internal fun tileSliderColors(palette: UiPalette) = SliderDefaults.colors(
    thumbColor = palette.accent,
    activeTrackColor = palette.accent,
    inactiveTrackColor = palette.accentSoft,
    activeTickColor = Color.Transparent,
    inactiveTickColor = Color.Transparent,
)

/** A slider with the ends labelled, as under the burn-in timing dial. */
@Composable
internal fun SliderBlock(
    palette: UiPalette,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onChange: (Float) -> Unit,
    onFinished: (Float) -> Unit,
    startLabel: String,
    middleLabel: String,
    endLabel: String,
) {
    Slider(
        value = value,
        onValueChange = onChange,
        onValueChangeFinished = { onFinished(value) },
        valueRange = range,
        steps = steps,
        colors = tileSliderColors(palette),
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = startLabel, color = palette.on3, fontSize = 11.5.sp)
        Text(text = middleLabel, color = palette.on3, fontSize = 11.5.sp)
        Text(text = endLabel, color = palette.on3, fontSize = 11.5.sp)
    }
}

/** The large − value + dial used by the burn-in timing settings. */
@Composable
internal fun DialRow(
    palette: UiPalette,
    value: @Composable () -> Unit,
    decreaseLabel: String,
    increaseLabel: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    canDecrease: Boolean,
    canIncrease: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundStepButton(palette, "-", decreaseLabel, canDecrease, onDecrease)
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { value() }
        RoundStepButton(palette, "+", increaseLabel, canIncrease, onIncrease)
    }
}

@Composable
private fun RoundStepButton(
    palette: UiPalette,
    sign: String,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(palette.accentSoft)
            .alpha(if (enabled) 1f else 0.35f)
            .semantics { contentDescription = label }
            .then(
                if (enabled) {
                    Modifier.glassPressable(onClick = onClick)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = sign,
            color = palette.onAccentSoft,
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** The quick-pick chips under a timing dial. */
@Composable
internal fun PresetRow(
    palette: UiPalette,
    labels: List<String>,
    selectedIndex: Int,
    onPick: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        labels.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val shape = RoundedCornerShape(10.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 34.dp)
                    .clip(shape)
                    .background(if (selected) palette.accentSoft else Color.Transparent)
                    .border(1.dp, if (selected) Color.Transparent else palette.outline.copy(alpha = 0.5f), shape)
                    .selectable(selected = selected, role = Role.RadioButton) { onPick(index) }
                    .semantics { this.selected = selected },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = if (selected) palette.onAccentSoft else palette.on2,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** A switch tile that can wear the accent fill for a moment, to answer "去开启". */
@Composable
internal fun HighlightedSwitchTile(
    palette: UiPalette,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    lit: Boolean,
) {
    val glow by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (lit) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 620),
        label = "highlight",
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(
                    topStart = TileSmallRadius,
                    topEnd = TileSmallRadius,
                    bottomStart = TileLargeRadius,
                    bottomEnd = TileLargeRadius,
                ),
            )
            .background(palette.tile)
            .padding(1.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(TileSmallRadius))
                .background(palette.accentSoft.copy(alpha = 0.55f * glow)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = label, color = palette.on, fontSize = 16.sp, modifier = Modifier.weight(1f))
                Switch(checked = checked, onCheckedChange = onCheckedChange, colors = tileSwitchColors(palette))
            }
        }
    }
}
