// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.media_ccc.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.InfoItem;
import com.vyonex.internal.fallback.MetaInfo;
import com.vyonex.internal.fallback.MultiInfoItemsCollector;
import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.channel.ChannelInfoItem;
import com.vyonex.internal.fallback.channel.ChannelInfoItemExtractor;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.linkhandler.SearchQueryHandler;
import com.vyonex.internal.fallback.search.SearchExtractor;
import com.vyonex.internal.fallback.services.media_ccc.extractors.infoItems.MediaCCCStreamInfoItemExtractor;
import com.vyonex.internal.fallback.services.media_ccc.linkHandler.MediaCCCConferencesListLinkHandlerFactory;
import com.vyonex.internal.fallback.services.media_ccc.linkHandler.MediaCCCSearchQueryHandlerFactory;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonParser;
import com.vyonex.internal.json.JsonParserException;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MediaCCCSearchExtractor extends SearchExtractor {
    private JsonObject eventResults;
    private MediaCCCConferenceKiosk conferenceResults;

    public MediaCCCSearchExtractor(final StreamingService service,
                                   final SearchQueryHandler query) {
        super(service, query);
    }

    @Nonnull @Override public String getSearchSuggestion() { return ""; }
    @Override public boolean isCorrectedSearch() { return false; }
    @Nonnull @Override public List<MetaInfo> getMetaInfo() { return List.of(); }

    @Nonnull @Override public InfoItemsPage<InfoItem> getInitialPage() {
        final MultiInfoItemsCollector items = new MultiInfoItemsCollector(getServiceId());
        final List<String> filters = getLinkHandler().getContentFilters();
        if (includes(filters, MediaCCCSearchQueryHandlerFactory.CONFERENCES)) {
            if (conferenceResults == null) {
                throw new IllegalStateException("Conference search was not fetched");
            }
            addMatchingConferences(items, conferenceResults.getInitialPage().getItems());
        }
        if (includes(filters, MediaCCCSearchQueryHandlerFactory.EVENTS)) {
            if (eventResults == null) throw new IllegalStateException("Event search was not fetched");
            final JsonArray events = eventResults.getArray("events");
            events.streamAsJsonObjects()
                    .filter(event -> event.getString("release_date") != null)
                    .map(MediaCCCStreamInfoItemExtractor::new)
                    .forEach(items::commit);
        }
        return new InfoItemsPage<>(items, null);
    }

    @Override public InfoItemsPage<InfoItem> getPage(final Page page) {
        return InfoItemsPage.emptyPage();
    }

    @Override public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        eventResults = null;
        conferenceResults = null;
        final List<String> filters = getLinkHandler().getContentFilters();
        if (includes(filters, MediaCCCSearchQueryHandlerFactory.EVENTS)) {
            final String url = getUrl();
            try {
                eventResults = JsonParser.object().from(
                        downloader.get(url, getExtractorLocalization()).responseBody());
            } catch (final JsonParserException error) {
                throw new ExtractionException("Could not parse event search response from " + url,
                        error);
            }
        }
        if (includes(filters, MediaCCCSearchQueryHandlerFactory.CONFERENCES)) {
            conferenceResults = new MediaCCCConferenceKiosk(getService(),
                    MediaCCCConferencesListLinkHandlerFactory.getInstance().fromId("conferences"),
                    MediaCCCConferenceKiosk.KIOSK_ID);
            conferenceResults.onFetchPage(downloader);
        }
    }

    private static boolean includes(final List<String> filters, final String wanted) {
        return filters.isEmpty() || filters.contains(MediaCCCSearchQueryHandlerFactory.ALL)
                || filters.contains(wanted);
    }

    private void addMatchingConferences(final MultiInfoItemsCollector items,
                                        final List<ChannelInfoItem> conferences) {
        final String query = getSearchString().toLowerCase(Locale.ROOT);
        for (final ChannelInfoItem conference : conferences) {
            if (conference.getName() == null
                    || !conference.getName().toLowerCase(Locale.ROOT).contains(query)) continue;
            items.commit(asExtractor(conference));
        }
    }

    private static ChannelInfoItemExtractor asExtractor(final ChannelInfoItem item) {
        return new ChannelInfoItemExtractor() {
            @Override public String getDescription() { return item.getDescription(); }
            @Override public long getSubscriberCount() { return item.getSubscriberCount(); }
            @Override public long getStreamCount() { return item.getStreamCount(); }
            @Override public boolean isVerified() { return item.isVerified(); }
            @Override public String getName() { return item.getName(); }
            @Override public String getUrl() { return item.getUrl(); }
            @Nonnull @Override public List<Image> getThumbnails() { return item.getThumbnails(); }
        };
    }
}
