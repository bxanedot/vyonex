// bxane
package com.vyonex.internal.primary.cipher

import kotlinx.coroutines.CancellationException

/** Executes the player-owned URL transform exposed by the YouTube player wrapper. */
internal class PlayerCipherUrlSolver private constructor(
    private val engine: QuickJsEngine,
) {
    companion object {
        private const val SIGNATURE_PROBE = "vyonex-signature-probe"
        private const val N_PROBE = "vyonex-n-probe"
        private const val PLAYER_URL = "https://youtube.com/watch?v=vyonex-probe"
        private val METHOD_NAME = Regex("^[A-Za-z_$][A-Za-z0-9_$]*$")

        suspend fun create(playerCode: String): PlayerCipherUrlSolver? =
            PlayerCipherScriptParser.parse(playerCode)?.let { create(it) }

        suspend fun create(parsed: PlayerCipherScriptParser.ParsedPlayer): PlayerCipherUrlSolver? {
            val engine = QuickJsEngine()
            try {
                engine.initialize()
                engine.setupYoutubeGlobals()
                engine.execute("globalThis._yt_player = {};")
                engine.execute(parsed.script)
                val method = engine.evaluate(findTransformMethodScript(), maxResultLength = 128)
                if (!METHOD_NAME.matches(method)) {
                    engine.dispose()
                    return null
                }
                engine.execute(createTransformFunctions(method))
                return PlayerCipherUrlSolver(engine)
            } catch (error: CancellationException) {
                engine.dispose()
                throw error
            } catch (_: Exception) {
                engine.dispose()
                return null
            }
        }

        private fun findTransformMethodScript(): String =
            """
            (function() {
              const factory = globalThis.__vyonexCipherFactory;
              const url = "$PLAYER_URL";
              const signature = "$SIGNATURE_PROBE";
              const n = "$N_PROBE";
                  const encodedSignature = encodeURIComponent(signature);
                  const initial = factory(url, "s", encodedSignature);
              if (!initial || typeof initial.set !== "function" || typeof initial.get !== "function") return "";
              for (const method of Object.getOwnPropertyNames(Object.getPrototypeOf(initial))) {
                if (["constructor", "set", "get", "clone"].includes(method)) continue;
                try {
                  const candidate = factory(url, "s", encodeURIComponent(signature));
                  candidate.set("n", n);
                  if (typeof candidate[method] !== "function") continue;
                  candidate[method]();
                  if (candidate.get("s") !== encodedSignature || candidate.get("n") !== n) return method;
                } catch (_) {
                }
              }
              return "";
            })()
            """.trimIndent()

        private fun createTransformFunctions(method: String): String {
            val methodKey = QuickJsEngine.jsStringLiteral(method)
            val playerUrl = QuickJsEngine.jsStringLiteral(PLAYER_URL)
            val signatureProbe = QuickJsEngine.jsStringLiteral(SIGNATURE_PROBE)
            return """
                globalThis.__vyonexSolveSignature = function(input) {
                  try {
                    const url = globalThis.__vyonexCipherFactory($playerUrl, "s", encodeURIComponent(input));
                    url.set("n", "$N_PROBE");
                    url[$methodKey]();
                    const value = url.get("s");
                    return value == null ? null : decodeURIComponent(String(value));
                  } catch (_) { return null; }
                };
                globalThis.__vyonexSolveN = function(input) {
                  try {
                    const url = globalThis.__vyonexCipherFactory($playerUrl, "s", encodeURIComponent($signatureProbe));
                    url.set("n", input);
                    url[$methodKey]();
                    const value = url.get("n");
                    return value == null ? null : String(value);
                  } catch (_) { return null; }
                };
            """.trimIndent()
        }
    }

    suspend fun solveSignature(input: String): String? = solve("__vyonexSolveSignature", input)

    suspend fun solveN(input: String): String? = solve("__vyonexSolveN", input)

    suspend fun dispose() = engine.dispose()

    private suspend fun solve(
        functionName: String,
        input: String,
    ): String? =
        try {
            engine.callFunction(functionName, input)?.takeIf { it.isNotBlank() && it != input }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }
}
