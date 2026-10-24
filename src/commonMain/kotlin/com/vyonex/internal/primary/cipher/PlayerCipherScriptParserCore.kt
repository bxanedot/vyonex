// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.cipher

private const val MAX_PLAYER_SCRIPT_LENGTH = 8 * 1024 * 1024
private const val FACTORY_EXPORT = "globalThis.__vyonexCipherFactory"

private enum class TokenKind { IDENTIFIER, STRING, OTHER }

private data class PlayerToken(
    val text: String,
    val start: Int,
    val end: Int,
    val kind: TokenKind,
    val roundDepth: Int,
    val curlyDepth: Int,
    val squareDepth: Int,
    var matching: Int = -1,
)

private data class PlayerWrapper(
    val bodyOpen: Int,
    val bodyClose: Int,
    val roundDepth: Int,
    val curlyDepth: Int,
    val squareDepth: Int,
)

private data class FunctionRegion(
    val name: String?,
    val bodyStart: Int,
    val bodyEnd: Int,
    val after: Int,
)

internal object PlayerCipherScriptParserCore {
    fun parse(source: String): ParsedPlayerCipherScript? {
        if (source.isBlank() || source.length > MAX_PLAYER_SCRIPT_LENGTH) return null
        val tokens = tokenizePlayerScript(source) ?: return null
        val wrapper = findPlayerWrapper(tokens) ?: return null
        val name = findTransformFactory(tokens, wrapper) ?: return null
        val insertionPoint = tokens[wrapper.bodyClose].start
        return ParsedPlayerCipherScript(
            script = source.substring(0, insertionPoint) + "$FACTORY_EXPORT=$name;\n" + source.substring(insertionPoint),
            factoryName = name,
        )
    }

    fun insertIntoPlayerWrapper(
        source: String,
        statements: String,
    ): String? {
        if (source.isBlank() || source.length > MAX_PLAYER_SCRIPT_LENGTH) return null
        val tokens = tokenizePlayerScript(source) ?: return null
        val wrapper = findPlayerWrapper(tokens) ?: return null
        val insertionPoint = tokens[wrapper.bodyClose].start
        return source.substring(0, insertionPoint) + statements + source.substring(insertionPoint)
    }

    private fun findPlayerWrapper(tokens: List<PlayerToken>): PlayerWrapper? {
        for (open in tokens.indices) {
            val token = tokens[open]
            if (token.text != "(" || token.roundDepth != 0 || token.curlyDepth != 0 || token.squareDepth != 0) continue
            val outerClose = token.matching
            if (outerClose <= open) continue
            val function = functionAt(tokens, open + 1, outerClose) ?: continue
            val callOpen = outerClose + 1
            if (callOpen >= tokens.size || tokens[callOpen].text != "(") continue
            val callClose = tokens[callOpen].matching
            if (callClose <= callOpen || callOpen + 1 >= callClose) continue
            val argument = tokens[callOpen + 1]
            if (argument.kind != TokenKind.IDENTIFIER || argument.text != "_yt_player") continue
            val body = tokens[function.bodyStart]
            return PlayerWrapper(
                bodyOpen = function.bodyStart,
                bodyClose = function.bodyEnd,
                roundDepth = body.roundDepth,
                curlyDepth = body.curlyDepth + 1,
                squareDepth = body.squareDepth,
            )
        }
        return null
    }

    private fun findTransformFactory(
        tokens: List<PlayerToken>,
        wrapper: PlayerWrapper,
    ): String? {
        var index = wrapper.bodyOpen + 1
        while (index < wrapper.bodyClose) {
            if (!atWrapperLevel(tokens[index], wrapper)) {
                index++
                continue
            }
            when {
                tokens[index].text in CONTROL_STATEMENTS -> index = skipControlStatement(tokens, index, wrapper)
                tokens[index].kind == TokenKind.IDENTIFIER &&
                    tokens.getOrNull(index + 1)?.text == ":" ->
                    index = skipStatement(tokens, index + 2, wrapper)
                tokens[index].text == "function" || tokens[index].text == "async" -> {
                    val function = functionAt(tokens, index, wrapper.bodyClose)
                    if (function?.name != null && createsTransformUrl(tokens, function.bodyStart, function.bodyEnd)) {
                        return function.name
                    }
                    index = function?.after ?: index + 1
                }
                tokens[index].text in VARIABLE_DECLARATIONS -> {
                    val result = variableFactory(tokens, index, wrapper)
                    if (result.name != null) return result.name
                    index = result.nextIndex.coerceAtLeast(index + 1)
                }
                tokens[index].kind == TokenKind.IDENTIFIER &&
                    tokens.getOrNull(index + 1)?.text == "=" -> {
                    val function =
                        functionAt(tokens, index + 2, wrapper.bodyClose)
                            ?: arrowAt(tokens, index + 2, wrapper.bodyClose, wrapper)
                    if (function != null && createsTransformUrl(tokens, function.bodyStart, function.bodyEnd)) {
                        return tokens[index].text
                    }
                    index = function?.after ?: index + 1
                }
                else -> index++
            }
        }
        return null
    }

    private data class VariableResult(val name: String?, val nextIndex: Int)

    private fun variableFactory(
        tokens: List<PlayerToken>,
        declaration: Int,
        wrapper: PlayerWrapper,
    ): VariableResult {
        var index = declaration + 1
        while (index < wrapper.bodyClose && atWrapperLevel(tokens[index], wrapper)) {
            if (tokens[index].kind != TokenKind.IDENTIFIER) return VariableResult(null, index + 1)
            val name = tokens[index].text
            index++
            if (tokens.getOrNull(index)?.text == "=") {
                val expression = index + 1
                val function =
                    functionAt(tokens, expression, wrapper.bodyClose)
                        ?: arrowAt(tokens, expression, wrapper.bodyClose, wrapper)
                if (function != null) {
                    if (createsTransformUrl(tokens, function.bodyStart, function.bodyEnd)) {
                        return VariableResult(name, function.after)
                    }
                    index = function.after
                } else {
                    index = expression
                }
            }
            val separator = nextDeclarationSeparator(tokens, index, wrapper)
            if (separator >= wrapper.bodyClose) return VariableResult(null, separator)
            when (tokens[separator].text) {
                "," -> index = separator + 1
                ";" -> return VariableResult(null, separator + 1)
                else -> return VariableResult(null, separator)
            }
        }
        return VariableResult(null, index)
    }

    private fun functionAt(
        tokens: List<PlayerToken>,
        start: Int,
        limit: Int,
    ): FunctionRegion? {
        var index = start
        if (tokens.getOrNull(index)?.text == "async") index++
        if (tokens.getOrNull(index)?.text != "function") return null
        index++
        if (tokens.getOrNull(index)?.text == "*") index++
        var name: String? = null
        if (tokens.getOrNull(index)?.kind == TokenKind.IDENTIFIER) {
            name = tokens[index].text
            index++
        }
        if (index >= limit || tokens[index].text != "(") return null
        val parametersEnd = tokens[index].matching
        val bodyOpen = parametersEnd + 1
        if (parametersEnd <= index || bodyOpen >= limit || tokens[bodyOpen].text != "{") return null
        val bodyEnd = tokens[bodyOpen].matching
        if (bodyEnd <= bodyOpen || bodyEnd >= limit) return null
        return FunctionRegion(name, bodyOpen, bodyEnd, bodyEnd + 1)
    }

    private fun arrowAt(
        tokens: List<PlayerToken>,
        start: Int,
        limit: Int,
        wrapper: PlayerWrapper,
    ): FunctionRegion? {
        var index = start
        if (tokens.getOrNull(index)?.text == "async") index++
        while (index < limit) {
            val token = tokens[index]
            if (atWrapperLevel(token, wrapper) && token.text == "=>") {
                val bodyStart = index + 1
                if (tokens.getOrNull(bodyStart)?.text == "{") {
                    val bodyEnd = tokens[bodyStart].matching
                    if (bodyEnd <= bodyStart) return null
                    return FunctionRegion(null, bodyStart, bodyEnd, bodyEnd + 1)
                }
                val end = nextDeclarationSeparator(tokens, bodyStart, wrapper).coerceAtMost(limit)
                return FunctionRegion(null, bodyStart, end, end)
            }
            if (atWrapperLevel(token, wrapper) && token.text in setOf(",", ";")) return null
            if (token.text in OPEN_DELIMITERS && token.matching > index) {
                index = token.matching + 1
            } else {
                index++
            }
        }
        return null
    }

    private fun createsTransformUrl(
        tokens: List<PlayerToken>,
        start: Int,
        end: Int,
    ): Boolean {
        for (index in start until (end - 7)) {
            if (tokens[index].kind == TokenKind.IDENTIFIER &&
                tokens[index + 1].text == "." &&
                tokens[index + 2].text == "set" &&
                tokens[index + 3].text == "(" &&
                tokens[index + 4].kind == TokenKind.STRING &&
                tokens[index + 4].text == "alr" &&
                tokens[index + 5].text == "," &&
                tokens.getOrNull(index + 6)?.kind == TokenKind.STRING &&
                tokens[index + 6].text == "yes" &&
                tokens[index + 7].text == ")"
            ) {
                return true
            }
        }
        return false
    }

    private fun nextDeclarationSeparator(
        tokens: List<PlayerToken>,
        start: Int,
        wrapper: PlayerWrapper,
    ): Int {
        var index = start
        while (index < wrapper.bodyClose) {
            val token = tokens[index]
            if (atWrapperLevel(token, wrapper) && token.text in setOf(",", ";")) return index
            if (token.text in OPEN_DELIMITERS && token.matching > index) {
                index = token.matching + 1
            } else {
                index++
            }
        }
        return wrapper.bodyClose
    }

    private fun skipControlStatement(
        tokens: List<PlayerToken>,
        start: Int,
        wrapper: PlayerWrapper,
    ): Int {
        val keyword = tokens[start].text
        var index = start + 1
        if (keyword in CONDITIONS && tokens.getOrNull(index)?.text == "(") {
            index = tokens[index].matching + 1
        }
        index = skipStatement(tokens, index, wrapper)
        if (keyword == "if" && tokens.getOrNull(index)?.text == "else") {
            index = skipStatement(tokens, index + 1, wrapper)
        } else if (keyword == "try") {
            while (tokens.getOrNull(index)?.text in setOf("catch", "finally")) {
                if (tokens.getOrNull(index + 1)?.text == "(") index = tokens[index + 1].matching + 1 else index++
                index = skipStatement(tokens, index, wrapper)
            }
        }
        return index
    }

    private fun skipStatement(
        tokens: List<PlayerToken>,
        start: Int,
        wrapper: PlayerWrapper,
    ): Int {
        if (start >= wrapper.bodyClose) return wrapper.bodyClose
        if (tokens[start].text == "{" && tokens[start].matching > start) return tokens[start].matching + 1
        if (tokens[start].text in CONTROL_STATEMENTS) return skipControlStatement(tokens, start, wrapper)
        var index = start
        while (index < wrapper.bodyClose) {
            val token = tokens[index]
            if (atWrapperLevel(token, wrapper) && token.text == ";") return index + 1
            if (token.text in OPEN_DELIMITERS && token.matching > index) {
                index = token.matching + 1
            } else {
                index++
            }
        }
        return index
    }

    private fun atWrapperLevel(
        token: PlayerToken,
        wrapper: PlayerWrapper,
    ): Boolean =
        token.roundDepth == wrapper.roundDepth &&
            token.curlyDepth == wrapper.curlyDepth &&
            token.squareDepth == wrapper.squareDepth

    private fun tokenizePlayerScript(source: String): List<PlayerToken>? {
        val tokens = ArrayList<PlayerToken>()
        val delimiters = ArrayList<Int>()
        var index = 0
        var roundDepth = 0
        var curlyDepth = 0
        var squareDepth = 0

        fun append(
            text: String,
            start: Int,
            end: Int,
            kind: TokenKind = TokenKind.OTHER,
        ): Int {
            tokens.add(PlayerToken(text, start, end, kind, roundDepth, curlyDepth, squareDepth))
            return tokens.lastIndex
        }

        while (index < source.length) {
            val char = source[index]
            if (char.isWhitespace()) {
                index++
                continue
            }
            if (char == '/' && source.getOrNull(index + 1) == '/') {
                index = skipLineComment(source, index + 2)
                continue
            }
            if (char == '/' && source.getOrNull(index + 1) == '*') {
                index = skipBlockComment(source, index + 2) ?: return null
                continue
            }
            if (char == '\'' || char == '"') {
                val string = readString(source, index) ?: return null
                append(string.first, index, string.second, TokenKind.STRING)
                index = string.second
                continue
            }
            if (char.code == 96) {
                val end = skipTemplate(source, index) ?: return null
                append("", index, end)
                index = end
                continue
            }
            if (char == '/' && regexCanStart(tokens)) {
                val end = skipRegex(source, index) ?: return null
                append("", index, end)
                index = end
                continue
            }
            if (isIdentifierStart(char)) {
                val start = index++
                while (source.getOrNull(index)?.let(::isIdentifierPart) == true) index++
                append(source.substring(start, index), start, index, TokenKind.IDENTIFIER)
                continue
            }
            if (char.isDigit()) {
                val start = index++
                while (source.getOrNull(index)?.let { it.isLetterOrDigit() || it == '_' || it == '.' } == true) index++
                append(source.substring(start, index), start, index)
                continue
            }

            val operator = OPERATORS.firstOrNull { source.startsWith(it, index) }
            val text = operator ?: char.toString()
            val start = index
            index += text.length
            when (text) {
                "(" -> {
                    val open = append(text, start, index)
                    delimiters.add(open)
                    roundDepth++
                }
                "[" -> {
                    val open = append(text, start, index)
                    delimiters.add(open)
                    squareDepth++
                }
                "{" -> {
                    val open = append(text, start, index)
                    delimiters.add(open)
                    curlyDepth++
                }
                ")", "]", "}" -> {
                    when (text) {
                        ")" -> if (roundDepth-- <= 0) return null
                        "]" -> if (squareDepth-- <= 0) return null
                        "}" -> if (curlyDepth-- <= 0) return null
                    }
                    val close = append(text, start, index)
                    if (delimiters.isEmpty()) return null
                    val open = delimiters.removeAt(delimiters.lastIndex)
                    if (tokens[open].text != OPEN_FOR_CLOSE[text]) return null
                    tokens[open].matching = close
                    tokens[close].matching = open
                }
                else -> append(text, start, index)
            }
        }
        return tokens.takeIf { delimiters.isEmpty() && roundDepth == 0 && curlyDepth == 0 && squareDepth == 0 }
    }

    private fun regexCanStart(tokens: List<PlayerToken>): Boolean {
        val previous = tokens.lastOrNull() ?: return true
        if (previous.kind == TokenKind.IDENTIFIER) return previous.text in REGEX_PREFIX_KEYWORDS
        if (previous.kind == TokenKind.STRING) return false
        if (previous.text in REGEX_PREFIX_OPERATORS) return true
        if (previous.text == ")") {
            val open = previous.matching
            return open > 0 && tokens[open - 1].text in setOf("if", "while", "for", "with", "switch", "catch")
        }
        return false
    }

    private fun readString(
        source: String,
        start: Int,
    ): Pair<String, Int>? {
        val quote = source[start]
        val value = StringBuilder()
        var index = start + 1
        while (index < source.length) {
            val char = source[index++]
            if (char == quote) return value.toString() to index
            if (char == '\n' || char == '\r') return null
            if (char != '\\') {
                value.append(char)
                continue
            }
            if (index >= source.length) return null
            when (val escaped = source[index++]) {
                '\n' -> Unit
                '\r' -> if (source.getOrNull(index) == '\n') index++
                'n' -> value.append('\n')
                'r' -> value.append('\r')
                't' -> value.append('\t')
                'b' -> value.append('\b')
                'f' -> value.append('\u000c')
                'v' -> value.append('\u000b')
                'x' -> {
                    val end = index + 2
                    if (end > source.length) return null
                    value.append(source.substring(index, end).toIntOrNull(16)?.toChar() ?: return null)
                    index = end
                }
                'u' -> {
                    if (source.getOrNull(index) == '{') {
                        val close = source.indexOf('}', index + 1)
                        if (close < 0) return null
                        value.append(source.substring(index + 1, close).toIntOrNull(16)?.toChar() ?: return null)
                        index = close + 1
                        continue
                    }
                    val end = index + 4
                    if (end > source.length) return null
                    value.append(source.substring(index, end).toIntOrNull(16)?.toChar() ?: return null)
                    index = end
                }
                else -> value.append(escaped)
            }
        }
        return null
    }

    private fun skipLineComment(
        source: String,
        start: Int,
    ): Int {
        var index = start
        while (index < source.length && source[index] != '\n' && source[index] != '\r') index++
        return index
    }

    private fun skipBlockComment(
        source: String,
        start: Int,
    ): Int? {
        var index = start
        while (index + 1 < source.length) {
            if (source[index] == '*' && source[index + 1] == '/') return index + 2
            index++
        }
        return null
    }

    private fun skipRegex(
        source: String,
        start: Int,
    ): Int? {
        var index = start + 1
        var inClass = false
        var escaped = false
        while (index < source.length) {
            val char = source[index++]
            if (char == '\n' || char == '\r') return null
            if (escaped) {
                escaped = false
            } else if (char == '\\') {
                escaped = true
            } else if (char == '[') {
                inClass = true
            } else if (char == ']') {
                inClass = false
            } else if (char == '/' && !inClass) {
                while (source.getOrNull(index)?.let(::isIdentifierPart) == true) index++
                return index
            }
        }
        return null
    }

    private fun skipTemplate(
        source: String,
        start: Int,
    ): Int? {
        var index = start + 1
        while (index < source.length) {
            when {
                source[index] == '\\' -> index = (index + 2).coerceAtMost(source.length)
                source[index].code == 96 -> return index + 1
                source[index] == '$' && source.getOrNull(index + 1) == '{' ->
                    index = skipTemplateExpression(source, index + 2) ?: return null
                else -> index++
            }
        }
        return null
    }

    private fun skipTemplateExpression(
        source: String,
        start: Int,
    ): Int? {
        var index = start
        var depth = 1
        while (index < source.length) {
            when {
                source[index] == '\'' || source[index] == '"' ->
                    index = readString(source, index)?.second ?: return null
                source[index].code == 96 -> index = skipTemplate(source, index) ?: return null
                source[index] == '/' && source.getOrNull(index + 1) == '/' -> index = skipLineComment(source, index + 2)
                source[index] == '/' && source.getOrNull(index + 1) == '*' ->
                    index = skipBlockComment(source, index + 2) ?: return null
                source[index] == '{' -> {
                    depth++
                    index++
                }
                source[index] == '}' -> {
                    depth--
                    if (depth == 0) return index + 1
                    index++
                }
                else -> index++
            }
        }
        return null
    }

    private fun isIdentifierStart(char: Char): Boolean = char == '$' || char == '_' || char.isLetter()

    private fun isIdentifierPart(char: Char): Boolean = isIdentifierStart(char) || char.isDigit()

    private val OPEN_DELIMITERS = setOf("(", "[", "{")
    private val OPEN_FOR_CLOSE = mapOf(")" to "(", "]" to "[", "}" to "{")
    private val CONTROL_STATEMENTS = setOf("if", "for", "while", "switch", "with", "try", "do")
    private val CONDITIONS = setOf("if", "for", "while", "switch", "with")
    private val VARIABLE_DECLARATIONS = setOf("var", "let", "const")
    private val REGEX_PREFIX_KEYWORDS = setOf("return", "throw", "case", "delete", "void", "typeof", "instanceof", "in", "of", "yield", "await", "else", "do")
    private val REGEX_PREFIX_OPERATORS = setOf("(", "[", "{", "=", ":", ",", ";", "!", "?", "=>", "&&", "||", "??", "+", "-", "*", "%", "&", "|", "^", "~", "<", ">", "==", "===", "!=", "!==")
    private val OPERATORS = listOf(">>>=", "===", "!==", ">>>", "**=", "=>", "==", "!=", "<=", ">=", "&&", "||", "??", "?.", "++", "--", "+=", "-=", "*=", "/=", "%=", "**", "<<", ">>", "&=", "|=", "^=", "...")
}
