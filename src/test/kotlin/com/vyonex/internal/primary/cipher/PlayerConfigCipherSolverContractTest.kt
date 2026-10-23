// bxane
package com.vyonex.internal.primary.cipher

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PlayerConfigCipherSolverContractTest {
    @Test
    fun configuredTransformsDoNotRewriteTrailerTextInsidePlayerStrings() = runBlocking {
        val playerCode =
            """
            const marker = "})(_yt_player);";
            (function(_yt_player) {
              function transform(a, b, input) { return marker + input; }
            })(_yt_player);
            """.trimIndent()
        val solver = PlayerConfigCipherSolver.create(playerCode, config())
        try {
            assertEquals("})(_yt_player);ok", solver.solveSignature("ok"))
            assertEquals("cba", solver.solveN("abc"))
        } finally {
            solver.dispose()
        }
    }

    private fun config() =
        RemotePlayerConfigParser.HardcodedPlayerConfig(
            sigFuncName = "sig",
            sigConstantArg = null,
            sigJsExpression = "transform(1,2,INPUT)",
            nFuncName = "n",
            nArrayIndex = null,
            nConstantArgs = null,
            nJsExpression = "INPUT.split('').reverse().join('')",
            signatureTimestamp = 1,
        )
}
