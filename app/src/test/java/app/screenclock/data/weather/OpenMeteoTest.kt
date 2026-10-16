package app.screenclock.data.weather

import app.screenclock.domain.weather.CityCandidate
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class OpenMeteoTest {
    @Test
    fun searchUrlStaysOnTheGeocodingHost() {
        val url = searchUrl("杭州")
        assertTrue(url.startsWith("https://geocoding-api.open-meteo.com/v1/search?"))
        assertTrue(url.contains("name=%E6%9D%AD%E5%B7%9E"))
        assertTrue(url.contains("count=5"))
        assertTrue(url.contains("language=zh"))
        assertTrue(allowedWeatherUrl(url))
        val york = searchUrl("New York")
        assertTrue(york.contains("name=New+York") || york.contains("name=New%20York"))
        assertFalse(york.contains("name=New York"))
    }

    @Test
    fun forecastUrlAsksOnlyForTheCurrentAndDailyFields() {
        val url = forecastUrl(30.2741, 120.1551)
        assertTrue(url.startsWith("https://api.open-meteo.com/v1/forecast?"))
        assertTrue(url.contains("current=temperature_2m,weather_code"))
        assertTrue(url.contains("daily=temperature_2m_max,temperature_2m_min"))
        assertTrue(url.contains("timezone=auto"))
        assertTrue(url.contains("forecast_days=1"))
        assertFalse(url.contains("hourly"))
        assertTrue(allowedWeatherUrl(url))
    }

    @Test
    fun otherHostsAndPlainHttpAreRejected() {
        assertFalse(allowedWeatherUrl("http://api.open-meteo.com/v1/forecast"))
        assertFalse(allowedWeatherUrl("https://example.com/v1/forecast"))
        assertFalse(allowedWeatherUrl("https://api.open-meteo.com.evil/v1/forecast"))
        assertFalse(allowedWeatherUrl("https://user@api.open-meteo.com/v1/forecast"))
        assertFalse(allowedWeatherUrl("https://api.open-meteo.com:444/v1/forecast"))
        assertFalse(allowedWeatherUrl("https://api.open-meteo.com/v1/search"))
        try {
            HttpWeatherTransport().get("https://example.com/v1/forecast")
            fail("other hosts must not be opened")
        } catch (error: IOException) {
            assertEquals("weather host", error.message)
        }
    }

    @Test
    fun searchParsesChinesePlacesAndSkipsBrokenRows() {
        val body = """
            {"results":[
              {"name":"杭州","latitude":30.27,"longitude":120.15,"admin1":"浙江","country":"中国"},
              {"name":"","latitude":1,"longitude":2},
              {"name":"坏","latitude":120,"longitude":10}
            ]}
        """.trimIndent()
        val places = parseCitySearch(body)
        assertEquals(listOf(CityCandidate("杭州 · 浙江", 30.27, 120.15)), places)
        assertEquals(emptyList<CityCandidate>(), parseCitySearch("""{"generationtime_ms":1}"""))
        assertEquals(emptyList<CityCandidate>(), parseCitySearch("""{"results":null}"""))
        assertNull(parseCitySearch("{"))
    }

    @Test
    fun duplicateNamesStayDistinguishable() {
        val body = """
            {"results":[
              {"name":"Springfield","latitude":39.78,"longitude":-89.64,"admin1":"Illinois","country":"美国"},
              {"name":"Springfield","latitude":37.21,"longitude":-93.29,"admin1":"Missouri","country":"美国"}
            ]}
        """.trimIndent()
        val places = parseCitySearch(body) ?: error("body")
        assertEquals("Springfield · Illinois", places[0].label)
        assertEquals("Springfield · Missouri", places[1].label)
    }

    @Test
    fun forecastRoundsTemperatureAndDropsAOneSidedRange() {
        val full = parseForecast(
            """
            {"current":{"temperature_2m":18.5,"weather_code":0},
             "daily":{"temperature_2m_max":[26.4],"temperature_2m_min":[14.1]}}
            """.trimIndent(),
            fetchedAtEpochMillis = 9,
        )
        assertEquals(19, full?.temperatureC)
        assertEquals(0, full?.code)
        assertEquals(26, full?.highC)
        assertEquals(14, full?.lowC)
        assertEquals(9L, full?.fetchedAtEpochMillis)
        val currentOnly = parseForecast("""{"current":{"temperature_2m":-1.2,"weather_code":123}}""", 3)
        assertEquals(-1, currentOnly?.temperatureC)
        assertEquals(123, currentOnly?.code)
        assertNull(currentOnly?.highC)
        assertNull(parseForecast("""{"current":{"weather_code":1}}""", 1))
        assertNull(parseForecast("nope", 1))
    }

    @Test
    fun blankQueryDoesNotRequestAndBadCoordinatesDoNotEither() {
        var calls = 0
        val client = OpenMeteoClient { calls += 1; "" }
        assertEquals(emptyList<CityCandidate>(), client.search("  "))
        assertEquals(0, calls)
        try {
            client.forecast(Double.NaN, 0.0, 0)
            fail("bad coordinates must fail closed")
        } catch (error: IOException) {
            assertEquals(0, calls)
            assertEquals("weather coordinate", error.message)
        }
    }

    @Test
    fun clientReadsASearchBodyFromTheTransport() {
        var seen = ""
        val client = OpenMeteoClient { url ->
            seen = url
            """{"results":[{"name":"杭州","latitude":30.27,"longitude":120.15,"admin1":"浙江","country":"中国"}]}"""
        }
        val places = client.search(" 杭州 ")
        assertTrue(allowedWeatherUrl(seen))
        assertEquals("杭州 · 浙江", places.single().label)
    }

    @Test
    fun deeplyNestedJsonIsRejectedInsteadOfCrashing() {
        val body = "[".repeat(64) + "]".repeat(64)
        assertNull(parseJson(body))
        assertNull(parseCitySearch(body))
    }
}
