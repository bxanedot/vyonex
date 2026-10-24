// bxane
package com.vyonex.internal.primary.extraction.potoken

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val WEB_PAGE_LIMIT = 4 * 1024 * 1024
private const val IDENTIFIER_LIMIT = 1024
private const val JSON_FIELD_LIMIT = 256

private val ATTESTATION_CALL = Regex("(?<![A-Za-z0-9_$])window\\s*\\.\\s*ytAtN\\s*\\(")
private val ATTESTATION_ASSIGNMENT = Regex("(?<![A-Za-z0-9_$])window\\s*\\.\\s*ytAtR\\s*=")
private val CONFIG_CALL = Regex("(?<![A-Za-z0-9_$])ytcfg\\s*\\.\\s*set\\s*\\(")
private val R_PROPERTY = Regex("(?<![A-Za-z0-9_$])R\\s*:")

/** Sensitive BotGuard page data. Do not log this value: it contains an event ID and raw challenge data. */
public class WebPageAttestationContext(
    public val challenge: String,
    public val eventId: String,
) {
    override fun toString(): String = "WebPageAttestationContext(challenge=${challenge.length} chars, eventId=${eventId.length} chars)"
}

/** Returns the paired page challenge and event ID, or null when the page is not usable. */
public fun extractWebPageAttestationContext(webPage: String): WebPageAttestationContext? {
    requireSize(webPage, WEB_PAGE_LIMIT, "web page")
    val code = maskNonCode(webPage)
    val challenge = findChallenge(webPage, code) ?: return null
    val eventId = findEventId(webPage, code) ?: return null
    return WebPageAttestationContext(challenge = encodeChallenge(challenge), eventId = eventId)
}

/** Returns the BotGuard challenge from a page, or null when it cannot be extracted. */
public fun extractWebPageAttestationChallenge(webPage: String): String? {
    requireSize(webPage, WEB_PAGE_LIMIT, "web page")
    val challenge = findChallenge(webPage, maskNonCode(webPage)) ?: return null
    return encodeChallenge(challenge)
}

private fun findChallenge(
    source: String,
    code: String,
): JsonElement? {
    for (call in ATTESTATION_CALL.findAll(code)) {
        val wrapper = callObjectRange(code, call.range.last) ?: continue
        val rawChallenge = challengeValue(source, code, wrapper) ?: continue
        parseChallenge(rawChallenge)?.let { return it }
    }
    for (assignment in ATTESTATION_ASSIGNMENT.findAll(code)) {
        val start = skipTrivia(source, assignment.range.last + 1)
        val rawChallenge = decodeString(source, start) ?: continue
        parseChallenge(rawChallenge)?.let { return it }
    }
    return null
}

private fun findEventId(
    source: String,
    code: String,
): String? {
    for (call in CONFIG_CALL.findAll(code)) {
        val range = callObjectRange(code, call.range.last) ?: continue
        val config = parseObject(source.substring(range)) ?: continue
        val eventId = runCatching { config["EVENT_ID"]?.jsonPrimitive?.contentOrNull }.getOrNull()
        if (!eventId.isNullOrBlank() && eventId.length <= IDENTIFIER_LIMIT) return eventId
    }
    return null
}

private fun challengeValue(
    source: String,
    code: String,
    range: IntRange,
): String? {
    val wrapper = source.substring(range)
    val parsed = parseObject(wrapper)
    parsed?.get("R")?.let { value ->
        return if (value is JsonPrimitive && value.isString) value.content else value.toString()
    }

    val wrapperCode = code.substring(range)
    val property = R_PROPERTY.find(wrapperCode) ?: return null
    val start = skipTrivia(source, range.first + property.range.last + 1)
    if (start >= source.length) return null
    return when (source[start]) {
        '{' -> balancedObject(source, code, start)
        '\'', '"' -> decodeString(source, start)
        else -> null
    }
}

private fun parseChallenge(raw: String): JsonElement? = parseObject(raw)?.get("bgChallenge")

private fun parseObject(value: String): JsonObject? =
    runCatching {
        Json.parseToJsonElement(value).jsonObject.also(::requireFieldCount)
    }.getOrNull()

private fun encodeChallenge(challenge: JsonElement): String =
    Json.encodeToString(JsonObject.serializer(), JsonObject(mapOf("bgChallenge" to challenge)))

private fun callObjectRange(
    code: String,
    openParenthesis: Int,
): IntRange? {
    var cursor = openParenthesis + 1
    while (cursor < code.length && code[cursor] != ')' && code[cursor] != '{') cursor++
    if (cursor >= code.length || code[cursor] != '{') return null
    val endExclusive = balancedObjectEnd(code, cursor) ?: return null
    return cursor until endExclusive
}

private fun balancedObject(
    source: String,
    code: String,
    start: Int,
): String? {
    val endExclusive = balancedObjectEnd(code, start) ?: return null
    return source.substring(start, endExclusive)
}

private fun balancedObjectEnd(
    code: String,
    start: Int,
): Int? {
    if (code.getOrNull(start) != '{') return null
    var depth = 0
    var cursor = start
    while (cursor < code.length) {
        when (code[cursor]) {
            '{' -> depth++
            '}' -> {
                depth--
                if (depth == 0) return cursor + 1
            }
        }
        cursor++
    }
    return null
}

private fun decodeString(
    source: String,
    start: Int,
): String? {
    val quote = source.getOrNull(start)?.takeIf { it == '\'' || it == '"' } ?: return null
    val value = StringBuilder()
    var cursor = start + 1
    while (cursor < source.length) {
        val char = source[cursor++]
        if (char == quote) return value.toString()
        if (char != '\\') {
            value.append(char)
            continue
        }
        if (cursor >= source.length) return null
        when (val escaped = source[cursor++]) {
            '\'', '"', '\\', '/' -> value.append(escaped)
            'b' -> value.append('\b')
            'f' -> value.append('\u000c')
            'n' -> value.append('\n')
            'r' -> value.append('\r')
            't' -> value.append('\t')
            'u', 'x' -> {
                val digits = if (escaped == 'u') 4 else 2
                if (cursor + digits > source.length) return null
                val codePoint = source.substring(cursor, cursor + digits).toIntOrNull(16) ?: return null
                value.append(codePoint.toChar())
                cursor += digits
            }
            '\n' -> Unit
            '\r' -> if (source.getOrNull(cursor) == '\n') cursor++
            else -> value.append(escaped)
        }
    }
    return null
}

private fun skipTrivia(
    source: String,
    from: Int,
): Int {
    var cursor = from
    while (cursor < source.length) {
        when {
            source[cursor].isWhitespace() -> cursor++
            source.startsWith("//", cursor) -> {
                while (cursor < source.length && source[cursor] != '\n' && source[cursor] != '\r') cursor++
            }
            source.startsWith("/*", cursor) -> {
                val end = source.indexOf("*/", cursor + 2)
                if (end < 0) return source.length
                cursor = end + 2
            }
            else -> return cursor
        }
    }
    return cursor
}

private fun maskNonCode(source: String): String {
    val chars = source.toCharArray()
    var cursor = 0
    while (cursor < source.length) {
        when {
            source.startsWith("<!--", cursor) -> {
                val start = cursor
                val end = source.indexOf("-->", cursor + 4)
                cursor = if (end < 0) source.length else end + 3
                blank(chars, start, cursor)
            }
            source.startsWith("//", cursor) -> {
                val start = cursor
                while (cursor < source.length && source[cursor] != '\n' && source[cursor] != '\r') cursor++
                blank(chars, start, cursor)
            }
            source.startsWith("/*", cursor) -> {
                val start = cursor
                cursor += 2
                while (cursor < source.length && !source.startsWith("*/", cursor)) cursor++
                cursor = (cursor + 2).coerceAtMost(source.length)
                blank(chars, start, cursor)
            }
            source[cursor] == '\'' || source[cursor] == '"' || source[cursor] == '`' -> {
                val start = cursor
                val quote = source[cursor++]
                while (cursor < source.length) {
                    if (source[cursor] == '\\') {
                        cursor = (cursor + 2).coerceAtMost(source.length)
                    } else if (source[cursor++] == quote) {
                        break
                    }
                }
                blank(chars, start, cursor)
            }
            else -> cursor++
        }
    }
    return String(chars)
}

private fun blank(
    chars: CharArray,
    start: Int,
    end: Int,
) {
    for (index in start until end) {
        if (chars[index] != '\n' && chars[index] != '\r') chars[index] = ' '
    }
}

internal fun requireSize(
    value: String,
    limit: Int,
    name: String,
) {
    if (value.length > limit) throw PoTokenException("$name exceeds its size limit")
}

internal fun requireFieldCount(value: JsonObject) {
    if (value.size > JSON_FIELD_LIMIT) throw PoTokenException("JSON object has too many fields")
}
