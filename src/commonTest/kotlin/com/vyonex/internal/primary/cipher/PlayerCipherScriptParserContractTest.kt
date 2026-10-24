// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.cipher

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlayerCipherScriptParserContractTest {
    @Test
    fun exportsTheTopLevelTransformFactoryFromThePlayerWrapper() {
        val parsed = requireNotNull(PlayerCipherScriptParser.parse(PLAYER_SCRIPT))

        assertEquals("helper", parsed.factoryName)
        assertTrue(parsed.script.contains("globalThis.__vyonexCipherFactory=helper;"))
        assertTrue(parsed.script.endsWith("})(_yt_player);"))
    }

    @Test
    fun ignoresUnrelatedCallsInsideNestedStatementsAndText() {
        val decoy =
            """
            var _yt_player = {};
            (function(g) {
              var text = "fake = function(u) { u.set('alr', 'yes'); }";
              var pattern = /url\.set\("alr", "yes"\)/;
              if (window.decoy) {
                function unrelated(url) { url.set("alr", "yes"); }
              }
            })(_yt_player);
            """.trimIndent()

        assertNull(PlayerCipherScriptParser.parse(decoy))
        assertNull(PlayerCipherScriptParser.parse("function unrelated(url) { url.set(\"alr\", \"yes\"); }"))
    }

    @Test
    fun recognizesFunctionDeclarationsAndVariableInitializers() {
        val declaration =
            """
            var _yt_player = {};
            (function(g) {
              function transform(url) { url.set("alr", "yes"); return url; }
            })(_yt_player);
            """.trimIndent()
        val variable =
            """
            var _yt_player = {};
            (function(g) {
              var transform = function(url) { url.set("alr", "yes"); return url; };
            })(_yt_player);
            """.trimIndent()

        assertEquals("transform", PlayerCipherScriptParser.parse(declaration)?.factoryName)
        assertEquals("transform", PlayerCipherScriptParser.parse(variable)?.factoryName)
    }

    @Test
    fun recognizesArrowFactoriesAndIgnoresUnbracedControlStatements() {
        val arrow =
            """
            var _yt_player = {};
            (function(g) {
              const transform = (url) => { url.set("\x61lr", "y\u0065s"); return url; };
            })(_yt_player);
            """.trimIndent()
        val nested =
            """
            var _yt_player = {};
            (function(g) {
              if (window.disabled) transform = function(url) { url.set("alr", "yes"); };
            })(_yt_player);
            """.trimIndent()
        val extraArgument =
            """
            var _yt_player = {};
            (function(g) {
              transform = function(url) { url.set("alr", "yes", 0); };
            })(_yt_player);
            """.trimIndent()

        assertEquals("transform", PlayerCipherScriptParser.parse(arrow)?.factoryName)
        assertNull(PlayerCipherScriptParser.parse(nested))
        assertNull(PlayerCipherScriptParser.parse(extraArgument))
    }

    @Test
    fun insertsStatementsBeforeTheWrapperCloses() {
        val source = PLAYER_SCRIPT
        val inserted = requireNotNull(PlayerCipherScriptParser.insertIntoPlayerWrapper(source, ";globalThis.ready=true;"))

        assertTrue(inserted.contains(";globalThis.ready=true;}"))
        assertTrue(inserted.endsWith("})(_yt_player);"))
    }

    @Test
    fun rejectsOversizedAndUnsupportedPlayerScripts() {
        assertNull(PlayerCipherScriptParser.parse(" ".repeat(8 * 1024 * 1024 + 1)))
        assertNull(PlayerCipherScriptParser.insertIntoPlayerWrapper("var _yt_player = {};", ";globalThis.ready=true;"))
    }

    private companion object {
        private val PLAYER_SCRIPT =
            """
            var _yt_player = {};
            (function(g) {
              var helper = function(url) { url.set("alr", "yes"); return url; };
              unrelated = function(url) { url.set("alr", "yes"); return url; };
              window.ready = true;
            })(_yt_player);
            """.trimIndent()
    }
}
