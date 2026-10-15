package app.screenclock.domain.time

/**
 * The line under the hour and minute groups.
 * Both themes use this, so a weather or occasion slot is present or absent together.
 * Blank text counts as absent. No empty placeholder.
 */
fun footerLine(weatherLine: String?, occasionLine: String?): String? {
    val weather = weatherLine?.takeIf { it.isNotBlank() }
    val occasion = occasionLine?.takeIf { it.isNotBlank() }
    return when {
        weather != null && occasion != null -> "$weather · $occasion"
        else -> weather ?: occasion
    }
}

/** TalkBack string for the whole clock. Blank slots are left out. */
fun clockContentDescription(
    dateLine: String?,
    spokenTime: String,
    weatherLine: String?,
    occasionLine: String? = null,
): String {
    return listOfNotNull(
        dateLine?.takeIf { it.isNotBlank() },
        spokenTime,
        weatherLine?.takeIf { it.isNotBlank() },
        occasionLine?.takeIf { it.isNotBlank() },
    ).joinToString("，")
}
