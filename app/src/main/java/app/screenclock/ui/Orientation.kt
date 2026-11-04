package app.screenclock.ui

import android.app.Activity
import android.content.pm.ActivityInfo

/** Locks the screen to the other orientation. Stays until the app is closed. */
internal fun Activity.toggleScreenOrientation(landscapeNow: Boolean) {
    requestedOrientation = if (landscapeNow) {
        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    } else {
        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }
}
