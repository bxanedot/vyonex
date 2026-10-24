// bxane
package com.vyonex.internal.primary.extraction.potoken

import io.ktor.http.Url
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.io.encoding.Base64

private const val ATTESTATION_LIMIT = 4 * 1024 * 1024
private const val INTERPRETER_LIMIT = 8 * 1024 * 1024
private const val INTEGRITY_LIMIT = 1024 * 1024
private const val DECODED_LIMIT = 64 * 1024
private const val LIST_COUNT_LIMIT = 64 * 1024
private val TRUSTED_INTERPRETER_HOSTS = setOf("www.google.com", "www.gstatic.com")
private val TRUSTED_INTERPRETER_PATH = Regex("^/js/th/[A-Za-z0-9_-]{20,256}\\.js$")

/** Sensitive integrity token bytes and its server lifetime. Do not log this value. */
public class IntegrityTokenData(
    public val tokenJavaScript: String,
    public val lifetimeSeconds: Long,
) {
    override fun toString(): String = "IntegrityTokenData(tokenJavaScript=${tokenJavaScript.length} chars, lifetimeSeconds=$lifetimeSeconds)"
}

public fun parseWebPageAttestationContext(webPage: String): WebPageAttestationContext =
    extractWebPageAttestationContext(webPage)
        ?: throw PoTokenException("Web page has no paired BotGuard challenge and event ID")

public fun parseAttestationInterpreterUrl(rawAttestation: String): String {
    requireSize(rawAttestation, ATTESTATION_LIMIT, "attestation response")
    val challenge =
        parseObject(rawAttestation, "Invalid BotGuard attestation response")
            .objectField("bgChallenge", "Attestation response has no BotGuard challenge")
    return interpreterUrl(challenge)
}

public fun requireTrustedAttestationInterpreterUrl(value: String): String {
    requireSize(value, 2048, "interpreter URL")
    val url =
        runCatching { Url(if (value.startsWith("//")) "https:$value" else value) }
            .getOrElse { throw PoTokenException("BotGuard returned an invalid interpreter URL") }
    val allowedPath =
        url.encodedPath == "/botguard.js" ||
            (url.host == "www.google.com" && TRUSTED_INTERPRETER_PATH.matches(url.encodedPath))
    if (
        url.protocol.name != "https" ||
        url.port != 443 ||
        url.host !in TRUSTED_INTERPRETER_HOSTS ||
        url.user != null ||
        url.password != null ||
        !allowedPath
    ) {
        throw PoTokenException("BotGuard returned an untrusted interpreter URL")
    }
    return url.toString()
}

public fun parseAttestationChallengeData(
    rawAttestation: String,
    interpreterJavascript: String,
): String {
    requireSize(rawAttestation, ATTESTATION_LIMIT, "attestation response")
    requireSize(interpreterJavascript, INTERPRETER_LIMIT, "interpreter")
    val challenge =
        parseObject(rawAttestation, "Invalid BotGuard attestation response")
            .objectField("bgChallenge", "Attestation response has no BotGuard challenge")
    val interpreterUrl = interpreterUrl(challenge)
    val payload =
        JsonObject(
            mapOf(
                "interpreterJavascript" to
                    JsonObject(
                        mapOf(
                            "privateDoNotAccessOrElseSafeScriptWrappedValue" to JsonPrimitive(interpreterJavascript),
                            "privateDoNotAccessOrElseTrustedResourceUrlWrappedValue" to JsonPrimitive(interpreterUrl),
                        ),
                    ),
                "interpreterHash" to challenge.required("interpreterHash", "Challenge is missing interpreter hash"),
                "program" to challenge.required("program", "Challenge is missing program"),
                "globalName" to challenge.required("globalName", "Challenge is missing global name"),
            ),
        )
    return Json.encodeToString(JsonObject.serializer(), payload)
}

public fun parseIntegrityTokenData(rawIntegrityTokenData: String): IntegrityTokenData {
    requireSize(rawIntegrityTokenData, INTEGRITY_LIMIT, "integrity response")
    val values =
        runCatching { Json.parseToJsonElement(rawIntegrityTokenData).jsonArray }
            .getOrElse { throw PoTokenException("Invalid BotGuard integrity response") }
    if (values.size !in 2..3) throw PoTokenException("Invalid BotGuard integrity response")

    val encodedToken = runCatching { values[0].jsonPrimitive.content }.getOrElse { throw PoTokenException("Invalid BotGuard integrity response") }
    val lifetime =
        runCatching { values[1].jsonPrimitive.longOrNull }.getOrNull()
            ?: throw PoTokenException("Invalid BotGuard integrity response")
    if (lifetime < 0) throw PoTokenException("Invalid BotGuard integrity lifetime")
    return IntegrityTokenData(decodeBase64Token(encodedToken), lifetime)
}

public fun stringToU8(identifier: String): String {
    requireSize(identifier, 1024, "identifier")
    val bytes = identifier.encodeToByteArray()
    if (bytes.size > 1024) throw PoTokenException("identifier exceeds its size limit")
    return bytes.toUint8Array()
}

public fun u8ToBase64(poToken: String): String {
    requireSize(poToken, DECODED_LIMIT, "token byte list")
    val values = poToken.split(',')
    if (values.size > LIST_COUNT_LIMIT || values.any { it.isEmpty() || it.length > 3 }) {
        throw PoTokenException("Invalid BotGuard token byte list")
    }
    val bytes = ByteArray(values.size)
    values.forEachIndexed { index, value ->
        val byte = value.toIntOrNull() ?: throw PoTokenException("Invalid BotGuard token byte")
        if (byte !in 0..255) throw PoTokenException("BotGuard token byte is out of range")
        bytes[index] = byte.toByte()
    }
    return Base64.UrlSafe.encode(bytes)
}

private fun interpreterUrl(challenge: JsonObject): String {
    val urlFields = challenge.objectField("interpreterUrl", "Attestation response has no BotGuard interpreter URL")
    val rawUrl = urlFields.stringField(
        "privateDoNotAccessOrElseTrustedResourceUrlWrappedValue",
        "Attestation response has no BotGuard interpreter URL",
    )
    requireSize(rawUrl, INTERPRETER_LIMIT, "interpreter URL")
    return requireTrustedAttestationInterpreterUrl(rawUrl)
}

private fun parseObject(
    json: String,
    invalidMessage: String,
): JsonObject =
    runCatching { Json.parseToJsonElement(json).jsonObject }
        .getOrElse { throw PoTokenException(invalidMessage) }
        .also(::requireFieldCount)

private fun JsonObject.objectField(
    key: String,
    missingMessage: String,
): JsonObject =
    (this[key] as? JsonObject)
        ?.also(::requireFieldCount)
        ?: throw PoTokenException(missingMessage)

private fun JsonObject.stringField(
    key: String,
    missingMessage: String,
): String =
    (this[key] as? JsonPrimitive)
        ?.takeIf { it.isString }
        ?.content
        ?: throw PoTokenException(missingMessage)

private fun JsonObject.required(
    key: String,
    missingMessage: String,
): JsonElement = this[key] ?: throw PoTokenException(missingMessage)

private fun decodeBase64Token(encoded: String): String {
    requireSize(encoded, DECODED_LIMIT, "integrity token")
    val normalized = encoded.replace('+', '-').replace('/', '_').replace('.', '=')
    if (normalized.length % 4 == 1 || normalized.dropLastWhile { it == '=' }.contains('=')) {
        throw PoTokenException("Cannot decode BotGuard base64 payload")
    }
    val padded = normalized + "=".repeat((4 - normalized.length % 4) % 4)
    val bytes =
        runCatching { Base64.UrlSafe.decode(padded) }
            .getOrElse { throw PoTokenException("Cannot decode BotGuard base64 payload") }
    if (bytes.size > DECODED_LIMIT) throw PoTokenException("BotGuard integrity token is too large")
    return bytes.toUint8Array()
}

private fun ByteArray.toUint8Array(): String =
    "new Uint8Array([${joinToString(",") { (it.toInt() and 0xff).toString() }}])"
