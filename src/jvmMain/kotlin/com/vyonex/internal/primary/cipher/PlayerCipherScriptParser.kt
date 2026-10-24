// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.cipher

internal actual object PlayerCipherScriptParser {
    actual fun parse(source: String): ParsedPlayerCipherScript? = PlayerCipherScriptParserCore.parse(source)

    actual fun insertIntoPlayerWrapper(
        source: String,
        statements: String,
    ): String? = PlayerCipherScriptParserCore.insertIntoPlayerWrapper(source, statements)
}
