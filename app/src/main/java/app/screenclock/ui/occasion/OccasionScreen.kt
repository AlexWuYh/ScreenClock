package app.screenclock.ui.occasion

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.screenclock.R
import app.screenclock.domain.occasion.OCCASION_LIMIT
import app.screenclock.domain.occasion.OCCASION_MESSAGE_MAX
import app.screenclock.domain.occasion.OCCASION_TITLE_MAX
import app.screenclock.domain.occasion.OccasionDraft
import app.screenclock.domain.occasion.OccasionKind
import app.screenclock.domain.occasion.occasionDayMax
import app.screenclock.ui.ChromeBar
import app.screenclock.ui.Cream
import app.screenclock.ui.Info
import app.screenclock.ui.Ink
import app.screenclock.ui.clock.ClockViewModel
import java.time.LocalDate

private const val YEAR_MIN: Int = 1900
private const val YEAR_MAX: Int = 2100

@Composable
fun OccasionScreen(viewModel: ClockViewModel) {
    val rows by viewModel.occasionRows.collectAsStateWithLifecycle()
    var editing by rememberSaveable { mutableStateOf(false) }
    BackHandler {
        if (editing) editing = false else viewModel.closeOccasions()
    }
    if (editing) {
        OccasionEditor(
            viewModel = viewModel,
            onBack = { editing = false },
            onSaved = { editing = false },
        )
    } else {
        OccasionList(
            rows = rows,
            onBack = viewModel::closeOccasions,
            onAdd = { editing = true },
            onOpen = { id -> viewModel.openCeremony(id, fromList = true) },
            onPin = viewModel::pinOccasion,
            onUnpin = viewModel::unpinOccasion,
            onDelete = viewModel::deleteOccasion,
        )
    }
}

@Composable
private fun OccasionList(
    rows: List<app.screenclock.domain.occasion.OccasionRow>,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onOpen: (String) -> Unit,
    onPin: (String) -> Unit,
    onUnpin: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    val full = rows.size >= OCCASION_LIMIT
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 12.dp),
    ) {
        TextButton(onClick = onBack) {
            Text(text = stringResource(R.string.back), color = Cream, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.occasion),
            color = Cream,
            fontFamily = FontFamily.Serif,
            fontSize = 36.sp,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (full) {
            Text(text = stringResource(R.string.occasion_full), color = Info, fontSize = 16.sp)
        }
        TextButton(onClick = onAdd, enabled = !full) {
            Text(text = stringResource(R.string.occasion_add), color = if (full) Info else Cream, fontSize = 18.sp)
        }
        if (rows.isEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = stringResource(R.string.occasion_empty), color = Info, fontSize = 18.sp)
        }
        rows.forEach { row ->
            Spacer(modifier = Modifier.height(16.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = row.title,
                    color = Cream,
                    fontFamily = FontFamily.Serif,
                    fontSize = 24.sp,
                    modifier = Modifier.then(
                        if (row.canOpen) {
                            Modifier.clickable { onOpen(row.id) }
                        } else {
                            Modifier
                        },
                    ),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = row.detail, color = Info, fontSize = 16.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = { if (row.pinned) onUnpin(row.id) else onPin(row.id) },
                        enabled = row.pinned || row.canPin,
                    ) {
                        Text(
                            text = stringResource(if (row.pinned) R.string.occasion_unpin else R.string.occasion_pin),
                            color = if (row.pinned || row.canPin) Cream else Info,
                            fontSize = 16.sp,
                        )
                    }
                    TextButton(onClick = { onDelete(row.id) }) {
                        Text(text = stringResource(R.string.occasion_delete), color = Cream, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun OccasionEditor(
    viewModel: ClockViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val thisYear = LocalDate.now().year.coerceIn(YEAR_MIN, YEAR_MAX)
    var title by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    var kindName by rememberSaveable { mutableStateOf(OccasionKind.SolarYearly.name) }
    var year by rememberSaveable { mutableIntStateOf(thisYear) }
    var month by rememberSaveable { mutableIntStateOf(1) }
    var day by rememberSaveable { mutableIntStateOf(1) }
    var leap by rememberSaveable { mutableStateOf(false) }
    val kind = OccasionKind.entries.find { it.name == kindName } ?: OccasionKind.SolarYearly
    val leapAllowed = kind == OccasionKind.LunarYearly && viewModel.occasionLeapAllowed(month)
    val leapValue = leap && leapAllowed
    val dayMax = occasionDayMax(kind, year, month)
    val dayValue = day.coerceIn(1, dayMax)
    val draft = OccasionDraft(
        title = title,
        kind = kind,
        year = year,
        month = month,
        day = dayValue,
        leap = leapValue,
        message = message,
    )
    val preview = viewModel.occasionPreview(draft)
    val canSave = viewModel.occasionCanSave(draft)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 12.dp),
    ) {
        TextButton(onClick = onBack) {
            Text(text = stringResource(R.string.back), color = Cream, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.occasion_add),
            color = Cream,
            fontFamily = FontFamily.Serif,
            fontSize = 36.sp,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(modifier = Modifier.height(20.dp))
        OccasionField(
            value = title,
            onValueChange = { title = it.take(OCCASION_TITLE_MAX) },
            label = stringResource(R.string.occasion_title),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Column(modifier = Modifier.selectableGroup()) {
            KindRow(stringResource(R.string.occasion_kind_once), kind == OccasionKind.SolarOnce) {
                kindName = OccasionKind.SolarOnce.name
            }
            KindRow(stringResource(R.string.occasion_kind_solar), kind == OccasionKind.SolarYearly) {
                kindName = OccasionKind.SolarYearly.name
            }
            KindRow(stringResource(R.string.occasion_kind_lunar), kind == OccasionKind.LunarYearly) {
                kindName = OccasionKind.LunarYearly.name
            }
        }
        if (kind == OccasionKind.SolarOnce) {
            StepRow(
                value = stringResource(R.string.occasion_year_value, year),
                onDecrease = { year = (year - 1).coerceAtLeast(YEAR_MIN) },
                onIncrease = { year = (year + 1).coerceAtMost(YEAR_MAX) },
            )
        }
        StepRow(
            value = stringResource(R.string.occasion_month_value, month),
            onDecrease = { month = if (month <= 1) 12 else month - 1 },
            onIncrease = { month = if (month >= 12) 1 else month + 1 },
        )
        StepRow(
            value = stringResource(R.string.occasion_day_value, dayValue),
            onDecrease = { day = if (dayValue <= 1) dayMax else dayValue - 1 },
            onIncrease = { day = if (dayValue >= dayMax) 1 else dayValue + 1 },
        )
        if (kind == OccasionKind.LunarYearly) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .toggleable(
                        value = leapValue,
                        enabled = leapAllowed,
                        role = Role.Switch,
                        onValueChange = { leap = it },
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.occasion_leap),
                    color = if (leapAllowed) Cream else Info,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = leapValue,
                    onCheckedChange = null,
                    enabled = leapAllowed,
                    modifier = Modifier.clearAndSetSemantics { },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Ink,
                        checkedTrackColor = Cream,
                        uncheckedThumbColor = Cream,
                        uncheckedTrackColor = ChromeBar,
                        uncheckedBorderColor = Info,
                        disabledCheckedThumbColor = Info,
                        disabledCheckedTrackColor = ChromeBar,
                        disabledUncheckedThumbColor = Info,
                        disabledUncheckedTrackColor = ChromeBar,
                    ),
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        OccasionField(
            value = message,
            onValueChange = { message = it.take(OCCASION_MESSAGE_MAX) },
            label = stringResource(R.string.occasion_message),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = preview, color = Info, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { if (viewModel.addOccasion(draft)) onSaved() },
            enabled = canSave,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Cream,
                contentColor = Ink,
                disabledContainerColor = ChromeBar,
                disabledContentColor = Info,
            ),
        ) {
            Text(text = stringResource(R.string.occasion_save), fontSize = 18.sp)
        }
    }
}

@Composable
private fun OccasionField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = TextStyle(color = Cream, fontSize = 18.sp),
        label = { Text(text = label, color = Info) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Cream,
            unfocusedTextColor = Cream,
            focusedBorderColor = Cream,
            unfocusedBorderColor = Info,
            cursorColor = Cream,
            focusedLabelColor = Cream,
            unfocusedLabelColor = Info,
        ),
    )
}

@Composable
private fun KindRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            modifier = Modifier.clearAndSetSemantics { },
            colors = RadioButtonDefaults.colors(selectedColor = Cream, unselectedColor = Info),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, color = Cream, fontSize = 18.sp)
    }
}

@Composable
private fun StepRow(value: String, onDecrease: () -> Unit, onIncrease: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onDecrease) {
            Text(text = stringResource(R.string.occasion_decrease), color = Cream, fontSize = 16.sp)
        }
        Text(
            text = value,
            color = Cream,
            fontSize = 20.sp,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onIncrease) {
            Text(text = stringResource(R.string.occasion_increase), color = Cream, fontSize = 16.sp)
        }
    }
}
