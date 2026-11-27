package app.screenclock.domain.appearance

/**
 * The one light/dark choice of the app. It covers the settings pages, the occasion
 * list and editor, the countdown page and the focus page. Clock themes keep their own
 * colours and are never inverted.
 *
 * The stored values deliberately match the old per-countdown "tone" setting, so an
 * existing install reads its value here without a migration step.
 */
enum class AppearanceTone {
    FollowSystem,
    Light,
    Dark,
    ;

    companion object {
        /** Missing or unknown values follow the system, like the setting did before. */
        fun fromStored(value: String?): AppearanceTone = entries.firstOrNull { it.name == value } ?: FollowSystem
    }
}

/** Resolves the stored choice against the system's current light/dark answer. */
fun appearanceIsDark(tone: AppearanceTone, systemDark: Boolean): Boolean = when (tone) {
    AppearanceTone.FollowSystem -> systemDark
    AppearanceTone.Light -> false
    AppearanceTone.Dark -> true
}

/** The order the corner button steps through: follow → light → dark → follow. */
fun nextAppearanceTone(tone: AppearanceTone): AppearanceTone = when (tone) {
    AppearanceTone.FollowSystem -> AppearanceTone.Light
    AppearanceTone.Light -> AppearanceTone.Dark
    AppearanceTone.Dark -> AppearanceTone.FollowSystem
}
