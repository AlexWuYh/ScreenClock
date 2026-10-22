package app.screenclock.controller

import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Launches a preference write that may fail on a full disk. A failed write leaves the old
 * value in place, which is better than crashing a clock that is meant to run all night.
 */
fun CoroutineScope.launchPersisting(block: suspend () -> Unit): Job = launch {
    try {
        block()
    } catch (_: IOException) {
    }
}
