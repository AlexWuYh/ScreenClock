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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Box
import app.screenclock.domain.occasion.OccasionIcon
import app.screenclock.domain.occasion.QuickDate
import app.screenclock.domain.occasion.quickDate
import app.screenclock.ui.FieldLabel
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
import androidx.compose.ui.text.font.FontWeight
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
import app.screenclock.ui.UiAccentDeep
import app.screenclock.ui.AppIcon
import app.screenclock.ui.AppIconKind
import app.screenclock.ui.LocalUiPalette
import app.screenclock.ui.IconBox
import app.screenclock.ui.centeredPage
import app.screenclock.ui.glassBackdrop
import app.screenclock.ui.glassPressable
import app.screenclock.ui.glassSurface
import app.screenclock.ui.Ink
import app.screenclock.ui.Paper
import app.screenclock.ui.PageTitle
import app.screenclock.ui.Panel
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
    val palette = LocalUiPalette.current
    val rows by viewModel.occasionRows.collectAsStateWithLifecycle()
    var editing by rememberSaveable { mutableStateOf(false) }
    // Null while adding; the id of the occasion while changing one.
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler {
        if (editing) editing = false else viewModel.closeOccasions()
    }
    if (editing) {
        OccasionEditor(
            viewModel = viewModel,
            editId = editingId,
            onBack = { editing = false },
            onSaved = { editing = false },
        )
    } else {
        OccasionList(
            rows = rows,
            onBack = viewModel::closeOccasions,
            onAdd = {
                editingId = null
                editing = true
            },
            onEdit = { id ->
                editingId = id
                editing = true
            },
            onOpen = { id -> viewModel.openCeremony(id, fromList = true) },
            onPlayAll = { viewModel.openCeremonySequence(fromList = true) },
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
    onEdit: (String) -> Unit,
    onOpen: (String) -> Unit,
    onPlayAll: () -> Unit,
    onPin: (String) -> Unit,
    onUnpin: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    val palette = LocalUiPalette.current
    val full = rows.size >= OCCASION_LIMIT
    var pendingDelete by rememberSaveable { mutableStateOf<String?>(null) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .glassBackdrop()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .centeredPage()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBox(
                icon = AppIconKind.Back,
                contentDescription = stringResource(R.string.back),
                onClick = onBack,
            )
            Spacer(modifier = Modifier.weight(1f))
            PlayAllButton(enabled = rows.any { it.canOpen }, onClick = onPlayAll)
            Spacer(modifier = Modifier.width(10.dp))
            AddButton(enabled = !full, onClick = onAdd)
        }
        Spacer(modifier = Modifier.height(14.dp))
        PageTitle(stringResource(R.string.occasion))
        Spacer(modifier = Modifier.height(10.dp))
        if (full) {
            Text(text = stringResource(R.string.occasion_full), color = palette.on3, fontSize = 14.sp)
        }
        if (rows.any { it.canOpen }) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = stringResource(R.string.auto_play_hint), color = palette.on3, fontSize = 13.sp)
        }
        if (rows.isEmpty()) {
            Spacer(modifier = Modifier.height(28.dp))
            Text(text = stringResource(R.string.occasion_empty), color = palette.on3, fontSize = 16.sp)
        }
        rows.forEach { row ->
            Spacer(modifier = Modifier.height(10.dp))
            OccasionRowCard(
                row = row,
                onOpen = onOpen,
                onEdit = onEdit,
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
            modifier = Modifier.glassSurface(corner = 20.dp, fill = palette.glassFillHigh),
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(20.dp),
            titleContentColor = palette.on,
            textContentColor = palette.on2,
            title = { Text(text = stringResource(R.string.occasion_delete_title)) },
            text = { Text(text = stringResource(R.string.occasion_delete_body, row?.title ?: "")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        onDelete(pending)
                    },
                ) {
                    Text(text = stringResource(R.string.occasion_delete), color = palette.alert)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(text = stringResource(R.string.occasion_cancel), color = palette.on2)
                }
            },
        )
    }
}

/** The main action of the page: a solid accent button, so it reads as the thing to press. */
@Composable
private fun PlayAllButton(enabled: Boolean, onClick: () -> Unit) {
    val palette = LocalUiPalette.current
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .glassSurface(corner = 14.dp, fill = if (enabled) palette.accent else palette.glassFill, border = if (enabled) palette.accent else palette.glassBorder)
            .glassPressable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(kind = AppIconKind.Play, tint = if (enabled) androidx.compose.ui.graphics.Color.White else palette.on3, size = 20.dp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.occasion_play_all),
            color = if (enabled) androidx.compose.ui.graphics.Color.White else palette.on3,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun AddButton(enabled: Boolean, onClick: () -> Unit) {
    val palette = LocalUiPalette.current
    Row(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .glassSurface(
                corner = 14.dp,
                fill = palette.glassFill,
                border = palette.glassBorder,
            )
            .glassPressable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(kind = AppIconKind.Plus, tint = if (enabled) palette.on else palette.on3, size = 18.dp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = stringResource(R.string.occasion_add), color = if (enabled) palette.on else palette.on3, fontSize = 15.sp)
    }
}

@Composable
private fun OccasionRowCard(
    row: app.screenclock.domain.occasion.OccasionRow,
    onOpen: (String) -> Unit,
    onEdit: (String) -> Unit,
    onPin: (String) -> Unit,
    onUnpin: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val palette = LocalUiPalette.current
    val open = if (row.canOpen) {
        Modifier.glassPressable { onOpen(row.id) }
    } else {
        Modifier
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(corner = 16.dp)
            .then(open)
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OccasionGlyph(icon = row.icon, tint = if (row.canOpen) palette.accent else palette.on3, size = 26.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.title,
                    color = if (row.canOpen) palette.on else palette.on3,
                    fontFamily = FontFamily.Serif,
                    fontSize = 20.sp,
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(text = row.detail, color = palette.on2, fontSize = 14.sp)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            RowPlayButton(enabled = row.canOpen, onClick = { onOpen(row.id) })
            Spacer(modifier = Modifier.weight(1f))
            IconBox(
                icon = AppIconKind.Edit,
                contentDescription = stringResource(R.string.occasion_edit),
                onClick = { onEdit(row.id) },
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconBox(
                icon = AppIconKind.Pin,
                contentDescription = stringResource(
                    if (row.pinned) R.string.occasion_unpin else R.string.occasion_pin,
                ),
                onClick = { if (row.pinned) onUnpin(row.id) else onPin(row.id) },
                enabled = row.pinned || row.canPin,
                tint = if (row.pinned) palette.accent else palette.on,
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconBox(
                icon = AppIconKind.Trash,
                contentDescription = stringResource(R.string.occasion_delete),
                onClick = onDelete,
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

/** Plays this one countdown. Labeled, so it is clear that tapping a record starts it. */
@Composable
private fun RowPlayButton(enabled: Boolean, onClick: () -> Unit) {
    val palette = LocalUiPalette.current
    Row(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .glassSurface(
                corner = 14.dp,
                fill = if (enabled) palette.accentSoft else palette.glassFill,
                border = if (enabled) palette.accent else palette.glassBorder,
            )
            .glassPressable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(kind = AppIconKind.Play, tint = if (enabled) palette.accent else palette.on3, size = 18.dp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.occasion_play),
            color = if (enabled) palette.on else palette.on3,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun OccasionEditor(
    viewModel: ClockViewModel,
    editId: String?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val palette = LocalUiPalette.current
    val thisYear = LocalDate.now().year.coerceIn(YEAR_MIN, YEAR_MAX)
    val initial = remember(editId) { editId?.let(viewModel::occasionDraftOf) }
    var title by rememberSaveable { mutableStateOf(initial?.title ?: "") }
    var message by rememberSaveable { mutableStateOf(initial?.message ?: "") }
    var iconName by rememberSaveable { mutableStateOf((initial?.icon ?: OccasionIcon.Default).name) }
    var kindName by rememberSaveable { mutableStateOf((initial?.kind ?: OccasionKind.SolarYearly).name) }
    var year by rememberSaveable { mutableIntStateOf(initial?.year ?: thisYear) }
    var month by rememberSaveable { mutableIntStateOf(initial?.month ?: 1) }
    var day by rememberSaveable { mutableIntStateOf(initial?.day ?: 1) }
    var leap by rememberSaveable { mutableStateOf(initial?.leap ?: false) }
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    val kind = OccasionKind.entries.find { it.name == kindName } ?: OccasionKind.SolarYearly
    val icon = OccasionIcon.fromStored(iconName)
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
        icon = icon,
    )
    // The date scan is the expensive part. It runs once per draft change on a
    // background dispatcher, not twice inside every recomposition of the form.
    var draftStatus by remember {
        mutableStateOf(OccasionDraftStatus(preview = "", canSave = false))
    }
    LaunchedEffect(draft) {
        draftStatus = withContext(Dispatchers.Default) { viewModel.occasionDraftStatus(draft, editing = editId != null) }
    }
    val preview = draftStatus.preview
    val canSave = draftStatus.canSave
    // Leaving with typed text asks first; a bedside swipe-back must not eat a draft.
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    val dirty = if (initial == null) title.isNotBlank() || message.isNotBlank() else draft != initial
    val leave = { if (dirty) confirmDiscard = true else onBack() }
    BackHandler(onBack = leave)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .glassBackdrop()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .centeredPage()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        IconBox(
            icon = AppIconKind.Back,
            contentDescription = stringResource(R.string.back),
            onClick = leave,
        )
        Spacer(modifier = Modifier.height(14.dp))
        PageTitle(stringResource(if (editId == null) R.string.occasion_add else R.string.occasion_edit_title))
        Spacer(modifier = Modifier.height(20.dp))
        OccasionField(
            value = title,
            onValueChange = { title = it.take(OCCASION_TITLE_MAX) },
            label = stringResource(R.string.occasion_title),
        )
        Spacer(modifier = Modifier.height(16.dp))
        FieldLabel(stringResource(R.string.occasion_icon))
        Spacer(modifier = Modifier.height(8.dp))
        IconPicker(selected = icon, onSelect = { iconName = it.name })
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
        FieldLabel(stringResource(R.string.occasion_quick))
        Spacer(modifier = Modifier.height(8.dp))
        QuickDates { picked ->
            val date = quickDate(LocalDate.now(), picked)
            kindName = OccasionKind.SolarOnce.name
            year = date.year.coerceIn(YEAR_MIN, YEAR_MAX)
            month = date.monthValue
            day = date.dayOfMonth
        }
        Spacer(modifier = Modifier.height(12.dp))
        val yearName = stringResource(R.string.occasion_wheel_year)
        val monthName = stringResource(R.string.occasion_wheel_month)
        val dayName = stringResource(R.string.occasion_wheel_day)
        if (kind == OccasionKind.SolarOnce) {
            StepRow(
                value = stringResource(R.string.occasion_year_value, year),
                decreaseLabel = stringResource(R.string.occasion_wheel_decrease, yearName),
                increaseLabel = stringResource(R.string.occasion_wheel_increase, yearName),
                onDecrease = { year = (year - 1).coerceAtLeast(YEAR_MIN) },
                onIncrease = { year = (year + 1).coerceAtMost(YEAR_MAX) },
                onPick = { pickingDate = true },
            )
        }
        StepRow(
            value = stringResource(R.string.occasion_month_value, month),
            decreaseLabel = stringResource(R.string.occasion_wheel_decrease, monthName),
            increaseLabel = stringResource(R.string.occasion_wheel_increase, monthName),
            onDecrease = { month = if (month <= 1) 12 else month - 1 },
            onIncrease = { month = if (month >= 12) 1 else month + 1 },
            onPick = { pickingDate = true },
        )
        StepRow(
            value = stringResource(R.string.occasion_day_value, dayValue),
            decreaseLabel = stringResource(R.string.occasion_wheel_decrease, dayName),
            increaseLabel = stringResource(R.string.occasion_wheel_increase, dayName),
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
                    tint = if (canSave) palette.accent else palette.on3,
                    size = 16.dp,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = preview, color = palette.on, fontSize = 15.sp)
            }
            Spacer(modifier = Modifier.height(18.dp))
        }
        if (!canSave) {
            Text(
                text = stringResource(
                    if (title.isBlank()) R.string.occasion_need_title else R.string.occasion_need_date,
                ),
                color = palette.on3,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }
        PrimaryButton(
            text = stringResource(R.string.occasion_save),
            icon = AppIconKind.Check,
            onClick = {
                val saved = if (editId == null) viewModel.addOccasion(draft) else viewModel.updateOccasion(editId, draft)
                if (saved) onSaved()
            },
            enabled = canSave,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            modifier = Modifier.glassSurface(corner = 20.dp, fill = palette.glassFillHigh),
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(20.dp),
            titleContentColor = palette.on,
            textContentColor = palette.on2,
            title = { Text(text = stringResource(R.string.occasion_discard_title)) },
            text = { Text(text = stringResource(R.string.occasion_discard_body)) },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false; onBack() }) {
                    Text(text = stringResource(R.string.occasion_discard), color = palette.alert)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) {
                    Text(text = stringResource(R.string.occasion_keep_editing), color = palette.on2)
                }
            },
        )
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
    val palette = LocalUiPalette.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = TextStyle(color = palette.on, fontSize = 17.sp),
        shape = RoundedCornerShape(14.dp),
        label = { Text(text = label) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = palette.on,
            unfocusedTextColor = palette.on,
            focusedBorderColor = palette.accent,
            unfocusedBorderColor = palette.line,
            cursorColor = palette.accent,
            focusedLabelColor = palette.accent,
            unfocusedLabelColor = palette.on3,
        ),
    )
}

@Composable
private fun IconPicker(selected: OccasionIcon, onSelect: (OccasionIcon) -> Unit) {
    val palette = LocalUiPalette.current
    val shape = RoundedCornerShape(14.dp)
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (option in OccasionIcon.entries) {
            val on = option == selected
            val label = stringResource(iconLabel(option))
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(shape)
                    .background(if (on) palette.accentSoft else palette.glassFillHigh)
                    .border(1.dp, if (on) palette.accent else palette.line, shape)
                    .semantics {
                        contentDescription = label
                        this.selected = on
                        role = Role.RadioButton
                    }
                    .glassPressable(role = Role.RadioButton) { onSelect(option) },
                contentAlignment = Alignment.Center,
            ) {
                OccasionGlyph(icon = option, tint = if (on) palette.accent else palette.on2, size = 24.dp)
            }
        }
    }
}

@Composable
private fun QuickDates(onPick: (QuickDate) -> Unit) {
    val palette = LocalUiPalette.current
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (pick in QuickDate.entries) {
            val shape = RoundedCornerShape(50)
            Text(
                text = stringResource(quickLabel(pick)),
                color = palette.on2,
                fontSize = 14.sp,
                modifier = Modifier
                    .heightIn(min = 40.dp)
                    .clip(shape)
                    .background(palette.glassFillHigh)
                    .border(1.dp, palette.line, shape)
                    .glassPressable { onPick(pick) }
                    .padding(horizontal = 16.dp, vertical = 9.dp),
            )
        }
    }
}

private fun iconLabel(icon: OccasionIcon): Int = when (icon) {
    OccasionIcon.Cake -> R.string.icon_cake
    OccasionIcon.Heart -> R.string.icon_heart
    OccasionIcon.Gift -> R.string.icon_gift
    OccasionIcon.Plane -> R.string.icon_plane
    OccasionIcon.Cap -> R.string.icon_cap
    OccasionIcon.Ring -> R.string.icon_ring
    OccasionIcon.Star -> R.string.icon_star
    OccasionIcon.Flag -> R.string.icon_flag
}

private fun quickLabel(pick: QuickDate): Int = when (pick) {
    QuickDate.InWeek -> R.string.quick_week
    QuickDate.InMonth -> R.string.quick_month
    QuickDate.InHundredDays -> R.string.quick_hundred
    QuickDate.NextYear -> R.string.quick_year
}
