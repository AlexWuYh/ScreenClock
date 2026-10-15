package app.screenclock.data.occasion

import app.screenclock.domain.occasion.Occasion
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class OccasionRepository(private val directory: File) {
    fun load(): List<Occasion> {
        val file = File(directory, FILE_NAME)
        if (!file.isFile) return emptyList()
        return try {
            decodeOccasions(file.readText(Charsets.UTF_8))
        } catch (_: IOException) {
            emptyList()
        }
    }

    fun save(items: List<Occasion>) {
        if (!directory.isDirectory && !directory.mkdirs()) {
            throw IOException("occasions directory is not available")
        }
        val text = encodeOccasions(items)
        val dest = File(directory, FILE_NAME)
        val temp = File(directory, "$FILE_NAME.tmp")
        temp.writeText(text, Charsets.UTF_8)
        try {
            Files.move(
                temp.toPath(),
                dest.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } catch (_: IOException) {
            dest.writeText(text, Charsets.UTF_8)
            if (!temp.delete()) temp.deleteOnExit()
        }
    }

    private companion object {
        const val FILE_NAME: String = "occasions.json"
    }
}
