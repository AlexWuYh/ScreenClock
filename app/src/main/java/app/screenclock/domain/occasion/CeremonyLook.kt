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

enum class CeremonyTone {
    FollowSystem,
    Light,
    Dark,
    ;

    companion object {
        fun fromStored(value: String?): CeremonyTone = entries.firstOrNull { it.name == value } ?: FollowSystem
    }
}

data class CeremonyLook(
    val style: CeremonyStyle = CeremonyStyle.Card,
    val tone: CeremonyTone = CeremonyTone.FollowSystem,
)

fun ceremonyIsDark(tone: CeremonyTone, systemDark: Boolean): Boolean = when (tone) {
    CeremonyTone.FollowSystem -> systemDark
    CeremonyTone.Light -> false
    CeremonyTone.Dark -> true
}

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
