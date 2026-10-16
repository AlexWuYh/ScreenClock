package app.screenclock.domain.weather

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherTest {
    @Test
    fun phrasesCoverTheShortChineseSet() {
        assertEquals("晴", weatherPhrase(0))
        assertEquals("晴", weatherPhrase(1))
        assertEquals("多云", weatherPhrase(2))
        assertEquals("阴", weatherPhrase(3))
        assertEquals("雾", weatherPhrase(45))
        assertEquals("雾", weatherPhrase(48))
        assertEquals("毛毛雨", weatherPhrase(51))
        assertEquals("毛毛雨", weatherPhrase(57))
        assertEquals("雨", weatherPhrase(61))
        assertEquals("雨", weatherPhrase(67))
        assertEquals("雪", weatherPhrase(71))
        assertEquals("雪", weatherPhrase(77))
        assertEquals("阵雨", weatherPhrase(80))
        assertEquals("阵雨", weatherPhrase(82))
        assertEquals("阵雪", weatherPhrase(85))
        assertEquals("阵雪", weatherPhrase(86))
        assertEquals("雷暴", weatherPhrase(95))
        assertEquals("雷暴", weatherPhrase(99))
        assertNull(weatherPhrase(null))
        assertNull(weatherPhrase(123))
    }

    @Test
    fun unknownCodeKeepsOnlyTheTemperature() {
        val reading = cache(temperatureC = 18, code = 123)
        assertEquals("18°", formatWeatherLine(reading, showRange = false))
        assertEquals("18°", formatWeatherLine(reading, showRange = true))
        assertEquals(
            "18° · 26° / 14°",
            formatWeatherLine(reading.copy(highC = 26, lowC = 14), showRange = true),
        )
    }

    @Test
    fun lineOmitsTheRangeUntilTheWidthAsksAndBothEndsExist() {
        val reading = cache(temperatureC = 18, code = 0, highC = 26, lowC = 14)
        assertEquals("晴 18°", formatWeatherLine(reading, showRange = false))
        assertEquals("晴 18° · 26° / 14°", formatWeatherLine(reading, showRange = true))
        assertEquals("晴 18°", formatWeatherLine(reading.copy(highC = null), showRange = true))
        assertEquals("晴 18°", formatWeatherLine(reading.copy(lowC = null), showRange = true))
        assertNull(formatWeatherLine(null, showRange = true))
    }

    @Test
    fun roundsHalfAwayFromZeroTowardPositive() {
        assertEquals(18, celsiusInt(18.4))
        assertEquals(19, celsiusInt(18.5))
        assertEquals(-1, celsiusInt(-1.5))
        assertNull(celsiusInt(Double.NaN))
    }

    @Test
    fun codesAreWholeNumbersAndTemperaturesStayPlausible() {
        assertEquals(61, weatherCodeInt(61.0))
        assertEquals(95, weatherCodeInt(95.0))
        assertNull(weatherCodeInt(61.5))
        assertNull(weatherCodeInt(Double.NaN))
        assertNull(celsiusInt(1e300))
        assertNull(celsiusInt(-200.0))
    }

    @Test
    fun cacheIsStaleAtThirtyMinutesAndWhenTheClockMovesBack() {
        val fetched = 1_000_000L
        assertTrue(cacheFresh(fetched, fetched))
        assertTrue(cacheFresh(fetched, fetched + WEATHER_CACHE_TTL_MILLIS - 1))
        assertFalse(cacheFresh(fetched, fetched + WEATHER_CACHE_TTL_MILLIS))
        assertFalse(cacheFresh(fetched, fetched - 1))
        assertFalse(cacheFresh(null, fetched))
    }

    @Test
    fun fetchWaitsWhileTheCacheIsFresh() {
        val city = SavedCity("杭州 · 浙江", 30.27, 120.15)
        val fresh = cache(temperatureC = 18, code = 0)
        assertFalse(shouldFetchWeather(enabled = false, city = city, cache = null, nowEpochMillis = 0))
        assertFalse(shouldFetchWeather(enabled = true, city = null, cache = null, nowEpochMillis = 0))
        assertFalse(shouldFetchWeather(enabled = true, city = city, cache = fresh, nowEpochMillis = fresh.fetchedAtEpochMillis))
        assertTrue(
            shouldFetchWeather(
                enabled = true,
                city = city,
                cache = fresh,
                nowEpochMillis = fresh.fetchedAtEpochMillis + WEATHER_CACHE_TTL_MILLIS,
            ),
        )
        assertTrue(shouldFetchWeather(enabled = true, city = city, cache = null, nowEpochMillis = 0))
    }

    @Test
    fun minuteLoopAttemptsAreSpacedByOneCacheWindow() {
        assertTrue(weatherAttemptAllowed(lastAttemptEpochMillis = null, nowEpochMillis = 0))
        assertFalse(weatherAttemptAllowed(lastAttemptEpochMillis = 1_000L, nowEpochMillis = 1_000L))
        assertFalse(
            weatherAttemptAllowed(
                lastAttemptEpochMillis = 1_000L,
                nowEpochMillis = 1_000L + WEATHER_CACHE_TTL_MILLIS - 1,
            ),
        )
        assertTrue(
            weatherAttemptAllowed(
                lastAttemptEpochMillis = 1_000L,
                nowEpochMillis = 1_000L + WEATHER_CACHE_TTL_MILLIS,
            ),
        )
        assertTrue(weatherAttemptAllowed(lastAttemptEpochMillis = 1_000L, nowEpochMillis = 999L))
    }

    @Test
    fun queryTrimsAndCapsAtFortyCharacters() {
        assertNull(normalizeCityQuery("   "))
        assertEquals("杭州", normalizeCityQuery("  杭州  "))
        val long = "杭".repeat(50)
        assertEquals(40, normalizeCityQuery(long)?.length)
        assertEquals("杭".repeat(40), normalizeCityQuery(long))
    }

    @Test
    fun duplicateNamesKeepARegionOrCountry() {
        assertEquals("杭州 · 浙江", placeLabel("杭州", "浙江", "中国"))
        assertEquals("巴黎 · 法国", placeLabel("巴黎", null, "法国"))
        assertEquals("上海 · 中国", placeLabel("上海", "上海", "中国"))
        assertEquals("柏林", placeLabel("柏林", "柏林", " 柏林 "))
    }

    @Test
    fun partialCityOrCacheIsAbsent() {
        assertNull(savedCityOrNull("  ", 30.0, 120.0))
        assertNull(savedCityOrNull("杭州", null, 120.0))
        assertNull(savedCityOrNull("杭州", 91.0, 120.0))
        assertNull(savedCityOrNull("杭州", Double.NaN, 120.0))
        assertEquals("杭州", savedCityOrNull(" 杭州 ", 30.0, 120.0)?.label)
        assertNull(weatherCacheOrNull(null, 0, 1, 2, 3))
        assertNull(weatherCacheOrNull(18, 0, 1, 2, null))
        val oneSided = weatherCacheOrNull(18, 0, 26, null, 5)
        assertEquals(18, oneSided?.temperatureC)
        assertNull(oneSided?.highC)
        assertNull(oneSided?.lowC)
    }

    @Test
    fun updatedAtUsesTheGivenZone() {
        val zone = ZoneId.of("Asia/Shanghai")
        val epoch = ZonedDateTime.of(2026, 10, 15, 14, 5, 9, 0, zone).toInstant().toEpochMilli()
        assertEquals("10月15日 14:05", formatUpdatedAt(epoch, zone))
    }

    private fun cache(
        temperatureC: Int,
        code: Int?,
        highC: Int? = null,
        lowC: Int? = null,
    ): WeatherCache {
        return WeatherCache(
            temperatureC = temperatureC,
            code = code,
            highC = highC,
            lowC = lowC,
            fetchedAtEpochMillis = 5_000L,
        )
    }
}
