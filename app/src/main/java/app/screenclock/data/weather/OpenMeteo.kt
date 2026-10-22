package app.screenclock.data.weather

import app.screenclock.domain.weather.CityCandidate
import app.screenclock.domain.weather.WeatherCache
import app.screenclock.domain.weather.celsiusInt
import app.screenclock.domain.weather.coordinateOk
import app.screenclock.domain.weather.normalizeCityQuery
import app.screenclock.domain.weather.placeLabel
import app.screenclock.domain.weather.weatherCodeInt
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URISyntaxException
import java.net.URLEncoder
import java.nio.charset.Charset
import java.util.Locale

private const val GEOCODING_HOST = "geocoding-api.open-meteo.com"
private const val FORECAST_HOST = "api.open-meteo.com"
private const val BODY_LIMIT_BYTES = 256 * 1024
private const val TOTAL_TIMEOUT_NANOS = 15_000_000_000L

fun interface WeatherTransport {
    fun get(url: String): String
}

/**
 * Open-Meteo only. HTTPS, the two documented hosts, and no redirect following.
 * A redirect would leave the allowlist, so a 3xx is a failure.
 */
internal fun allowedWeatherUrl(url: String): Boolean {
    val uri = try {
        URI(url)
    } catch (_: URISyntaxException) {
        return false
    }
    if (!uri.scheme.equals("https", ignoreCase = true)) return false
    if (uri.userInfo != null) return false
    if (uri.port != -1 && uri.port != 443) return false
    val host = uri.host ?: return false
    val path = uri.path ?: return false
    return when {
        host.equals(GEOCODING_HOST, ignoreCase = true) -> path == "/v1/search"
        host.equals(FORECAST_HOST, ignoreCase = true) -> path == "/v1/forecast"
        else -> false
    }
}

internal fun searchUrl(query: String): String {
    val name = URLEncoder.encode(query, Charset.forName("UTF-8").name())
    return "https://$GEOCODING_HOST/v1/search?name=$name&count=5&language=zh&format=json"
}

/** Plain decimal text; `Double.toString` would emit `1.0E-5` near the equator or meridian. */
private fun coordinateText(value: Double): String = String.format(Locale.ROOT, "%.4f", value)

internal fun forecastUrl(latitude: Double, longitude: Double): String {
    require(coordinateOk(latitude, longitude))
    return "https://$FORECAST_HOST/v1/forecast" +
        "?latitude=${coordinateText(latitude)}&longitude=${coordinateText(longitude)}" +
        "&current=temperature_2m,weather_code" +
        "&daily=temperature_2m_max,temperature_2m_min" +
        "&timezone=auto&forecast_days=1"
}

/**
 * City search body. Null means the body is not usable. An empty list means no matches.
 * Results without a name or a coordinate are skipped.
 */
internal fun parseCitySearch(body: String): List<CityCandidate>? {
    val root = parseJson(body) as? JsonValue.Obj ?: return null
    val raw = root.fields["results"]
    if (raw == null || raw is JsonValue.Null) return emptyList()
    val items = (raw as? JsonValue.Arr)?.items ?: return null
    return items.mapNotNull { item ->
        val name = item.str("name")?.trim().orEmpty()
        val latitude = item.num("latitude")
        val longitude = item.num("longitude")
        if (name.isEmpty() || latitude == null || longitude == null || !coordinateOk(latitude, longitude)) {
            return@mapNotNull null
        }
        CityCandidate(
            label = placeLabel(name, item.str("admin1"), item.str("country")),
            latitude = latitude,
            longitude = longitude,
        )
    }
}

/** Forecast body. Null when the current temperature is missing or the body is not JSON. */
internal fun parseForecast(body: String, fetchedAtEpochMillis: Long): WeatherCache? {
    val root = parseJson(body) as? JsonValue.Obj ?: return null
    val current = root.fields["current"] as? JsonValue.Obj ?: return null
    val temperature = current.num("temperature_2m")?.let(::celsiusInt) ?: return null
    val code = current.num("weather_code")?.let(::weatherCodeInt)
    val daily = root.fields["daily"] as? JsonValue.Obj
    val high = daily?.firstNumber("temperature_2m_max")?.let(::celsiusInt)
    val low = daily?.firstNumber("temperature_2m_min")?.let(::celsiusInt)
    val range = if (high != null && low != null) high to low else null
    return WeatherCache(
        temperatureC = temperature,
        code = code,
        highC = range?.first,
        lowC = range?.second,
        fetchedAtEpochMillis = fetchedAtEpochMillis,
    )
}

private fun JsonValue.Obj.firstNumber(key: String): Double? {
    val array = fields[key] as? JsonValue.Arr ?: return null
    return (array.items.firstOrNull() as? JsonValue.Num)?.value
}

class OpenMeteoClient(private val transport: WeatherTransport = HttpWeatherTransport()) : WeatherSource {
    /** Empty query does not touch the network. Invalid JSON throws [IOException]. */
    override fun search(rawQuery: String): List<CityCandidate> {
        val query = normalizeCityQuery(rawQuery) ?: return emptyList()
        val body = transport.get(searchUrl(query))
        return parseCitySearch(body) ?: throw IOException("weather body")
    }

    override fun forecast(latitude: Double, longitude: Double, fetchedAtEpochMillis: Long): WeatherCache {
        if (!coordinateOk(latitude, longitude)) throw IOException("weather coordinate")
        val body = transport.get(forecastUrl(latitude, longitude))
        return parseForecast(body, fetchedAtEpochMillis) ?: throw IOException("weather body")
    }
}

class HttpWeatherTransport : WeatherTransport {
    override fun get(url: String): String {
        if (!allowedWeatherUrl(url)) throw IOException("weather host")
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = false
        connection.connectTimeout = 8_000
        connection.readTimeout = 8_000
        connection.useCaches = false
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "ScreenClock")
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) throw IOException("weather http")
            val deadline = System.nanoTime() + TOTAL_TIMEOUT_NANOS
            val bytes = connection.inputStream.readLimited(BODY_LIMIT_BYTES, deadline)
            return bytes.toString(Charset.forName("UTF-8"))
        } finally {
            connection.disconnect()
        }
    }
}

private fun InputStream.readLimited(maxBytes: Int, deadlineNanos: Long): ByteArray {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(4096)
    var total = 0
    while (true) {
        // readTimeout is per read; a server that drips bytes must not hold the thread forever.
        if (System.nanoTime() - deadlineNanos > 0) throw IOException("weather timeout")
        val count = read(buffer)
        if (count < 0) break
        total += count
        if (total > maxBytes) throw IOException("weather body")
        out.write(buffer, 0, count)
    }
    return out.toByteArray()
}
