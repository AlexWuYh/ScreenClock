package app.screenclock.ui.focus

import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalView
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
import app.screenclock.ui.AppIconKind
import app.screenclock.ui.ChromeBar
import app.screenclock.ui.Cream
import app.screenclock.ui.FieldLabel
import app.screenclock.ui.IconBox
import app.screenclock.ui.Info
import app.screenclock.ui.Ink
import app.screenclock.ui.Muted
import app.screenclock.ui.PageTitle
import app.screenclock.ui.PrimaryButton
import app.screenclock.ui.SectionCard
import app.screenclock.ui.SectionHeader
import app.screenclock.ui.SegmentOption
import app.screenclock.ui.SegmentedRow
import app.screenclock.ui.appSliderColors
import app.screenclock.ui.clock.ClockViewModel
import app.screenclock.ui.theme.flip.FlipFlaps
import app.screenclock.ui.theme.flip.focusFlapKey
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
        IconBox(
            icon = AppIconKind.Back,
            contentDescription = stringResource(R.string.back),
            onClick = onBack,
        )
        Spacer(modifier = Modifier.height(14.dp))
        PageTitle(stringResource(R.string.focus))
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.focus_hold_hint),
            color = Muted,
            fontSize = 15.sp,
        )
        Spacer(modifier = Modifier.height(22.dp))
        SectionCard {
            SectionHeader(icon = AppIconKind.Timer, text = stringResource(R.string.focus))
            SegmentedRow(
                listOf(
                    SegmentOption(
                        label = stringResource(R.string.focus_preset_25),
                        selected = choice == FocusChoice.Short,
                        onClick = { choice = FocusChoice.Short },
                    ),
                    SegmentOption(
                        label = stringResource(R.string.focus_preset_50),
                        selected = choice == FocusChoice.Long,
                        onClick = { choice = FocusChoice.Long },
                    ),
                    SegmentOption(
                        label = stringResource(R.string.focus_custom),
                        selected = choice == FocusChoice.Custom,
                        onClick = { choice = FocusChoice.Custom },
                    ),
                ),
            )
        if (choice == FocusChoice.Custom) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FieldLabel(stringResource(R.string.focus_custom))
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.focus_minutes, minutes),
                    color = Cream,
                    fontSize = 15.sp,
                )
            }
            Slider(
                value = customMinutes,
                onValueChange = { customMinutes = it },
                valueRange = FOCUS_MIN_MINUTES.toFloat()..FOCUS_MAX_MINUTES.toFloat(),
                steps = FOCUS_MAX_MINUTES - FOCUS_MIN_MINUTES - 1,
                colors = appSliderColors(),
            )
        }
        }
        Spacer(modifier = Modifier.height(22.dp))
        PrimaryButton(
            text = stringResource(R.string.focus_start),
            icon = AppIconKind.Timer,
            onClick = { onStart(minutes) },
            modifier = Modifier.fillMaxWidth(),
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
    // One cross-process read per screen entry, not one per second of countdown.
    val animate = remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) > 0f
    }
    val pad = if (expanded) 32.dp else 16.dp
    val view = LocalView.current
    var holding by remember { mutableStateOf(false) }
    val holdProgress = remember { Animatable(0f) }
    LaunchedEffect(holding) {
        if (holding) {
            holdProgress.snapTo(0f)
            holdProgress.animateTo(1f, tween(durationMillis = FOCUS_HOLD_MILLIS.toInt()))
        } else {
            holdProgress.snapTo(0f)
        }
    }
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
                            // Holding shows a short bar and buzzes once at the threshold,
                            // so a bedside press that lingers is not a silent exit.
                            holding = true
                            try {
                                coroutineScope {
                                    val hold = launch {
                                        delay(FOCUS_HOLD_MILLIS)
                                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                        giveUp.value()
                                    }
                                    try {
                                        tryAwaitRelease()
                                    } finally {
                                        hold.cancel()
                                    }
                                }
                            } finally {
                                holding = false
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
        FlipFlaps(
            groups = face.groups,
            animate = animate,
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .clearAndSetSemantics { },
            keyOf = { index -> focusFlapKey(face.groups.size, index) },
        )
        if (holding && phase.value != FocusPhase.Done) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .fillMaxWidth(0.42f)
                    .height(3.dp)
                    .background(ChromeBar),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(holdProgress.value.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(Cream),
                )
            }
        }
    }
}


