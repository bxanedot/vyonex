// bxane
package com.vyonex.internal.primary.cipher

import com.vyonex.internal.primary.InnerTubeLogger
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PlayerCipherChallengeSolverContractTest {
    @Test
    fun solvesExistingSplitJoinSignatureAndThrottleContracts() = runBlocking {
        val engine = QuickJsEngine()
        val solver = PlayerCipherChallengeSolver(engine, InnerTubeLogger.NONE)
        try {
            val result =
                solver.solve(
                    playerUrl = PLAYER_URL,
                    fullPlayerJs = PLAYER_FUNCTIONS,
                    requestOrder = listOf("sig" to listOf("abcdef"), "n" to listOf("abcdef")),
                    preferPrepared = false,
                )

            assertEquals("bcdef", result.sigByChallenge["abcdef"])
            assertEquals("fedcba", result.nByChallenge["abcdef"])
        } finally {
            solver.dispose()
            engine.dispose()
        }
    }

    private companion object {
        const val PLAYER_URL = "https://www.youtube.com/s/player/abc123/player_ias.vflset/en_US/base.js"
        const val PLAYER_FUNCTIONS = """
            function nTransform(value) {
                value = value.split(""); value.reverse(); return value.join("");
            }
            function signatureTransform(value) {
                value = value.split(""); value.splice(0, 1); return value.join("");
            }
        """
    }
}
