package app.screenclock.domain.occasion

import java.time.LocalDate

/** The two designs of the countdown page. */
enum class CeremonyStyle {
    /** A glass card on a soft gradient. */
    Card,

    /** Edge-to-edge type with nothing around it. */
    Immersive,
    ;

    companion object {
        fun fromStored(value: String?): CeremonyStyle = entries.firstOrNull { it.name == value } ?: Card
    }
}

data class CeremonyLook(
    val style: CeremonyStyle = CeremonyStyle.Card,
    /** Seconds a finished countdown stays up before auto play moves on. */
    val autoPlaySeconds: Int = AUTO_PLAY_DEFAULT_SECONDS,
)

const val AUTO_PLAY_DEFAULT_SECONDS: Int = 10

/** The stay times offered in settings. */
val AUTO_PLAY_SECONDS_CHOICES: List<Int> = listOf(5, 10, 20, 30, 60)

/** A stored value that is not one of the choices falls back to the default. */
fun normalizeAutoPlaySeconds(value: Int?): Int =
    if (value != null && value in AUTO_PLAY_SECONDS_CHOICES) value else AUTO_PLAY_DEFAULT_SECONDS

/** The quick-pick buttons of the editor. */
enum class QuickDate {
    InWeek,
    InMonth,
    InHundredDays,
    NextYear,
}

fun quickDate(today: LocalDate, pick: QuickDate): LocalDate = when (pick) {
    QuickDate.InWeek -> today.plusDays(7)
    QuickDate.InMonth -> today.plusDays(30)
    QuickDate.InHundredDays -> today.plusDays(100)
    // plusYears keeps 29 Feb valid by landing on 28 Feb.
    QuickDate.NextYear -> today.plusYears(1)
}
