package app.screenclock.ui.clock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.screenclock.R
import app.screenclock.ui.ChromeBar
import app.screenclock.ui.Cream
import app.screenclock.ui.Ink
import app.screenclock.domain.time.ClockThemeId
import app.screenclock.domain.time.ColorwayId
import app.screenclock.domain.time.clockContentDescription
import app.screenclock.domain.weather.formatWeatherLine
import app.screenclock.ui.theme.ThemedClock
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun ClockShell(viewModel: ClockViewModel) {
    val chrome by viewModel.chrome.collectAsStateWithLifecycle()
    val clock by viewModel.clock.collectAsStateWithLifecycle()
    val burnIn by viewModel.burnInSession.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = checkNotNull(LocalActivity.current) {
        "ClockShell must run inside an Activity"
    }
    val widthClass = calculateWindowSizeClass(activity).widthSizeClass
    val slot = when (widthClass) {
        WindowWidthSizeClass.Compact -> ClockWidth.Compact
        else -> ClockWidth.Expanded
    }
    val hideAt = chrome.hideAtEpochMillis
    LaunchedEffect(hideAt, chrome.visible) {
        if (!chrome.visible || hideAt == null) return@LaunchedEffect
        val remaining = hideAt - System.currentTimeMillis()
        if (remaining > 0L) delay(remaining)
        viewModel.onChromeTick(System.currentTimeMillis())
    }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                viewModel.onTimeChanged()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        onDispose { context.unregisterReceiver(receiver) }
    }

    val face = clock?.face
    val dateLine = clock?.dateLine
    val weatherLine = formatWeatherLine(clock?.weather, showRange = slot == ClockWidth.Expanded)
    val description = when {
        face == null -> ""
        else -> clockContentDescription(dateLine, face.spoken, weatherLine)
    }
    val animate = Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    ) > 0f
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .clipToBounds(),
    ) {
        BurnInShift(
            phase = burnIn.phase,
            paused = burnIn.paused,
            anchorMillis = burnIn.anchorMillis,
            expanded = slot == ClockWidth.Expanded,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(viewModel) {
                    detectTapGestures(
                        onPress = {
                            val dismiss = viewModel.onBurnInContact()
                            val released = tryAwaitRelease()
                            if (released && !dismiss) {
                                viewModel.onBlankTap(System.currentTimeMillis())
                            }
                        },
                    )
                }
                .semantics(mergeDescendants = true) {
                    contentDescription = description
                    onClick {
                        viewModel.onClockActivate(System.currentTimeMillis())
                        true
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            if (face != null) {
                ThemedClock(
                    theme = clock?.theme ?: ClockThemeId.Flip,
                    colorway = clock?.colorway ?: ColorwayId.InkBlack,
                    face = face,
                    dateLine = dateLine,
                    weatherLine = weatherLine,
                    occasionLine = null,
                    expanded = slot == ClockWidth.Expanded,
                    animate = animate,
                    modifier = Modifier
                        .fillMaxSize()
                        .clearAndSetSemantics { },
                )
            }
        }
        if (chrome.visible) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(ChromeBar)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = viewModel::openSettings) {
                        Text(
                            text = stringResource(R.string.settings),
                            color = Cream,
                            fontSize = 18.sp,
                        )
                    }
                    TextButton(onClick = viewModel::openFocus) {
                        Text(
                            text = stringResource(R.string.focus),
                            color = Cream,
                            fontSize = 18.sp,
                        )
                    }
                }
            }
        }
    }
}
