package app.screenclock.controller

import app.screenclock.data.occasion.OccasionStore
import app.screenclock.domain.occasion.LunarCalendar
import app.screenclock.domain.occasion.OCCASION_LIMIT
import app.screenclock.domain.occasion.Occasion
import app.screenclock.domain.occasion.OccasionDraft
import app.screenclock.domain.occasion.OccasionIcon
import app.screenclock.domain.occasion.OccasionKind
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeOccasionStore : OccasionStore {
    val saved = mutableListOf<List<Occasion>>()
    var items: List<Occasion> = emptyList()

    override fun load(): List<Occasion> = items

    override fun save(items: List<Occasion>) {
        saved += items
        this.items = items
    }
}

private val unusedCalendar = object : LunarCalendar {
    override fun lunarYear(date: LocalDate): Int = error("solar items do not ask the lunar calendar")
    override fun solarOf(lunarYear: Int, month: Int, day: Int, leap: Boolean): LocalDate? = error("unused")
    override fun leapMonthOf(lunarYear: Int): Int = 0
    override fun leapMonthEver(month: Int): Boolean = false
}

class OccasionControllerTest {
    private val now = LocalDateTime.of(2026, 10, 16, 10, 0)

    private fun controller(
        store: FakeOccasionStore,
        minutes: MutableStateFlow<LocalDateTime> = MutableStateFlow(now),
    ): OccasionController {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        return OccasionController(scope, store, unusedCalendar, minutes, io = Dispatchers.Unconfined)
    }

    private fun item(
        id: String,
        month: Int = 12,
        day: Int = 25,
        pinned: Boolean = false,
        kind: OccasionKind = OccasionKind.SolarYearly,
    ) = Occasion(
        id = id,
        title = "生日",
        kind = kind,
        year = if (kind == OccasionKind.SolarOnce) 2027 else null,
        month = month,
        day = day,
        leap = false,
        message = "到了",
        pinned = pinned,
    )

    private fun awaitTrue(condition: () -> Boolean) = runBlocking {
        withTimeout(2_000) {
            while (!condition()) yield()
        }
    }

    @Test
    fun loadingPublishesRowsAndThePinnedLine() {
        val store = FakeOccasionStore().apply {
            items = listOf(item("a"), item("b", pinned = true))
        }
        val controller = controller(store)
        awaitTrue { controller.occasionRows.value.size == 2 }
        assertEquals("b", controller.pinnedOccasionId.value)
        assertTrue(controller.occasionLine.value!!.startsWith("距生日"))
    }

    @Test
    fun pinningOneItemClearsTheOther() {
        val store = FakeOccasionStore().apply {
            items = listOf(item("a"), item("b", pinned = true))
        }
        val controller = controller(store)
        awaitTrue { controller.pinnedOccasionId.value == "b" }
        controller.pinOccasion("a")
        awaitTrue { controller.pinnedOccasionId.value == "a" }
        assertEquals(listOf(false, true), store.items.map { it.pinned }.sorted())
    }

    @Test
    fun editingChangesTheFieldsButKeepsTheIdAndThePin() {
        val store = FakeOccasionStore().apply { items = listOf(item("a", pinned = true), item("b")) }
        val controller = controller(store)
        awaitTrue { controller.occasionRows.value.size == 2 }
        val draft = controller.occasionDraftOf("a")!!
        assertEquals(OccasionIcon.Default, draft.icon)
        assertTrue(
            controller.updateOccasion("a", draft.copy(title = "结婚纪念", icon = OccasionIcon.Ring, month = 11, day = 2)),
        )
        awaitTrue { store.saved.isNotEmpty() && store.saved.last().first().title == "结婚纪念" }
        val saved = store.saved.last().first { it.id == "a" }
        assertEquals(OccasionIcon.Ring, saved.icon)
        assertEquals(11, saved.month)
        assertTrue(saved.pinned)
        assertEquals(2, store.saved.last().size)
    }

    @Test
    fun editingStillWorksWhenTheListIsFull() {
        val store = FakeOccasionStore().apply {
            items = (1..30).map { item("id$it", month = 12, day = 1 + it % 20) }
        }
        val controller = controller(store)
        awaitTrue { controller.occasionRows.value.size == 30 }
        val draft = controller.occasionDraftOf("id1")!!
        // A new one is refused at the cap, an edit of an existing one is not.
        assertEquals(false, controller.occasionDraftStatus(draft).canSave)
        assertEquals(true, controller.occasionDraftStatus(draft, editing = true).canSave)
        assertTrue(controller.updateOccasion("id1", draft.copy(title = "改名")))
    }

    @Test
    fun movingAPinnedOccasionIntoThePastKeepsItButDropsThePin() {
        val store = FakeOccasionStore().apply { items = listOf(item("a", pinned = true)) }
        val controller = controller(store)
        awaitTrue { controller.pinnedOccasionId.value == "a" }
        val draft = controller.occasionDraftOf("a")!!
        assertTrue(controller.updateOccasion("a", draft.copy(kind = OccasionKind.SolarOnce, year = 2020, month = 1, day = 1)))
        awaitTrue { controller.pinnedOccasionId.value == null }
        assertEquals(1, controller.occasionRows.value.size)
        assertEquals(false, store.saved.last().single().pinned)
    }

    @Test
    fun deletingRemovesTheRowAndPersists() {
        val store = FakeOccasionStore().apply { items = listOf(item("a"), item("b")) }
        val controller = controller(store)
        awaitTrue { controller.occasionRows.value.size == 2 }
        controller.deleteOccasion("a")
        awaitTrue { controller.occasionRows.value.size == 1 }
        assertEquals("b", controller.occasionRows.value.single().id)
        assertTrue(store.saved.last().none { it.id == "a" })
    }

    @Test
    fun theThirtyItemCapRejectsTheNextDraft() {
        val items = (1..OCCASION_LIMIT).map { item("id$it") }
        val store = FakeOccasionStore().apply { this.items = items }
        val controller = controller(store)
        awaitTrue { controller.occasionRows.value.size == OCCASION_LIMIT }
        val draft = OccasionDraft(
            title = "新的",
            kind = OccasionKind.SolarYearly,
            year = 2026,
            month = 5,
            day = 1,
            leap = false,
            message = "",
        )
        assertFalse(controller.occasionDraftStatus(draft).canSave)
        assertFalse(controller.addOccasion(draft))
    }

    @Test
    fun anImpossibleDateCannotBeSaved() {
        val controller = controller(FakeOccasionStore())
        val draft = OccasionDraft(
            title = "错的",
            kind = OccasionKind.SolarYearly,
            year = 2026,
            month = 2,
            day = 31,
            leap = false,
            message = "",
        )
        val status = controller.occasionDraftStatus(draft)
        assertFalse(status.canSave)
        assertEquals("近期没有这一天", status.preview)
    }

    @Test
    fun theCeremonyKeepsTheOccurrenceDateItOpenedWith() {
        val store = FakeOccasionStore().apply { items = listOf(item("a", month = 12, day = 25)) }
        val controller = controller(store)
        awaitTrue { controller.occasionRows.value.size == 1 }
        assertTrue(controller.openCeremony("a", fromList = true))
        assertEquals(LocalDate.of(2026, 12, 25), controller.ceremonyDate())
        assertEquals(CeremonyExit.Occasions, controller.closeCeremony())
        assertNull(controller.ceremonyDate())
    }

    @Test
    fun aPassedOneTimeDateCannotOpenTheCeremony() {
        val store = FakeOccasionStore().apply {
            items = listOf(item("a", kind = OccasionKind.SolarOnce).copy(year = 2020, month = 1, day = 1))
        }
        val controller = controller(store)
        awaitTrue { controller.occasionRows.value.size == 1 }
        assertFalse(controller.occasionRows.value.single().canOpen)
        assertFalse(controller.openCeremony("a", fromList = true))
    }
}
