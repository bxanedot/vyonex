// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors

import com.vyonex.internal.fallback.InfoItem
import com.vyonex.internal.fallback.MetaInfo
import com.vyonex.internal.fallback.MultiInfoItemsCollector
import com.vyonex.internal.fallback.Page
import com.vyonex.internal.fallback.StreamingService
import com.vyonex.internal.fallback.downloader.Downloader
import com.vyonex.internal.fallback.exceptions.ExtractionException
import com.vyonex.internal.fallback.linkhandler.SearchQueryHandler
import com.vyonex.internal.fallback.search.SearchExtractor
import com.vyonex.internal.fallback.services.bandcamp.extractors.streaminfoitem.BandcampSearchStreamInfoItemExtractor
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.IOException
import java.net.URI
import java.util.Locale

public class BandcampSearchExtractor(
    service: StreamingService,
    queryHandler: SearchQueryHandler,
) : SearchExtractor(service, queryHandler) {
    override fun getSearchSuggestion(): String = ""

    override fun isCorrectedSearch(): Boolean = false

    override fun getMetaInfo(): List<MetaInfo> = emptyList()

    @Throws(IOException::class, ExtractionException::class)
    override fun getPage(page: Page): InfoItemsPage<InfoItem> {
        val html = downloader.get(
            page.url,
            mapOf("Cookie" to listOf("identity")),
        ).responseBody()
        val document = Jsoup.parse(html, page.url)
        val items = MultiInfoItemsCollector(serviceId)
        for (card in document.select(".searchresult")) {
            val label = card.selectFirst(".result-info .itemtype") ?: continue
            when (label.text().trim().uppercase(Locale.ROOT)) {
                "ARTIST" -> items.commit(BandcampChannelInfoItemExtractor(card))
                "ALBUM" -> items.commit(BandcampPlaylistInfoItemExtractor(card))
                "TRACK" -> items.commit(BandcampSearchStreamInfoItemExtractor(card, null))
            }
        }
        return InfoItemsPage(items, findNextPage(document, page.url))
    }

    @Throws(IOException::class, ExtractionException::class)
    override fun getInitialPage(): InfoItemsPage<InfoItem> = getPage(Page(getUrl()))

    override fun onFetchPage(downloader: Downloader) = Unit

    public companion object {
        @JvmStatic
        public fun findNextPage(document: Document, currentUrl: String): Page? {
            val direct: Element? = document.selectFirst(
                ".pagelist a.next[href], .pagelist a[rel~=next][href], .pagelist li.next a[href]",
            )
            val pagination: Element? = document.selectFirst(".pagelist")
            val selected: Element? = if (pagination == null) null else selectedPage(pagination)
            val following: Element? = if (selected == null) null else selected.nextElementSibling()
            val fallback: Element? = if (following == null) null else following.selectFirst("a[href]")
            val next: Element = direct ?: fallback ?: return null

            return try {
                val base = URI.create(currentUrl)
                val target = resolvePageLink(base, URI.create(next.attr("href"))).normalize()
                if (!sameWebOrigin(base, target)) return null
                if (base.rawPath == target.rawPath && base.rawQuery == target.rawQuery) return null
                Page(target.toString())
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
}
