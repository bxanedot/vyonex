// bxane
package com.vyonex.internal.primary.cipher

import io.ktor.http.Url

internal fun validatedPlayerScriptUrl(value: String): Url {
    val url = Url(value)
    val isYouTubeHost =
        url.host == "youtube.com" ||
            url.host.endsWith(".youtube.com") ||
            url.host == "youtube-nocookie.com" ||
            url.host.endsWith(".youtube-nocookie.com")
    require(
        url.protocol.name == "https" &&
            isYouTubeHost &&
            url.encodedPath.startsWith("/s/player/") &&
            url.encodedPath.endsWith(".js"),
    ) {
        "Player script URL must use an approved YouTube HTTPS endpoint"
    }
    return url
}
