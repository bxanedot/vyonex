// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.cipher

internal data class ParsedPlayerCipherScript(
    val script: String,
    val factoryName: String,
)

internal expect object PlayerCipherScriptParser {
    fun parse(source: String): ParsedPlayerCipherScript?

    fun insertIntoPlayerWrapper(
        source: String,
        statements: String,
    ): String?
}
