package app.screenclock.controller

import app.screenclock.data.focus.FocusData
import app.screenclock.data.focus.FocusStore
import app.screenclock.domain.focus.FocusMode
import app.screenclock.domain.focus.FocusPrefs
import app.screenclock.domain.focus.FocusRun
import app.screenclock.domain.focus.FocusSession
import app.screenclock.domain.focus.FocusStats
import app.screenclock.domain.focus.addFocusMillis
import app.screenclock.domain.focus.todayFocusMillis
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeFocusStore : FocusStore {
    override val data = MutableStateFlow(FocusData())
    var failWrites = false
    val sessionWrites = mutableListOf<FocusSession?>()

    override suspend fun savePrefs(prefs: FocusPrefs) {
        if (failWrites) throw IOException("disk full")
        data.value = data.value.copy(prefs = prefs)
    }

    override suspend fun saveSession(session: FocusSession?) {
        saveSessionAndCredit(session, 0L, LocalDate.MIN)
    }

    override suspend fun saveSessionAndCredit(session: FocusSession?, creditMillis: Long, day: LocalDate) {
        if (failWrites) throw IOException("disk full")
        sessionWrites += session
        data.value = data.value.copy(
            session = session,
            stats = addFocusMillis(data.value.stats, day, creditMillis),
        )
    }
}

class FocusControllerTest {
    private val min = 60_000L
    private val day = LocalDate.of(2026, 10, 23)
    private val dayStart = day.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()

    private fun controller(
        store: FakeFocusStore,
        time: FakeClockTime = FakeClockTime(nowMillis = dayStart + 10 * 60 * min),
    ): FocusController {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        return FocusController(scope, store, time, ZoneOffset.UTC)
    }

    private fun today(c: FocusController) = todayFocusMillis(c.state.value.stats, day)

    @Test
    fun theStoredStateIsReadOnceAndMarkedKnown() {
        val store = FakeFocusStore().apply {
            data.value = FocusData(prefs = FocusPrefs(FocusMode.LongBreak))
        }
        val c = controller(store)
        assertTrue(c.state.value.known)
        assertEquals(FocusMode.LongBreak, c.state.value.prefs.mode)
        assertNull(c.state.value.session)
    }

    @Test
    fun playStartsPausesAndResumes() {
        val time = FakeClockTime(nowMillis = dayStart + 600 * min)
        val store = FakeFocusStore()
        val c = controller(store, time)
        c.toggle()
        assertTrue(c.state.value.session!!.run is FocusRun.Running)
        time.nowMillis += 10 * min
        c.toggle()
        assertEquals(FocusRun.Paused(15 * min), c.state.value.session!!.run)
        // A long pause costs nothing.
        time.nowMillis += 100 * min
        c.toggle()
        assertEquals(FocusRun.Running(time.nowMillis + 15 * min), c.state.value.session!!.run)
        // Every change reached the store.
        assertEquals(c.state.value.session, store.data.value.session)
    }

    @Test
    fun aSegmentThatRunsOutCreditsTheWholeFocusTimeOnce() {
        val time = FakeClockTime(nowMillis = dayStart + 600 * min)
        val store = FakeFocusStore()
        val c = controller(store, time)
        c.toggle()
        time.nowMillis += 25 * min
        c.refresh()
        assertEquals(FocusRun.Done, c.state.value.session!!.run)
        assertEquals(25 * min, today(c))
        // Asking again does not credit twice.
        c.refresh()
        assertEquals(25 * min, today(c))
        assertEquals(25 * min, todayFocusMillis(store.data.value.stats, day))
    }

    @Test
    fun resetKeepsWhatWasFocusedAndGoesBackToReady() {
        val time = FakeClockTime(nowMillis = dayStart + 600 * min)
        val c = controller(FakeFocusStore(), time)
        c.toggle()
        time.nowMillis += 7 * min
        c.reset()
        assertNull(c.state.value.session)
        assertEquals(7 * min, today(c))
    }

    @Test
    fun skipEndsTheSegmentAndCountsOnlyTheElapsedPart() {
        val time = FakeClockTime(nowMillis = dayStart + 600 * min)
        val c = controller(FakeFocusStore(), time)
        c.toggle()
        time.nowMillis += 5 * min
        c.skip()
        assertEquals(FocusRun.Done, c.state.value.session!!.run)
        assertEquals(5 * min, today(c))
        // A reset after the skip must not count the segment again.
        c.reset()
        assertEquals(5 * min, today(c))
    }

    @Test
    fun breaksNeverCountTowardTheTotal() {
        val time = FakeClockTime(nowMillis = dayStart + 600 * min)
        val c = controller(FakeFocusStore(), time)
        c.selectMode(FocusMode.ShortBreak)
        c.toggle()
        time.nowMillis += 5 * min
        c.refresh()
        assertEquals(FocusRun.Done, c.state.value.session!!.run)
        assertEquals(0L, today(c))
    }

    @Test
    fun changingModeEndsTheRunningSegmentAndKeepsItsTime() {
        val time = FakeClockTime(nowMillis = dayStart + 600 * min)
        val store = FakeFocusStore()
        val c = controller(store, time)
        c.toggle()
        time.nowMillis += 4 * min
        c.setCustomMinutes(45)
        assertNull(c.state.value.session)
        assertEquals(FocusMode.Custom, c.state.value.prefs.mode)
        assertEquals(45, c.state.value.prefs.customMinutes)
        assertEquals(4 * min, today(c))
        assertEquals(FocusMode.Custom, store.data.value.prefs.mode)
        // Out-of-range minutes are pulled into range.
        c.setCustomMinutes(999)
        assertEquals(180, c.state.value.prefs.customMinutes)
    }

    @Test
    fun aRunningSegmentComesBackAfterARestartAndFinishesIfItsEndPassed() {
        val time = FakeClockTime(nowMillis = dayStart + 600 * min)
        val store = FakeFocusStore()
        val first = controller(store, time)
        first.toggle()
        // The process dies; the next launch happens after the end.
        time.nowMillis += 40 * min
        val second = controller(store, time)
        assertEquals(FocusRun.Done, second.state.value.session!!.run)
        assertEquals(25 * min, todayFocusMillis(second.state.value.stats, day))
    }

    @Test
    fun aPausedSegmentSurvivesARestartUntouched() {
        val time = FakeClockTime(nowMillis = dayStart + 600 * min)
        val store = FakeFocusStore()
        val first = controller(store, time)
        first.toggle()
        time.nowMillis += 10 * min
        first.toggle()
        time.nowMillis += 500 * min
        val second = controller(store, time)
        assertEquals(FocusRun.Paused(15 * min), second.state.value.session!!.run)
    }

    @Test
    fun theDayItEndedOnGetsTheCreditNotTheDayItWasNoticed() {
        val time = FakeClockTime(nowMillis = dayStart + 23 * 60 * min + 50 * min)
        val store = FakeFocusStore()
        val first = controller(store, time)
        first.toggle()
        // Noticed after midnight; the segment ended at 00:15 on the next day.
        time.nowMillis = dayStart + 24 * 60 * min + 40 * min
        val second = controller(store, time)
        val nextDay = day.plusDays(1)
        assertEquals(25 * min, todayFocusMillis(second.state.value.stats, nextDay))
        assertEquals(0L, todayFocusMillis(second.state.value.stats, day))
        assertEquals(Instant.ofEpochMilli(dayStart + 24 * 60 * min + 15 * min).atZone(ZoneOffset.UTC).toLocalDate(), nextDay)
    }

    @Test
    fun aFailedWriteKeepsTheTimerRunningAndSaysSo() {
        val store = FakeFocusStore().apply { failWrites = true }
        val c = controller(store)
        var failed = false
        val collector = CoroutineScope(Dispatchers.Unconfined)
        collector.launchCollect(c) { failed = true }
        c.toggle()
        assertTrue(c.state.value.session!!.run is FocusRun.Running)
        assertTrue(failed)
    }
}

private fun CoroutineScope.launchCollect(c: FocusController, onFail: () -> Unit) {
    launch(start = CoroutineStart.UNDISPATCHED) { c.events.collect { onFail() } }
}
