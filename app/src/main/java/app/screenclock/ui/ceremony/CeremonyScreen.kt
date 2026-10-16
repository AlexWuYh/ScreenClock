package app.screenclock.ui.ceremony

import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.screenclock.R
import app.screenclock.domain.occasion.CeremonyMode
import app.screenclock.domain.occasion.millisUntilCeremonyTick
import app.screenclock.ui.AppIconKind
import app.screenclock.ui.Cream
import app.screenclock.ui.IconBox
import app.screenclock.ui.Info
import app.screenclock.ui.Ink
import app.screenclock.ui.clock.ClockViewModel
import app.screenclock.ui.theme.flip.FlipFlaps
import java.time.LocalDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun CeremonyScreen(viewModel: ClockViewModel) {
    val activity = checkNotNull(LocalActivity.current) {
        "CeremonyScreen must run inside an Activity"
    }
    val context = LocalContext.current
    val expanded = calculateWindowSizeClass(activity).widthSizeClass != WindowWidthSizeClass.Compact
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    val face = viewModel.ceremonyNow(now)
    val occurrence = viewModel.ceremonyDate()
    val generation by viewModel.wallClockGeneration.collectAsStateWithLifecycle()
    // delay follows uptime, so sleep and a changed clock have to start a new wait.
    LaunchedEffect(occurrence, generation) {
        if (occurrence == null) return@LaunchedEffect
        while (isActive) {
            val current = LocalDateTime.now()
            now = current
            val wait = millisUntilCeremonyTick(current, occurrence) ?: break
            delay(wait)
        }
        now = LocalDateTime.now()
    }
    BackHandler { viewModel.closeCeremony() }
    if (face == null || occurrence == null) {
        LaunchedEffect(Unit) { viewModel.closeCeremony() }
        Box(modifier = Modifier.fillMaxSize().background(Ink))
        return
    }
    val leave = rememberUpdatedState(viewModel::closeCeremony)
    // One cross-process read per screen entry, not one per second of countdown.
    val animate = remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) > 0f
    }
    val pad = if (expanded) 32.dp else 16.dp
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .clipToBounds(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .then(
                    if (face.mode == CeremonyMode.Done) {
                        Modifier.pointerInput(Unit) {
                            detectTapGestures(onPress = {
                                if (tryAwaitRelease()) leave.value.invoke()
                            })
                        }
                    } else {
                        Modifier
                    },
                )
                .semantics(mergeDescendants = true) {
                    contentDescription = face.spoken
                    if (face.mode == CeremonyMode.Done) {
                        onClick {
                            leave.value.invoke()
                            true
                        }
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            if (face.mode != CeremonyMode.Done) {
                Text(
                    text = face.title,
                    color = Cream,
                    fontFamily = FontFamily.Serif,
                    fontSize = if (expanded) 28.sp else 22.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clearAndSetSemantics { },
                )
                if (face.mode == CeremonyMode.Days) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = face.solarLabel,
                        color = Info,
                        fontSize = if (expanded) 18.sp else 16.sp,
                        modifier = Modifier.clearAndSetSemantics { },
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                FlipFlaps(
                    groups = face.groups,
                    animate = animate,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clearAndSetSemantics { },
                    keyOf = { index ->
                        if (face.mode == CeremonyMode.Clock) {
                            listOf("h", "m", "s").getOrElse(index) { index }
                        } else {
                            "d${face.groups.lastIndex - index}"
                        }
                    },
                )
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = face.message,
                        color = Cream,
                        fontFamily = FontFamily.Serif,
                        fontSize = if (expanded) 48.sp else 36.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.clearAndSetSemantics { },
                    )
                }
            }
        }
        IconBox(
            icon = AppIconKind.Back,
            contentDescription = stringResource(R.string.back),
            onClick = viewModel::closeCeremony,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}
