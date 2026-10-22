// bxane
package com.vyonex.internal.fallback.services.media_ccc.extractors;

import com.grack.nanojson.JsonObject;

import com.vyonex.internal.fallback.InfoItem;
import com.vyonex.internal.fallback.ListExtractor;
import com.vyonex.internal.fallback.MultiInfoItemsCollector;
import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.channel.tabs.ChannelTabExtractor;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.services.media_ccc.extractors.infoItems.MediaCCCStreamInfoItemExtractor;

import java.io.IOException;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * MediaCCC does not really have channel tabs, but rather a list of videos for each conference,
 * so this class just acts as a videos channel tab extractor.
 */
public class MediaCCCChannelTabExtractor extends ChannelTabExtractor {
    @Nullable
    private JsonObject conferenceData;

    /**
     * @param conferenceData will be not-null if conference data has already been fetched by
     *                       {@link MediaCCCConferenceExtractor}. Otherwise, if this parameter is
     *                       {@code null}, conference data will be fetched anew.
     */
    public MediaCCCChannelTabExtractor(final StreamingService service,
                                       final ListLinkHandler linkHandler,
                                       @Nullable final JsonObject conferenceData) {
        super(service, linkHandler);
        this.conferenceData = conferenceData;
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader)
            throws ExtractionException, IOException {
        if (conferenceData == null) {
            // only fetch conference data if we don't have it already
            conferenceData = MediaCCCConferenceExtractor.fetchConferenceData(downloader, getId());
        }
    }

    @Nonnull
    @Override
    public ListExtractor.InfoItemsPage<InfoItem> getInitialPage() {
        final MultiInfoItemsCollector collector =
                new MultiInfoItemsCollector(getServiceId());
        Objects.requireNonNull(conferenceData) // will surely be != null after onFetchPage
                .getArray("events")
                .streamAsJsonObjects()
                .forEach(event -> collector.commit(new MediaCCCStreamInfoItemExtractor(event)));
        return new ListExtractor.InfoItemsPage<>(collector, null);
    }

    @Override
    public ListExtractor.InfoItemsPage<InfoItem> getPage(final Page page) {
        return ListExtractor.InfoItemsPage.emptyPage();
    }
}
