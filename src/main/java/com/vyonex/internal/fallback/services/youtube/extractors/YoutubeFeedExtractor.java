// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.vyonex.internal.fallback.ListExtractor;
import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ContentNotAvailableException;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.feed.FeedExtractor;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper;
import com.vyonex.internal.fallback.stream.StreamInfoItem;
import com.vyonex.internal.fallback.stream.StreamInfoItemsCollector;

import java.io.IOException;

import javax.annotation.Nonnull;

public class YoutubeFeedExtractor extends FeedExtractor {
    private static final String WEBSITE_CHANNEL_BASE_URL = "https://www.youtube.com/channel/";

    public YoutubeFeedExtractor(final StreamingService service, final ListLinkHandler linkHandler) {
        super(service, linkHandler);
    }

    private Document document;

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        final String channelIdOrUser = getLinkHandler().getId();
        final String feedUrl = YoutubeParsingHelper.getFeedUrlFrom(channelIdOrUser);

        final Response response = downloader.get(feedUrl);
        if (response.responseCode() == 404) {
            throw new ContentNotAvailableException("Could not get feed: 404 - not found");
        }
        document = Jsoup.parse(response.responseBody());
    }

    @Nonnull
    @Override
    public ListExtractor.InfoItemsPage<StreamInfoItem> getInitialPage() {
        final Elements entries = document.select("feed > entry");
        final StreamInfoItemsCollector collector = new StreamInfoItemsCollector(getServiceId());

        for (final Element entryElement : entries) {
            collector.commit(new YoutubeFeedInfoItemExtractor(entryElement));
        }

        return new InfoItemsPage<>(collector, null);
    }

    @Nonnull
    @Override
    public String getId() {
        return getUrl().replace(WEBSITE_CHANNEL_BASE_URL, "");
    }

    @Nonnull
    @Override
    public String getUrl() {
        final Element authorUriElement = document.select("feed > author > uri")
                .first();
        if (authorUriElement != null) {
            final String authorUriElementText = authorUriElement.text();
            if (!authorUriElementText.equals("")) {
                return authorUriElementText;
            }
        }

        final Element linkElement = document.select("feed > link[rel*=alternate]")
                .first();
        if (linkElement != null) {
            return linkElement.attr("href");
        }

        return "";
    }

    @Nonnull
    @Override
    public String getName() {
        final Element nameElement = document.select("feed > author > name")
                .first();
        if (nameElement == null) {
            return "";
        }

        return nameElement.text();
    }

    @Override
    public InfoItemsPage<StreamInfoItem> getPage(final Page page) {
        return InfoItemsPage.emptyPage();
    }
}
