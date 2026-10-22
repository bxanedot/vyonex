// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.cipher

import com.vyonex.internal.fallback.utils.JavaScript
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CipherScriptSupportTest {
    private val playerCode = """
        var nTransform = function(a) { a = a.split(""); a.reverse(); return a.join(""); };
        var signatureTransform = function(a) { a = a.split(""); a.splice(0, 1); return a.join(""); };
    """.trimIndent()

    private val helperPlayerCode = """
        var transforms = {
            reverse: function(a) { a.reverse(); },
            drop: function(a) { a.splice(0, 1); }
        };
        var nTransform = function(value) {
            value = value.split(''); transforms.reverse(value); return value.join('');
        };
        var signatureTransform = function(value) {
            value = value.split(''); transforms.drop(value); return value.join('');
        };
    """.trimIndent()

    @Test
    fun runsSignatureTransformFromPlayerCode() {
        val script = requireNotNull(CipherScriptSupport.signatureSolverScript(playerCode))
        assertEquals("bcdef", JavaScript.run(script, "deobfuscate", "abcdef"))
    }

    @Test
    fun runsThrottlingTransformFromPlayerCode() {
        val script = requireNotNull(CipherScriptSupport.nSolverScript(playerCode))
        assertEquals("fedcba", JavaScript.run(script, "_solveN", "abcdef"))
    }

    @Test
    fun supportsQuotedSeparatorsAndNestedHelperObjects() {
        val signatureScript =
            requireNotNull(CipherScriptSupport.signatureSolverScript(helperPlayerCode))
        val nScript = requireNotNull(CipherScriptSupport.nSolverScript(helperPlayerCode))

        assertEquals("bcdef", JavaScript.run(signatureScript, "deobfuscate", "abcdef"))
        assertEquals("fedcba", JavaScript.run(nScript, "_solveN", "abcdef"))
    }
}
