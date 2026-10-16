package app.screenclock.data.weather

import app.screenclock.domain.weather.SavedCity
import app.screenclock.domain.weather.WeatherCache
import app.screenclock.domain.weather.WeatherPreferences
import kotlinx.coroutines.flow.Flow

/** Weather preferences and the one cached forecast. */
interface WeatherStore {
    val weather: Flow<WeatherPreferences>
    suspend fun setEnabled(enabled: Boolean)
    suspend fun setUseLocation(useLocation: Boolean)
    suspend fun replaceCity(city: SavedCity, useLocation: Boolean)
    suspend fun saveCache(cache: WeatherCache)
}
