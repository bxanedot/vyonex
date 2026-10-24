// bxane
package com.vyonex.internal.primary.cipher

import com.vyonex.internal.primary.InnerTubeLogger
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
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

    @Test
    fun solvesBothTransformsThroughThePlayerUrlFactory() = runBlocking {
        val engine = QuickJsEngine()
        val solver = PlayerCipherChallengeSolver(engine, InnerTubeLogger.NONE)
        try {
            val result =
                solver.solve(
                    playerUrl = PLAYER_URL,
                    fullPlayerJs = URL_FACTORY_PLAYER,
                    requestOrder = listOf("sig" to listOf("vyonex-signature"), "n" to listOf("vyonex-n")),
                    preferPrepared = false,
                )

            assertEquals("erutangis-xenoyv", result.sigByChallenge["vyonex-signature"])
            assertEquals("n-xenoyv", result.nByChallenge["vyonex-n"])
        } finally {
            solver.dispose()
            engine.dispose()
        }
    }

    @Test
    fun loadsPreparedPlayerScriptWithoutTheOriginalSource() = runBlocking {
        val stored = arrayOfNulls<String>(1)
        val firstEngine = QuickJsEngine()
        val first = PlayerCipherChallengeSolver(firstEngine, InnerTubeLogger.NONE)
        try {
            first.setPreparedPlayerScriptCache({ stored[0] }, { _, value -> stored[0] = value })
            first.solve(PLAYER_URL, URL_FACTORY_PLAYER, listOf("n" to listOf("vyonex-n")), preferPrepared = false)
            assertNotNull(stored[0])
            assertTrue(requireNotNull(stored[0]).startsWith("VYONEX_PLAYER_CIPHER_V1\n"))
        } finally {
            first.dispose()
            firstEngine.dispose()
        }

        val secondEngine = QuickJsEngine()
        val second = PlayerCipherChallengeSolver(secondEngine, InnerTubeLogger.NONE)
        try {
            second.setPreparedPlayerScriptCache({ stored[0] }, { _, value -> stored[0] = value })
            val result = second.solve(PLAYER_URL, "", listOf("n" to listOf("vyonex-n")))
            assertEquals("n-xenoyv", result.nByChallenge["vyonex-n"])
        } finally {
            second.dispose()
            secondEngine.dispose()
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
        const val URL_FACTORY_PLAYER = """
            var _yt_player = {};
            (function(g) {
              var Transform = function(url, key, signature) { this.values = {s: signature || "", n: ""}; };
              Transform.prototype.set = function(key, value) { this.values[key] = value; };
              Transform.prototype.get = function(key) { return this.values[key]; };
              Transform.prototype.clone = function() { return new Transform("", "", this.values.s); };
              Transform.prototype.apply = function() {
                this.values.s = this.values.s.split("").reverse().join("");
                this.values.n = this.values.n.split("").reverse().join("");
              };
              makeUrl = function(url, key, signature) {
                var result = new Transform(url, key, signature);
                result.set("alr", "yes");
                return result;
              };
            })(_yt_player);
        """
    }
}
