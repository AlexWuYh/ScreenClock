package app.screenclock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.selected
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val GlassText = Cream
internal val UiAccentGlassFill = UiAccent.copy(alpha = 0.18f)
internal val UiAccentGlassBorder = UiAccent.copy(alpha = 0.52f)

/** One quiet glass card per settings section. */
@Composable
internal fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(corner = 20.dp)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        content = content,
    )
}

@Composable
internal fun SectionHeader(icon: AppIconKind, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 10.dp),
    ) {
        AppIcon(kind = icon, tint = UiAccentDeep, size = 17.dp)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            color = GlassText,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.4.sp,
            modifier = Modifier.semantics { heading() },
        )
    }
}

@Composable
internal fun PageTitle(text: String) {
    Text(
        text = text,
        color = GlassText,
        fontFamily = FontFamily.Serif,
        fontSize = 34.sp,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.semantics { heading() },
    )
}

/** Circular glass icon button used for back, steppers and row actions. */
@Composable
internal fun IconBox(
    icon: AppIconKind,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = GlassText,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .glassSurface(corner = 14.dp, fill = GlassFillHigh)
            .semantics { this.contentDescription = contentDescription }
            .glassPressable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(kind = icon, tint = if (enabled) tint else Muted, size = 20.dp)
    }
}

@Composable
internal fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    note: String? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .toggleableRow(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, color = if (enabled) GlassText else Muted, fontSize = 16.sp)
            if (note != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = note, color = Muted, fontSize = 13.sp)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            modifier = Modifier.clearAndSetSemantics { },
            colors = appSwitchColors(),
        )
    }
}

private fun Modifier.toggleableRow(value: Boolean, enabled: Boolean, role: Role, onValueChange: (Boolean) -> Unit) =
    toggleable(value = value, enabled = enabled, role = role, onValueChange = onValueChange)

@Composable
internal fun appSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = UiAccentDeep,
    checkedTrackColor = UiAccent.copy(alpha = 0.30f),
    checkedBorderColor = UiAccentDeep.copy(alpha = 0.70f),
    uncheckedThumbColor = Color(0xFF6B7F8B),
    uncheckedTrackColor = GlassFillHigh,
    uncheckedBorderColor = Color(0xFF8FA3AE),
)

@Composable
internal fun appSliderColors() = SliderDefaults.colors(
    thumbColor = UiAccentDeep,
    activeTrackColor = UiAccent,
    inactiveTrackColor = UiAccent.copy(alpha = 0.22f),
    activeTickColor = Color.Transparent,
    inactiveTickColor = Color.Transparent,
)

@Composable
internal fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: AppIconKind? = null,
) {
    val tint = if (enabled) UiAccentDeep else Muted
    Box(
        modifier = modifier
            .heightIn(min = 52.dp)
            .glassSurface(
                corner = 16.dp,
                fill = if (enabled) UiAccentGlassFill else GlassFill,
                border = if (enabled) UiAccentGlassBorder else GlassBorder,
            )
            .glassPressable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                AppIcon(kind = icon, tint = tint, size = 18.dp)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = if (enabled) Cream else Muted,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
internal fun TonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: AppIconKind? = null,
) {
    Box(
        modifier = modifier
            .heightIn(min = 52.dp)
            .glassButtonSurface()
            .glassPressable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                AppIcon(kind = icon, tint = GlassText, size = 18.dp)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text = text, color = GlassText, fontSize = 17.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/** − value + row with a tappable value in the middle, shared by the forms. */
@Composable
internal fun StepRow(
    value: String,
    decreaseLabel: String,
    increaseLabel: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onPick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBox(icon = AppIconKind.Minus, contentDescription = decreaseLabel, onClick = onDecrease)
        Spacer(modifier = Modifier.width(12.dp))
        val valueBox = if (onPick != null) {
            Modifier.glassPressable(onClick = onPick)
        } else {
            Modifier
        }
        Box(
            modifier = valueBox
                .weight(1f)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = value, color = GlassText, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(modifier = Modifier.width(12.dp))
        IconBox(icon = AppIconKind.Plus, contentDescription = increaseLabel, onClick = onIncrease)
    }
}

/** One muted field label above a control. */
@Composable
internal fun FieldLabel(text: String) {
    Text(
        text = text,
        color = Muted,
        fontSize = 13.sp,
        letterSpacing = 0.3.sp,
    )
}

internal data class SegmentOption(
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit,
)

/** A single-choice pill row on glass: selected blue stays separate from face amber. */
@Composable
internal fun SegmentedRow(options: List<SegmentOption>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(corner = 14.dp, fill = GlassFillHigh)
            .selectableGroup(),
    ) {
        options.forEach { option ->
            val shape = RoundedCornerShape(10.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(4.dp)
                    .heightIn(min = 44.dp)
                    .then(
                        if (option.selected) {
                            Modifier
                                .clip(shape)
                                .background(UiAccentGlassFill)
                                .border(1.dp, UiAccentGlassBorder, shape)
                        } else {
                            Modifier.clip(shape)
                        },
                    )
                    .glassPressable(role = Role.RadioButton, onClick = option.onClick)
                    .semantics { selected = option.selected },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option.label,
                    color = if (option.selected) GlassText else Info,
                    fontSize = 14.sp,
                    fontWeight = if (option.selected) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** A row that opens another page or a link. */
@Composable
internal fun NavRow(
    icon: AppIconKind,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: AppIconKind = AppIconKind.ChevronRight,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(12.dp))
            .glassPressable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(kind = icon, tint = UiAccentDeep, size = 18.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            color = GlassText,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f),
        )
        AppIcon(kind = trailing, tint = Muted, size = 18.dp)
    }
}

/** Keeps a management page readable on tablets: content is centred and at most [maxWidth] wide. */
@Composable
internal fun Modifier.centeredPage(maxWidth: androidx.compose.ui.unit.Dp = 640.dp): Modifier {
    val screen = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp
    val side = ((screen - maxWidth) / 2).coerceAtLeast(0.dp)
    return padding(horizontal = side)
}
