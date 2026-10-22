package app.screenclock.data.occasion

import app.screenclock.domain.occasion.Occasion
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class OccasionRepository(
    private val directory: File,
    private val moveFile: (File, File, Boolean) -> Unit = ::moveOccasionFile,
) : OccasionStore {
    override fun load(): List<Occasion> {
        val file = File(directory, FILE_NAME)
        if (!file.isFile) return emptyList()
        return try {
            val text = file.readText(Charsets.UTF_8)
            if (!isOccasionDocument(text)) quarantine(file)
            decodeOccasions(text)
        } catch (_: IOException) {
            emptyList()
        }
    }

    override fun save(items: List<Occasion>) {
        if (!directory.isDirectory && !directory.mkdirs()) {
            throw IOException("occasions directory is not available")
        }
        val text = encodeOccasions(items)
        val dest = File(directory, FILE_NAME)
        val temp = File(directory, "$FILE_NAME.tmp")
        temp.writeText(text, Charsets.UTF_8)
        try {
            try {
                moveFile(temp, dest, true)
            } catch (atomicFailure: IOException) {
                // A regular same-directory replace is still a rename; never write to dest.
                try {
                    moveFile(temp, dest, false)
                } catch (replaceFailure: IOException) {
                    replaceFailure.addSuppressed(atomicFailure)
                    throw replaceFailure
                }
            }
        } finally {
            if (temp.exists() && !temp.delete()) temp.deleteOnExit()
        }
    }

    /**
     * An unreadable file is kept as `.corrupt` so the next save cannot overwrite the
     * only copy of the user's dates.
     */
    private fun quarantine(file: File) {
        val aside = File(directory, "$FILE_NAME.corrupt")
        try {
            Files.move(file.toPath(), aside.toPath(), StandardCopyOption.REPLACE_EXISTING)
        } catch (_: IOException) {
        }
    }

    private companion object {
        const val FILE_NAME: String = "occasions.json"
    }
}

private fun moveOccasionFile(temp: File, dest: File, atomic: Boolean) {
    if (atomic) {
        Files.move(
            temp.toPath(),
            dest.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE,
        )
    } else {
        Files.move(temp.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
}
