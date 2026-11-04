package app.screenclock.domain.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckTest {
    @Test
    fun newerVersionsCompareNumerically() {
        assertTrue(isNewerVersion("v0.5.1", "0.5.0"))
        assertTrue(isNewerVersion("0.10.0", "0.9.9"))
        assertTrue(isNewerVersion("1.0", "0.9.9"))
    }

    @Test
    fun sameOrOlderIsNotNewer() {
        assertFalse(isNewerVersion("0.5.0", "0.5.0"))
        assertFalse(isNewerVersion("v0.5", "0.5.0"))
        assertFalse(isNewerVersion("0.4.9", "0.5.0"))
    }

    @Test
    fun unreadableVersionsAreNeverNewer() {
        assertFalse(isNewerVersion("nightly", "0.5.0"))
        assertFalse(isNewerVersion("0.6.0-beta", "0.5.0"))
        assertFalse(isNewerVersion("0.6.0", ""))
    }

    @Test
    fun tagBecomesPlainVersion() {
        assertEquals("0.6.0", versionFromTag("v0.6.0"))
        assertNull(versionFromTag("release-1"))
        assertEquals("$RELEASES_PAGE/tag/v0.6.0", releasePageUrl("0.6.0"))
    }
}
