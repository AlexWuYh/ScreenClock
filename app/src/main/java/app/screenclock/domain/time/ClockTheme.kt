package app.screenclock.domain.time

/** Which clock drawing is on the home screen. Separate from [ColorwayId]. */
enum class ClockThemeId {
    Flip,
    Tube,
    ;

    companion object {
        fun fromStored(value: String?): ClockThemeId {
            return entries.firstOrNull { it.name == value } ?: Flip
        }
    }
}

/** Flip finish. v1 only ships ink black, so a future light finish does not rewrite the fold. */
enum class ColorwayId {
    InkBlack,
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
