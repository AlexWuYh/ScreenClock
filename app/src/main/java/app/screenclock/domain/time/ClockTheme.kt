package app.screenclock.domain.time

/** Which clock drawing is on the home screen. Separate from [ColorwayId]. */
enum class ClockThemeId {
    /** Two flap cards, the two-stage fold. */
    Flip,
    /** Glass tubes that glow once per minute. */
    Tube,
    /** Thin digits and a quiet colon over a dusk lake. */
    Digital,
    /** A numbered dial with thin hands over snow ridges. */
    Analog,
    /** Thin glowing digits over the night earth. */
    Earth,
    /** Soft digits over flowing pastel waves. */
    Fluid,
    /** Neon gradient digits on a synthwave grid. */
    Neon,
    ;

    companion object {
        fun fromStored(value: String?): ClockThemeId {
            return entries.firstOrNull { it.name == value } ?: Flip
        }
    }
}

/** Flip finish. A new finish is colours only; the fold itself never changes. */
enum class ColorwayId {
    /** Dark cards, white digits. */
    InkBlack,
    /** Light cards, dark digits:「素白」. */
    SnowWhite,
    ;

    companion object {
        fun fromStored(value: String?): ColorwayId {
            return entries.firstOrNull { it.name == value } ?: InkBlack
        }
    }
}

data class ClockPreferences(
    val hourMode: HourMode = HourMode.FollowSystem,
    val showDate: Boolean = true,
    val showYear: Boolean = true,
    val showWeekday: Boolean = true,
    val showLunarDate: Boolean = true,
    val showHoliday: Boolean = true,
    val theme: ClockThemeId = ClockThemeId.Flip,
    val colorway: ColorwayId = ColorwayId.InkBlack,
)
