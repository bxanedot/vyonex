// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.cipher

/** Java entry points for the shared player-script parser. */
object CipherScriptSupport {
    @JvmStatic
    fun signatureSolverScript(playerCode: String): String? {
        val parsed = PlayerScriptParser.parse(playerCode)
        if (parsed.sigFunctionCode == null) return null
        val script = PlayerScriptParser.generateSolverScript(
            parsed.copy(nFunctionCode = null, nInvoker = null),
        )
        return "$script\nfunction deobfuscate(input) { return _solveSig(input); }"
    }

    @JvmStatic
    fun nSolverScript(playerCode: String): String? {
        val parsed = PlayerScriptParser.parse(playerCode)
        if (parsed.nFunctionCode == null) return null
        return PlayerScriptParser.generateSolverScript(
            parsed.copy(sigFunctionCode = null, sigInvoker = null),
        )
    }
}
