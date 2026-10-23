// bxane
package com.vyonex.internal.primary.extraction.potoken

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PoTokenJavaScriptContractTest {
    @Test
    fun decodesIntegrityTokenBytesAndLifetime() {
        val token = parseIntegrityTokenData("""["AQID",120]""")

        assertEquals("new Uint8Array([1,2,3])", token.tokenJavaScript)
        assertEquals(120L, token.lifetimeSeconds)
    }

    @Test
    fun rejectsNegativeIntegrityTokenLifetime() {
        assertThrows(PoTokenException::class.java) {
            parseIntegrityTokenData("""["AQID",-1]""")
        }
    }

    @Test
    fun buildsInterpreterPayloadFromAttestationData() {
        val raw =
            """{"bgChallenge":{"interpreterUrl":{"privateDoNotAccessOrElseTrustedResourceUrlWrappedValue":"https://www.gstatic.com/botguard.js"},"interpreterHash":"hash","program":"new Uint8Array([])","globalName":"window"}}"""

        val payload = parseAttestationChallengeData(raw, "const token = 1;")

        assertEquals(
            Json.parseToJsonElement(
                """{"interpreterJavascript":{"privateDoNotAccessOrElseSafeScriptWrappedValue":"const token = 1;","privateDoNotAccessOrElseTrustedResourceUrlWrappedValue":"https://www.gstatic.com/botguard.js"},"interpreterHash":"hash","program":"new Uint8Array([])","globalName":"window"}""",
            ),
            Json.parseToJsonElement(payload),
        )
    }

    @Test
    fun rejectsInterpreterUrlsOutsideTheAllowedOriginsAndPaths() {
        assertThrows(PoTokenException::class.java) {
            requireTrustedAttestationInterpreterUrl("https://www.gstatic.com.evil.test/botguard.js")
        }
    }

    @Test
    fun convertsByteListsAndUtf8Identifiers() {
        assertEquals("AQID", u8ToBase64("1,2,3"))
        assertEquals("new Uint8Array([97,195,191])", stringToU8("aÿ"))
    }
}
