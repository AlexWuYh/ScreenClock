package app.screenclock.domain.appearance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppearanceTest {
    @Test
    fun theToneFollowsTheSystemOnlyWhenAsked() {
        assertTrue(appearanceIsDark(AppearanceTone.FollowSystem, systemDark = true))
        assertFalse(appearanceIsDark(AppearanceTone.FollowSystem, systemDark = false))
        assertFalse(appearanceIsDark(AppearanceTone.Light, systemDark = true))
        assertTrue(appearanceIsDark(AppearanceTone.Dark, systemDark = false))
    }

    @Test
    fun theButtonStepsThroughTheThreeChoicesAndWraps() {
        assertEquals(AppearanceTone.Light, nextAppearanceTone(AppearanceTone.FollowSystem))
        assertEquals(AppearanceTone.Dark, nextAppearanceTone(AppearanceTone.Light))
        assertEquals(AppearanceTone.FollowSystem, nextAppearanceTone(AppearanceTone.Dark))
    }

    @Test
    fun anOldStoredCountdownToneReadsAsTheGlobalTone() {
        // The old per-countdown setting stored these exact names, so an upgrade keeps the choice.
        assertEquals(AppearanceTone.FollowSystem, AppearanceTone.fromStored(null))
        assertEquals(AppearanceTone.FollowSystem, AppearanceTone.fromStored("FollowSystem"))
        assertEquals(AppearanceTone.Light, AppearanceTone.fromStored("Light"))
        assertEquals(AppearanceTone.Dark, AppearanceTone.fromStored("Dark"))
        assertEquals(AppearanceTone.FollowSystem, AppearanceTone.fromStored("sepia"))
    }
}
