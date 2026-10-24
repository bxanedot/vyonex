// bxane
package com.vyonex.internal.primary.cipher

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path

class PlayerCipherDownloadedScriptContractTest {
    @Test
    fun parsesDownloadedPlayerProbeWhenConfigured() {
        val path = System.getenv("VYONEX_PLAYER_PATH") ?: return
        val source = Files.readString(Path.of(path))
        val parsed = PlayerCipherScriptParser.parse(source)
        assertTrue(parsed != null, "No transform factory found in downloaded player")
        println("VyonEx transform entry: ${parsed?.factoryName}")
        runBlocking {
          val engine = QuickJsEngine()
          try {
            engine.initialize()
            engine.setupYoutubeGlobals()
            engine.execute("globalThis._yt_player = {};")
            engine.execute(requireNotNull(parsed).script)
            val result =
                engine.evaluate(
                    """
                    (function() {
                      function solve(kind, value) {
                        const factory = globalThis.__vyonexCipherFactory;
                        const signature = kind === "sig" ? value : "vyonex-signature-probe";
                        const n = kind === "n" ? value : "vyonex-n-probe";
                        const prototype = Object.getPrototypeOf(factory("https://youtube.com/watch?v=vyonex-probe", "s", encodeURIComponent(signature)));
                        for (const method of Object.getOwnPropertyNames(prototype)) {
                          if (["constructor", "set", "get", "clone"].includes(method)) continue;
                          const url = factory("https://youtube.com/watch?v=vyonex-probe", "s", encodeURIComponent(signature));
                          url.set("n", n);
                          try { url[method](); } catch (_) { continue; }
                          const after = kind === "sig" ? url.get("s") : url.get("n");
                          if (after != null && after !== (kind === "sig" ? encodeURIComponent(value) : value)) return method + ":" + after;
                        }
                        return "none";
                      }
                      return solve("sig", "vyonex-signature-probe") + "|" + solve("n", "vyonex-n-probe");
                    })()
                    """.trimIndent(),
                    maxResultLength = 2_000,
                )
            println("VyonEx player probe: $result")
          } finally {
              engine.dispose()
          }
        }
    }

}
