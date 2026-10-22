package app.screenclock.domain.weather

import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt

/** One saved place. The label is what the settings page shows. */
data class SavedCity(
    val label: String,
    val latitude: Double,
    val longitude: Double,
)

/** One row from city search, before the user picks it. */
data class CityCandidate(
    val label: String,
    val latitude: Double,
    val longitude: Double,
)

/**
 * Last forecast kept on the device.
 * [code] is the WMO code, or null when the response had no code.
 * High and low are both present or both absent.
 */
data class WeatherCache(
    val temperatureC: Int,
    val code: Int?,
    val highC: Int?,
    val lowC: Int?,
    val fetchedAtEpochMillis: Long,
)

data class WeatherPreferences(
    val enabled: Boolean = false,
    val useLocation: Boolean = false,
    val city: SavedCity? = null,
    val cache: WeatherCache? = null,
)

const val WEATHER_CACHE_TTL_MILLIS: Long = 30L * 60L * 1000L

/** Past this age the face hides the weather rather than show an old reading as current. */
const val WEATHER_DISPLAY_MAX_AGE_MILLIS: Long = 6L * 60L * 60L * 1000L
const val CITY_QUERY_MAX: Int = 40

/** Short Chinese for a WMO code. Unknown and null codes have no phenomenon word. */
fun weatherPhrase(code: Int?): String? = when (code) {
    0, 1 -> "晴"
    2 -> "多云"
    3 -> "阴"
    45, 48 -> "雾"
    51, 53, 55, 56, 57 -> "毛毛雨"
    61, 63, 65, 66, 67 -> "雨"
    71, 73, 75, 77 -> "雪"
    80, 81, 82 -> "阵雨"
    85, 86 -> "阵雪"
    95, 96, 99 -> "雷暴"
    else -> null
}

/** Nearest integer Celsius. Non-finite values are not a temperature. */
/** Nearest integer Celsius. Non-finite and absurd values are not a temperature. */
fun celsiusInt(value: Double): Int? {
    if (!value.isFinite()) return null
    if (value < TEMPERATURE_MIN_C || value > TEMPERATURE_MAX_C) return null
    return value.roundToInt()
}

/** Weather codes are whole numbers. Anything else is unknown, never rounded into a code. */
fun weatherCodeInt(value: Double): Int? {
    if (!value.isFinite() || value % 1.0 != 0.0) return null
    if (value < Int.MIN_VALUE || value > Int.MAX_VALUE) return null
    return value.toInt()
}

private const val TEMPERATURE_MIN_C: Double = -100.0
private const val TEMPERATURE_MAX_C: Double = 100.0

/**
 * One clock line. Compact is `晴 18°`. Expanded adds today's high and low: `晴 18° · 26° / 14°`.
 * An unknown code keeps the temperature and still may show the range. Null cache hides the slot.
 */
fun formatWeatherLine(cache: WeatherCache?, showRange: Boolean): String? {
    if (cache == null) return null
    val temp = "${cache.temperatureC}°"
    val phrase = weatherPhrase(cache.code)
    val base = if (phrase == null) temp else "$phrase $temp"
    if (!showRange) return base
    val high = cache.highC ?: return base
    val low = cache.lowC ?: return base
    return "$base · $high° / $low°"
}

/** Fresh when the age is in `[0, 30 min)`. A clock that moved backwards is stale. */
fun cacheFresh(
    fetchedAtEpochMillis: Long?,
    nowEpochMillis: Long,
    ttlMillis: Long = WEATHER_CACHE_TTL_MILLIS,
): Boolean {
    if (fetchedAtEpochMillis == null) return false
    val age = nowEpochMillis - fetchedAtEpochMillis
    return age in 0 until ttlMillis
}

fun shouldFetchWeather(
    enabled: Boolean,
    city: SavedCity?,
    cache: WeatherCache?,
    nowEpochMillis: Long,
): Boolean {
    if (!enabled || city == null) return false
    return !cacheFresh(cache?.fetchedAtEpochMillis, nowEpochMillis)
}

/**
 * The minute loop refreshes at most once per cache window, so a failing network is not
 * polled every minute. A missing or backwards timestamp allows the attempt. User actions
 * (search, picking a city, returning to the front) skip this gate.
 */
fun weatherAttemptAllowed(
    lastAttemptEpochMillis: Long?,
    nowEpochMillis: Long,
    retryAfterMillis: Long = WEATHER_CACHE_TTL_MILLIS,
): Boolean {
    if (lastAttemptEpochMillis == null) return true
    val age = nowEpochMillis - lastAttemptEpochMillis
    return age < 0L || age >= retryAfterMillis
}

/** Trimmed query of 1–40 characters, or null when there is nothing to search. */
fun normalizeCityQuery(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    return trimmed.take(CITY_QUERY_MAX)
}

/** Name plus region, or country when the region is missing or repeats the name. */
fun placeLabel(name: String, admin1: String?, country: String?): String {
    val place = name.trim()
    val region = admin1?.trim()?.takeIf { it.isNotEmpty() && it != place }
    val nation = country?.trim()?.takeIf { it.isNotEmpty() && it != place && it != region }
    val extra = region ?: nation
    return if (extra == null) place else "$place · $extra"
}

fun coordinateOk(latitude: Double, longitude: Double): Boolean {
    return latitude.isFinite() && longitude.isFinite() &&
        latitude in -90.0..90.0 && longitude in -180.0..180.0
}

/** A city needs a label and a coordinate. A partial record is no city. */
fun savedCityOrNull(label: String?, latitude: Double?, longitude: Double?): SavedCity? {
    val name = label?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    if (latitude == null || longitude == null || !coordinateOk(latitude, longitude)) return null
    return SavedCity(name, latitude, longitude)
}

/** Temperature and fetch time are required. A one-sided high/low pair is dropped. */
fun weatherCacheOrNull(
    temperatureC: Int?,
    code: Int?,
    highC: Int?,
    lowC: Int?,
    fetchedAtEpochMillis: Long?,
): WeatherCache? {
    if (temperatureC == null || fetchedAtEpochMillis == null) return null
    val high = highC?.takeIf { lowC != null }
    val low = lowC?.takeIf { highC != null }
    return WeatherCache(temperatureC, code, high, low, fetchedAtEpochMillis)
}

/** Settings text after「更新于」. */
fun formatUpdatedAt(epochMillis: Long, zone: ZoneId): String {
    val time = Instant.ofEpochMilli(epochMillis).atZone(zone)
    val hour = time.hour.toString().padStart(2, '0')
    val minute = time.minute.toString().padStart(2, '0')
    return "${time.monthValue}月${time.dayOfMonth}日 $hour:$minute"
}
