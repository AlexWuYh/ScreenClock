package app.screenclock.ui.focus

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.screenclock.R
import app.screenclock.domain.focus.FOCUS_MAX_MINUTES
import app.screenclock.domain.focus.FOCUS_MIN_MINUTES
import app.screenclock.domain.focus.FocusMode
import app.screenclock.domain.focus.FocusRun
import app.screenclock.domain.focus.elapsedMillis
import app.screenclock.domain.focus.focusClockText
import app.screenclock.domain.focus.focusProgress
import app.screenclock.domain.focus.focusSpoken
import app.screenclock.domain.focus.formatFocusTotal
import app.screenclock.domain.focus.millisUntilFocusDigitChange
import app.screenclock.domain.focus.minutes
import app.screenclock.domain.focus.remainingMillis
import app.screenclock.domain.focus.todayFocusMillis
import app.screenclock.domain.focus.totalMillis
import app.screenclock.domain.occasion.CeremonyStyle
import app.screenclock.domain.occasion.CeremonyTone
import app.screenclock.domain.occasion.ceremonyIsDark
import app.screenclock.ui.AppIcon
import app.screenclock.ui.AppIconKind
import app.screenclock.ui.ceremony.CeremonyPalette
import app.screenclock.ui.ceremony.PageControls
import app.screenclock.ui.ceremony.ceremonyPalette
import app.screenclock.ui.ceremony.rememberAutoHideControls
import app.screenclock.ui.ceremony.revealOnTouch
import app.screenclock.ui.clock.ClockViewModel
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private val Tabular = TextStyle(fontFeatureSettings = "tnum")

@Composable
fun FocusScreen(viewModel: ClockViewModel) {
    val ui by viewModel.focus.collectAsStateWithLifecycle()
    val look by viewModel.ceremonyLook.collectAsStateWithLifecycle()
    val generation by viewModel.wallClockGeneration.collectAsStateWithLifecycle()
    val dark = ceremonyIsDark(look.tone, isSystemInDarkTheme())
    // The focus page wears the immersive design of the countdown page, in the same tone.
    val palette = remember(dark) { ceremonyPalette(CeremonyStyle.Immersive, dark) }
    val context = LocalContext.current
    val animate = rememberAnimatorEnabled()
    val view = LocalView.current
    val controls = rememberAutoHideControls()
    var showCustom by rememberSaveable { mutableStateOf(false) }

    val session = ui.session
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    // delay follows uptime, so a changed clock or a return to the app starts a new wait.
    LaunchedEffect(session, generation) {
        while (isActive) {
            val current = System.currentTimeMillis()
            now = current
            val wait = millisUntilFocusDigitChange(session, current) ?: break
            delay(wait)
        }
        now = System.currentTimeMillis()
    }

    // A short buzz when a running segment reaches zero while the page is showing.
    var wasRunning by remember { mutableStateOf(false) }
    val running = session?.run is FocusRun.Running
    val done = session?.run == FocusRun.Done
    LaunchedEffect(running, done) {
        if (wasRunning && done) view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        wasRunning = running
    }

    BackHandler { viewModel.closeFocus() }

    val remaining = session?.remainingMillis(now) ?: ui.prefs.totalMillis()
    val timeText = focusClockText(remaining)
    val progress = focusProgress(session, now)
    val liveFocused = if (session != null && session.mode == FocusMode.Focus && session.run !is FocusRun.Done) {
        session.elapsedMillis(now)
    } else {
        0L
    }
    val todayText = formatFocusTotal(todayFocusMillis(ui.stats, LocalDate.now()) + liveFocused)
    val spoken = focusSpoken(session, ui.prefs, now)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg)
            .revealOnTouch(controls)
            .drawBehind { drawGlows(palette) }
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .clipToBounds(),
    ) {
        val landscape = maxWidth > maxHeight
        val short = maxHeight < 500.dp
        val ringSize = when {
            short -> 160.dp
            landscape -> minOf(maxHeight * 0.52f, 360.dp).coerceAtLeast(240.dp)
            else -> minOf(maxWidth * 0.68f, 340.dp).coerceAtLeast(220.dp)
        }
        val ring = @Composable {
            TimerRing(
                palette = palette,
                size = ringSize,
                timeText = timeText,
                progress = progress,
                running = running,
                animate = animate,
                caption = when {
                    done -> R.string.focus_status_done
                    session?.run is FocusRun.Paused -> R.string.focus_status_paused
                    else -> null
                },
                spoken = spoken,
            )
        }
        val playLabel = stringResource(
            when {
                running -> R.string.focus_pause
                session?.run is FocusRun.Paused -> R.string.focus_resume
                else -> R.string.focus_play
            },
        )
        val buttons = @Composable {
            Controls(
                palette = palette,
                short = short,
                running = running,
                playLabel = playLabel,
                onReset = viewModel::resetFocus,
                onToggle = viewModel::toggleFocus,
                onSkip = viewModel::skipFocus,
            )
        }
        val side = @Composable {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (short) 15.dp else 20.dp),
            ) {
                Stats(palette, todayText, short)
                Modes(
                    palette = palette,
                    short = short,
                    selected = ui.prefs.mode,
                    customMinutes = ui.prefs.customMinutes,
                    onSelect = { mode ->
                        if (mode == FocusMode.Custom) showCustom = true else viewModel.selectFocusMode(mode)
                    },
                )
            }
        }
        when {
            landscape -> Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = if (short) 20.dp else 30.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (short) 20.dp else 48.dp),
            ) {
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    if (short) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            ring()
                            buttons()
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(30.dp)) {
                            ring()
                            buttons()
                        }
                    }
                }
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) { side() }
            }
            else -> Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(30.dp),
            ) {
                Box(Modifier.padding(top = 36.dp)) { side() }
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { ring() }
                buttons()
            }
        }
        PageControls(
            controls = controls,
            palette = palette,
            onBack = viewModel::closeFocus,
            onToggleTone = {
                viewModel.setCeremonyTone(if (palette.dark) CeremonyTone.Light else CeremonyTone.Dark)
            },
        )
    }

    if (showCustom) {
        CustomMinutesDialog(
            palette = palette,
            initial = ui.prefs.customMinutes,
            onDismiss = { showCustom = false },
            onConfirm = { minutes ->
                showCustom = false
                viewModel.setFocusCustomMinutes(minutes)
            },
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGlows(palette: CeremonyPalette) {
    val big = maxOf(size.width, size.height)
    fun glow(color: Color, at: Offset, radius: Float) {
        drawRect(Brush.radialGradient(listOf(color, Color.Transparent), center = at, radius = radius))
    }
    glow(palette.glowA, Offset(size.width * 0.15f, size.height * 0.1f), big * 0.75f)
    glow(palette.glowB, Offset(size.width * 0.85f, size.height * 0.9f), big * 0.75f)
}

// ───────────────────────── ring ─────────────────────────

@Composable
private fun TimerRing(
    palette: CeremonyPalette,
    size: Dp,
    timeText: String,
    progress: Float,
    running: Boolean,
    animate: Boolean,
    caption: Int?,
    spoken: String,
) {
    val track = if (palette.dark) Color.White.copy(alpha = .08f) else Color(0xFF0F172A).copy(alpha = .08f)
    val glow = if (palette.dark) Color(255, 217, 102).copy(alpha = .15f) else Color(217, 119, 6).copy(alpha = .10f)
    // The ring empties as the time goes, one linear step per second like the design.
    val left by animateFloatAsState(
        targetValue = 1f - progress,
        animationSpec = tween(if (animate) 1000 else 0, easing = LinearEasing),
        label = "ring",
    )
    val pulse = rememberInfiniteTransition(label = "halo")
    val haloScale by pulse.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "haloScale",
    )
    val haloAlpha by pulse.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "haloAlpha",
    )
    Box(
        modifier = Modifier
            .size(size)
            .semantics(mergeDescendants = true) { contentDescription = spoken },
        contentAlignment = Alignment.Center,
    ) {
        if (running) {
            Box(
                Modifier
                    .size(size)
                    .scale(if (animate) haloScale else 1f)
                    .background(Brush.radialGradient(listOf(glow.copy(alpha = glow.alpha * (if (animate) haloAlpha else 1f)), Color.Transparent)), CircleShape),
            )
        }
        Canvas(Modifier.size(size).clearAndSetSemantics { }) {
            val stroke = this.size.minDimension * 0.05f
            val inset = stroke / 2f + this.size.minDimension * 0.02f
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            drawCircle(track, radius = arcSize.minDimension / 2f, style = Stroke(stroke))
            if (left > 0.002f) {
                drawArc(
                    color = palette.text1,
                    startAngle = -90f,
                    sweepAngle = 360f * left,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clearAndSetSemantics { }) {
            // Long segments carry an hour digit, so the type shrinks to stay inside the ring.
            val shrink = if (timeText.length > 5) 5f / timeText.length else 1f
            Text(
                text = timeText,
                color = palette.text1,
                fontSize = (size.value * 0.27f * shrink).sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-2).sp,
                maxLines = 1,
                softWrap = false,
                style = Tabular,
            )
            if (caption != null) {
                Text(text = stringResource(caption), color = palette.text3, fontSize = (size.value * 0.06f).coerceAtLeast(12f).sp)
            }
        }
    }
}

// ───────────────────────── controls ─────────────────────────

@Composable
private fun Controls(
    palette: CeremonyPalette,
    short: Boolean,
    running: Boolean,
    playLabel: String,
    onReset: () -> Unit,
    onToggle: () -> Unit,
    onSkip: () -> Unit,
) {
    val sub = if (short) 40.dp else 56.dp
    val main = if (short) 60.dp else 84.dp
    val gap = if (short) 12.dp else 30.dp
    val content: @Composable () -> Unit = {
        RoundButton(sub, palette.chip, palette.chipBorder, AppIconKind.Reset, palette.text2, stringResource(R.string.focus_reset), onReset)
        RoundButton(main, palette.text1, Color.Transparent, if (running) AppIconKind.Pause else AppIconKind.Play, palette.bg, playLabel, onToggle, iconSize = main * 0.42f)
        RoundButton(sub, palette.chip, palette.chipBorder, AppIconKind.Skip, palette.text2, stringResource(R.string.focus_skip), onSkip)
    }
    if (short) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(gap)) { content() }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(gap)) { content() }
    }
}

@Composable
private fun RoundButton(
    size: Dp,
    fill: Color,
    border: Color,
    icon: AppIconKind,
    tint: Color,
    label: String,
    onClick: () -> Unit,
    iconSize: Dp = size * 0.36f,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(fill)
            .border(1.dp, border, CircleShape)
            .semantics { contentDescription = label; role = Role.Button }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(kind = icon, tint = tint, size = iconSize)
    }
}

// ───────────────────────── stats and modes ─────────────────────────

@Composable
private fun Stats(palette: CeremonyPalette, total: String, short: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.focus_stats),
            color = palette.text2,
            fontSize = if (short) 12.sp else 14.sp,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = total,
            color = palette.text1,
            fontSize = if (short) 12.sp else 14.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
        )
    }
}

@Composable
private fun Modes(
    palette: CeremonyPalette,
    short: Boolean,
    selected: FocusMode,
    customMinutes: Int,
    onSelect: (FocusMode) -> Unit,
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(palette.chip)
            .border(1.dp, palette.chipBorder, shape)
            .horizontalScroll(rememberScrollState())
            .padding(if (short) 4.dp else 6.dp)
            .semantics { contentDescription = "" },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (mode in FocusMode.entries) {
            val on = mode == selected
            val label = when (mode) {
                FocusMode.Focus -> stringResource(R.string.focus_mode_focus)
                FocusMode.ShortBreak -> stringResource(R.string.focus_mode_short)
                FocusMode.LongBreak -> stringResource(R.string.focus_mode_long)
                FocusMode.Custom -> if (on) stringResource(R.string.focus_mode_custom_n, customMinutes) else stringResource(R.string.focus_mode_custom)
            }
            Text(
                text = label,
                color = if (on) palette.bg else palette.text2,
                fontSize = if (short) 12.sp else 14.sp,
                fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .clip(shape)
                    .background(if (on) palette.text1 else Color.Transparent)
                    .semantics { this.selected = on; role = Role.RadioButton }
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(mode) }
                    .padding(horizontal = if (short) 10.dp else 16.dp, vertical = if (short) 6.dp else 11.dp),
            )
        }
    }
}

// ───────────────────────── custom length ─────────────────────────

@Composable
private fun CustomMinutesDialog(
    palette: CeremonyPalette,
    initial: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(initial.toString()) }
    val modalBg = if (palette.dark) Color(0xFF1A1E2B) else Color.White
    val shape = RoundedCornerShape(32.dp)
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(modalBg)
                .border(1.dp, palette.divider, shape)
                .padding(horizontal = 24.dp, vertical = 28.dp),
        ) {
            Text(
                text = stringResource(R.string.focus_custom_title),
                color = palette.text1,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter(Char::isDigit).take(3) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                label = { Text(stringResource(R.string.focus_custom_label)) },
                placeholder = { Text(stringResource(R.string.focus_custom_hint, FOCUS_MIN_MINUTES, FOCUS_MAX_MINUTES)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = palette.text1,
                    unfocusedTextColor = palette.text1,
                    focusedBorderColor = palette.text1,
                    unfocusedBorderColor = palette.divider,
                    focusedLabelColor = palette.text2,
                    unfocusedLabelColor = palette.text3,
                    cursorColor = palette.text1,
                    focusedPlaceholderColor = palette.text3,
                    unfocusedPlaceholderColor = palette.text3,
                ),
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DialogButton(stringResource(R.string.occasion_cancel), palette.chip, palette.text2, palette.chipBorder, Modifier.weight(1f), onDismiss)
                DialogButton(stringResource(R.string.focus_ok), palette.text1, palette.bg, Color.Transparent, Modifier.weight(1f)) {
                    // Empty or out-of-range input is pulled into range, like the design.
                    onConfirm((text.toIntOrNull() ?: FOCUS_MIN_MINUTES).coerceIn(FOCUS_MIN_MINUTES, FOCUS_MAX_MINUTES))
                }
            }
        }
    }
}

@Composable
private fun DialogButton(label: String, fill: Color, textColor: Color, border: Color, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(shape)
            .background(fill)
            .border(1.dp, border, shape)
            .semantics { role = Role.Button }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** One setting observer per visible page, with a resume read for missed changes. */
@Composable
private fun rememberAnimatorEnabled(): Boolean {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val resolver = context.contentResolver
    var enabled by remember(resolver) {
        mutableStateOf(Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f)
    }
    DisposableEffect(resolver, owner) {
        val refresh = {
            enabled = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
        }
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) = refresh()
        }
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        owner.lifecycle.addObserver(lifecycleObserver)
        refresh()
        onDispose {
            owner.lifecycle.removeObserver(lifecycleObserver)
            resolver.unregisterContentObserver(observer)
        }
    }
    return enabled
}
