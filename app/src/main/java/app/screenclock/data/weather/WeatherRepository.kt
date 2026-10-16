package app.screenclock.data.weather

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.screenclock.data.settings.clockSettingsStore
import app.screenclock.domain.weather.SavedCity
import app.screenclock.domain.weather.WeatherCache
import app.screenclock.domain.weather.WeatherPreferences
import app.screenclock.domain.weather.savedCityOrNull
import app.screenclock.domain.weather.weatherCacheOrNull
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val EnabledKey = booleanPreferencesKey("weather_enabled")
private val UseLocationKey = booleanPreferencesKey("weather_use_location")
private val CityLabelKey = stringPreferencesKey("weather_city_label")
private val CityLatKey = doublePreferencesKey("weather_city_lat")
private val CityLonKey = doublePreferencesKey("weather_city_lon")
private val TempKey = intPreferencesKey("weather_temp_c")
private val CodeKey = intPreferencesKey("weather_code")
private val HighKey = intPreferencesKey("weather_high_c")
private val LowKey = intPreferencesKey("weather_low_c")
private val FetchedKey = longPreferencesKey("weather_fetched_at")

class WeatherRepository(private val context: Context) : WeatherStore {
    override val weather: Flow<WeatherPreferences> = context.clockSettingsStore.data
        .map { prefs -> prefs.toWeatherPreferences() }
        .catch { error ->
            if (error is IOException) emit(WeatherPreferences()) else throw error
        }

    override suspend fun setEnabled(enabled: Boolean) {
        context.clockSettingsStore.edit { prefs -> prefs[EnabledKey] = enabled }
    }

    override suspend fun setUseLocation(useLocation: Boolean) {
        context.clockSettingsStore.edit { prefs -> prefs[UseLocationKey] = useLocation }
    }

    /** Replaces the one city and drops the previous forecast in the same write. */
    override suspend fun replaceCity(city: SavedCity, useLocation: Boolean) {
        val safe = savedCityOrNull(city.label, city.latitude, city.longitude) ?: return
        context.clockSettingsStore.edit { prefs ->
            prefs[UseLocationKey] = useLocation
            prefs[CityLabelKey] = safe.label
            prefs[CityLatKey] = safe.latitude
            prefs[CityLonKey] = safe.longitude
            prefs.remove(TempKey)
            prefs.remove(CodeKey)
            prefs.remove(HighKey)
            prefs.remove(LowKey)
            prefs.remove(FetchedKey)
        }
    }

    override suspend fun saveCache(cache: WeatherCache) {
        context.clockSettingsStore.edit { prefs ->
            prefs[TempKey] = cache.temperatureC
            if (cache.code != null) prefs[CodeKey] = cache.code else prefs.remove(CodeKey)
            if (cache.highC != null && cache.lowC != null) {
                prefs[HighKey] = cache.highC
                prefs[LowKey] = cache.lowC
            } else {
                prefs.remove(HighKey)
                prefs.remove(LowKey)
            }
            prefs[FetchedKey] = cache.fetchedAtEpochMillis
        }
    }
}

private fun Preferences.toWeatherPreferences(): WeatherPreferences {
    return WeatherPreferences(
        enabled = this[EnabledKey] ?: false,
        useLocation = this[UseLocationKey] ?: false,
        city = savedCityOrNull(this[CityLabelKey], this[CityLatKey], this[CityLonKey]),
        cache = weatherCacheOrNull(
            temperatureC = this[TempKey],
            code = this[CodeKey],
            highC = this[HighKey],
            lowC = this[LowKey],
            fetchedAtEpochMillis = this[FetchedKey],
        ),
    )
}
