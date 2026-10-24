// bxane
package com.vyonex.internal.primary.cipher

import com.vyonex.internal.primary.InnerTubeLogger
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PlayerCipherChallengeSolverSharedContractTest {
    @Test
    fun solvesBothTransformsThroughThePlayerUrlFactory() = runTest {
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
    fun loadsPreparedPlayerScriptWithoutTheOriginalSource() = runTest {
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
