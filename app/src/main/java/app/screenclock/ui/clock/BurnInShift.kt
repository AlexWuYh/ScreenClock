package app.screenclock.ui.clock

import android.os.SystemClock
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import app.screenclock.domain.burnin.BurnInMotion
import app.screenclock.domain.burnin.BurnInOffset
import app.screenclock.domain.burnin.BurnInPhase
import app.screenclock.domain.burnin.ScreensaverTheme
import app.screenclock.domain.burnin.burnInMotion
import app.screenclock.domain.burnin.driftOffset
import app.screenclock.domain.burnin.wanderOffset
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Moves the current theme without changing its digits.
 * Drift updates about once a second. The screensaver follows frames, and only while it is running.
 */
@Composable
internal fun BurnInShift(
    phase: BurnInPhase,
    paused: Boolean,
    videoTheme: ScreensaverTheme,
    anchorMillis: Long,
    expanded: Boolean,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    val travel = if (expanded) 24.dp else 12.dp
    val travelPx = with(LocalDensity.current) { travel.toPx() }
    val videoPlaying = videoTheme.isVideo && phase == BurnInPhase.Running && !paused
    // A video round replaces the wander; the clock stays put underneath and is simply covered.
    val motion = if (videoPlaying) BurnInMotion.Still else burnInMotion(phase, paused)
    val anchor = rememberUpdatedState(anchorMillis)
    var offset by remember { mutableStateOf(BurnInOffset(0f, 0f)) }
    LaunchedEffect(motion, travelPx) {
        when (motion) {
            BurnInMotion.Still -> offset = BurnInOffset(0f, 0f)
            BurnInMotion.Drift -> while (isActive) {
                offset = driftOffset(SystemClock.elapsedRealtime())
                delay(DRIFT_FRAME_MILLIS)
            }
            BurnInMotion.Wander -> while (isActive) {
                val elapsed = SystemClock.elapsedRealtime() - anchor.value
                offset = wanderOffset(elapsed, travelPx, travelPx)
                withFrameNanos { }
            }
        }
    }
    // Only the digits and info lines move (see ClockScaffold); the backdrop stays put, so a
    // shifted frame never uncovers the black window behind a light scene.
    val offsetState = rememberUpdatedState(offset)
    CompositionLocalProvider(LocalBurnInOffset provides { offsetState.value }) {
        Box(
            modifier = modifier,
            contentAlignment = contentAlignment,
        ) {
            content()
            if (videoPlaying) ScreensaverVideo(videoTheme, Modifier.matchParentSize())
        }
    }
}

/** The current burn-in shift in pixels, read at draw time so the clock does not recompose. */
internal val LocalBurnInOffset = staticCompositionLocalOf<() -> BurnInOffset> { { BurnInOffset(0f, 0f) } }

private const val DRIFT_FRAME_MILLIS: Long = 1_000L
