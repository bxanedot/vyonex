// bxane
package com.vyonex.internal.primary.cipher

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path

class PlayerCipherScriptParserContractTest {
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

    @Test
    fun exportsTheTopLevelTransformFactoryFromThePlayerWrapper() {
        val source =
            """
            var _yt_player = {};
            (function(g) {
              var helper = function(url) { url.set("alr", "yes"); return url; };
              unrelated = function(url) { url.set("alr", "yes"); return url; };
              window.ready = true;
            })(_yt_player);
            """.trimIndent()

        val parsed = requireNotNull(PlayerCipherScriptParser.parse(source))

        assertEquals("helper", parsed.factoryName)
        assertTrue(parsed.script.contains("globalThis.__vyonexCipherFactory=helper;"))
        assertTrue(parsed.script.endsWith("})(_yt_player);"))
    }

    @Test
    fun ignoresUnrelatedSetCallsAndUnsupportedWrapperShapes() {
        val decoy =
            """
            var _yt_player = {};
            (function(g) {
              if (window.decoy) {
                function unrelated(url) { url.set("alr", "yes"); }
              }
            })(_yt_player);
            """.trimIndent()

        assertNull(PlayerCipherScriptParser.parse(decoy))
        assertNull(PlayerCipherScriptParser.parse("function unrelated(url) { url.set(\"alr\", \"yes\"); }"))
    }
}
