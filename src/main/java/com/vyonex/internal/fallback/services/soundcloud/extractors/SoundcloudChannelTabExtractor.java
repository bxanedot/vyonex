// bxane
package com.vyonex.internal.fallback.services.soundcloud.extractors;

import com.vyonex.internal.fallback.InfoItem;
import com.vyonex.internal.fallback.MultiInfoItemsCollector;
import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.channel.tabs.ChannelTabExtractor;
import com.vyonex.internal.fallback.channel.tabs.ChannelTabs;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudParsingHelper;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static com.vyonex.internal.fallback.services.soundcloud.SoundcloudParsingHelper.SOUNDCLOUD_API_V2_URL;
import static com.vyonex.internal.fallback.utils.Utils.isNullOrEmpty;

public class SoundcloudChannelTabExtractor extends ChannelTabExtractor {

    private static final String USERS_ENDPOINT = SOUNDCLOUD_API_V2_URL + "users/";

    /** Empty page cap against infinite pagination loops. */
    private static final int MAX_EMPTY_PAGES = 3;

    private final String userId;

    public SoundcloudChannelTabExtractor(final StreamingService service,
                                         final ListLinkHandler linkHandler) {
        super(service, linkHandler);
        userId = getLinkHandler().getId();
    }

    @Nonnull
    private String getEndpoint() throws ParsingException {
        switch (getName()) {
            case ChannelTabs.TRACKS:
                return "/tracks";
            case ChannelTabs.PLAYLISTS:
                return "/playlists_without_albums";
            case ChannelTabs.ALBUMS:
                return "/albums";
            case ChannelTabs.LIKES:
                return "/likes";
        }
        throw new ParsingException("Unsupported tab: " + getName());
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader) {
    }

    @Nonnull
    @Override
    public String getId() {
        return userId;
    }

    @Nonnull
    @Override
    public InfoItemsPage<InfoItem> getInitialPage() throws IOException, ExtractionException {
        return getPage(new Page(USERS_ENDPOINT + userId + getEndpoint() + "?client_id="
                + SoundcloudParsingHelper.clientId() + "&limit=20" + "&linked_partitioning=1"));
    }

    @Override
    public InfoItemsPage<InfoItem> getPage(final Page page)
            throws IOException, ExtractionException {
        if (page == null || isNullOrEmpty(page.getUrl())) {
            throw new IllegalArgumentException("Page doesn't contain an URL");
        }

        final MultiInfoItemsCollector collector = new MultiInfoItemsCollector(getServiceId());
        final Set<String> visitedPages = new HashSet<>();

        String currentPageUrl = page.getUrl();
        String nextPageUrl = "";
        int emptyPageCount = 0;

        while (!isNullOrEmpty(currentPageUrl)) {
            if (!visitedPages.add(currentPageUrl)) {
                // Prevent infinite loops when the API points back to an already visited page.
                nextPageUrl = "";
                break;
            }

            final int itemsBefore = collector.getItems().size();
            final String candidateNextPage = SoundcloudParsingHelper
                    .getInfoItemsFromApi(collector, currentPageUrl);
            final boolean hasNewItems = collector.getItems().size() > itemsBefore;

            if (hasNewItems) {
                nextPageUrl = candidateNextPage;
                break;
            }

            emptyPageCount++;
            if (emptyPageCount >= MAX_EMPTY_PAGES || isNullOrEmpty(candidateNextPage)) {
                // Give up after too many empty responses or when SoundCloud stops providing tokens.
                nextPageUrl = "";
                break;
            }

            currentPageUrl = candidateNextPage;
        }

        final Page nextPage = isNullOrEmpty(nextPageUrl) ? null : new Page(nextPageUrl);
        return new InfoItemsPage<>(collector, nextPage);
    }
}
