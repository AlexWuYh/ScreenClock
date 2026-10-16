package app.screenclock.controller

import app.screenclock.data.focus.FocusStore
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeFocusStore : FocusStore {
    override val endEpochMillis = MutableStateFlow<Long?>(null)
    var failNextWrite = false

    /** When set, writes suspend until the test opens the gate, in call order. */
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun setEnd(endEpochMillis: Long) {
        gate?.await()
        if (failNextWrite) throw IOException("disk full")
        this.endEpochMillis.value = endEpochMillis
    }

    override suspend fun clear() {
        gate?.await()
        endEpochMillis.value = null
    }
}

class FocusControllerTest {
    private fun controller(store: FakeFocusStore, time: FakeClockTime = FakeClockTime()): FocusController {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        return FocusController(scope, store, time)
    }

    @Test
    fun startingWritesTheEndAndTheStoreEchoIsAccepted() {
        val store = FakeFocusStore()
        val time = FakeClockTime(nowMillis = 1_000L)
        val controller = controller(store, time)
        val end = controller.start(25)
        assertEquals(1_000L + 25 * 60_000L, end)
        assertEquals(end, controller.focusEnd.value)
        assertEquals(end, store.endEpochMillis.value)
        assertTrue(controller.focusKnown.value)
    }

    @Test
    fun aStaleStoreEchoDoesNotClobberTheRunningSession() {
        val store = FakeFocusStore()
        val controller = controller(store)
        store.gate = CompletableDeferred()
        val started = controller.start(25)
        // While the write is in flight the store re-emits an older end.
        store.endEpochMillis.value = 77L
        assertEquals(started, controller.focusEnd.value)
        store.gate!!.complete(Unit)
        assertEquals(started, controller.focusEnd.value)
    }

    @Test
    fun abandoningWinsOverAnInFlightWrite() = runBlocking {
        val store = FakeFocusStore()
        store.gate = CompletableDeferred()
        val controller = controller(store)
        controller.start(25)
        controller.abandon()
        assertNull(controller.focusEnd.value)
        // The writes land in call order: the abandoned end first, then the clear.
        // Neither may bring the session back.
        store.gate!!.complete(Unit)
        assertNull(controller.focusEnd.value)
    }

    @Test
    fun aFailedWriteRollsBackTheSession() {
        val store = FakeFocusStore()
        store.failNextWrite = true
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val controller = FocusController(scope, store, FakeClockTime())
        val events = mutableListOf<FocusEvent>()
        val job = scope.launch { controller.events.collect { events += it } }
        controller.start(25)
        assertNull(controller.focusEnd.value)
        assertTrue(events.contains(FocusEvent.PersistFailed))
        job.cancel()
    }

    @Test
    fun restoringFromDiskPublishesTheSession() {
        val store = FakeFocusStore()
        store.endEpochMillis.value = 42L
        val controller = controller(store)
        assertEquals(42L, controller.focusEnd.value)
    }
}
