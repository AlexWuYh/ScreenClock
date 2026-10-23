package app.screenclock.domain.focus

import java.time.LocalDate

const val FOCUS_MIN_MINUTES: Int = 1
const val FOCUS_MAX_MINUTES: Int = 180
const val FOCUS_DEFAULT_CUSTOM_MINUTES: Int = 30

private const val MINUTE_MILLIS: Long = 60_000L

/** What a segment is for. Focus time counts toward the day's total; breaks do not. */
enum class FocusMode(val minutes: Int?) {
    Focus(25),
    ShortBreak(5),
    LongBreak(15),
    Custom(null),
    ;

    companion object {
        fun fromStored(value: String?): FocusMode = entries.firstOrNull { it.name == value } ?: Focus
    }
}

/** The mode picked on the page, and the minutes behind 「自定义」. */
data class FocusPrefs(
    val mode: FocusMode = FocusMode.Focus,
    val customMinutes: Int = FOCUS_DEFAULT_CUSTOM_MINUTES,
)

fun normalizeFocusMinutes(minutes: Int): Int = minutes.coerceIn(FOCUS_MIN_MINUTES, FOCUS_MAX_MINUTES)

fun FocusPrefs.minutes(): Int = mode.minutes ?: normalizeFocusMinutes(customMinutes)

fun FocusPrefs.totalMillis(): Long = minutes().toLong() * MINUTE_MILLIS

/** Where a started segment stands. Running keeps an end moment, Paused keeps what is left. */
sealed interface FocusRun {
    data class Running(val endEpochMillis: Long) : FocusRun
    data class Paused(val leftMillis: Long) : FocusRun
    data object Done : FocusRun
}

/** A started segment. Before the first press of play there is no session, only [FocusPrefs]. */
data class FocusSession(
    val mode: FocusMode,
    val totalMillis: Long,
    val run: FocusRun,
)

fun startFocusSession(prefs: FocusPrefs, nowEpochMillis: Long): FocusSession {
    val total = prefs.totalMillis()
    return FocusSession(prefs.mode, total, FocusRun.Running(nowEpochMillis + total))
}

fun FocusSession.remainingMillis(nowEpochMillis: Long): Long = when (val r = run) {
    is FocusRun.Running -> (r.endEpochMillis - nowEpochMillis).coerceIn(0L, totalMillis)
    is FocusRun.Paused -> r.leftMillis.coerceIn(0L, totalMillis)
    FocusRun.Done -> 0L
}

/** How much of the segment has gone by. A finished one counts as whole. */
fun FocusSession.elapsedMillis(nowEpochMillis: Long): Long =
    if (run == FocusRun.Done) totalMillis else (totalMillis - remainingMillis(nowEpochMillis)).coerceIn(0L, totalMillis)

/** Pausing keeps the remainder. A segment that has already run out is finished instead. */
fun pauseFocus(session: FocusSession, nowEpochMillis: Long): FocusSession {
    if (session.run !is FocusRun.Running) return session
    val left = session.remainingMillis(nowEpochMillis)
    return session.copy(run = if (left <= 0L) FocusRun.Done else FocusRun.Paused(left))
}

/** Resuming sets a fresh end moment from what was left. */
fun resumeFocus(session: FocusSession, nowEpochMillis: Long): FocusSession {
    val paused = session.run as? FocusRun.Paused ?: return session
    return if (paused.leftMillis <= 0L) {
        session.copy(run = FocusRun.Done)
    } else {
        session.copy(run = FocusRun.Running(nowEpochMillis + paused.leftMillis))
    }
}

/** A running segment whose end has passed becomes Done. Anything else is returned unchanged. */
fun settleFocus(session: FocusSession, nowEpochMillis: Long): FocusSession {
    val running = session.run as? FocusRun.Running ?: return session
    return if (running.endEpochMillis <= nowEpochMillis) session.copy(run = FocusRun.Done) else session
}

/** Pressing skip ends the segment now, so only the elapsed part counts. */
fun skipFocus(session: FocusSession): FocusSession = session.copy(run = FocusRun.Done)

/** Focus time that ending [session] at [nowEpochMillis] adds to today. Breaks add nothing. */
fun focusCreditMillis(session: FocusSession, nowEpochMillis: Long): Long =
    if (session.mode == FocusMode.Focus) session.elapsedMillis(nowEpochMillis) else 0L

// ───────────────────────── today's total ─────────────────────────

/** Focused time on one calendar day. A different day reads as zero. */
data class FocusStats(val date: LocalDate? = null, val millis: Long = 0L)

fun todayFocusMillis(stats: FocusStats, today: LocalDate): Long =
    if (stats.date == today) stats.millis.coerceAtLeast(0L) else 0L

fun addFocusMillis(stats: FocusStats, today: LocalDate, addMillis: Long): FocusStats {
    if (addMillis <= 0L) return stats
    return FocusStats(today, todayFocusMillis(stats, today) + addMillis)
}

/** 「45分」「2小时」「2小时45分」. Under a minute is 「0分」. */
fun formatFocusTotal(millis: Long): String {
    val minutes = (millis.coerceAtLeast(0L) / MINUTE_MILLIS).toInt()
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours == 0 -> "${rest}分"
        rest == 0 -> "${hours}小时"
        else -> "${hours}小时${rest}分"
    }
}

// ───────────────────────── what the page shows ─────────────────────────

/** 「25:00」, or 「1:30:00」 from an hour up. Seconds round up so a minute shows until it is gone. */
fun focusClockText(remainingMillis: Long): String {
    val total = (remainingMillis.coerceAtLeast(0L) + 999L) / 1000L
    val hours = total / 3600L
    val minutes = (total % 3600L) / 60L
    val seconds = total % 60L
    val mm = minutes.toString().padStart(2, '0')
    val ss = seconds.toString().padStart(2, '0')
    return if (hours >= 1L) "$hours:$mm:$ss" else "$mm:$ss"
}

/** The share of the ring already used, 0 when nothing has started. */
fun focusProgress(session: FocusSession?, nowEpochMillis: Long): Float {
    if (session == null || session.totalMillis <= 0L) return 0f
    return (session.elapsedMillis(nowEpochMillis).toFloat() / session.totalMillis.toFloat()).coerceIn(0f, 1f)
}

/** Milliseconds until the displayed second changes, or null when nothing is counting. */
fun millisUntilFocusDigitChange(session: FocusSession?, nowEpochMillis: Long): Long? {
    val running = session?.run as? FocusRun.Running ?: return null
    val remaining = running.endEpochMillis - nowEpochMillis
    if (remaining <= 0L) return null
    val into = remaining % 1000L
    return if (into == 0L) 1000L else into
}

fun focusSpoken(session: FocusSession?, prefs: FocusPrefs, nowEpochMillis: Long): String {
    if (session == null) return "准备开始，${prefs.minutes()}分钟"
    if (session.run == FocusRun.Done) return "时间到"
    val remaining = spokenDuration(session.remainingMillis(nowEpochMillis))
    return if (session.run is FocusRun.Paused) "已暂停，剩余$remaining" else "剩余$remaining"
}

private fun spokenDuration(millis: Long): String {
    val total = (millis.coerceAtLeast(0L) + 999L) / 1000L
    val hours = total / 3600L
    val minutes = (total % 3600L) / 60L
    val seconds = total % 60L
    return if (hours >= 1L) "${hours}小时${minutes}分${seconds}秒" else "${minutes}分${seconds}秒"
}

/** The screen stays on while a segment counts down or shows its end; a paused one may sleep. */
fun focusKeepsScreen(session: FocusSession?): Boolean = when (session?.run) {
    is FocusRun.Running, FocusRun.Done -> true
    is FocusRun.Paused, null -> false
}
