package app.screenclock.domain.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClockThemeTest {
    @Test
    fun everyThemeAndColorwaySurvivesAStoreRoundTrip() {
        for (theme in ClockThemeId.entries) {
            assertEquals(theme, ClockThemeId.fromStored(theme.name))
        }
        for (colorway in ColorwayId.entries) {
            assertEquals(colorway, ColorwayId.fromStored(colorway.name))
        }
        assertEquals(ClockThemeId.Neon, ClockThemeId.fromStored("Neon"))
        assertEquals(ColorwayId.SnowWhite, ColorwayId.fromStored("SnowWhite"))
    }

    @Test
    fun storedThemeAndColorwayFallBackToTheDefaults() {
        assertEquals(ClockThemeId.Flip, ClockThemeId.fromStored(null))
        assertEquals(ClockThemeId.Flip, ClockThemeId.fromStored("slow"))
        assertEquals(ClockThemeId.Tube, ClockThemeId.fromStored("Tube"))
        assertEquals(ColorwayId.InkBlack, ColorwayId.fromStored(null))
        assertEquals(ColorwayId.InkBlack, ColorwayId.fromStored("Paper"))
        assertEquals(ColorwayId.InkBlack, ColorwayId.fromStored(ColorwayId.InkBlack.name))
    }

    @Test
    fun footerIsAbsentUntilWeatherOrOccasionExists() {
        assertNull(footerLine(null, null))
        assertNull(footerLine("  ", ""))
    }

    @Test
    fun footerUsesTheSameLineForEitherTheme() {
        assertEquals("晴 18°", footerLine("晴 18°", null))
        assertEquals("距妈妈生日 86 天", footerLine(null, "距妈妈生日 86 天"))
        assertEquals("晴 18° · 距妈妈生日 86 天", footerLine("晴 18°", "距妈妈生日 86 天"))
    }

    @Test
    fun spokenClockKeepsTheDateAndAddsWeatherWhenItExists() {
        assertEquals("10月1日 周四，14点30分", clockContentDescription("10月1日 周四", "14点30分", null))
        assertEquals("14点30分", clockContentDescription(null, "14点30分", "  "))
        assertEquals(
            "10月1日 周四，14点30分，晴 18°",
            clockContentDescription("10月1日 周四", "14点30分", "晴 18°"),
        )
        assertEquals(
            "10月1日 周四，14点30分，距妈妈生日 86 天",
            clockContentDescription("10月1日 周四", "14点30分", null, "距妈妈生日 86 天"),
        )
    }
}
