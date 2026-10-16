package app.screenclock.controller

import app.screenclock.data.focus.FocusStore
import app.screenclock.domain.focus.focusEndEpochMillis
import app.screenclock.domain.focus.storedFocusEnd
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface FocusEvent {
    /** The end moment could not be written; the session was rolled back. */
    data object PersistFailed : FocusEvent
}

/**
 * One focus session at a time. The end moment is written optimistically and the
 * DataStore echo is reconciled with [pendingFocusEnd] / [awaitFocusClear], so a
 * slow write cannot resurrect a session the user just abandoned.
 */
class FocusController(
    private val scope: CoroutineScope,
    private val store: FocusStore,
    private val time: ClockTime,
) {
    private val focusEndState = MutableStateFlow<Long?>(null)
    val focusEnd: StateFlow<Long?> = focusEndState.asStateFlow()

    private val focusKnownState = MutableStateFlow(false)
    val focusKnown: StateFlow<Boolean> = focusKnownState.asStateFlow()

    private val eventState = MutableSharedFlow<FocusEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<FocusEvent> = eventState.asSharedFlow()

    private var pendingFocusEnd: Long? = null
    private var awaitFocusClear = false

    init {
        scope.launch {
            store.endEpochMillis.collect { stored -> placeFocus(stored) }
        }
    }

    /** Starts a session and returns its end moment, so the caller can navigate at once. */
    fun start(minutes: Int): Long {
        val end = focusEndEpochMillis(time.nowMillis(), minutes)
        pendingFocusEnd = end
        awaitFocusClear = false
        focusEndState.value = end
        scope.launch {
            try {
                store.setEnd(end)
            } catch (_: IOException) {
                if (pendingFocusEnd != end) return@launch
                pendingFocusEnd = null
                focusEndState.value = null
                eventState.tryEmit(FocusEvent.PersistFailed)
            }
        }
        return end
    }

    /** Long-press cancel and the tap after zero both clear the stored end. */
    fun abandon() {
        pendingFocusEnd = null
        awaitFocusClear = true
        focusEndState.value = null
        scope.launch { store.clear() }
    }

    private fun placeFocus(stored: Long?) {
        val incoming = storedFocusEnd(stored)
        val pending = pendingFocusEnd
        if (pending != null) {
            if (incoming != pending) {
                focusKnownState.value = true
                return
            }
            pendingFocusEnd = null
        }
        if (awaitFocusClear) {
            if (incoming != null) {
                focusKnownState.value = true
                return
            }
            awaitFocusClear = false
        }
        focusEndState.value = incoming
        focusKnownState.value = true
    }
}
