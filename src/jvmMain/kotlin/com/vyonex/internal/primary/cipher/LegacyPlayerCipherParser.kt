// bxane
package com.vyonex.internal.primary.cipher

internal actual fun legacyPlayerCipherFunctions(playerCode: String): LegacyPlayerCipherFunctions {
    val parsed = PlayerScriptParser.parse(playerCode)
    return LegacyPlayerCipherFunctions(
        solverScript = PlayerScriptParser.generateSolverScript(parsed),
        hasNTransform = parsed.nFunctionCode != null,
        hasSignatureTransform = parsed.sigFunctionCode != null,
    )
}
