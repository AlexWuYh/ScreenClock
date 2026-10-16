package app.screenclock.data.weather

import app.screenclock.domain.weather.CityCandidate
import app.screenclock.domain.weather.WeatherCache

/** The one weather backend: city search and one forecast. */
interface WeatherSource {
    fun search(rawQuery: String): List<CityCandidate>
    fun forecast(latitude: Double, longitude: Double, fetchedAtEpochMillis: Long): WeatherCache
}
