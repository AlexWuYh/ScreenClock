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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** One quiet card per settings section, so headers and rows stop floating on black. */
@Composable
internal fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Panel)
            .border(1.dp, Hairline, RoundedCornerShape(20.dp))
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
        AppIcon(kind = icon, tint = Amber, size = 17.dp)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            color = Cream,
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
        color = Cream,
        fontFamily = FontFamily.Serif,
        fontSize = 34.sp,
        modifier = Modifier.semantics { heading() },
    )
}

/** Circular icon button used for back, steppers and row actions. */
@Composable
internal fun IconBox(
    icon: AppIconKind,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Cream,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(PanelHigh)
            .clickable(role = Role.Button, enabled = enabled, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
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
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, color = if (enabled) Cream else Muted, fontSize = 16.sp)
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

@Composable
internal fun appSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = Ink,
    checkedTrackColor = Amber,
    checkedBorderColor = Amber,
    uncheckedThumbColor = Info,
    uncheckedTrackColor = PanelHigh,
    uncheckedBorderColor = Hairline,
)

@Composable
internal fun appSliderColors() = SliderDefaults.colors(
    thumbColor = Amber,
    activeTrackColor = Amber,
    inactiveTrackColor = Hairline,
)

@Composable
internal fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: AppIconKind? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Amber,
            contentColor = Ink,
            disabledContainerColor = PanelHigh,
            disabledContentColor = Muted,
        ),
    ) {
        if (icon != null) {
            AppIcon(kind = icon, tint = if (enabled) Ink else Muted, size = 18.dp)
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = text, fontSize = 17.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
internal fun TonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: AppIconKind? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PanelHigh,
            contentColor = Cream,
        ),
    ) {
        if (icon != null) {
            AppIcon(kind = icon, tint = Cream, size = 18.dp)
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = text, fontSize = 17.sp, fontWeight = FontWeight.Medium)
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
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(role = Role.Button, onClick = onPick)
        } else {
            Modifier
        }
        Box(
            modifier = valueBox
                .weight(1f)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = value, color = Cream, fontSize = 18.sp, fontWeight = FontWeight.Medium)
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

/** A single-choice pill row: the selection is the only amber on the surface. */
@Composable
internal fun SegmentedRow(options: List<SegmentOption>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PanelHigh)
            .border(1.dp, Hairline, RoundedCornerShape(14.dp))
            .padding(4.dp),
    ) {
        options.forEach { option ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (option.selected) Amber else Color.Transparent)
                    .selectable(
                        selected = option.selected,
                        role = Role.Tab,
                        onClick = option.onClick,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option.label,
                    color = if (option.selected) Ink else Info,
                    fontSize = 14.sp,
                    fontWeight = if (option.selected) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
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
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(kind = icon, tint = Amber, size = 18.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            color = Cream,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f),
        )
        AppIcon(kind = trailing, tint = Muted, size = 18.dp)
    }
}
