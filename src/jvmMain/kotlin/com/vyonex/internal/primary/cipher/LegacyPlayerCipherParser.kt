// bxane
package com.vyonex.internal.primary.cipher

internal actual fun legacyPlayerCipherSolverScript(playerCode: String): String? {
    val parsed = PlayerScriptParser.parse(playerCode)
    if (parsed.nFunctionCode == null && parsed.sigFunctionCode == null) return null
    return PlayerScriptParser.generateSolverScript(parsed)
}
