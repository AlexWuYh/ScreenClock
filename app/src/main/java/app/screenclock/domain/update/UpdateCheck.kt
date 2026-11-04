package app.screenclock.domain.update

/** Result of one manual update check. */
sealed interface UpdateResult {
    data object UpToDate : UpdateResult
    data class Available(val version: String) : UpdateResult
    data object Failed : UpdateResult
}

/** Page opened for a new version. Built from the tag, never taken from the response. */
const val RELEASES_PAGE: String = "https://github.com/AlexWuYh/ScreenClock/releases"

fun releasePageUrl(version: String): String = "$RELEASES_PAGE/tag/v$version"

/** `v1.2.3` or `1.2.3` to its numeric parts. Anything else (pre-release suffixes, words) is not a version. */
fun parseVersion(raw: String): List<Int>? {
    val text = raw.trim().removePrefix("v").removePrefix("V")
    if (text.isEmpty()) return null
    val parts = text.split('.')
    if (parts.size !in 1..4) return null
    return parts.map { part ->
        if (part.isEmpty() || part.length > 6 || !part.all { it in '0'..'9' }) return null
        part.toInt()
    }
}

/** True when [latest] is strictly newer than [current]. Unreadable versions never count as newer. */
fun isNewerVersion(latest: String, current: String): Boolean {
    val a = parseVersion(latest) ?: return false
    val b = parseVersion(current) ?: return false
    val size = maxOf(a.size, b.size)
    for (i in 0 until size) {
        val x = a.getOrElse(i) { 0 }
        val y = b.getOrElse(i) { 0 }
        if (x != y) return x > y
    }
    return false
}

/** Normalised version text ("0.6.0") of a release tag, or null when the tag is not a plain version. */
fun versionFromTag(tag: String): String? {
    val parts = parseVersion(tag) ?: return null
    return parts.joinToString(".")
}
