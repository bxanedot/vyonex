// bxane
package com.vyonex.internal.primary.cipher

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PlayerCipherUrlSolverContractTest {
    @Test
    fun solvesSignatureAndNFromAnOfflinePlayerWrapper() = runBlocking {
        val solver = PlayerCipherUrlSolver.create(PLAYER_SCRIPT)
        assertNotNull(solver)
        try {
            assertEquals("eborp-erutangis-xenoyv", solver?.solveSignature("vyonex-signature-probe"))
            assertEquals("eborp-n-xenoyv", solver?.solveN("vyonex-n-probe"))
        } finally {
            solver?.dispose()
        }
    }

    @Test
    fun rejectsPlayersWithoutAWrapperOwnedTransformFactory() = runBlocking {
        assertNull(PlayerCipherUrlSolver.create("var _yt_player = {}; (function(g) {})(_yt_player);"))
    }

    private companion object {
        private const val PLAYER_SCRIPT =
            """
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
