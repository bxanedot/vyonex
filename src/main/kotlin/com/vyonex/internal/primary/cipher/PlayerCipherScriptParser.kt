// bxane
package com.vyonex.internal.primary.cipher

import org.mozilla.javascript.Context
import org.mozilla.javascript.CompilerEnvirons
import org.mozilla.javascript.Node
import org.mozilla.javascript.Parser
import org.mozilla.javascript.ast.Assignment
import org.mozilla.javascript.ast.AstNode
import org.mozilla.javascript.ast.Block
import org.mozilla.javascript.ast.ExpressionStatement
import org.mozilla.javascript.ast.FunctionCall
import org.mozilla.javascript.ast.FunctionNode
import org.mozilla.javascript.ast.Name
import org.mozilla.javascript.ast.ParenthesizedExpression
import org.mozilla.javascript.ast.PropertyGet
import org.mozilla.javascript.ast.StringLiteral
import org.mozilla.javascript.ast.VariableDeclaration

/** Locates YouTube's URL-transform factory without importing its player-parser implementation. */
internal object PlayerCipherScriptParser {
    private const val MAX_PLAYER_SCRIPT_LENGTH = 8 * 1024 * 1024
    private const val FACTORY_EXPORT = "globalThis.__vyonexCipherFactory"

    data class ParsedPlayer(
        val script: String,
        val factoryName: String,
    )

    fun parse(source: String): ParsedPlayer? {
        val wrapper = findPlayerWrapper(source) ?: return null
        val entryName = wrapper.body.statements().firstNotNullOfOrNull(::transformFactoryName) ?: return null

        return ParsedPlayer(
            script = insertAtWrapperEnd(source, wrapper, "$FACTORY_EXPORT=$entryName;\n"),
            factoryName = entryName,
        )
    }

    fun insertIntoPlayerWrapper(
        source: String,
        statements: String,
    ): String? {
        val wrapper = findPlayerWrapper(source) ?: return null
        return insertAtWrapperEnd(source, wrapper, statements)
    }

    private data class PlayerWrapper(
        val body: Block,
        val insertionPoint: Int,
    )

    private fun findPlayerWrapper(source: String): PlayerWrapper? {
        if (source.isBlank() || source.length > MAX_PLAYER_SCRIPT_LENGTH) return null

        val root =
            try {
                val environment = CompilerEnvirons().apply { languageVersion = Context.VERSION_ES6 }
                Parser(environment).parse(source, "player.js", 1)
            } catch (_: Exception) {
                return null
            }

        val wrapper =
            root.statements
                .asSequence()
                .filterIsInstance<ExpressionStatement>()
                .mapNotNull { it.expression as? FunctionCall }
                .mapNotNull { call ->
                    val target = call.target as? ParenthesizedExpression ?: return@mapNotNull null
                    val function = target.expression as? FunctionNode ?: return@mapNotNull null
                    val playerArgument = call.arguments.firstOrNull() as? Name ?: return@mapNotNull null
                    if (playerArgument.identifier != "_yt_player") return@mapNotNull null
                    function
                }.firstOrNull() ?: return null

        val body = wrapper.body as? Block ?: return null
        val insertionPoint = body.absolutePosition + body.length - 1
        if (insertionPoint !in source.indices || source[insertionPoint] != '}') return null

        return PlayerWrapper(body, insertionPoint)
    }

    private fun insertAtWrapperEnd(
        source: String,
        wrapper: PlayerWrapper,
        statements: String,
    ): String = source.substring(0, wrapper.insertionPoint) + statements + source.substring(wrapper.insertionPoint)

    private fun transformFactoryName(statement: AstNode): String? {
        return when (statement) {
            is ExpressionStatement -> {
                val assignment = statement.expression as? Assignment ?: return null
                val name = assignment.left as? Name ?: return null
                val function = assignment.right as? FunctionNode ?: return null
                name.identifier.takeIf { function.createsTransformUrl() }
            }

            is VariableDeclaration -> {
                statement.variables.firstNotNullOfOrNull { variable ->
                    val name = variable.target as? Name ?: return@firstNotNullOfOrNull null
                    val function = variable.initializer as? FunctionNode ?: return@firstNotNullOfOrNull null
                    name.identifier.takeIf { function.createsTransformUrl() }
                }
            }

            is FunctionNode -> statement.functionName?.identifier?.takeIf { statement.createsTransformUrl() }

            else -> null
        }
    }

    private fun FunctionNode.createsTransformUrl(): Boolean {
        var found = false
        body.visit { node ->
            if (node is FunctionCall &&
                node.target is PropertyGet &&
                (node.target as PropertyGet).property.identifier == "set" &&
                node.arguments.size == 2 &&
                (node.arguments[0] as? StringLiteral)?.value == "alr" &&
                (node.arguments[1] as? StringLiteral)?.value == "yes"
            ) {
                found = true
            }
            !found
        }
        return found
    }

    private fun Block.statements(): Sequence<AstNode> = sequence {
        var node: Node? = firstChild
        while (node != null) {
            (node as? AstNode)?.let { yield(it) }
            node = node.next
        }
    }
}
