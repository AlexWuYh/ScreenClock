package app.screenclock.controller

import app.screenclock.data.focus.FocusStore
import app.screenclock.domain.focus.FocusMode
import app.screenclock.domain.focus.FocusPrefs
import app.screenclock.domain.focus.FocusRun
import app.screenclock.domain.focus.FocusSession
import app.screenclock.domain.focus.FocusStats
import app.screenclock.domain.focus.addFocusMillis
import app.screenclock.domain.focus.focusCreditMillis
import app.screenclock.domain.focus.normalizeFocusMinutes
import app.screenclock.domain.focus.pauseFocus
import app.screenclock.domain.focus.resumeFocus
import app.screenclock.domain.focus.settleFocus
import app.screenclock.domain.focus.skipFocus
import app.screenclock.domain.focus.startFocusSession
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** What the focus page draws. [known] turns true once the stored state has been read. */
data class FocusUi(
    val prefs: FocusPrefs = FocusPrefs(),
    val session: FocusSession? = null,
    val stats: FocusStats = FocusStats(),
    val known: Boolean = false,
)

sealed interface FocusEvent {
    /** A write failed. The page keeps working from memory; the state may not survive a restart. */
    data object PersistFailed : FocusEvent
}

/**
 * The focus timer. Memory is the source of truth after the first read: a started segment
 * lives as a [FocusSession], and each change is written behind it, in order. Time that was
 * actually focused is added to today's total when a focus segment ends, however it ends.
 */
class FocusController(
    private val scope: CoroutineScope,
    private val store: FocusStore,
    private val time: ClockTime,
    private val zone: ZoneId = ZoneId.systemDefault(),
) {
    private val uiState = MutableStateFlow(FocusUi())
    val state: StateFlow<FocusUi> = uiState.asStateFlow()

    private val eventState = MutableSharedFlow<FocusEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<FocusEvent> = eventState.asSharedFlow()

    private val writeLock = Mutex()
    private var settleJob: Job? = null

    init {
        scope.launch {
            val stored = store.data.first()
            uiState.value = FocusUi(stored.prefs, stored.session, stored.stats, known = true)
            settleDue()
            scheduleSettle()
        }
    }

    /** Picks a mode. Whatever was running ends first, so its focused time still counts. */
    fun selectMode(mode: FocusMode) {
        val current = uiState.value
        applyPrefs(current.prefs.copy(mode = mode))
    }

    fun setCustomMinutes(minutes: Int) {
        applyPrefs(uiState.value.prefs.copy(mode = FocusMode.Custom, customMinutes = normalizeFocusMinutes(minutes)))
    }

    /** Play and pause share one button: start, pause, resume, or start again after the end. */
    fun toggle() {
        val now = time.nowMillis()
        val current = uiState.value
        val next = when (val session = current.session) {
            null -> startFocusSession(current.prefs, now)
            else -> when (session.run) {
                is FocusRun.Running -> pauseFocus(session, now)
                is FocusRun.Paused -> resumeFocus(session, now)
                FocusRun.Done -> startFocusSession(current.prefs, now)
            }
        }
        // A pause that ran out turns into Done; its time still has to be counted once.
        val ranOut = current.session?.run is FocusRun.Running && next.run == FocusRun.Done
        val credit = if (ranOut) focusCreditMillis(current.session!!, now) else 0L
        commit(next, credit, dayOf(now))
        scheduleSettle()
    }

    /** Back to the full length of the chosen mode. Time already focused is kept in the total. */
    fun reset() {
        val now = time.nowMillis()
        val session = uiState.value.session ?: return
        val credit = if (session.run == FocusRun.Done) 0L else focusCreditMillis(session, now)
        commit(null, credit, dayOf(now))
        scheduleSettle()
    }

    /** Ends the segment right away. Only the part already focused counts. */
    fun skip() {
        val now = time.nowMillis()
        val session = uiState.value.session ?: return
        if (session.run == FocusRun.Done) return
        commit(skipFocus(session), focusCreditMillis(session, now), dayOf(now))
        scheduleSettle()
    }

    /** The wall clock moved or the app came back: finish anything whose end has passed. */
    fun refresh() {
        settleDue()
        scheduleSettle()
    }

    private fun applyPrefs(prefs: FocusPrefs) {
        val now = time.nowMillis()
        val current = uiState.value
        val session = current.session
        val credit = if (session != null && session.run != FocusRun.Done) focusCreditMillis(session, now) else 0L
        uiState.value = current.copy(
            prefs = prefs,
            session = null,
            stats = addFocusMillis(current.stats, dayOf(now), credit),
        )
        persist {
            store.savePrefs(prefs)
            store.saveSessionAndCredit(null, credit, dayOf(now))
        }
        scheduleSettle()
    }

    private fun commit(next: FocusSession?, creditMillis: Long, day: LocalDate) {
        val current = uiState.value
        uiState.value = current.copy(
            session = next,
            stats = addFocusMillis(current.stats, day, creditMillis),
        )
        persist { store.saveSessionAndCredit(next, creditMillis, day) }
    }

    private fun settleDue() {
        val now = time.nowMillis()
        val session = uiState.value.session ?: return
        val running = session.run as? FocusRun.Running ?: return
        val settled = settleFocus(session, now)
        if (settled == session) return
        // The day it ended on, not the day the app noticed.
        val day = Instant.ofEpochMilli(running.endEpochMillis).atZone(zone).toLocalDate()
        commit(settled, focusCreditMillis(session, running.endEpochMillis), day)
    }

    private fun scheduleSettle() {
        settleJob?.cancel()
        val running = uiState.value.session?.run as? FocusRun.Running ?: return
        settleJob = scope.launch {
            delay((running.endEpochMillis - time.nowMillis()).coerceAtLeast(0L))
            settleDue()
        }
    }

    private fun dayOf(epochMillis: Long): LocalDate = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

    private fun persist(block: suspend () -> Unit) {
        scope.launch {
            writeLock.withLock {
                try {
                    block()
                } catch (_: IOException) {
                    eventState.tryEmit(FocusEvent.PersistFailed)
                }
            }
        }
    }
}
