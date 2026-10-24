// bxane
package com.vyonex.internal.primary.cipher

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PlayerScriptUrlValidatorContractTest {
    @Test
    fun acceptsPlayerScriptsOnApprovedYouTubeHosts() {
        val url = validatedPlayerScriptUrl(PLAYER_URL)

        assertEquals("www.youtube.com", url.host)
        assertEquals("/s/player/abc123/player_ias.vflset/en_US/base.js", url.encodedPath)
    }

    @Test
    fun rejectsNonHttpsHostsAndNonPlayerPaths() {
        listOf(
            PLAYER_URL.replace("https://", "http://"),
            PLAYER_URL.replace("www.youtube.com", "youtube.com.attacker.test"),
            PLAYER_URL.replace("/s/player/", "/watch/"),
            PLAYER_URL.removeSuffix(".js"),
        ).forEach { value ->
            assertFailsWith<IllegalArgumentException> { validatedPlayerScriptUrl(value) }
        }
    }

    private companion object {
        const val PLAYER_URL = "https://www.youtube.com/s/player/abc123/player_ias.vflset/en_US/base.js"
    }
}
