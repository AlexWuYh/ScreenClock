package app.screenclock.domain.occasion

import java.time.LocalDate

/** One countdown in a run: the item and the date its countdown aims at. */
data class CeremonyStop(val id: String, val occurrence: LocalDate)

/**
 * The countdowns of a run, nearest target date first. Items that cannot open (passed,
 * no date) are left out. Equal dates keep their list order.
 */
fun ceremonyPlaylist(items: List<Occasion>, today: LocalDate, calendar: LunarCalendar): List<CeremonyStop> =
    items.mapNotNull { item ->
        val next = nextOccurrence(today, item, calendar)
        val date = next.date
        if (date != null && canPin(next.status)) CeremonyStop(item.id, date) else null
    }.sortedBy { it.occurrence }

/** The stop after [index], or null after the last: a run ends where the dates end. */
fun nextStopIndex(index: Int, size: Int): Int? = if (index + 1 < size) index + 1 else null
