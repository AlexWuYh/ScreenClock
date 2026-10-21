package app.screenclock.ui.occasion

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.screenclock.R
import app.screenclock.controller.OccasionDraftStatus
import app.screenclock.domain.occasion.OCCASION_LIMIT
import app.screenclock.domain.occasion.OCCASION_MESSAGE_MAX
import app.screenclock.domain.occasion.OCCASION_TITLE_MAX
import app.screenclock.domain.occasion.OccasionDraft
import app.screenclock.domain.occasion.OccasionKind
import app.screenclock.domain.occasion.occasionDayMax
import app.screenclock.ui.Alert
import app.screenclock.ui.UiAccentGlassBorder
import app.screenclock.ui.UiAccentGlassFill
import app.screenclock.ui.GlassBorder
import app.screenclock.ui.GlassFill
import app.screenclock.ui.GlassFillHigh
import app.screenclock.ui.UiAccentDeep
import app.screenclock.ui.AppIcon
import app.screenclock.ui.AppIconKind
import app.screenclock.ui.Cream
import app.screenclock.ui.Hairline
import app.screenclock.ui.IconBox
import app.screenclock.ui.glassPressable
import app.screenclock.ui.glassSurface
import app.screenclock.ui.Ink
import app.screenclock.ui.Paper
import app.screenclock.ui.Muted
import app.screenclock.ui.PageTitle
import app.screenclock.ui.Panel
import app.screenclock.ui.Info
import app.screenclock.ui.PrimaryButton
import app.screenclock.ui.SegmentOption
import app.screenclock.ui.SegmentedRow
import app.screenclock.ui.StepRow
import app.screenclock.ui.ToggleRow
import app.screenclock.ui.clock.ClockViewModel
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    var pendingDelete by rememberSaveable { mutableStateOf<String?>(null) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBox(
                icon = AppIconKind.Back,
                contentDescription = stringResource(R.string.back),
                onClick = onBack,
            )
            Spacer(modifier = Modifier.weight(1f))
            AddButton(enabled = !full, onClick = onAdd)
        }
        Spacer(modifier = Modifier.height(14.dp))
        PageTitle(stringResource(R.string.occasion))
        Spacer(modifier = Modifier.height(10.dp))
        if (full) {
            Text(text = stringResource(R.string.occasion_full), color = Muted, fontSize = 14.sp)
        }
        if (rows.isEmpty()) {
            Spacer(modifier = Modifier.height(28.dp))
            Text(text = stringResource(R.string.occasion_empty), color = Muted, fontSize = 16.sp)
        }
        rows.forEach { row ->
            Spacer(modifier = Modifier.height(10.dp))
            OccasionRowCard(
                row = row,
                onOpen = onOpen,
                onPin = onPin,
                onUnpin = onUnpin,
                onDelete = { pendingDelete = row.id },
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
    val pending = pendingDelete
    if (pending != null) {
        val row = rows.find { it.id == pending }
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            modifier = Modifier.glassSurface(corner = 20.dp, fill = GlassFillHigh),
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(20.dp),
            titleContentColor = Cream,
            textContentColor = Info,
            title = { Text(text = stringResource(R.string.occasion_delete_title)) },
            text = { Text(text = stringResource(R.string.occasion_delete_body, row?.title ?: "")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        onDelete(pending)
                    },
                ) {
                    Text(text = stringResource(R.string.occasion_delete), color = Alert)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(text = stringResource(R.string.occasion_cancel), color = Info)
                }
            },
        )
    }
}

@Composable
private fun AddButton(enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(44.dp)
            .glassSurface(
                corner = 14.dp,
                fill = if (enabled) UiAccentGlassFill else GlassFill,
                border = if (enabled) UiAccentGlassBorder else GlassBorder,
            )
            .glassPressable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(kind = AppIconKind.Plus, tint = if (enabled) Cream else Muted, size = 18.dp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = stringResource(R.string.occasion_add), color = if (enabled) Cream else Muted, fontSize = 15.sp)
    }
}

@Composable
private fun OccasionRowCard(
    row: app.screenclock.domain.occasion.OccasionRow,
    onOpen: (String) -> Unit,
    onPin: (String) -> Unit,
    onUnpin: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val open = if (row.canOpen) {
        Modifier.glassPressable { onOpen(row.id) }
    } else {
        Modifier
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(corner = 16.dp)
            .then(open)
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.title,
                color = if (row.canOpen) Cream else Muted,
                fontFamily = FontFamily.Serif,
                fontSize = 20.sp,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(text = row.detail, color = Info, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        IconBox(
            icon = AppIconKind.Pin,
            contentDescription = stringResource(
                if (row.pinned) R.string.occasion_unpin else R.string.occasion_pin,
            ),
            onClick = { if (row.pinned) onUnpin(row.id) else onPin(row.id) },
            enabled = row.pinned || row.canPin,
            tint = if (row.pinned) UiAccentDeep else Cream,
        )
        Spacer(modifier = Modifier.width(4.dp))
        IconBox(
            icon = AppIconKind.Trash,
            contentDescription = stringResource(R.string.occasion_delete),
            onClick = onDelete,
        )
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
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    val kind = OccasionKind.entries.find { it.name == kindName } ?: OccasionKind.SolarYearly
    val leapMonths by viewModel.leapMonths.collectAsStateWithLifecycle()
    val leapAllowed = kind == OccasionKind.LunarYearly && month in leapMonths
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
    // The date scan is the expensive part. It runs once per draft change on a
    // background dispatcher, not twice inside every recomposition of the form.
    var draftStatus by remember {
        mutableStateOf(OccasionDraftStatus(preview = "", canSave = false))
    }
    LaunchedEffect(draft) {
        draftStatus = withContext(Dispatchers.Default) { viewModel.occasionDraftStatus(draft) }
    }
    val preview = draftStatus.preview
    val canSave = draftStatus.canSave
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        IconBox(
            icon = AppIconKind.Back,
            contentDescription = stringResource(R.string.back),
            onClick = onBack,
        )
        Spacer(modifier = Modifier.height(14.dp))
        PageTitle(stringResource(R.string.occasion_add))
        Spacer(modifier = Modifier.height(20.dp))
        OccasionField(
            value = title,
            onValueChange = { title = it.take(OCCASION_TITLE_MAX) },
            label = stringResource(R.string.occasion_title),
        )
        Spacer(modifier = Modifier.height(16.dp))
        SegmentedRow(
            listOf(
                SegmentOption(
                    label = stringResource(R.string.occasion_kind_once),
                    selected = kind == OccasionKind.SolarOnce,
                    onClick = { kindName = OccasionKind.SolarOnce.name },
                ),
                SegmentOption(
                    label = stringResource(R.string.occasion_kind_solar),
                    selected = kind == OccasionKind.SolarYearly,
                    onClick = { kindName = OccasionKind.SolarYearly.name },
                ),
                SegmentOption(
                    label = stringResource(R.string.occasion_kind_lunar),
                    selected = kind == OccasionKind.LunarYearly,
                    onClick = { kindName = OccasionKind.LunarYearly.name },
                ),
            ),
        )
        Spacer(modifier = Modifier.height(16.dp))
        val stepDecrease = stringResource(R.string.occasion_decrease)
        val stepIncrease = stringResource(R.string.occasion_increase)
        if (kind == OccasionKind.SolarOnce) {
            StepRow(
                value = stringResource(R.string.occasion_year_value, year),
                decreaseLabel = stepDecrease,
                increaseLabel = stepIncrease,
                onDecrease = { year = (year - 1).coerceAtLeast(YEAR_MIN) },
                onIncrease = { year = (year + 1).coerceAtMost(YEAR_MAX) },
                onPick = { pickingDate = true },
            )
        }
        StepRow(
            value = stringResource(R.string.occasion_month_value, month),
            decreaseLabel = stepDecrease,
            increaseLabel = stepIncrease,
            onDecrease = { month = if (month <= 1) 12 else month - 1 },
            onIncrease = { month = if (month >= 12) 1 else month + 1 },
            onPick = { pickingDate = true },
        )
        StepRow(
            value = stringResource(R.string.occasion_day_value, dayValue),
            decreaseLabel = stepDecrease,
            increaseLabel = stepIncrease,
            onDecrease = { day = if (dayValue <= 1) dayMax else dayValue - 1 },
            onIncrease = { day = if (dayValue >= dayMax) 1 else dayValue + 1 },
            onPick = { pickingDate = true },
        )
        if (kind == OccasionKind.LunarYearly) {
            ToggleRow(
                label = stringResource(R.string.occasion_leap),
                checked = leapValue,
                enabled = leapAllowed,
                onCheckedChange = { leap = it },
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        OccasionField(
            value = message,
            onValueChange = { message = it.take(OCCASION_MESSAGE_MAX) },
            label = stringResource(R.string.occasion_message),
        )
        Spacer(modifier = Modifier.height(18.dp))
        if (preview.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(
                    kind = if (canSave) AppIconKind.Check else AppIconKind.Search,
                    tint = if (canSave) UiAccentDeep else Muted,
                    size = 16.dp,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = preview, color = Cream, fontSize = 15.sp)
            }
            Spacer(modifier = Modifier.height(18.dp))
        }
        PrimaryButton(
            text = stringResource(R.string.occasion_save),
            icon = AppIconKind.Check,
            onClick = { if (viewModel.addOccasion(draft)) onSaved() },
            enabled = canSave,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
    if (pickingDate) {
        DateWheelDialog(
            title = stringResource(R.string.occasion_pick_title),
            showYear = kind == OccasionKind.SolarOnce,
            yearRange = YEAR_MIN..YEAR_MAX,
            initialYear = year,
            initialMonth = month,
            initialDay = dayValue,
            dayMaxOf = { pickedYear, pickedMonth -> occasionDayMax(kind, pickedYear, pickedMonth) },
            onPick = { pickedYear, pickedMonth, pickedDay ->
                year = pickedYear
                month = pickedMonth
                day = pickedDay
                pickingDate = false
            },
            onDismiss = { pickingDate = false },
        )
    }
}

@Composable
private fun OccasionField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = TextStyle(color = Cream, fontSize = 17.sp),
        shape = RoundedCornerShape(14.dp),
        label = { Text(text = label) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Cream,
            unfocusedTextColor = Cream,
            focusedBorderColor = UiAccentDeep,
            unfocusedBorderColor = Hairline,
            cursorColor = UiAccentDeep,
            focusedLabelColor = UiAccentDeep,
            unfocusedLabelColor = Muted,
        ),
    )
}
