package app.screenclock.ui.settings

import app.screenclock.domain.settings.SETTINGS_DETAIL_MAX_DP
import app.screenclock.domain.settings.SettingsWidth
import app.screenclock.domain.settings.settingsDetailWidth
import app.screenclock.domain.settings.settingsIsShort
import app.screenclock.domain.settings.settingsListWidth
import app.screenclock.domain.settings.settingsPreviewHeight
import app.screenclock.domain.settings.settingsThemeColumns
import app.screenclock.domain.settings.settingsWidth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsLayoutTest {
    @Test
    fun phoneWidthsKeepTheStackedIndex() {
        assertEquals(SettingsWidth.Compact, settingsWidth(359f))
        assertEquals(SettingsWidth.Compact, settingsWidth(599.9f))
        assertEquals(0f, settingsListWidth(400f), 0.01f)
    }

    @Test
    fun aWindowWideEnoughForTwoColumnsShowsTheListBesideTheDetail() {
        assertEquals(SettingsWidth.Wide, settingsWidth(600f))
        assertEquals(300f, settingsListWidth(700f), 0.01f)
        assertEquals(SettingsWidth.WideLarge, settingsWidth(900f))
        assertEquals(360f, settingsListWidth(1_280f), 0.01f)
        // The detail column is what is left of the window, never the whole width.
        assertEquals(400f, settingsDetailWidth(700f), 0.01f)
        assertEquals(920f, settingsDetailWidth(1_280f), 0.01f)
        assertTrue(SETTINGS_DETAIL_MAX_DP <= 640f)
    }

    @Test
    fun theThemeGridWidensOnlyWhenTheColumnHasRoom() {
        assertEquals(1, settingsThemeColumns(300f))
        assertEquals(2, settingsThemeColumns(360f))
        assertEquals(2, settingsThemeColumns(559f))
        assertEquals(3, settingsThemeColumns(560f))
    }

    @Test
    fun aShortLandscapeWindowKeepsPreviewsModest() {
        assertFalse(settingsIsShort(500f))
        assertTrue(settingsIsShort(390f))
        assertEquals(120f, settingsPreviewHeight(400f, short = true), 0.01f)
        assertEquals(96f, settingsPreviewHeight(120f, short = true), 0.01f)
        assertEquals(260f, settingsPreviewHeight(600f, short = false), 0.01f)
    }
}
