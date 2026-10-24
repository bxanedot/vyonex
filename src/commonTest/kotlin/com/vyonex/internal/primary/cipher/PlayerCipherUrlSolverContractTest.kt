// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.cipher

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PlayerCipherUrlSolverContractTest {
    @Test
    fun solvesSignatureAndNFromAnOfflinePlayerWrapper() = runTest {
        val solver = assertNotNull(PlayerCipherUrlSolver.create(PLAYER_SCRIPT))
        try {
            assertEquals("eborp-erutangis-xenoyv", solver.solveSignature("vyonex-signature-probe"))
            assertEquals("eborp-n-xenoyv", solver.solveN("vyonex-n-probe"))
        } finally {
            solver.dispose()
        }
    }

    @Test
    fun rejectsPlayersWithoutAWrapperOwnedTransformFactory() = runTest {
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
