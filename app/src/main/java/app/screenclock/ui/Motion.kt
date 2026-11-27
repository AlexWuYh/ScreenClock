package app.screenclock.ui

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Whether the system is told to run animations. Judging by the duration scale keeps the
 * app honest when a user (or a test) turns animations off: such a page opens with no slide.
 * One observer per visible page, with a resume read for changes missed while away.
 */
@Composable
internal fun rememberAnimatorEnabled(): Boolean {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val resolver = context.contentResolver
    fun read(): Boolean =
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    var enabled by remember(resolver) { mutableStateOf(read()) }
    DisposableEffect(resolver, owner) {
        val refresh = { enabled = read() }
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) = refresh()
        }
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        resolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer,
        )
        owner.lifecycle.addObserver(lifecycleObserver)
        refresh()
        onDispose {
            owner.lifecycle.removeObserver(lifecycleObserver)
            resolver.unregisterContentObserver(observer)
        }
    }
    return enabled
}
