package app.screenclock.data.occasion

import app.screenclock.data.weather.JsonValue
import app.screenclock.data.weather.parseJson
import app.screenclock.domain.occasion.OCCASION_LIMIT
import app.screenclock.domain.occasion.Occasion
import app.screenclock.domain.occasion.OccasionIcon
import app.screenclock.domain.occasion.OccasionKind
import app.screenclock.domain.occasion.normalizeMessage
import app.screenclock.domain.occasion.normalizeTitle
import app.screenclock.domain.occasion.occasionDayMax

internal fun encodeOccasions(items: List<Occasion>): String {
    val body = items.joinToString(separator = ",") { encodeItem(it) }
    return "{\"items\":[$body]}"
}

/** True when [text] is a well-formed occasions document, even an empty one. */
internal fun isOccasionDocument(text: String): Boolean {
    val root = parseJson(text) as? JsonValue.Obj ?: return false
    return root.fields["items"] is JsonValue.Arr
}

internal fun decodeOccasions(text: String): List<Occasion> {
    val root = parseJson(text) as? JsonValue.Obj ?: return emptyList()
    val items = root.fields["items"] as? JsonValue.Arr ?: return emptyList()
    val decoded = ArrayList<Occasion>(items.items.size)
    for (value in items.items) {
        val item = decodeItem(value) ?: continue
        if (decoded.any { it.id == item.id }) continue
        decoded += item
        if (decoded.size == OCCASION_LIMIT) break
    }
    // Only one occasion can be pinned; a hand-edited file keeps the first.
    val firstPin = decoded.indexOfFirst { it.pinned }
    return decoded.mapIndexed { index, item ->
        if (item.pinned && index != firstPin) item.copy(pinned = false) else item
    }
}

private fun encodeItem(item: Occasion): String {
    val fields = ArrayList<String>(9)
    fields += "\"id\":${jsonString(item.id)}"
    fields += "\"title\":${jsonString(item.title)}"
    fields += "\"kind\":${jsonString(kindWire(item.kind))}"
    if (item.kind == OccasionKind.SolarOnce && item.year != null) {
        fields += "\"year\":${item.year}"
    }
    fields += "\"month\":${item.month}"
    fields += "\"day\":${item.day}"
    fields += "\"leap\":${item.leap && item.kind == OccasionKind.LunarYearly}"
    fields += "\"message\":${jsonString(item.message)}"
    fields += "\"pinned\":${item.pinned}"
    fields += "\"icon\":${jsonString(item.icon.name)}"
    return "{${fields.joinToString(",")}}"
}

private fun decodeItem(value: JsonValue): Occasion? {
    val obj = value as? JsonValue.Obj ?: return null
    val id = obj.fields["id"].asString()?.takeIf { it.isNotBlank() } ?: return null
    val title = normalizeTitle(obj.fields["title"].asString() ?: return null) ?: return null
    val kind = kindOf(obj.fields["kind"].asString()) ?: return null
    val month = obj.fields["month"].asInt() ?: return null
    val day = obj.fields["day"].asInt() ?: return null
    // A hand-edited or old file must not smuggle in rows that can never resolve.
    if (month !in 1..12) return null
    val leap = obj.fields["leap"].asBoolean() ?: false
    val year = obj.fields["year"].asInt()
    if (kind == OccasionKind.SolarOnce && year == null) return null
    // Feb 31 or Apr 31 would be a permanent "not found" row.
    if (day !in 1..occasionDayMax(kind, year ?: 0, month)) return null
    val message = normalizeMessage(obj.fields["message"].asString() ?: "")
    val pinned = obj.fields["pinned"].asBoolean() ?: false
    return Occasion(
        id = id,
        title = title,
        kind = kind,
        year = if (kind == OccasionKind.SolarOnce) year else null,
        month = month,
        day = day,
        leap = kind == OccasionKind.LunarYearly && leap,
        message = message,
        pinned = pinned,
        icon = OccasionIcon.fromStored(obj.fields["icon"].asString()),
    )
}

private fun kindWire(kind: OccasionKind): String {
    return when (kind) {
        OccasionKind.SolarOnce -> "solar_once"
        OccasionKind.SolarYearly -> "solar_yearly"
        OccasionKind.LunarYearly -> "lunar_yearly"
    }
}

private fun kindOf(raw: String?): OccasionKind? {
    return when (raw) {
        "solar_once" -> OccasionKind.SolarOnce
        "solar_yearly" -> OccasionKind.SolarYearly
        "lunar_yearly" -> OccasionKind.LunarYearly
        else -> null
    }
}

private fun jsonString(value: String): String {
    val out = StringBuilder(value.length + 2)
    out.append('"')
    for (char in value) {
        when (char) {
            '\\' -> out.append("\\\\")
            '"' -> out.append("\\\"")
            '\n' -> out.append("\\n")
            '\r' -> out.append("\\r")
            '\t' -> out.append("\\t")
            else -> if (char < ' ') {
                out.append("\\u")
                out.append(char.code.toString(16).padStart(4, '0'))
            } else {
                out.append(char)
            }
        }
    }
    out.append('"')
    return out.toString()
}

private fun JsonValue?.asString(): String? {
    return (this as? JsonValue.Str)?.value
}

private fun JsonValue?.asBoolean(): Boolean? {
    return (this as? JsonValue.Bool)?.value
}

private fun JsonValue?.asInt(): Int? {
    val number = (this as? JsonValue.Num)?.value ?: return null
    if (!number.isFinite() || number % 1.0 != 0.0) return null
    if (number < Int.MIN_VALUE.toDouble() || number > Int.MAX_VALUE.toDouble()) return null
    return number.toInt()
}
