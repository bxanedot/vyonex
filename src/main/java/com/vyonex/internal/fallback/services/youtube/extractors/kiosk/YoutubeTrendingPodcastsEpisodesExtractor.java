// bxane
package com.vyonex.internal.fallback.services.youtube.extractors.kiosk;

import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;

public class YoutubeTrendingPodcastsEpisodesExtractor extends YoutubeDesktopBaseKioskExtractor {

    public YoutubeTrendingPodcastsEpisodesExtractor(final StreamingService streamingService,
                                                    final ListLinkHandler linkHandler,
                                                    final String kioskId) {
        super(streamingService, linkHandler, kioskId, "FEpodcasts_destination", "qgcCCAM%3D");
    }
}
