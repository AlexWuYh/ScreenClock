package app.screenclock.ui.clock

import android.media.MediaPlayer
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.annotation.RawRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.viewinterop.AndroidView
import app.screenclock.R
import app.screenclock.domain.burnin.ScreensaverTheme

@RawRes
internal fun screensaverVideoRes(theme: ScreensaverTheme): Int? = when (theme) {
    ScreensaverTheme.Drift -> null
    ScreensaverTheme.Glitch -> R.raw.screensaver_glitch
    ScreensaverTheme.Tunnel -> R.raw.screensaver_tunnel
    ScreensaverTheme.Portrait -> R.raw.screensaver_portrait
}

/**
 * Full-bleed muted looping video. It lives only while the screensaver round runs, so the
 * player is created on surface creation and released with the surface. Touches fall through
 * to the clock's own gesture handler, which ends the round.
 */
@Composable
internal fun ScreensaverVideo(theme: ScreensaverTheme, modifier: Modifier = Modifier) {
    val res = screensaverVideoRes(theme) ?: return
    val context = LocalContext.current
    val resState = rememberUpdatedState(res)
    Box(modifier = modifier.background(Color.Black).clearAndSetSemantics { }) {
        AndroidView(
            modifier = Modifier.matchParentSize(),
            factory = {
                SurfaceView(context).apply {
                    var player: MediaPlayer? = null
                    holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: SurfaceHolder) {
                            player = startPlayer(holder, resState.value)
                        }

                        override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, ht: Int) = Unit

                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                            player?.let { runCatching { it.release() } }
                            player = null
                        }
                    })
                }
            },
        )
    }
}

private fun SurfaceView.startPlayer(holder: SurfaceHolder, @RawRes res: Int): MediaPlayer? {
    // A broken codec or file must leave the black backdrop, never crash the clock.
    return runCatching {
        MediaPlayer().apply {
            resources.openRawResourceFd(res).use { fd ->
                setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
            }
            setDisplay(holder)
            setVolume(0f, 0f)
            isLooping = true
            setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
            setOnPreparedListener { it.start() }
            setOnErrorListener { mp, _, _ -> runCatching { mp.release() }; true }
            prepareAsync()
        }
    }.getOrNull()
}
