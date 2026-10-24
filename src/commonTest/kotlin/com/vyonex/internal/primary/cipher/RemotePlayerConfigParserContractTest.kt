// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.cipher

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RemotePlayerConfigParserContractTest {
    @Test
    fun parsesPlayerEntriesAndExpandsAliases() {
        val result = RemotePlayerConfigParser.parse(
            """{"schemaVersion":1,"players":{"1234abcd":{"sig":"x(1,2,INPUT)","nClass":"A","sts":12345,"aliases":["abcdef01"]}}}""",
        )

        val success = assertIs<RemotePlayerConfigParser.ParseResult.Success>(result)
        assertEquals(setOf("1234abcd", "abcdef01"), success.configs.keys)
        assertEquals(12345, success.configs.getValue("abcdef01").signatureTimestamp)
        assertEquals("(function(n){try{var u=new g.A('https://x.googlevideo.com/videoplayback?n='+n,true);var t=u.get('n');return(t&&t!==n)?t:n;}catch(e){return n;}})(INPUT)", success.configs.getValue("1234abcd").nJsExpression)
    }

    @Test
    fun rejectsUnsupportedSchemasAndMalformedJson() {
        assertEquals(
            RemotePlayerConfigParser.ParseResult.Failure("unsupported schemaVersion 2 (supported: 1)"),
            RemotePlayerConfigParser.parse("""{"schemaVersion":2,"players":{}}"""),
        )
        assertEquals(
            RemotePlayerConfigParser.ParseResult.Failure("malformed JSON"),
            RemotePlayerConfigParser.parse("{"),
        )
    }

    @Test
    fun skipsMalformedEntriesAndRejectsAliasCollisions() {
        val partial = assertIs<RemotePlayerConfigParser.ParseResult.Success>(
            RemotePlayerConfigParser.parse(
                """{"schemaVersion":1,"players":{"invalid":{"sig":"x(1,2,INPUT)","nClass":"A","sts":1}}}""",
            ),
        )
        assertEquals(listOf("invalid"), partial.skippedEntries)
        assertEquals(emptyMap(), partial.configs)

        assertIs<RemotePlayerConfigParser.ParseResult.Failure>(
            RemotePlayerConfigParser.parse(
                """{"schemaVersion":1,"players":{"1234abcd":{"sig":"x(1,2,INPUT)","nClass":"A","sts":1},"abcdef01":{"sig":"x(1,2,INPUT)","nClass":"A","sts":2,"aliases":["1234abcd"]}}}""",
            ),
        )
    }

    @Test
    fun extractsKnownPlayerHashUrlShapes() {
        assertEquals("1234abcd", RemotePlayerConfigParser.extractPlayerHash("https://www.youtube.com/player/1234abcd/"))
        assertEquals("abcdef01", RemotePlayerConfigParser.extractPlayerHash("https://www.youtube.com/s/player/abcdef01/player_ias.vflset/en/base.js"))
        assertEquals(null, RemotePlayerConfigParser.extractPlayerHash("https://www.youtube.com/player/ABC12345/"))
    }
}
