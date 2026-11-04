package app.screenclock.data.update

import app.screenclock.data.weather.JsonValue
import app.screenclock.data.weather.parseJson
import app.screenclock.data.weather.str
import app.screenclock.domain.update.UpdateResult
import app.screenclock.domain.update.isNewerVersion
import app.screenclock.domain.update.versionFromTag
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI

private const val LATEST_URL = "https://api.github.com/repos/AlexWuYh/ScreenClock/releases/latest"
private const val BODY_LIMIT_BYTES = 256 * 1024

fun interface UpdateTransport {
    fun get(url: String): String
}

/** Latest tag from a GitHub release body. Draft and pre-release builds are ignored. */
internal fun parseLatestTag(body: String): String? {
    val root = parseJson(body) as? JsonValue.Obj ?: return null
    if ((root.fields["draft"] as? JsonValue.Bool)?.value == true) return null
    if ((root.fields["prerelease"] as? JsonValue.Bool)?.value == true) return null
    return root.str("tag_name")?.let(::versionFromTag)
}

/**
 * Asked only when the user taps "check for updates". One GET to GitHub's public release
 * endpoint; the answer says whether a newer version exists and nothing is downloaded.
 */
class UpdateChecker(private val transport: UpdateTransport = HttpUpdateTransport()) {
    fun check(currentVersion: String): UpdateResult {
        return try {
            val latest = parseLatestTag(transport.get(LATEST_URL)) ?: return UpdateResult.Failed
            if (isNewerVersion(latest, currentVersion)) UpdateResult.Available(latest) else UpdateResult.UpToDate
        } catch (_: IOException) {
            UpdateResult.Failed
        }
    }
}

class HttpUpdateTransport : UpdateTransport {
    override fun get(url: String): String {
        if (url != LATEST_URL) throw IOException("update host")
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = false
        connection.connectTimeout = 8_000
        connection.readTimeout = 8_000
        connection.useCaches = false
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("User-Agent", "ScreenClock")
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) throw IOException("update http")
            val deadline = System.nanoTime() + 15_000_000_000L
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(4096)
            var total = 0
            connection.inputStream.use { input ->
                while (true) {
                    if (System.nanoTime() - deadline > 0) throw IOException("update timeout")
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > BODY_LIMIT_BYTES) throw IOException("update body")
                    out.write(buffer, 0, count)
                }
            }
            return out.toString(Charsets.UTF_8.name())
        } finally {
            connection.disconnect()
        }
    }
}
