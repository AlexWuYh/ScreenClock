package app.screenclock.controller

import app.screenclock.data.occasion.OccasionStore
import app.screenclock.domain.occasion.CeremonyFace
import app.screenclock.domain.occasion.LunarCalendar
import app.screenclock.domain.occasion.OCCASION_LIMIT
import app.screenclock.domain.occasion.Occasion
import app.screenclock.domain.occasion.OccasionDraft
import app.screenclock.domain.occasion.OccasionKind
import app.screenclock.domain.occasion.OccasionRow
import app.screenclock.domain.occasion.addOccasionItem
import app.screenclock.domain.occasion.canKeep
import app.screenclock.domain.occasion.canPin
import app.screenclock.domain.occasion.ceremonyFace
import app.screenclock.domain.occasion.deleteOccasionItem
import app.screenclock.domain.occasion.formatSolarDate
import app.screenclock.domain.occasion.nextOccurrence
import app.screenclock.domain.occasion.normalizeMessage
import app.screenclock.domain.occasion.normalizeTitle
import app.screenclock.domain.occasion.occasionRows
import app.screenclock.domain.occasion.pinOccasion as pinOneOccasion
import app.screenclock.domain.occasion.pinnedOccasionLine
import app.screenclock.domain.occasion.previewText
import app.screenclock.domain.occasion.refreshPins
import app.screenclock.domain.occasion.unpinOccasion as unpinOneOccasion
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** What the occasion editor shows next to the form: one preview line and the save gate. */
data class OccasionDraftStatus(
    val preview: String,
    val canSave: Boolean,
)

/** Where the ceremony goes back to when the user leaves it. */
enum class CeremonyExit {
    Clock,
    Occasions,
}

/**
 * The occasion list and the fullscreen ceremony session. The session lives only in
 * memory: after a process restart the user lands on the clock, and the pinned bar
 * simply reappears from the stored list.
 */
class OccasionController(
    private val scope: CoroutineScope,
    private val store: OccasionStore,
    private val calendar: LunarCalendar,
    private val minutes: StateFlow<LocalDateTime>,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    private val occasionLock = Mutex()
    private val saveFailedState = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Emits when the list could not be written; the new item exists only until the app closes. */
    val saveFailed: SharedFlow<Unit> = saveFailedState.asSharedFlow()

    private val occasionsState = MutableStateFlow<List<Occasion>>(emptyList())
    private val occasionRowsState = MutableStateFlow<List<OccasionRow>>(emptyList())
    val occasionRows: StateFlow<List<OccasionRow>> = occasionRowsState.asStateFlow()
    private val occasionLineState = MutableStateFlow<String?>(null)
    val occasionLine: StateFlow<String?> = occasionLineState.asStateFlow()
    private val leapMonthsState = MutableStateFlow<Set<Int>>(emptySet())
    val leapMonths: StateFlow<Set<Int>> = leapMonthsState.asStateFlow()
    private val pinnedOccasionIdState = MutableStateFlow<String?>(null)
    val pinnedOccasionId: StateFlow<String?> = pinnedOccasionIdState.asStateFlow()

    @Volatile
    private var occasionsReady = false
    private var ceremony: CeremonySession? = null

    init {
        // Which months can ever be a leap month is a slow scan; the editor hides the
        // switch until it lands, so the main thread never scans years.
        val leapMonthsJob = scope.launch(io) {
            val months = buildSet {
                for (month in 1..12) {
                    if (calendar.leapMonthEver(month)) add(month)
                }
            }
            leapMonthsState.value = months
        }
        scope.launch {
            val loaded = withContext(io) { store.load() }
            leapMonthsJob.join()
            occasionLock.withLock {
                val now = minutes.value
                val refreshed = refreshPins(loaded, now.toLocalDate(), calendar)
                occasionsState.value = refreshed
                publishOccasions(now)
                if (refreshed != loaded) persistOccasions(refreshed)
                occasionsReady = true
            }
        }
        scope.launch {
            minutes.collect { shown ->
                occasionLock.withLock {
                    if (!occasionsReady) return@withLock
                    val refreshed = refreshPins(occasionsState.value, shown.toLocalDate(), calendar)
                    val changed = refreshed != occasionsState.value
                    if (changed) occasionsState.value = refreshed
                    publishOccasions(shown)
                    if (changed) persistOccasions(refreshed)
                }
            }
        }
    }

    /** One scan per draft change: the preview line and whether the draft can be saved. */
    fun occasionDraftStatus(draft: OccasionDraft, editing: Boolean = false): OccasionDraftStatus {
        val next = nextOccurrence(minutes.value.toLocalDate(), draft, calendar)
        // Changing an existing occasion does not add one, so the 30-item cap only blocks adding.
        val canSave = occasionsReady &&
            normalizeTitle(draft.title) != null &&
            (editing || occasionsState.value.size < OCCASION_LIMIT) &&
            canKeep(next.status)
        return OccasionDraftStatus(preview = previewText(next), canSave = canSave)
    }

    /** Returns false when the title, the date, or the 30-item cap rejects the draft. */
    fun addOccasion(draft: OccasionDraft): Boolean {
        if (!occasionDraftStatus(draft).canSave) return false
        val title = normalizeTitle(draft.title) ?: return false
        val item = Occasion(
            id = UUID.randomUUID().toString(),
            title = title,
            kind = draft.kind,
            year = if (draft.kind == OccasionKind.SolarOnce) draft.year else null,
            month = draft.month,
            day = draft.day,
            leap = draft.kind == OccasionKind.LunarYearly && draft.leap,
            message = normalizeMessage(draft.message),
            pinned = false,
            icon = draft.icon,
        )
        scope.launch {
            occasionLock.withLock {
                if (!occasionsReady) return@withLock
                if (!canKeep(nextOccurrence(minutes.value.toLocalDate(), item, calendar).status)) {
                    return@withLock
                }
                val added = addOccasionItem(occasionsState.value, item) ?: return@withLock
                occasionsState.value = added
                publishOccasions(minutes.value)
                persistOccasions(added)
            }
        }
        return true
    }

    /** The stored values of one occasion, shaped like an editor draft. Null when it is gone. */
    fun occasionDraftOf(id: String): OccasionDraft? {
        val item = occasionsState.value.find { it.id == id } ?: return null
        return OccasionDraft(
            title = item.title,
            kind = item.kind,
            year = item.year ?: minutes.value.year,
            month = item.month,
            day = item.day,
            leap = item.leap,
            message = item.message,
            icon = item.icon,
        )
    }

    /** Replaces an occasion's fields in place. Its id and pin stay, unless the new date can no longer be pinned. */
    fun updateOccasion(id: String, draft: OccasionDraft): Boolean {
        if (!occasionDraftStatus(draft, editing = true).canSave) return false
        val title = normalizeTitle(draft.title) ?: return false
        scope.launch {
            occasionLock.withLock {
                if (!occasionsReady) return@withLock
                val old = occasionsState.value.find { it.id == id } ?: return@withLock
                val edited = old.copy(
                    title = title,
                    kind = draft.kind,
                    year = if (draft.kind == OccasionKind.SolarOnce) draft.year else null,
                    month = draft.month,
                    day = draft.day,
                    leap = draft.kind == OccasionKind.LunarYearly && draft.leap,
                    message = normalizeMessage(draft.message),
                    icon = draft.icon,
                )
                val today = minutes.value.toLocalDate()
                if (!canKeep(nextOccurrence(today, edited, calendar).status)) return@withLock
                val next = refreshPins(occasionsState.value.map { if (it.id == id) edited else it }, today, calendar)
                occasionsState.value = next
                publishOccasions(minutes.value)
                persistOccasions(next)
            }
        }
        return true
    }

    fun deleteOccasion(id: String) {
        scope.launch {
            occasionLock.withLock {
                if (!occasionsReady) return@withLock
                val next = deleteOccasionItem(occasionsState.value, id)
                if (next == occasionsState.value) return@withLock
                occasionsState.value = next
                publishOccasions(minutes.value)
                persistOccasions(next)
            }
        }
    }

    fun pinOccasion(id: String) {
        scope.launch {
            occasionLock.withLock {
                if (!occasionsReady) return@withLock
                val now = minutes.value
                val next = pinOneOccasion(occasionsState.value, id, now.toLocalDate(), calendar)
                if (next == occasionsState.value) return@withLock
                occasionsState.value = next
                publishOccasions(now)
                persistOccasions(next)
            }
        }
    }

    fun unpinOccasion(id: String) {
        scope.launch {
            occasionLock.withLock {
                if (!occasionsReady) return@withLock
                val next = unpinOneOccasion(occasionsState.value, id)
                if (next == occasionsState.value) return@withLock
                occasionsState.value = next
                publishOccasions(minutes.value)
                persistOccasions(next)
            }
        }
    }

    /**
     * Opens the fullscreen countdown for a future or today's date.
     * The occurrence date stays fixed until the user leaves, so a finished yearly date
     * does not jump a year. Returns false when the item cannot be opened.
     */
    fun openCeremony(id: String, fromList: Boolean): Boolean {
        val item = occasionsState.value.find { it.id == id } ?: return false
        val next = nextOccurrence(minutes.value.toLocalDate(), item, calendar)
        val date = next.date ?: return false
        if (!canPin(next.status)) return false
        ceremony = CeremonySession(id = id, occurrence = date, returnToList = fromList)
        return true
    }

    fun closeCeremony(): CeremonyExit {
        val session = ceremony
        ceremony = null
        return if (session?.returnToList == true) CeremonyExit.Occasions else CeremonyExit.Clock
    }

    fun ceremonyNow(now: LocalDateTime): CeremonyFace? {
        val session = ceremony ?: return null
        val item = occasionsState.value.find { it.id == session.id } ?: return null
        return ceremonyFace(
            title = item.title,
            icon = item.icon,
            message = item.message,
            solarLabel = formatSolarDate(session.occurrence),
            now = now,
            occurrence = session.occurrence,
        )
    }

    fun ceremonyDate(): LocalDate? {
        val session = ceremony ?: return null
        if (occasionsState.value.none { it.id == session.id }) return null
        return session.occurrence
    }

    private fun publishOccasions(now: LocalDateTime) {
        val items = occasionsState.value
        val today = now.toLocalDate()
        occasionRowsState.value = occasionRows(items, today, calendar)
        occasionLineState.value = pinnedOccasionLine(items, now, calendar)
        pinnedOccasionIdState.value = items.firstOrNull { it.pinned }?.id
    }

    /** A failed write keeps the list in memory. The next edit tries the file again. */
    private suspend fun persistOccasions(items: List<Occasion>) {
        try {
            withContext(io) { store.save(items) }
        } catch (_: IOException) {
            // The in-memory list stays. The next edit writes the file again.
            saveFailedState.tryEmit(Unit)
        }
    }

    private data class CeremonySession(
        val id: String,
        val occurrence: LocalDate,
        val returnToList: Boolean,
    )
}
