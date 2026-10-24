// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors

import com.vyonex.internal.fallback.Image
import com.vyonex.internal.fallback.VyonEx
import com.vyonex.internal.fallback.downloader.Downloader
import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.exceptions.ReCaptchaException
import com.vyonex.internal.fallback.services.bandcamp.BandcampArtistDetailsParseException
import com.vyonex.internal.fallback.services.bandcamp.BandcampArtistDetailsParser
import com.vyonex.internal.fallback.services.bandcamp.BandcampArtistDetailsPayload
import com.vyonex.internal.fallback.services.bandcamp.BandcampItemUrlParseException
import com.vyonex.internal.fallback.services.bandcamp.BandcampItemUrlParser
import com.vyonex.internal.fallback.services.bandcamp.linkHandler.BandcampUrlParser
import com.vyonex.internal.fallback.localization.DateWrapper
import com.vyonex.internal.fallback.services.bandcamp.linkHandler.BandcampRadioUrlParser
import com.vyonex.internal.json.JsonObject
import com.vyonex.internal.json.JsonParser
import com.vyonex.internal.json.JsonParserException
import com.vyonex.internal.json.JsonWriter
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.nio.charset.StandardCharsets
import java.time.DateTimeException
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

public object BandcampExtractorHelper {
    public const val BASE_URL = BandcampUrlParser.BASE_URL
    public const val BASE_API_URL = "$BASE_URL/api"
    private const val IMAGE_BASE_URL = "https://f4.bcbits.com/img/"
    private val DATE_FORMAT = DateTimeFormatter.ofPattern(
        "dd MMM yyyy HH:mm:ss zzz",
        Locale.ENGLISH,
    )

    @JvmStatic
    @Throws(ParsingException::class)
    public fun getStreamUrlFromIds(bandId: Long, itemId: Long, itemType: String?): String {
        val type = itemType?.takeIf(String::isNotEmpty)
            ?: throw ParsingException("Bandcamp item type is missing")
        val endpoint = "$BASE_API_URL/mobile/22/tralbum_details?band_id=$bandId&tralbum_id=$itemId&tralbum_type=${type[0]}"
        try {
            val url = BandcampItemUrlParser.parse(currentDownloader().get(endpoint).responseBody())
            return forceHttps(url)
                ?: throw ParsingException("Bandcamp item response has no URL")
        } catch (error: IOException) {
            throw ParsingException("Could not resolve Bandcamp item URL", error)
        } catch (error: ReCaptchaException) {
            throw ParsingException("Could not resolve Bandcamp item URL", error)
        } catch (error: BandcampItemUrlParseException) {
            throw ParsingException("Could not resolve Bandcamp item URL", error)
        }
    }

    @JvmStatic
    @Throws(ParsingException::class)
    public fun getArtistDetails(id: String): JsonObject = getArtistDetails(currentDownloader(), id)

    @JvmStatic
    @Throws(ParsingException::class)
    public fun getArtistDetails(downloader: Downloader, id: String): JsonObject {
        try {
            return JsonParser.`object`().from(getArtistDetailsSource(downloader, id))
        } catch (error: JsonParserException) {
            throw ParsingException("Could not load Bandcamp artist details", error)
        }
    }

    internal fun getArtistDetailsPayload(downloader: Downloader, id: String): BandcampArtistDetailsPayload =
        try {
            BandcampArtistDetailsParser.parse(getArtistDetailsSource(downloader, id))
        } catch (error: BandcampArtistDetailsParseException) {
            throw ParsingException("Could not load Bandcamp artist details", error)
        }

    internal fun getArtistDetailsPayload(id: String): BandcampArtistDetailsPayload =
        getArtistDetailsPayload(currentDownloader(), id)

    private fun getArtistDetailsSource(downloader: Downloader, id: String): String {
        val payload = JsonWriter.string().`object`().value("band_id", id).end().done()
            .toByteArray(StandardCharsets.UTF_8)
        return try {
            downloader.postWithContentTypeJson("$BASE_API_URL/mobile/22/band_details", emptyMap(), payload)
                .responseBody()
        } catch (error: IOException) {
            throw ParsingException("Could not load Bandcamp artist details", error)
        } catch (error: ReCaptchaException) {
            throw ParsingException("Could not load Bandcamp artist details", error)
        }
    }

    @JvmStatic
    public fun getImageUrl(id: Long, album: Boolean): String =
        "$IMAGE_BASE_URL${if (album) "a" else ""}${id}_10.jpg"

    @JvmStatic
    @Throws(ParsingException::class)
    public fun isArtistDomain(url: String?): Boolean {
        val uri = parseWebUri(url)
        val host = uri.host.lowercase(Locale.ROOT)
        if (host == "bandcamp.com") return false
        if (host.endsWith(".bandcamp.com")) return true
        if (VyonEx.getDownloader() == null) return false

        try {
            val html = currentDownloader().get(forceHttps(uri.toString())!!).responseBody()
            return Jsoup.parse(html).selectFirst(".cart-wrapper a[href='https://bandcamp.com/cart']") != null
        } catch (error: IOException) {
            throw ParsingException("Could not verify custom Bandcamp artist domain", error)
        } catch (error: ReCaptchaException) {
            throw ParsingException("Could not verify custom Bandcamp artist domain", error)
        }
    }

    @JvmStatic
    public fun isRadioUrl(url: String?): Boolean {
        return BandcampRadioUrlParser.accepts(url)
    }

    @JvmStatic
    @Throws(ParsingException::class)
    public fun parseDate(textDate: String?): DateWrapper {
        try {
            val date = ZonedDateTime.parse(textDate ?: throw NullPointerException(), DATE_FORMAT)
            return DateWrapper(date.toInstant())
        } catch (error: DateTimeException) {
            throw ParsingException("Could not parse Bandcamp date: $textDate", error)
        } catch (error: NullPointerException) {
            throw ParsingException("Could not parse Bandcamp date: $textDate", error)
        }
    }

    @JvmStatic
    @Throws(ParsingException::class)
    public fun getJsonAttribute(html: String, attribute: String): JsonObject {
        return try {
            JsonParser.`object`().from(getJsonAttributeText(html, attribute))
        } catch (error: JsonParserException) {
            throw ParsingException("Page contains invalid $attribute data", error)
        }
    }

    @Throws(ParsingException::class)
    internal fun getJsonAttributeText(html: String, attribute: String): String =
        Jsoup.parse(html).getElementsByAttribute(attribute).firstOrNull()?.attr(attribute)
            ?: throw ParsingException("Page is missing $attribute data")

    @JvmStatic
    public fun getImagesFromSearchResult(searchResult: Element): List<Image> =
        getImagesFromImageUrl(searchResult.selectFirst(".art img[src]")?.attr("src"))

    @JvmStatic
    public fun getImagesFromImageUrl(imageUrl: String?): List<Image> {
        if (imageUrl.isNullOrBlank()) return emptyList()
        val address = if (imageUrl.startsWith("//")) "https:$imageUrl" else imageUrl
        val uri = try {
            URI(address)
        } catch (_: URISyntaxException) {
            return emptyList()
        }
        val scheme = uri.scheme ?: return emptyList()
        if (uri.host == null || uri.userInfo != null ||
            !(scheme.equals("http", true) || scheme.equals("https", true))
        ) return emptyList()
        return listOf(
            Image(
                forceHttps(uri.toString())!!,
                Image.HEIGHT_UNKNOWN,
                Image.WIDTH_UNKNOWN,
                Image.ResolutionLevel.UNKNOWN,
            ),
        )
    }

    @JvmStatic
    public fun getImagesFromImageId(id: Long, album: Boolean): List<Image> =
        if (id == 0L) emptyList() else getImagesFromImageUrl(getImageUrl(id, album))

    private fun parseWebUri(value: String?): URI {
        val uri = try {
            URI(value ?: throw URISyntaxException("", "URL is null"))
        } catch (error: URISyntaxException) {
            throw ParsingException("Invalid Bandcamp URL: $value", error)
        }
        val scheme = uri.scheme
        if (uri.host == null || uri.userInfo != null ||
            !(scheme.equals("http", true) || scheme.equals("https", true))
        ) {
            throw ParsingException("Invalid Bandcamp URL: $value")
        }
        return uri
    }

    private fun forceHttps(value: String?): String? = value?.let {
        if (it.startsWith("http://")) "https://${it.substring(7)}" else it
    }

    @Throws(ParsingException::class)
    private fun currentDownloader(): Downloader = VyonEx.getDownloader()
        ?: throw ParsingException("VyonEx has not been initialized")
}
