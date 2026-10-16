package app.screenclock.data.weather

internal sealed interface JsonValue {
    data class Obj(val fields: Map<String, JsonValue>) : JsonValue
    data class Arr(val items: List<JsonValue>) : JsonValue
    data class Str(val value: String) : JsonValue
    data class Num(val value: Double) : JsonValue
    data class Bool(val value: Boolean) : JsonValue
    data object Null : JsonValue
}

private class JsonParseException : Exception()

/** Nested containers deeper than this are rejected instead of recursing without end. */
private const val MAX_JSON_DEPTH: Int = 32

/** Parse one JSON value. Returns null when the text is not that JSON. */
internal fun parseJson(text: String): JsonValue? {
    return try {
        JsonReader(text).parse()
    } catch (_: JsonParseException) {
        null
    } catch (_: StackOverflowError) {
        // Depth is capped below; this is the last line of defence for hostile input.
        null
    }
}

private class JsonReader(private val text: String) {
    private var index = 0
    private var depth = 0

    fun parse(): JsonValue {
        skip()
        val value = readValue()
        skip()
        if (index != text.length) throw JsonParseException()
        return value
    }

    private fun readValue(): JsonValue {
        skip()
        if (index >= text.length) throw JsonParseException()
        if (depth >= MAX_JSON_DEPTH) throw JsonParseException()
        depth++
        try {
            return when (text[index]) {
                '{' -> readObject()
                '[' -> readArray()
                '"' -> JsonValue.Str(readString())
                't' -> {
                    literal("true")
                    JsonValue.Bool(true)
                }
                'f' -> {
                    literal("false")
                    JsonValue.Bool(false)
                }
                'n' -> {
                    literal("null")
                    JsonValue.Null
                }
                else -> readNumber()
            }
        } finally {
            depth--
        }
    }

    private fun readObject(): JsonValue.Obj {
        expect('{')
        val fields = linkedMapOf<String, JsonValue>()
        skip()
        if (peek('}')) return JsonValue.Obj(fields)
        while (true) {
            skip()
            if (index >= text.length || text[index] != '"') throw JsonParseException()
            val key = readString()
            skip()
            expect(':')
            fields[key] = readValue()
            skip()
            when {
                peek(',') -> continue
                peek('}') -> return JsonValue.Obj(fields)
                else -> throw JsonParseException()
            }
        }
    }

    private fun readArray(): JsonValue.Arr {
        expect('[')
        val items = mutableListOf<JsonValue>()
        skip()
        if (peek(']')) return JsonValue.Arr(items)
        while (true) {
            items += readValue()
            skip()
            when {
                peek(',') -> continue
                peek(']') -> return JsonValue.Arr(items)
                else -> throw JsonParseException()
            }
        }
    }

    private fun readString(): String {
        expect('"')
        val out = StringBuilder()
        while (index < text.length) {
            val char = text[index++]
            when (char) {
                '"' -> return out.toString()
                '\\' -> out.append(readEscape())
                else -> {
                    if (char < ' ') throw JsonParseException()
                    out.append(char)
                }
            }
        }
        throw JsonParseException()
    }

    private fun readEscape(): Char {
        if (index >= text.length) throw JsonParseException()
        return when (val escaped = text[index++]) {
            '"', '\\', '/' -> escaped
            'b' -> '\b'
            'f' -> '\u000C'
            'n' -> '\n'
            'r' -> '\r'
            't' -> '\t'
            'u' -> {
                if (index + 4 > text.length) throw JsonParseException()
                val hex = text.substring(index, index + 4)
                index += 4
                (hex.toIntOrNull(16) ?: throw JsonParseException()).toChar()
            }
            else -> throw JsonParseException()
        }
    }

    private fun readNumber(): JsonValue.Num {
        val start = index
        if (index < text.length && text[index] == '-') index++
        if (index >= text.length || !text[index].isDigit()) throw JsonParseException()
        while (index < text.length && text[index].isDigit()) index++
        if (index < text.length && text[index] == '.') {
            index++
            if (index >= text.length || !text[index].isDigit()) throw JsonParseException()
            while (index < text.length && text[index].isDigit()) index++
        }
        if (index < text.length && (text[index] == 'e' || text[index] == 'E')) {
            index++
            if (index < text.length && (text[index] == '+' || text[index] == '-')) index++
            if (index >= text.length || !text[index].isDigit()) throw JsonParseException()
            while (index < text.length && text[index].isDigit()) index++
        }
        val value = text.substring(start, index).toDoubleOrNull() ?: throw JsonParseException()
        if (!value.isFinite()) throw JsonParseException()
        return JsonValue.Num(value)
    }

    private fun literal(word: String) {
        if (!text.startsWith(word, index)) throw JsonParseException()
        index += word.length
    }

    private fun skip() {
        while (index < text.length && text[index].isWhitespace()) index++
    }

    private fun peek(char: Char): Boolean {
        if (index < text.length && text[index] == char) {
            index++
            return true
        }
        return false
    }

    private fun expect(char: Char) {
        if (!peek(char)) throw JsonParseException()
    }
}

internal fun JsonValue.str(key: String): String? {
    return ((this as? JsonValue.Obj)?.fields?.get(key) as? JsonValue.Str)?.value
}

internal fun JsonValue.num(key: String): Double? {
    return ((this as? JsonValue.Obj)?.fields?.get(key) as? JsonValue.Num)?.value
}
