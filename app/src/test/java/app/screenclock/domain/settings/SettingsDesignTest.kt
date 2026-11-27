package app.screenclock.domain.settings

import app.screenclock.domain.burnin.IDLE_DEFAULT_MILLIS
import app.screenclock.domain.burnin.IDLE_MAX_MILLIS
import app.screenclock.domain.burnin.IDLE_MIN_MILLIS
import app.screenclock.domain.burnin.RUN_DEFAULT_MILLIS
import app.screenclock.domain.burnin.RUN_MAX_MILLIS
import app.screenclock.domain.burnin.RUN_MIN_MILLIS
import app.screenclock.domain.occasion.CeremonyStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsDesignTest {
    private fun input() = SettingsSummaryInput(
        themeName = "翻页",
        showsColorway = true,
        colorwayName = "墨黑",
        hourModeName = "跟随系统",
        showsDate = true,
        visibleDateParts = 4,
        datePositionAbove = true,
        digitSizeName = "标准",
        weatherEnabled = true,
        weatherPlace = "成都",
        burnInEnabled = true,
        screensaverName = "数字漂移",
        idleMinutes = 5,
        nightDimEnabled = true,
        nightStartHour = 22,
        nightEndHour = 7,
        nightWarmEnabled = true,
        chargeKeepAwake = false,
        ceremonyStyle = CeremonyStyle.Card,
        autoPlaySeconds = 10,
        version = "0.6.0",
    )

    @Test
    fun eachEntryShowsTheCurrentValueInOneLine() {
        val sample = input()
        assertEquals("翻页 · 墨黑", themeSummary(sample))
        assertEquals("跟随系统 · 日期含 4 项", displaySummary(sample))
        assertEquals("日期在上 · 数字标准", layoutSummary(sample))
        assertEquals("成都 · 已开启", weatherSummary(sample))
        assertEquals("数字漂移 · 5 分钟后", burnInSummary(sample))
        assertEquals("22:00 – 7:00 · 暖色", nightSummary(sample))
        assertEquals("卡片 · 到点后停留 10 秒", occasionSettingsSummary(sample))
        assertEquals("版本 0.6.0", aboutSummary(sample))
    }

    @Test
    fun switchedOffEntriesAndMissingColourwaysStayShort() {
        val off = input().copy(
            showsColorway = false,
            themeName = "简约数字",
            showsDate = false,
            weatherEnabled = false,
            weatherPlace = null,
            burnInEnabled = false,
            nightDimEnabled = false,
            digitSizeName = "紧凑",
        )
        assertEquals("简约数字", themeSummary(off))
        assertEquals("跟随系统 · 不显示日期", displaySummary(off))
        assertEquals("已关闭", weatherSummary(off))
        assertEquals("已关闭", burnInSummary(off))
        assertEquals("已关闭", nightSummary(off))
        assertEquals("日期在上 · 数字紧凑", layoutSummary(off))
    }

    @Test
    fun weatherWithoutACitySaysSoInsteadOfClaimingItWorks() {
        val sample = input().copy(weatherPlace = null)
        assertEquals("已开启 · 未选城市", weatherSummary(sample))
    }

    @Test
    fun idleStepsOneMinuteAndStopsAtTheRangeEnds() {
        val five = 5L * 60_000L
        assertEquals(4L * 60_000L, stepIdleMillis(five, -1))
        assertEquals(6L * 60_000L, stepIdleMillis(five, 1))
        assertEquals(IDLE_MIN_MILLIS, stepIdleMillis(IDLE_MIN_MILLIS, -1))
        assertEquals(IDLE_MAX_MILLIS, stepIdleMillis(IDLE_MAX_MILLIS, 1))
        assertEquals(IDLE_DEFAULT_MILLIS, idleFromMinutes(5))
        assertEquals(IDLE_MAX_MILLIS, idleFromMinutes(999))
    }

    @Test
    fun runStepsTenThirtyThenSixtySecondsAsItGrows() {
        assertEquals(10_000L, runStepMillis(30_000L))
        // A stored value can only sit on the ten-second grid, so the boundary is exact.
        assertEquals(10_000L, runStepMillis(110_000L))
        assertEquals(30_000L, runStepMillis(119_000L))
        assertEquals(30_000L, runStepMillis(120_000L))
        assertEquals(30_000L, runStepMillis(590_000L))
        assertEquals(60_000L, runStepMillis(600_000L))
        assertEquals(60_000L, runStepMillis(1_800_000L))
        // Below two minutes the step is ten seconds; crossing it keeps the coarse step after.
        assertEquals(120_000L, stepRunMillis(110_000L, 1))
        assertEquals(100_000L, stepRunMillis(110_000L, -1))
        assertEquals(RUN_MIN_MILLIS, stepRunMillis(RUN_MIN_MILLIS, -1))
        assertEquals(RUN_MAX_MILLIS, stepRunMillis(RUN_MAX_MILLIS, 1))
        assertEquals(RUN_DEFAULT_MILLIS, runFromSeconds(120))
    }

    @Test
    fun theRhythmBarGivesTheShorterPhaseLessRoom() {
        val (idle, run) = rhythmWeights(5, 2f)
        assertTrue(idle > run)
        val (longIdle, longRun) = rhythmWeights(60, 30f)
        assertTrue(longIdle > longRun)
        // Both phases always keep visible width.
        assertTrue(longRun > 0f)
    }

    @Test
    fun theNightBarSplitsAtMidnightAndStaysEmptyForAnEmptyWindow() {
        val spans = nightBarSpans(22, 7)
        assertEquals(2, spans.size)
        assertEquals(0.9166f, spans[0].start, 0.002f)
        assertEquals(1f, spans[0].endInclusive, 0.001f)
        assertEquals(0f, spans[1].start, 0.001f)
        assertEquals(0.2916f, spans[1].endInclusive, 0.002f)
        assertEquals(1, nightBarSpans(1, 5).size)
        assertTrue(nightBarSpans(8, 8).isEmpty())
    }
}
