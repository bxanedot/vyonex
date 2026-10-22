// bxane
package com.vyonex.internal.fallback.services.youtube.extractors.kiosk;

import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;

public class YoutubeLiveExtractor extends YoutubeDesktopBaseKioskExtractor {

    public YoutubeLiveExtractor(final StreamingService streamingService,
                                final ListLinkHandler linkHandler,
                                final String kioskId) {
        super(streamingService, linkHandler, kioskId, "UC4R8DWoMoI7CAwX8_LjQHig",
                "EgdsaXZldGFikgEDCKEK");
    }
}
