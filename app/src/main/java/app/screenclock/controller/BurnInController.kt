package app.screenclock.controller

import app.screenclock.data.bedside.BedsideStore
import app.screenclock.data.burnin.BurnInStore
import app.screenclock.domain.bedside.BedsidePreferences
import app.screenclock.domain.burnin.BurnInClock
import app.screenclock.domain.burnin.BurnInPhase
import app.screenclock.domain.burnin.BurnInPreferences
import app.screenclock.domain.burnin.burnInSetEnabled
import app.screenclock.domain.burnin.burnInSetHostHidden
import app.screenclock.domain.burnin.burnInSetPagePaused
import app.screenclock.domain.burnin.burnInTick
import app.screenclock.domain.burnin.burnInTouch
import app.screenclock.domain.burnin.freshBurnInClock
import app.screenclock.domain.burnin.millisUntilBurnInEdge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * The burn-in state machine, the bedside switches, and the plugged flag. The phase
 * is runtime only: a new process starts armed again. Page pause (settings, focus,
 * ceremony) and host hidden compose into one hold, so ending one of two holds does
 * not restart the idle count.
 */
class BurnInController(
    private val scope: CoroutineScope,
    private val burnInStore: BurnInStore,
    private val bedsideStore: BedsideStore,
    private val time: ClockTime,
) {
    private val burnInPrefsState = MutableStateFlow(BurnInPreferences())
    val burnInPrefs: StateFlow<BurnInPreferences> = burnInPrefsState.asStateFlow()

    private val bedsideState = MutableStateFlow(BedsidePreferences())
    val bedside: StateFlow<BedsidePreferences> = bedsideState.asStateFlow()

    private val pluggedState = MutableStateFlow(false)
    val plugged: StateFlow<Boolean> = pluggedState.asStateFlow()

    private val burnInState = MutableStateFlow(freshBurnInClock(enabled = false, nowMillis = 0L))
    val burnInSession: StateFlow<BurnInClock> = burnInState.asStateFlow()

    private var burnInJob: Job? = null
    private var burnInPlaced = false

    init {
        scope.launch {
            burnInStore.preferences.collect { prefs -> placeBurnIn(prefs) }
        }
        scope.launch {
            bedsideStore.preferences.collect { prefs -> bedsideState.value = prefs }
        }
    }

    /** From the battery sticky broadcast. Plugged is not persisted. */
    fun setPlugged(plugged: Boolean) {
        pluggedState.value = plugged
    }

    /**
     * A press on the clock. Returns true when the press ends a running screensaver,
     * so that same tap should not also open the control layer.
     */
    fun onContact(): Boolean {
        val state = burnInState.value
        val dismiss = state.phase == BurnInPhase.Running && !state.paused
        val next = burnInTouch(state, time.elapsedMillis())
        if (next != state) {
            burnInState.value = next
            rescheduleBurnIn()
        }
        return dismiss
    }

    /** Settings, focus, the occasion list, and ceremony share this hold. */
    fun setPagePaused(paused: Boolean) {
        val next = burnInSetPagePaused(burnInState.value, paused, time.elapsedMillis())
        if (next == burnInState.value) return
        burnInState.value = next
        rescheduleBurnIn()
    }

    /** The activity left the screen, or came back. */
    fun setHostHidden(hidden: Boolean) {
        val next = burnInSetHostHidden(burnInState.value, hidden, time.elapsedMillis())
        if (next == burnInState.value) return
        burnInState.value = next
        rescheduleBurnIn()
    }

    fun setEnabled(enabled: Boolean) {
        scope.launchPersisting { burnInStore.setEnabled(enabled) }
    }

    fun setIdleMillis(idleMillis: Long) {
        scope.launchPersisting { burnInStore.setIdleMillis(idleMillis) }
    }

    fun setRunMillis(runMillis: Long) {
        scope.launchPersisting { burnInStore.setRunMillis(runMillis) }
    }

    fun setNightDimEnabled(enabled: Boolean) {
        scope.launchPersisting { bedsideStore.setDimEnabled(enabled) }
    }

    fun setNightStartHour(hour: Int) {
        scope.launchPersisting { bedsideStore.setStartHour(hour) }
    }

    fun setNightEndHour(hour: Int) {
        scope.launchPersisting { bedsideStore.setEndHour(hour) }
    }

    fun setNightWarmEnabled(enabled: Boolean) {
        scope.launchPersisting { bedsideStore.setWarmEnabled(enabled) }
    }

    fun setChargeKeepAwake(enabled: Boolean) {
        scope.launchPersisting { bedsideStore.setChargeKeepAwake(enabled) }
    }

    private fun placeBurnIn(prefs: BurnInPreferences) {
        val now = time.elapsedMillis()
        val current = burnInState.value
        val next = if (!burnInPlaced) {
            burnInPlaced = true
            freshBurnInClock(prefs.enabled, now, current.pagePaused, current.hostHidden)
        } else if ((current.phase != BurnInPhase.Off) != prefs.enabled) {
            burnInSetEnabled(current, prefs.enabled, now)
        } else {
            current
        }
        burnInPrefsState.value = prefs
        burnInState.value = next
        rescheduleBurnIn()
    }

    private fun rescheduleBurnIn() {
        burnInJob?.cancel()
        burnInJob = scope.launch {
            while (isActive) {
                val prefs = burnInPrefsState.value
                val wait = millisUntilBurnInEdge(
                    burnInState.value,
                    prefs.idleMillis,
                    prefs.runMillis,
                    time.elapsedMillis(),
                ) ?: return@launch
                delay(wait)
                val latest = burnInPrefsState.value
                val updated = burnInTick(
                    burnInState.value,
                    latest.idleMillis,
                    latest.runMillis,
                    time.elapsedMillis(),
                )
                if (updated == burnInState.value) return@launch
                burnInState.value = updated
            }
        }
    }
}
