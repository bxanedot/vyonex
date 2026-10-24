// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.linkHandler

import com.vyonex.internal.fallback.VyonEx
import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.exceptions.ReCaptchaException
import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampExtractorHelper
import com.vyonex.internal.json.JsonParser
import com.vyonex.internal.json.JsonParserException
import org.jsoup.Jsoup
import java.io.IOException

public object BandcampChannelLinkHandlerFactory : ListLinkHandlerFactory() {
    @JvmStatic
    public fun getInstance(): BandcampChannelLinkHandlerFactory = this

    @Throws(ParsingException::class)
    override fun getId(url: String): String {
        val downloader = VyonEx.getDownloader()
            ?: throw ParsingException("VyonEx has not been initialized")
        try {
            val html = downloader.get(BandcampUrl.https(url)).responseBody()
            val data = Jsoup.parse(html).getElementsByAttribute("data-band").attr("data-band")
            return JsonParser.`object`().from(data).getLong("id").toString()
        } catch (error: IOException) {
            throw ParsingException("Could not read artist data", error)
        } catch (error: ReCaptchaException) {
            throw ParsingException("Could not read artist data", error)
        } catch (error: JsonParserException) {
            throw ParsingException("Could not read artist data", error)
        }
    }

    @Throws(ParsingException::class)
    override fun getUrl(id: String, contentFilter: List<String>, sortFilter: String): String {
        val details = BandcampExtractorHelper.getArtistDetails(id)
        if (details.getBoolean("error")) throw ParsingException("Artist ID has no Bandcamp profile")
        return BandcampUrl.https(details.getString("bandcamp_url"))
    }

    @Throws(ParsingException::class)
    override fun onAcceptUrl(url: String): Boolean = BandcampUrl.isArtistPage(url)
}
