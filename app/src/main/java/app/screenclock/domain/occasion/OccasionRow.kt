package app.screenclock.domain.occasion

import java.time.LocalDate

data class OccasionRow(
    val id: String,
    val title: String,
    val icon: OccasionIcon,
    val detail: String,
    val pinned: Boolean,
    val canPin: Boolean,
    val canOpen: Boolean,
)

fun occasionRows(items: List<Occasion>, today: LocalDate, calendar: LunarCalendar): List<OccasionRow> {
    return items.map { item ->
        val next = nextOccurrence(today, item, calendar)
        OccasionRow(
            id = item.id,
            title = item.title,
            icon = item.icon,
            detail = previewText(next),
            pinned = item.pinned,
            canPin = canPin(next.status),
            canOpen = next.date != null && canPin(next.status),
        )
    }
}
