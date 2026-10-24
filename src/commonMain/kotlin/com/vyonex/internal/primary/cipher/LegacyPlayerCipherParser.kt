// bxane
package com.vyonex.internal.primary.cipher

internal data class LegacyPlayerCipherFunctions(
    val solverScript: String,
    val hasNTransform: Boolean,
    val hasSignatureTransform: Boolean,
)

internal expect fun legacyPlayerCipherFunctions(playerCode: String): LegacyPlayerCipherFunctions
