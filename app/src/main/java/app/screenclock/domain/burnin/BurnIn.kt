package app.screenclock.domain.burnin

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** How long the clock sits untouched before one screensaver round. */
const val IDLE_MIN_MILLIS: Long = 60_000L

const val IDLE_MAX_MILLIS: Long = 60L * 60L * 1000L

const val IDLE_DEFAULT_MILLIS: Long = 5L * 60L * 1000L

/** How long that one round lasts. */
const val RUN_MIN_MILLIS: Long = 10_000L

const val RUN_MAX_MILLIS: Long = 30L * 60L * 1000L

const val RUN_DEFAULT_MILLIS: Long = 2L * 60L * 1000L

const val RUN_STEP_MILLIS: Long = 10_000L

/** Radius of the resting drift. The length stays inside 1–2px. */
const val DRIFT_AMPLITUDE_PX: Float = 1.5f

const val DRIFT_PERIOD_MILLIS: Long = 60_000L

/** One lap of the screensaver path. */
const val WANDER_PERIOD_MILLIS: Long = 48_000L

enum class BurnInPhase {
    Off,
    Armed,
    Running,
    Spent,
}

/**
 * What a running round shows. [Drift] keeps the clock and wanders its digits; the others
 * play a looping video over the whole screen until a touch ends the round.
 */
enum class ScreensaverTheme(val storageKey: String) {
    Drift("drift"),
    Glitch("glitch"),
    Tunnel("tunnel"),
    Portrait("portrait"),
    ;

    val isVideo: Boolean get() = this != Drift

    companion object {
        /** Unknown or missing keys fall back to [Drift], so a downgrade never leaves a blank screen. */
        fun fromStorageKey(key: String?): ScreensaverTheme =
            entries.firstOrNull { it.storageKey == key } ?: Drift
    }
}

data class BurnInPreferences(
    val enabled: Boolean = false,
    val theme: ScreensaverTheme = ScreensaverTheme.Drift,
    val idleMillis: Long = IDLE_DEFAULT_MILLIS,
    val runMillis: Long = RUN_DEFAULT_MILLIS,
)

/**
 * Runtime saver session. [anchorMillis] is the monotonic start of the current phase.
 * [pagePaused] covers settings, focus, and ceremony. [hostHidden] covers leaving the app.
 * Either one blocks the screensaver. Time is not persisted: a new process starts armed.
 */
data class BurnInClock(
    val phase: BurnInPhase,
    val anchorMillis: Long,
    val pagePaused: Boolean = false,
    val hostHidden: Boolean = false,
) {
    val paused: Boolean get() = pagePaused || hostHidden
}

enum class BurnInMotion {
    Still,
    Drift,
    Wander,
}

data class BurnInOffset(
    val xPx: Float,
    val yPx: Float,
)

/** Idle snaps to a whole minute. Run snaps to 10 seconds. Both stay inside the closed range. */
fun burnInPreferences(
    enabled: Boolean,
    idleMillis: Long,
    runMillis: Long,
    theme: ScreensaverTheme = ScreensaverTheme.Drift,
): BurnInPreferences {
    return BurnInPreferences(
        enabled = enabled,
        theme = theme,
        idleMillis = normalizeIdleMillis(idleMillis),
        runMillis = normalizeRunMillis(runMillis),
    )
}

fun normalizeIdleMillis(value: Long): Long {
    val clamped = value.coerceIn(IDLE_MIN_MILLIS, IDLE_MAX_MILLIS)
    val minutes = ((clamped + 30_000L) / 60_000L).coerceIn(1L, 60L)
    return minutes * 60_000L
}

fun normalizeRunMillis(value: Long): Long {
    val clamped = value.coerceIn(RUN_MIN_MILLIS, RUN_MAX_MILLIS)
    val snapped = ((clamped + RUN_STEP_MILLIS / 2) / RUN_STEP_MILLIS) * RUN_STEP_MILLIS
    return snapped.coerceIn(RUN_MIN_MILLIS, RUN_MAX_MILLIS)
}

fun freshBurnInClock(
    enabled: Boolean,
    nowMillis: Long,
    pagePaused: Boolean = false,
    hostHidden: Boolean = false,
): BurnInClock {
    return BurnInClock(
        phase = if (enabled) BurnInPhase.Armed else BurnInPhase.Off,
        anchorMillis = nowMillis,
        pagePaused = pagePaused,
        hostHidden = hostHidden,
    )
}

/** Turning the feature off stops a running round. Turning it on while it is already on does not reset the timer. */
fun burnInSetEnabled(state: BurnInClock, enabled: Boolean, nowMillis: Long): BurnInClock {
    if (!enabled) {
        if (state.phase == BurnInPhase.Off) return state
        return state.copy(phase = BurnInPhase.Off, anchorMillis = nowMillis)
    }
    if (state.phase != BurnInPhase.Off) return state
    return state.copy(phase = BurnInPhase.Armed, anchorMillis = nowMillis)
}

/** A touch re-arms from Running or Spent. Off ignores it. Pause flags stay as they are. */
fun burnInTouch(state: BurnInClock, nowMillis: Long): BurnInClock {
    if (state.phase == BurnInPhase.Off) return state
    return state.copy(phase = BurnInPhase.Armed, anchorMillis = nowMillis)
}

fun burnInSetPagePaused(state: BurnInClock, paused: Boolean, nowMillis: Long): BurnInClock {
    if (state.pagePaused == paused) return state
    return applyHold(state, nowMillis, pagePaused = paused, hostHidden = state.hostHidden)
}

fun burnInSetHostHidden(state: BurnInClock, hidden: Boolean, nowMillis: Long): BurnInClock {
    if (state.hostHidden == hidden) return state
    return applyHold(state, nowMillis, pagePaused = state.pagePaused, hostHidden = hidden)
}

/**
 * Advance with a virtual clock.
 * Spent never starts another round. A backwards clock restarts the current wait instead of firing.
 */
fun burnInTick(
    state: BurnInClock,
    idleMillis: Long,
    runMillis: Long,
    nowMillis: Long,
): BurnInClock {
    if (state.paused || state.phase == BurnInPhase.Off || state.phase == BurnInPhase.Spent) return state
    if (nowMillis < state.anchorMillis) return state.copy(anchorMillis = nowMillis)
    val idle = normalizeIdleMillis(idleMillis)
    val run = normalizeRunMillis(runMillis)
    val elapsed = nowMillis - state.anchorMillis
    return when (state.phase) {
        BurnInPhase.Armed ->
            if (elapsed >= idle) state.copy(phase = BurnInPhase.Running, anchorMillis = nowMillis) else state
        BurnInPhase.Running ->
            if (elapsed >= run) state.copy(phase = BurnInPhase.Spent, anchorMillis = nowMillis) else state
        BurnInPhase.Off, BurnInPhase.Spent -> state
    }
}

/** Milliseconds until the next edge, or null when no edge is pending. Zero means the edge is due now. */
fun millisUntilBurnInEdge(
    state: BurnInClock,
    idleMillis: Long,
    runMillis: Long,
    nowMillis: Long,
): Long? {
    if (state.paused || state.phase == BurnInPhase.Off || state.phase == BurnInPhase.Spent) return null
    if (nowMillis < state.anchorMillis) return 0L
    val limit = when (state.phase) {
        BurnInPhase.Armed -> normalizeIdleMillis(idleMillis)
        BurnInPhase.Running -> normalizeRunMillis(runMillis)
        BurnInPhase.Off, BurnInPhase.Spent -> return null
    }
    return (limit - (nowMillis - state.anchorMillis)).coerceAtLeast(0L)
}

fun burnInMotion(phase: BurnInPhase, paused: Boolean): BurnInMotion {
    if (paused || phase == BurnInPhase.Off) return BurnInMotion.Still
    if (phase == BurnInPhase.Running) return BurnInMotion.Wander
    return BurnInMotion.Drift
}

/** Resting clock: a 1.5px circle. [nowMillis] is monotonic, so a touch does not jump the circle. */
fun driftOffset(nowMillis: Long): BurnInOffset {
    val period = DRIFT_PERIOD_MILLIS
    val angle = (nowMillis.mod(period).toDouble() / period.toDouble()) * 2.0 * PI
    return BurnInOffset(
        xPx = (DRIFT_AMPLITUDE_PX * cos(angle)).toFloat(),
        yPx = (DRIFT_AMPLITUDE_PX * sin(angle)).toFloat(),
    )
}

/**
 * Screensaver path inside the box the caller measured.
 * Starts at the center. Both axes stay within the given travel, which is already inside the safe area.
 */
fun wanderOffset(elapsedMillis: Long, travelXPx: Float, travelYPx: Float): BurnInOffset {
    val travelX = travelXPx.coerceAtLeast(0f)
    val travelY = travelYPx.coerceAtLeast(0f)
    if (elapsedMillis <= 0L || (travelX == 0f && travelY == 0f)) return BurnInOffset(0f, 0f)
    val period = WANDER_PERIOD_MILLIS
    val angle = (elapsedMillis.mod(period).toDouble() / period.toDouble()) * 2.0 * PI
    return BurnInOffset(
        xPx = (travelX * sin(angle)).toFloat(),
        yPx = (travelY * sin(2.0 * angle)).toFloat(),
    )
}

private fun applyHold(
    state: BurnInClock,
    nowMillis: Long,
    pagePaused: Boolean,
    hostHidden: Boolean,
): BurnInClock {
    val wasPaused = state.paused
    var phase = state.phase
    var anchor = state.anchorMillis
    val nowPaused = pagePaused || hostHidden
    if (nowPaused && phase == BurnInPhase.Running) {
        phase = BurnInPhase.Armed
    }
    if (wasPaused && !nowPaused && (phase == BurnInPhase.Armed || phase == BurnInPhase.Running)) {
        phase = BurnInPhase.Armed
        anchor = nowMillis
    }
    return BurnInClock(
        phase = phase,
        anchorMillis = anchor,
        pagePaused = pagePaused,
        hostHidden = hostHidden,
    )
}
