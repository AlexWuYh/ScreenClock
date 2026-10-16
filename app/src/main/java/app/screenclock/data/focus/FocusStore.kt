package app.screenclock.data.focus

import kotlinx.coroutines.flow.Flow

/** The stored end moment of the running focus session, or null when there is none. */
interface FocusStore {
    val endEpochMillis: Flow<Long?>
    suspend fun setEnd(endEpochMillis: Long)
    suspend fun clear()
}
