package app.screenclock.data.bedside

import app.screenclock.domain.bedside.BedsidePreferences
import kotlinx.coroutines.flow.Flow

/** Bedside settings persistence: the night window, warm tint, and charge keep-awake. */
interface BedsideStore {
    val preferences: Flow<BedsidePreferences>
    suspend fun setDimEnabled(enabled: Boolean)
    suspend fun setStartHour(hour: Int)
    suspend fun setEndHour(hour: Int)
    suspend fun setWarmEnabled(enabled: Boolean)
    suspend fun setChargeKeepAwake(enabled: Boolean)
}
