package app.screenclock.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemePreviewLayoutTest {
    @Test
    fun narrowScreensShowOneLegibleThemePerRow() {
        assertEquals(1, themePreviewColumns(220f))
        assertEquals(1, themePreviewColumns(359.9f))
        assertEquals(2, themePreviewColumns(360f))
        assertEquals(2, themePreviewColumns(680f))
    }

    @Test
    fun imageHeightsKeepAControlledRatioAndNeverBecomeWideStrips() {
        assertEquals(132f, themePreviewHeight(116f), 0.01f)
        assertEquals(176.08f, themePreviewHeight(284f), 0.05f)
        assertEquals(214.52f, themePreviewHeight(346f), 0.05f)
        assertEquals(220f, themePreviewHeight(500f), 0.01f)
    }
}
