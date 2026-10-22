package app.screenclock.data.occasion

import app.screenclock.domain.occasion.Occasion
import app.screenclock.domain.occasion.OccasionKind
import java.io.File
import java.io.IOException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class OccasionRepositoryTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    private val item = Occasion(
        id = "saved",
        title = "跨年",
        kind = OccasionKind.SolarYearly,
        year = null,
        month = 1,
        day = 1,
        leap = false,
        message = "到了",
        pinned = true,
    )

    @Test
    fun bothMovesFailWithoutTouchingTheOldBytesOrTheRestoredList() {
        val directory = temporaryFolder.newFolder("occasions")
        val working = OccasionRepository(directory)
        working.save(listOf(item))
        val destination = File(directory, "occasions.json")
        val originalBytes = destination.readBytes()
        val attempts = mutableListOf<Boolean>()
        val broken = OccasionRepository(directory) { temp, dest, atomic ->
            attempts += atomic
            assertEquals(destination, dest)
            assertEquals(directory, temp.parentFile)
            throw IOException("injected move failure")
        }

        val failure = assertThrows(IOException::class.java) { broken.save(emptyList()) }
        assertEquals(listOf(true, false), attempts)
        assertEquals(1, failure.suppressed.size)
        assertArrayEquals(originalBytes, destination.readBytes())
        assertFalse(File(directory, "occasions.json.tmp").exists())
        assertEquals(listOf(item), OccasionRepository(directory).load())
    }

    @Test
    fun corruptFileIsKeptAsideBeforeAnythingCanOverwriteIt() {
        val directory = temporaryFolder.newFolder("corrupt")
        File(directory, "occasions.json").writeText("{not json", Charsets.UTF_8)

        assertEquals(emptyList<Occasion>(), OccasionRepository(directory).load())

        assertEquals("{not json", File(directory, "occasions.json.corrupt").readText())
        assertFalse(File(directory, "occasions.json").exists())
    }

    @Test
    fun emptyButValidFileIsNotTreatedAsCorrupt() {
        val directory = temporaryFolder.newFolder("empty")
        File(directory, "occasions.json").writeText("{\"items\":[]}", Charsets.UTF_8)

        OccasionRepository(directory).load()

        assertFalse(File(directory, "occasions.json.corrupt").exists())
    }
}
