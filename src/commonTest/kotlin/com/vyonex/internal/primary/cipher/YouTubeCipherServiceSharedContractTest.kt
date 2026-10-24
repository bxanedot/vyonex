// bxane
package com.vyonex.internal.primary.cipher

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class YouTubeCipherServiceSharedContractTest {
    @Test
    fun downloadsPlayerAndSolvesThrottleChallengeWithSharedParser() = runTest {
        val requests = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            requests += request.url.toString()
            respond(
                content = ByteReadChannel(URL_FACTORY_PLAYER),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/javascript"),
            )
        })
        val service = YouTubeCipherService(client)
        try {
            assertEquals(
                "https://r.googlevideo.com/videoplayback?n=cba",
                service.processNParameter(PLAYER_URL, "https://r.googlevideo.com/videoplayback?n=abc"),
            )
            assertEquals(listOf(PLAYER_URL), requests)
        } finally {
            service.dispose()
            client.close()
        }
    }

    private companion object {
        const val PLAYER_URL = "https://www.youtube.com/s/player/abc123/player_ias.vflset/en_US/base.js"
        const val URL_FACTORY_PLAYER = """
            var _yt_player = {};
            (function(g) {
              var Transform = function(url, key, signature) { this.values = {s: signature || "", n: ""}; };
              Transform.prototype.set = function(key, value) { this.values[key] = value; };
              Transform.prototype.get = function(key) { return this.values[key]; };
              Transform.prototype.clone = function() { return new Transform("", "", this.values.s); };
              Transform.prototype.apply = function() {
                this.values.s = this.values.s.split("").reverse().join("");
                this.values.n = this.values.n.split("").reverse().join("");
              };
              makeUrl = function(url, key, signature) {
                var result = new Transform(url, key, signature);
                result.set("alr", "yes");
                return result;
              };
            })(_yt_player);
        """
    }
}
