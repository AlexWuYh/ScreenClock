package app.screenclock.domain.occasion

import java.time.LocalDate

const val OCCASION_LIMIT: Int = 30
const val OCCASION_TITLE_MAX: Int = 20
const val OCCASION_MESSAGE_MAX: Int = 20
const val OCCASION_MESSAGE_DEFAULT: String = "到了"
const val OCCASION_SCAN_YEARS: Int = 20

enum class OccasionKind {
    SolarOnce,
    SolarYearly,
    LunarYearly,
}

enum class OccasionStatus {
    Future,
    Today,
    Passed,
    NotFound,
}

data class Occasion(
    val id: String,
    val title: String,
    val kind: OccasionKind,
    val year: Int?,
    val month: Int,
    val day: Int,
    val leap: Boolean,
    val message: String,
    val pinned: Boolean,
    val icon: OccasionIcon = OccasionIcon.Default,
)

data class OccasionDraft(
    val title: String,
    val kind: OccasionKind,
    val year: Int,
    val month: Int,
    val day: Int,
    val leap: Boolean,
    val message: String,
    val icon: OccasionIcon = OccasionIcon.Default,
)

data class NextOccurrence(
    val status: OccasionStatus,
    val date: LocalDate? = null,
    val skipped: Boolean = false,
    val missingLeapThisYear: Boolean = false,
)

/** Blank or over-long titles cannot be saved. Length is counted in characters. */
fun normalizeTitle(raw: String): String? {
    val title = raw.trim()
    if (title.isEmpty() || title.length > OCCASION_TITLE_MAX) return null
    return title
}

/** Empty becomes 「到了」. Longer text keeps the first 20 characters. */
fun normalizeMessage(raw: String): String {
    val message = raw.trim()
    if (message.isEmpty()) return OCCASION_MESSAGE_DEFAULT
    return message.take(OCCASION_MESSAGE_MAX)
}

fun canKeep(status: OccasionStatus): Boolean {
    return status == OccasionStatus.Future ||
        status == OccasionStatus.Today ||
        status == OccasionStatus.Passed
}

fun canPin(status: OccasionStatus): Boolean {
    return status == OccasionStatus.Future || status == OccasionStatus.Today
}

fun addOccasionItem(items: List<Occasion>, item: Occasion): List<Occasion>? {
    if (items.size >= OCCASION_LIMIT) return null
    if (item.id.isBlank() || items.any { it.id == item.id }) return null
    return items + item
}

fun deleteOccasionItem(items: List<Occasion>, id: String): List<Occasion> {
    return items.filterNot { it.id == id }
}

/** Pinning one item clears every other pin. A passed or missing item stays unpinned. */
fun pinOccasion(
    items: List<Occasion>,
    id: String,
    today: LocalDate,
    calendar: LunarCalendar,
): List<Occasion> {
    val target = items.find { it.id == id } ?: return items
    if (!canPin(nextOccurrence(today, target, calendar).status)) return items
    return items.map { it.copy(pinned = it.id == id) }
}

fun unpinOccasion(items: List<Occasion>, id: String): List<Occasion> {
    return items.map { if (it.id == id) it.copy(pinned = false) else it }
}

/** A pinned one-time date that has passed, or a date that cannot be found, loses the pin. */
fun refreshPins(
    items: List<Occasion>,
    today: LocalDate,
    calendar: LunarCalendar,
): List<Occasion> {
    return items.map { item ->
        if (item.pinned && !canPin(nextOccurrence(today, item, calendar).status)) {
            item.copy(pinned = false)
        } else {
            item
        }
    }
}
