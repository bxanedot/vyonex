// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors

import com.vyonex.internal.fallback.exceptions.ExtractionException
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URI

internal sealed interface BandcampSearchItem {
    data class Artist(
        val name: String,
        val url: String,
        val description: String,
        val imageUrl: String,
    ) : BandcampSearchItem

    data class Album(
        val name: String,
        val url: String,
        val uploaderName: String,
        val trackCount: Long,
        val imageUrl: String,
    ) : BandcampSearchItem

    data class Track(
        val name: String,
        val url: String,
        val uploaderName: String,
        val imageUrl: String,
    ) : BandcampSearchItem
}

internal data class BandcampSearchPage(
    val items: List<BandcampSearchItem>,
    val nextPageUrl: String?,
)

internal object BandcampSearchPageParser {
    private val trackCount = Regex("^\\s*(\\d+)")

    fun parse(html: String, currentUrl: String): BandcampSearchPage {
        val document = Jsoup.parse(html, currentUrl)
        if (isBotCheck(document)) {
            throw ExtractionException("Bandcamp search returned a bot check instead of results")
        }
        val items = document.select(".searchresult").mapNotNull(::parseItem)
        return BandcampSearchPage(items, findNextPageUrl(document, currentUrl))
    }

    private fun isBotCheck(document: Document): Boolean {
        if (document.selectFirst("form#captcha, .captcha, form[id*=challenge], [data-sitekey]") != null) {
            return true
        }
        if (document.selectFirst(".searchresult") != null) return false
        val text = document.body().text()
        return listOf("verify you are human", "unusual traffic", "automated requests")
            .any { text.contains(it, ignoreCase = true) }
    }

    private fun parseItem(card: Element): BandcampSearchItem? {
        val info = card.selectFirst(".result-info") ?: return null
        val type = info.selectFirst(".itemtype")?.text()?.trim()?.uppercase() ?: return null
        val name = info.selectFirst(".heading")?.text().orEmpty()
        val url = info.selectFirst(".itemurl")?.text().orEmpty()
        val subtitle = info.selectFirst(".subhead")?.text().orEmpty()
        val imageUrl = card.selectFirst(".art img[src]")?.attr("src").orEmpty()
        return when (type) {
            "ARTIST" -> BandcampSearchItem.Artist(name, url, subtitle, imageUrl)
            "ALBUM" -> BandcampSearchItem.Album(
                name = name,
                url = url,
                uploaderName = artistFrom(subtitle),
                trackCount = trackCount.find(info.selectFirst(".length")?.text().orEmpty())
                    ?.groupValues?.get(1)?.toLongOrNull() ?: -1L,
                imageUrl = imageUrl,
            )
            "TRACK" -> BandcampSearchItem.Track(name, url, artistFrom(subtitle), imageUrl)
            else -> null
        }
    }

    private fun artistFrom(subtitle: String): String {
        val separator = subtitle.lastIndexOf(" by ", ignoreCase = true)
        return if (separator < 0) subtitle else subtitle.substring(separator + 4).trim()
    }

    fun findNextPageUrl(document: Document, currentUrl: String): String? {
        val direct = document.selectFirst(
            ".pagelist a.next[href], .pagelist a[rel~=next][href], .pagelist li.next a[href]",
        )
        val pagination = document.selectFirst(".pagelist")
        val selected = pagination?.let(::selectedPage)
        val following = selected?.nextElementSibling()
        val fallback = following?.selectFirst("a[href]")
        val next = direct ?: fallback ?: return null

        return try {
            val base = URI.create(currentUrl)
            val target = resolvePageLink(base, URI.create(next.attr("href"))).normalize()
            if (!sameWebOrigin(base, target)) return null
            if (base.rawPath == target.rawPath && base.rawQuery == target.rawQuery) return null
            target.toString()
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: NullPointerException) {
            null
        }
    }

    private fun resolvePageLink(base: URI, link: URI): URI {
        if (link.scheme == null && link.rawAuthority == null &&
            link.rawPath.isNullOrEmpty() && link.rawQuery != null
        ) {
            val path = base.rawPath.orEmpty()
            val fragment = link.rawFragment?.let { "#$it" }.orEmpty()
            return URI.create("${base.scheme}://${base.rawAuthority}$path?${link.rawQuery}$fragment")
        }
        return base.resolve(link)
    }

    private fun selectedPage(pagination: Element): Element? =
        pagination.selectFirst("li.current, li.selected, li.active")
            ?: pagination.selectFirst("li span")?.closest("li")

    private fun sameWebOrigin(base: URI, target: URI): Boolean {
        val baseScheme = base.scheme ?: return false
        val targetScheme = target.scheme ?: return false
        val baseHost = base.host ?: return false
        val targetHost = target.host ?: return false
        if (base.rawUserInfo != null || target.rawUserInfo != null) return false
        if (!isWebScheme(baseScheme) || !isWebScheme(targetScheme)) return false
        return baseScheme.equals(targetScheme, ignoreCase = true) &&
            baseHost.equals(targetHost, ignoreCase = true) &&
            effectivePort(base) == effectivePort(target)
    }

    private fun isWebScheme(scheme: String): Boolean =
        scheme.equals("http", ignoreCase = true) || scheme.equals("https", ignoreCase = true)

    private fun effectivePort(uri: URI): Int = when {
        uri.port >= 0 -> uri.port
        uri.scheme.equals("https", ignoreCase = true) -> 443
        else -> 80
    }
}
