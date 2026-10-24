// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube

import com.vyonex.internal.fallback.VyonEx
import com.vyonex.internal.fallback.downloader.Response
import com.vyonex.internal.fallback.exceptions.ExtractionException
import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.exceptions.ReCaptchaException
import java.io.IOException

public class PeertubeInstance(
    private val instanceUrl: String,
    private var instanceName: String,
) {
    public constructor(url: String) : this(url, "PeerTube")

    public fun getUrl(): String = instanceUrl

    public fun getName(): String = instanceName

    @Throws(Exception::class)
    public fun fetchInstanceMetaData() {
        val downloader = VyonEx.getDownloader()
            ?: throw Exception("VyonEx has not been initialized")
        val response: Response = try {
            downloader.get("$instanceUrl/api/v1/config")
        } catch (error: ReCaptchaException) {
            throw Exception("Unable to load PeerTube instance metadata from $instanceUrl", error)
        } catch (error: IOException) {
            throw Exception("Unable to load PeerTube instance metadata from $instanceUrl", error)
        }

        try {
            val config = PeertubeParsingHelper.readApiObject(response, "instance configuration")
            val discoveredName = config.getObject("instance").getString("name")
            if (discoveredName.isNullOrBlank()) {
                throw ParsingException("PeerTube configuration has no instance name")
            }
            instanceName = discoveredName
        } catch (error: ExtractionException) {
            throw Exception("Unable to parse PeerTube instance metadata", error)
        }
    }

    public companion object {
        @JvmField
        public val DEFAULT_INSTANCE = PeertubeInstance("https://framatube.org", "FramaTube")
    }
}
