package app.screenclock.ui.focus

import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.screenclock.R
import app.screenclock.domain.focus.FOCUS_HOLD_MILLIS
import app.screenclock.domain.focus.FOCUS_MAX_MINUTES
import app.screenclock.domain.focus.FOCUS_MIN_MINUTES
import app.screenclock.domain.focus.FOCUS_PRESET_LONG_MINUTES
import app.screenclock.domain.focus.FOCUS_PRESET_SHORT_MINUTES
import app.screenclock.domain.focus.FocusPhase
import app.screenclock.domain.focus.focusFace
import app.screenclock.domain.focus.focusSpoken
import app.screenclock.domain.focus.millisUntilFocusDigitChange
import app.screenclock.domain.focus.normalizeFocusMinutes
import app.screenclock.ui.ChromeBar
import app.screenclock.ui.Cream
import app.screenclock.ui.Info
import app.screenclock.ui.Ink
import app.screenclock.ui.clock.ClockViewModel
import app.screenclock.ui.theme.flip.FlipCard
import app.screenclock.ui.theme.flip.clockStacked
import app.screenclock.ui.theme.flip.flapLayout
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private enum class FocusChoice {
    Short,
    Long,
    Custom,
}

@Composable
fun FocusScreen(viewModel: ClockViewModel) {
    val end by viewModel.focusEnd.collectAsStateWithLifecycle()
    val session = end
    BackHandler {
        if (session == null) {
            viewModel.closeFocusPicker()
        } else if (focusFace(session, System.currentTimeMillis()).phase == FocusPhase.Done) {
            viewModel.abandonFocus()
        }
    }
    if (session == null) {
        FocusPicker(
            onStart = viewModel::startFocus,
            onBack = viewModel::closeFocusPicker,
        )
    } else {
        FocusCountdown(
            endEpochMillis = session,
            onGiveUp = viewModel::abandonFocus,
            onFinished = viewModel::abandonFocus,
        )
    }
}

@Composable
private fun FocusPicker(
    onStart: (Int) -> Unit,
    onBack: () -> Unit,
) {
    var choice by remember { mutableStateOf(FocusChoice.Short) }
    var customMinutes by remember { mutableFloatStateOf(45f) }
    val minutes = when (choice) {
        FocusChoice.Short -> FOCUS_PRESET_SHORT_MINUTES
        FocusChoice.Long -> FOCUS_PRESET_LONG_MINUTES
        FocusChoice.Custom -> normalizeFocusMinutes(customMinutes.toInt())
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 12.dp),
    ) {
        TextButton(onClick = onBack) {
            Text(
                text = stringResource(R.string.back),
                color = Cream,
                fontSize = 16.sp,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.focus),
            color = Cream,
            fontFamily = FontFamily.Serif,
            fontSize = 36.sp,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.focus_hold_hint),
            color = Info,
            fontSize = 16.sp,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Column(modifier = Modifier.selectableGroup()) {
            FocusChoiceRow(
                label = stringResource(R.string.focus_preset_25),
                selected = choice == FocusChoice.Short,
                onClick = { choice = FocusChoice.Short },
            )
            FocusChoiceRow(
                label = stringResource(R.string.focus_preset_50),
                selected = choice == FocusChoice.Long,
                onClick = { choice = FocusChoice.Long },
            )
            FocusChoiceRow(
                label = stringResource(R.string.focus_custom),
                selected = choice == FocusChoice.Custom,
                onClick = { choice = FocusChoice.Custom },
            )
        }
        if (choice == FocusChoice.Custom) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.focus_custom),
                    color = Info,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.focus_minutes, minutes),
                    color = Cream,
                    fontSize = 16.sp,
                )
            }
            Slider(
                value = customMinutes,
                onValueChange = { customMinutes = it },
                valueRange = FOCUS_MIN_MINUTES.toFloat()..FOCUS_MAX_MINUTES.toFloat(),
                steps = FOCUS_MAX_MINUTES - FOCUS_MIN_MINUTES - 1,
                colors = SliderDefaults.colors(
                    thumbColor = Cream,
                    activeTrackColor = Cream,
                    inactiveTrackColor = ChromeBar,
                ),
            )
        }
        Spacer(modifier = Modifier.height(28.dp))
        Button(
            onClick = { onStart(minutes) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Cream,
                contentColor = Ink,
            ),
        ) {
            Text(
                text = stringResource(R.string.focus_start),
                fontSize = 18.sp,
            )
        }
    }
}

@Composable
private fun FocusChoiceRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            modifier = Modifier.clearAndSetSemantics { },
            colors = RadioButtonDefaults.colors(
                selectedColor = Cream,
                unselectedColor = Info,
            ),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = Cream,
            fontSize = 18.sp,
        )
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
private fun FocusCountdown(
    endEpochMillis: Long,
    onGiveUp: () -> Unit,
    onFinished: () -> Unit,
) {
    val activity = checkNotNull(LocalActivity.current) {
        "FocusCountdown must run inside an Activity"
    }
    val context = LocalContext.current
    val expanded = calculateWindowSizeClass(activity).widthSizeClass != WindowWidthSizeClass.Compact
    var nowMillis by remember(endEpochMillis) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(endEpochMillis) {
        while (isActive) {
            val now = System.currentTimeMillis()
            nowMillis = now
            val wait = millisUntilFocusDigitChange(endEpochMillis, now) ?: break
            delay(wait)
        }
        nowMillis = System.currentTimeMillis()
    }
    val face = focusFace(endEpochMillis, nowMillis)
    val spoken = focusSpoken(face)
    val giveUpLabel = stringResource(R.string.focus_end_action)
    val phase = rememberUpdatedState(face.phase)
    val giveUp = rememberUpdatedState(onGiveUp)
    val finished = rememberUpdatedState(onFinished)
    val animate = Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    ) > 0f
    val pad = if (expanded) 32.dp else 16.dp
    KeepScreenOn()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .clipToBounds()
            .pointerInput(endEpochMillis) {
                detectTapGestures(
                    onPress = {
                        if (phase.value == FocusPhase.Done) {
                            if (tryAwaitRelease()) finished.value()
                        } else {
                            coroutineScope {
                                val hold = launch {
                                    delay(FOCUS_HOLD_MILLIS)
                                    giveUp.value()
                                }
                                try {
                                    tryAwaitRelease()
                                } finally {
                                    hold.cancel()
                                }
                            }
                        }
                    },
                )
            }
            .semantics(mergeDescendants = true) {
                contentDescription = spoken
                if (phase.value == FocusPhase.Done) {
                    onClick {
                        finished.value()
                        true
                    }
                } else {
                    customActions = listOf(
                        CustomAccessibilityAction(giveUpLabel) {
                            giveUp.value()
                            true
                        },
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        FocusFlaps(
            groups = face.groups,
            animate = animate,
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .clearAndSetSemantics { },
        )
    }
}

@Composable
private fun KeepScreenOn() {
    val activity = checkNotNull(LocalActivity.current) {
        "KeepScreenOn must run inside an Activity"
    }
    DisposableEffect(activity) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
}

@Composable
private fun FocusFlaps(
    groups: List<String>,
    animate: Boolean,
    modifier: Modifier = Modifier,
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
                    key(flapSlot(count, index)) {
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
                    key(flapSlot(count, index)) {
                        FlipCard(digits, animate, card)
                    }
                }
            }
        }
    }
}

private fun flapSlot(count: Int, index: Int): String {
    val names = if (count >= 3) listOf("h", "m", "s") else listOf("m", "s")
    return names.getOrElse(index) { index.toString() }
}
