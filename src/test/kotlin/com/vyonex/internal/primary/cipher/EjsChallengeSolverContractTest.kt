// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.cipher

import com.vyonex.internal.primary.InnerTubeLogger
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EjsChallengeSolverContractTest {
    @Test
    fun solvesNamedSignatureAndThrottleTransformsAsOneBatch() = runBlocking {
        val engine = QuickJsEngine()
        val solver = EjsChallengeSolver(engine, InnerTubeLogger.NONE)
        try {
            val result =
                solver.solve(
                    playerUrl = "https://www.youtube.com/s/player/abc123/player_ias.vflset/en_US/base.js",
                    fullPlayerJs = PLAYER_CODE,
                    requestOrder = listOf("sig" to listOf("abcdef"), "n" to listOf("abcdef")),
                    preferPreprocessed = false,
                )

            assertEquals("bcdef", result.sigByChallenge["abcdef"])
            assertEquals("fedcba", result.nByChallenge["abcdef"])
        } finally {
            engine.dispose()
        }
    }

    private companion object {
        const val PLAYER_CODE = """
            function nTransform(value) {
                value = value.split(""); value.reverse(); return value.join("");
            }
            function signatureTransform(value) {
                value = value.split(""); value.splice(0, 1); return value.join("");
            }
        """
    }
}
