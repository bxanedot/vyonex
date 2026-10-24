// bxane
package com.vyonex.internal.primary.cipher

import com.vyonex.internal.primary.InnerTubeLogger
import com.vyonex.internal.primary.d
import com.vyonex.internal.primary.utils.sha1
import com.vyonex.internal.primary.w
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Resolves player signature and throttling transforms using VyonEx-owned parsing. */
internal class PlayerCipherChallengeSolver(
    private val engine: QuickJsEngine,
    private val logger: InnerTubeLogger,
) {
    companion object {
        private const val TAG = "PlayerCipherChallengeSolver"
        private const val MAX_PREPARED_PLAYERS = 4
        private const val MAX_PLAYER_JS_LENGTH = 8 * 1024 * 1024
        private const val MAX_CHALLENGE_LENGTH = 64 * 1024
        private const val MAX_CHALLENGES = 256
        private const val MAX_CHALLENGE_PAYLOAD_LENGTH = 8 * 1024 * 1024
        private const val MAX_SOLVER_OUTPUT_LENGTH = 256 * 1024
        private const val CACHE_HEADER = "VYONEX_PLAYER_CIPHER_V1\n"
        private val CACHE_FACTORY_NAME = Regex("^[A-Za-z_$][A-Za-z0-9_$]*$")
        private val parserFingerprint = sha1("VyonExPlayerCipherParser:v1")
    }

    data class SolveResult(
        val sigByChallenge: Map<String, String>,
        val nByChallenge: Map<String, String>,
    )

    private val preparedMutex = Mutex()
    private val preparedPlayers = LinkedHashMap<String, ParsedPlayerCipherScript>()
    private val playerSolverMutex = Mutex()
    private var playerSolverUrl: String? = null
    private var playerSolver: PlayerCipherUrlSolver? = null
    private var readPreparedPlayer: suspend (String) -> String? = { null }
    private var writePreparedPlayer: suspend (String, String?) -> Unit = { _, _ -> }

    suspend fun ensureReady() {
        engine.initialize()
        engine.setupYoutubeGlobals()
    }

    fun setPreparedPlayerScriptCache(
        read: suspend (String) -> String?,
        write: suspend (String, String?) -> Unit,
    ) {
        readPreparedPlayer = read
        writePreparedPlayer = write
    }

    suspend fun solve(
        playerUrl: String,
        fullPlayerJs: String,
        requestOrder: List<Pair<String, List<String>>>,
        preferPrepared: Boolean = true,
    ): SolveResult {
        if (requestOrder.isEmpty() || requestOrder.all { it.second.isEmpty() }) {
            return SolveResult(emptyMap(), emptyMap())
        }
        if (fullPlayerJs.length > MAX_PLAYER_JS_LENGTH ||
            requestOrder.sumOf { it.second.size } > MAX_CHALLENGES ||
            requestOrder.sumOf { (_, challenges) -> challenges.sumOf { it.length.toLong() } } > MAX_CHALLENGE_PAYLOAD_LENGTH ||
            requestOrder.any { (_, challenges) -> challenges.any { it.length > MAX_CHALLENGE_LENGTH } }
        ) {
            return SolveResult(emptyMap(), emptyMap())
        }

        return try {
            ensureReady()
            val playerResult = solveWithPlayerUrlTransforms(playerUrl, fullPlayerJs, requestOrder, preferPrepared)
            if (playerResult.solvesAll(requestOrder)) return playerResult
            val parserResult = solveWithPlayerParser(fullPlayerJs, requestOrder)
            SolveResult(
                sigByChallenge = parserResult.sigByChallenge + playerResult.sigByChallenge,
                nByChallenge = parserResult.nByChallenge + playerResult.nByChallenge,
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            logger.w(TAG, "player cipher solve failed player=${playerUrl.logId()} type=${error.logType()}")
            SolveResult(emptyMap(), emptyMap())
        }
    }

    suspend fun dispose() {
        playerSolverMutex.withLock {
            playerSolver?.dispose()
            playerSolver = null
            playerSolverUrl = null
        }
        preparedMutex.withLock { preparedPlayers.clear() }
    }

    private suspend fun solveWithPlayerUrlTransforms(
        playerUrl: String,
        playerCode: String,
        requestOrder: List<Pair<String, List<String>>>,
        preferPrepared: Boolean,
    ): SolveResult {
        val solver =
            playerSolverMutex.withLock {
                if (playerSolverUrl == playerUrl) return@withLock playerSolver
                playerSolver?.dispose()
                playerSolver = null
                playerSolverUrl = null

                val cacheKey = playerScriptCacheKey(playerUrl)
                val parsed =
                    (if (preferPrepared) loadPreparedPlayer(playerUrl, cacheKey) else null)
                        ?: PlayerCipherScriptParser.parse(playerCode)
                if (parsed != null) persistPreparedPlayer(playerUrl, cacheKey, parsed)
                parsed?.let { PlayerCipherUrlSolver.create(it) }.also {
                    playerSolverUrl = playerUrl
                    playerSolver = it
                }
            } ?: return SolveResult(emptyMap(), emptyMap())

        val sigMap = mutableMapOf<String, String>()
        val nMap = mutableMapOf<String, String>()
        for ((kind, challenges) in requestOrder) {
            for (challenge in challenges.distinct()) {
                val solved =
                    when (kind) {
                        "sig" -> solver.solveSignature(challenge)
                        "n" -> solver.solveN(challenge)
                        else -> null
                    }?.takeIf { it.isNotBlank() && it.length <= MAX_SOLVER_OUTPUT_LENGTH }
                if (solved != null) {
                    when (kind) {
                        "sig" -> sigMap[challenge] = solved
                        "n" -> nMap[challenge] = solved
                    }
                }
            }
        }
        return SolveResult(sigMap, nMap)
    }

    private suspend fun solveWithPlayerParser(
        playerCode: String,
        requestOrder: List<Pair<String, List<String>>>,
    ): SolveResult {
        val parsed = legacyPlayerCipherFunctions(playerCode)
        if (!parsed.hasNTransform && !parsed.hasSignatureTransform) return SolveResult(emptyMap(), emptyMap())
        val parserScript = parsed.solverScript.takeIf(String::isNotBlank) ?: return SolveResult(emptyMap(), emptyMap())
        val isolatedScript =
            """
            (function() {
              delete globalThis._solveN;
              delete globalThis._solveSig;
              $parserScript
              globalThis._solveN = typeof _solveN === "function" ? _solveN : undefined;
              globalThis._solveSig = typeof _solveSig === "function" ? _solveSig : undefined;
            })();
            """.trimIndent()

        return try {
            engine.execute(isolatedScript)
            val sigMap = mutableMapOf<String, String>()
            val nMap = mutableMapOf<String, String>()
            for ((kind, challenges) in requestOrder) {
                val functionName = when (kind) {
                    "sig" -> "_solveSig"
                    "n" -> "_solveN"
                    else -> continue
                }
                for (challenge in challenges.distinct()) {
                    val solved =
                        try {
                            engine.callFunction(functionName, challenge)
                        } catch (error: CancellationException) {
                            throw error
                        } catch (error: Exception) {
                            logger.d(TAG, "player function parser failed kind=$kind type=${error.logType()}")
                            null
                        }?.takeIf { it.isNotBlank() && it.length <= MAX_SOLVER_OUTPUT_LENGTH }
                    if (solved != null) {
                        when (kind) {
                            "sig" -> sigMap[challenge] = solved
                            "n" -> nMap[challenge] = solved
                        }
                    }
                }
            }
            SolveResult(sigMap, nMap)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            logger.d(TAG, "player function parser failed type=${error.logType()}")
            SolveResult(emptyMap(), emptyMap())
        }
    }

    private suspend fun loadPreparedPlayer(
        playerUrl: String,
        cacheKey: String?,
    ): ParsedPlayerCipherScript? {
        preparedMutex.withLock {
            preparedPlayers.remove(playerUrl)?.also {
                preparedPlayers[playerUrl] = it
                return it
            }
        }
        cacheKey ?: return null
        val stored =
            try {
                readPreparedPlayer(cacheKey)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                null
            } ?: return null
        val parsed = decodePreparedPlayer(stored)
        if (parsed == null) {
            safeWritePreparedPlayer(cacheKey, null)
            return null
        }
        preparedMutex.withLock { rememberPreparedPlayer(playerUrl, parsed) }
        return parsed
    }

    private suspend fun persistPreparedPlayer(
        playerUrl: String,
        cacheKey: String?,
        parsed: ParsedPlayerCipherScript,
    ) {
        preparedMutex.withLock { rememberPreparedPlayer(playerUrl, parsed) }
        cacheKey?.let { safeWritePreparedPlayer(it, encodePreparedPlayer(parsed)) }
    }

    private suspend fun safeWritePreparedPlayer(
        cacheKey: String,
        value: String?,
    ) {
        try {
            writePreparedPlayer(cacheKey, value)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // Cache failures must not affect extraction.
        }
    }

    private fun rememberPreparedPlayer(
        playerUrl: String,
        parsed: ParsedPlayerCipherScript,
    ) {
        preparedPlayers.remove(playerUrl)
        preparedPlayers[playerUrl] = parsed
        while (preparedPlayers.size > MAX_PREPARED_PLAYERS) {
            preparedPlayers.remove(preparedPlayers.keys.first())
        }
    }

    private fun encodePreparedPlayer(parsed: ParsedPlayerCipherScript): String =
        "$CACHE_HEADER${parsed.factoryName}\n${parsed.script}"

    private fun decodePreparedPlayer(encoded: String): ParsedPlayerCipherScript? {
        if (encoded.length > MAX_PLAYER_JS_LENGTH + CACHE_HEADER.length + 128 || !encoded.startsWith(CACHE_HEADER)) return null
        val nameStart = CACHE_HEADER.length
        val nameEnd = encoded.indexOf('\n', nameStart)
        if (nameEnd < 0) return null
        val name = encoded.substring(nameStart, nameEnd)
        if (!CACHE_FACTORY_NAME.matches(name)) return null
        val script = encoded.substring(nameEnd + 1)
        if (script.isBlank() || script.length > MAX_PLAYER_JS_LENGTH ||
            "globalThis.__vyonexCipherFactory=$name;" !in script
        ) {
            return null
        }
        return ParsedPlayerCipherScript(script, name)
    }

    private fun playerScriptCacheKey(playerUrl: String): String? =
        playerScriptCacheKey(playerUrl, parserFingerprint)

    private fun String.logId(): String = RemotePlayerConfigParser.extractPlayerHash(this) ?: "unknown"

    private fun Throwable.logType(): String = this::class.simpleName ?: "Exception"
}

internal fun playerScriptCacheKey(
    playerUrl: String,
    parserFingerprint: String,
): String? {
    if (!Regex("^[a-f0-9]{40}$").matches(parserFingerprint)) return null
    runCatching { validatedPlayerScriptUrl(playerUrl) }.getOrNull() ?: return null
    return sha1("$playerUrl:$parserFingerprint")
}

private fun PlayerCipherChallengeSolver.SolveResult.solvesAll(requestOrder: List<Pair<String, List<String>>>): Boolean =
    requestOrder.all { (kind, challenges) ->
        val solved = when (kind) {
            "sig" -> sigByChallenge
            "n" -> nByChallenge
            else -> return@all false
        }
        challenges.all(solved::containsKey)
    }
