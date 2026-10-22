// bxane
package com.vyonex.internal.fallback.services.soundcloud.extractors;

import static com.vyonex.internal.fallback.ServiceList.SoundCloud;
import static com.vyonex.internal.fallback.services.soundcloud.SoundcloudParsingHelper.SOUNDCLOUD_API_V2_URL;
import static com.vyonex.internal.fallback.utils.Utils.isNullOrEmpty;

import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.kiosk.KioskExtractor;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.localization.ContentCountry;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudParsingHelper;
import com.vyonex.internal.fallback.stream.StreamInfoItem;
import com.vyonex.internal.fallback.stream.StreamInfoItemsCollector;

import java.io.IOException;

import javax.annotation.Nonnull;

public class SoundcloudChartsExtractor extends KioskExtractor<StreamInfoItem> {

    private String initialFetchNextPageUrl;
    private StreamInfoItemsCollector initialFetchCollector;

    public SoundcloudChartsExtractor(final StreamingService service,
                                     final ListLinkHandler linkHandler,
                                     final String kioskId) {
        super(service, linkHandler, kioskId);
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader)
        throws ExtractionException, IOException {
        // Check if already run
        if (initialFetchNextPageUrl != null) {
            return;
        }

        initialFetchCollector = new StreamInfoItemsCollector(getServiceId());

        final String apiUrl = SOUNDCLOUD_API_V2_URL + "charts"
            + "?genre=soundcloud:genres:all-music"
            + "&client_id=" + SoundcloudParsingHelper.clientId()
            + "&kind=trending";

        final ContentCountry contentCountry = SoundCloud.getContentCountry();
        String apiUrlWithRegion = null;
        if (getService().getSupportedCountries().contains(contentCountry)) {
            apiUrlWithRegion = apiUrl + "&region=soundcloud:regions:"
                + contentCountry.getCountryCode();
        }

        try {
            initialFetchNextPageUrl = SoundcloudParsingHelper.getStreamsFromApi(
                initialFetchCollector,
                apiUrlWithRegion == null ? apiUrl : apiUrlWithRegion, true);
        } catch (final IOException e) {
            // Request to other region may be geo-restricted.
            // See historical region retry issue #537.
            // We retry without the specified region.
            initialFetchNextPageUrl = SoundcloudParsingHelper.getStreamsFromApi(
                initialFetchCollector, apiUrl, true);
        }
    }

    @Nonnull
    @Override
    public String getName() {
        return getId();
    }

    @Override
    public InfoItemsPage<StreamInfoItem> getPage(final Page page) throws IOException,
            ExtractionException {
        if (page == null || isNullOrEmpty(page.getUrl())) {
            throw new IllegalArgumentException("Page doesn't contain an URL");
        }

        final StreamInfoItemsCollector collector = new StreamInfoItemsCollector(getServiceId());
        final String nextPageUrl = SoundcloudParsingHelper.getStreamsFromApi(collector,
                page.getUrl(), true);

        return new InfoItemsPage<>(collector, new Page(nextPageUrl));
    }

    @Nonnull
    @Override
    public InfoItemsPage<StreamInfoItem> getInitialPage() throws IOException, ExtractionException {
        return new InfoItemsPage<>(initialFetchCollector, new Page(initialFetchNextPageUrl));
    }
}
