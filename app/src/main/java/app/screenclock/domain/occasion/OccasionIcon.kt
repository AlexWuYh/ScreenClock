package app.screenclock.domain.occasion

/** The picture shown beside an occasion's title. Drawn in the app, never an emoji font. */
enum class OccasionIcon {
    Cake,
    Heart,
    Gift,
    Plane,
    Cap,
    Ring,
    Star,
    Flag,
    ;

    companion object {
        /** What an occasion saved before icons existed shows. */
        val Default: OccasionIcon = Flag

        fun fromStored(value: String?): OccasionIcon {
            return entries.firstOrNull { it.name == value } ?: Default
        }
    }
}
