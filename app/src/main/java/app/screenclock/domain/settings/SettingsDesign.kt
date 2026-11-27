package app.screenclock.domain.settings

import app.screenclock.domain.burnin.IDLE_MAX_MILLIS
import app.screenclock.domain.burnin.IDLE_MIN_MILLIS
import app.screenclock.domain.burnin.RUN_MAX_MILLIS
import app.screenclock.domain.burnin.RUN_MIN_MILLIS
import app.screenclock.domain.burnin.normalizeIdleMillis
import app.screenclock.domain.burnin.normalizeRunMillis
import app.screenclock.domain.occasion.CeremonyStyle

/**
 * The one-line value shown next to each settings entry on the home list. Every name comes
 * from resources; only the glue (separators, counts, 已关闭 / 已开启) is decided here, so the
 * wording stays lockable by a unit test without duplicating labels.
 */
data class SettingsSummaryInput(
    val themeName: String,
    val showsColorway: Boolean,
    val colorwayName: String,
    val hourModeName: String,
    val showsDate: Boolean,
    val visibleDateParts: Int,
    val datePositionAbove: Boolean,
    val digitSizeName: String,
    val weatherEnabled: Boolean,
    val weatherPlace: String?,
    val burnInEnabled: Boolean,
    val screensaverName: String,
    val idleMinutes: Int,
    val nightDimEnabled: Boolean,
    val nightStartHour: Int,
    val nightEndHour: Int,
    val nightWarmEnabled: Boolean,
    val chargeKeepAwake: Boolean,
    val ceremonyStyle: CeremonyStyle,
    val autoPlaySeconds: Int,
    val version: String,
)

internal fun hourText(hour: Int): String = "${((hour % 24) + 24) % 24}:00"

fun themeSummary(input: SettingsSummaryInput): String =
    if (input.showsColorway) "${input.themeName} · ${input.colorwayName}" else input.themeName

fun displaySummary(input: SettingsSummaryInput): String = buildString {
    append(input.hourModeName)
    append(" · ")
    if (input.showsDate) {
        append("日期含 ")
        append(input.visibleDateParts)
        append(" 项")
    } else {
        append("不显示日期")
    }
}

fun layoutSummary(input: SettingsSummaryInput): String {
    val position = if (input.datePositionAbove) "日期在上" else "日期在下"
    return "$position · 数字${input.digitSizeName}"
}

fun weatherSummary(input: SettingsSummaryInput): String {
    if (!input.weatherEnabled) return "已关闭"
    val place = input.weatherPlace
    return if (place.isNullOrBlank()) "已开启 · 未选城市" else "$place · 已开启"
}

fun burnInSummary(input: SettingsSummaryInput): String {
    if (!input.burnInEnabled) return "已关闭"
    return "${input.screensaverName} · ${input.idleMinutes} 分钟后"
}

fun nightSummary(input: SettingsSummaryInput): String {
    if (!input.nightDimEnabled) return "已关闭"
    return buildString {
        append(hourText(input.nightStartHour))
        append(" – ")
        append(hourText(input.nightEndHour))
        if (input.nightWarmEnabled) append(" · 暖色")
    }
}

fun occasionSettingsSummary(input: SettingsSummaryInput): String {
    val style = if (input.ceremonyStyle == CeremonyStyle.Card) "卡片" else "沉浸"
    return "$style · 到点后停留 ${input.autoPlaySeconds} 秒"
}

fun aboutSummary(input: SettingsSummaryInput): String = "版本 ${input.version}"

// ───────────────────────── burn-in stepping ─────────────────────────

/** Idle counts in whole minutes; the design's stepper adds or removes one minute. */
const val IDLE_STEP_MILLIS: Long = 60_000L

/** Quick picks under the idle dial, in minutes. */
val IDLE_PRESET_MINUTES: List<Int> = listOf(1, 5, 10, 15, 30, 60)

/** Quick picks under the run dial, in seconds. */
val RUN_PRESET_SECONDS: List<Int> = listOf(30, 60, 120, 300, 600, 1_800)

/**
 * The run duration steps coarsely as it grows: ten seconds below two minutes, thirty below
 * ten, then whole minutes. Stepping never leaves the allowed range.
 */
fun runStepMillis(valueMillis: Long): Long {
    val current = normalizeRunMillis(valueMillis)
    return when {
        current < 2L * 60_000L -> 10_000L
        current < 10L * 60_000L -> 30_000L
        else -> 60_000L
    }
}

/** One step down; the step size is read from the value being left. */
fun stepIdleMillis(valueMillis: Long, direction: Int): Long {
    val current = normalizeIdleMillis(valueMillis)
    val next = current + direction * IDLE_STEP_MILLIS
    return next.coerceIn(IDLE_MIN_MILLIS, IDLE_MAX_MILLIS)
}

/** One step up or down, using the step size of the value before the move. */
fun stepRunMillis(valueMillis: Long, direction: Int): Long {
    val current = normalizeRunMillis(valueMillis)
    val step = runStepMillis(current)
    val next = current + direction * step
    return next.coerceIn(RUN_MIN_MILLIS, RUN_MAX_MILLIS)
}

/** A preset is always inside the range, so it can be applied directly. */
fun idleFromMinutes(minutes: Int): Long =
    normalizeIdleMillis(minutes.coerceAtLeast(1).toLong() * 60_000L)

fun runFromSeconds(seconds: Int): Long = normalizeRunMillis(seconds.coerceAtLeast(1).toLong() * 1_000L)

/**
 * How much width the "one round" bar gives each phase. A square root keeps a five-minute
 * idle and a half-hour run both visible, so the bar compares without one eating the other.
 */
fun rhythmWeights(idleMinutes: Int, runMinutes: Float): Pair<Float, Float> {
    val idle = 0.6f + kotlin.math.sqrt(idleMinutes.coerceAtLeast(1).toFloat())
    val run = 0.6f + kotlin.math.sqrt(runMinutes.coerceAtLeast(0.1f))
    return idle to run
}

/**
 * The dark stretch of the 24-hour night bar, as hour fractions. A window that crosses
 * midnight becomes two pieces; equal hours are empty, exactly like [nightDimActive].
 */
fun nightBarSpans(startHour: Int, endHour: Int): List<ClosedFloatingPointRange<Float>> {
    val start = ((startHour % 24) + 24) % 24
    val end = ((endHour % 24) + 24) % 24
    if (start == end) return emptyList()
    val a = start / 24f
    val b = end / 24f
    return if (start < end) listOf(a..b) else listOf(a..1f, 0f..b)
}

// ───────────────────────── route + layout geometry ─────────────────────────

/** The pages the settings shell can show. [Home] is the grouped index. */
enum class SettingsPage {
    Home,
    Theme,
    Display,
    Layout,
    Weather,
    BurnIn,
    Night,
    Occasion,
    About,
}

/** Which pages sit under which home heading, in the design's order. */
val SettingsGroups: List<Pair<String, List<SettingsPage>>> = listOf(
    "钟面" to listOf(SettingsPage.Theme, SettingsPage.Display, SettingsPage.Layout, SettingsPage.Weather),
    "保护与夜间" to listOf(SettingsPage.BurnIn, SettingsPage.Night),
    "其他" to listOf(SettingsPage.Occasion, SettingsPage.About),
)

/** Where the window width puts the shell: a stacked index, or a list beside its detail. */
enum class SettingsWidth {
    Compact,
    Wide,
    WideLarge,
}

const val SETTINGS_WIDE_MIN_DP: Float = 600f
const val SETTINGS_WIDE_LARGE_DP: Float = 900f
const val SETTINGS_LIST_WIDTH_DP: Float = 300f
const val SETTINGS_LIST_WIDTH_LARGE_DP: Float = 360f
const val SETTINGS_DETAIL_MAX_DP: Float = 640f

/** A short landscape window hides the preview block so the controls stay reachable. */
const val SETTINGS_SHORT_MAX_DP: Float = 500f

fun settingsWidth(widthDp: Float): SettingsWidth = when {
    widthDp >= SETTINGS_WIDE_LARGE_DP -> SettingsWidth.WideLarge
    widthDp >= SETTINGS_WIDE_MIN_DP -> SettingsWidth.Wide
    else -> SettingsWidth.Compact
}

fun settingsListWidth(widthDp: Float): Float = when (settingsWidth(widthDp)) {
    SettingsWidth.Compact -> 0f
    SettingsWidth.Wide -> SETTINGS_LIST_WIDTH_DP
    SettingsWidth.WideLarge -> SETTINGS_LIST_WIDTH_LARGE_DP
}

fun settingsDetailWidth(windowWidthDp: Float): Float =
    (windowWidthDp - settingsListWidth(windowWidthDp)).coerceAtLeast(0f)

fun settingsIsShort(heightDp: Float): Boolean = heightDp < SETTINGS_SHORT_MAX_DP

/** Three previews per row only when the column has room; never a sliver. */
fun settingsThemeColumns(detailWidthDp: Float): Int = when {
    detailWidthDp >= SETTINGS_THREE_COLUMN_MIN_DP -> 3
    detailWidthDp >= 360f -> 2
    else -> 1
}

/** Three columns need at least this much room, which is what decides the grid above. */
const val SETTINGS_THREE_COLUMN_MIN_DP: Float = 560f

/** Preview height still follows the tile width, but a short window keeps it modest. */
fun settingsPreviewHeight(tileWidthDp: Float, short: Boolean): Float {
    val ratio = tileWidthDp * if (short) 0.5f else 0.58f
    return if (short) ratio.coerceIn(96f, 120f) else ratio.coerceIn(120f, 260f)
}

/** True when a page should scroll to and highlight a specific row (the charge hint's jump). */
data class SettingsJump(val page: SettingsPage, val key: String)
