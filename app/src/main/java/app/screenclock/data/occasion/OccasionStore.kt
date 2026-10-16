package app.screenclock.data.occasion

import app.screenclock.domain.occasion.Occasion

/** The occasion list on disk. A failed write throws IOException and keeps memory as is. */
interface OccasionStore {
    fun load(): List<Occasion>
    fun save(items: List<Occasion>)
}
