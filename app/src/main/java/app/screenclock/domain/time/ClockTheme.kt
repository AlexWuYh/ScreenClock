package app.screenclock.domain.time

/** Which clock drawing is on the home screen. Separate from [ColorwayId]. */
enum class ClockThemeId {
    /** Two flap cards, the two-stage fold. */
    Flip,
    /** Glass tubes that glow once per minute. */
    Tube,
    /** One bold digital line with a quiet colon. */
    Digital,
    /** A desk-clock panel with seven-segment digits. */
    Lcd,
    /** A dial with hour and minute hands. No second hand on the home screen. */
    Analog,
    /** Outlined digits over a drawn moon. */
    Moon,
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
    val theme: ClockThemeId = ClockThemeId.Flip,
    val colorway: ColorwayId = ColorwayId.InkBlack,
)
