// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.linkHandler

import com.vyonex.internal.fallback.VyonEx
import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.exceptions.ReCaptchaException
import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory
import com.vyonex.internal.fallback.services.bandcamp.BandcampArtistDetailsParseException
import com.vyonex.internal.fallback.services.bandcamp.BandcampArtistDetailsParser
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampExtractorHelper
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
            return BandcampArtistDetailsParser.parseBandId(data).toString()
        } catch (error: IOException) {
            throw ParsingException("Could not read artist data", error)
        } catch (error: ReCaptchaException) {
            throw ParsingException("Could not read artist data", error)
        } catch (error: BandcampArtistDetailsParseException) {
            throw ParsingException("Could not read artist data", error)
        }
    }

    @Throws(ParsingException::class)
    override fun getUrl(id: String, contentFilter: List<String>, sortFilter: String): String {
        val details = BandcampExtractorHelper.getArtistDetailsPayload(id)
        if (details.hasError) throw ParsingException("Artist ID has no Bandcamp profile")
        return BandcampUrl.https(details.url?.takeIf(String::isNotBlank)
            ?: throw ParsingException("Artist response has no Bandcamp URL"))
    }

    @Throws(ParsingException::class)
    override fun onAcceptUrl(url: String): Boolean = BandcampUrl.isArtistPage(url)
}
