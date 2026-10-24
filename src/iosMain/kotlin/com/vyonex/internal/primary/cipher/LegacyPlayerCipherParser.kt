// bxane
package com.vyonex.internal.primary.cipher

internal actual fun legacyPlayerCipherFunctions(playerCode: String): LegacyPlayerCipherFunctions =
    LegacyPlayerCipherFunctions(solverScript = "", hasNTransform = false, hasSignatureTransform = false)
