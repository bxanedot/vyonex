// bxane
package com.vyonex.internal.primary.cipher

import kotlinx.coroutines.CancellationException

private const val N_PROBE_INPUT = "KdrqFlzJXl9EcCwlmEy"
private const val SIGNATURE_FUNCTION = "__vyonexConfiguredSignature"
private const val N_FUNCTION = "__vyonexConfiguredThrottle"
private val VALID_N_RESULT = Regex("^[A-Za-z0-9_-]+$")

internal fun configuredCipherPlayerUrl(playerUrl: String): String? =
    RemotePlayerConfigParser.extractPlayerHash(playerUrl)?.let { hash ->
        "https://www.youtube.com/s/player/$hash/player_ias.vflset/en_GB/base.js"
    }

internal class PlayerConfigCipherSolver private constructor(
    private val engine: QuickJsEngine,
) {
    companion object {
        suspend fun create(
            playerCode: String,
            config: RemotePlayerConfigParser.HardcodedPlayerConfig,
        ): PlayerConfigCipherSolver {
            val engine = QuickJsEngine()
            try {
                engine.initialize()
                engine.setupYoutubeGlobals()
                engine.execute("globalThis._yt_player = globalThis._yt_player || {};")
                engine.execute(buildModifiedPlayerScript(playerCode, config))

                val probe = engine.callFunction(N_FUNCTION, N_PROBE_INPUT)
                check(
                    probe != null &&
                        probe != N_PROBE_INPUT &&
                        probe.length >= 5 &&
                        VALID_N_RESULT.matches(probe),
                ) { "configured throttle transform did not pass its probe" }
                return PlayerConfigCipherSolver(engine)
            } catch (error: CancellationException) {
                engine.dispose()
                throw error
            } catch (error: Exception) {
                engine.dispose()
                throw error
            }
        }

        internal fun buildModifiedPlayerScript(
            playerCode: String,
            config: RemotePlayerConfigParser.HardcodedPlayerConfig,
        ): String {
            val signature = requireNotNull(config.sigJsExpression).replace("INPUT", "input")
            val throttle = requireNotNull(config.nJsExpression).replace("INPUT", "input")
            val bridge =
                ";globalThis.$SIGNATURE_FUNCTION=function(input){try{return $signature;}catch(_){return null;}};" +
                    "globalThis.$N_FUNCTION=function(input){try{return $throttle;}catch(_){return input;}};"
            return PlayerCipherScriptParser.insertIntoPlayerWrapper(playerCode, bridge)
                ?: "$playerCode\n$bridge"
        }
    }

    suspend fun solveSignature(input: String): String? = call(SIGNATURE_FUNCTION, input)?.takeUnless(String::isBlank)

    suspend fun solveN(input: String): String? = call(N_FUNCTION, input)?.takeUnless { it.isBlank() || it == input }

    private suspend fun call(
        functionName: String,
        input: String,
    ): String? =
        try {
            engine.callFunction(functionName, input)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }

    suspend fun dispose() = engine.dispose()
}
