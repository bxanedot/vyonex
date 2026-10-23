// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.media_ccc.linkHandler;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MediaCCCLinkHandlerTest {
    @Test
    void parsesCurrentHyphenatedLiveRoomUrls() throws ParsingException {
        final String url = "https://streaming.media.ccc.de/matrix-conf-2026/sofya";
        final MediaCCCStreamLinkHandlerFactory factory = MediaCCCStreamLinkHandlerFactory.getInstance();

        assertTrue(factory.onAcceptUrl(url));
        assertEquals("matrix-conf-2026/sofya", factory.getId(url));
        assertEquals(url, factory.getUrl("matrix-conf-2026/sofya"));
    }

    @Test
    void mapsEventAndConferenceIdsToCanonicalUrls() throws ParsingException {
        final MediaCCCStreamLinkHandlerFactory streams = MediaCCCStreamLinkHandlerFactory.getInstance();
        final MediaCCCConferenceLinkHandlerFactory conferences =
                MediaCCCConferenceLinkHandlerFactory.getInstance();

        assertEquals("event-guid", streams.getId("https://media.ccc.de/v/event-guid"));
        assertEquals("https://media.ccc.de/v/event-guid", streams.getUrl("event-guid"));
        assertEquals("event-guid", streams.getId("https://media.ccc.de/public/events/event-guid"));
        assertEquals("rc3-2021", conferences.getId("https://api.media.ccc.de/public/conferences/rc3-2021"));
        assertEquals("rc3-2021", conferences.getId("https://media.ccc.de/public/conferences/rc3-2021"));
        assertEquals("rc3-2021", conferences.getId("https://media.ccc.de/b/rc3-2021"));
        assertEquals("https://media.ccc.de/c/rc3-2021", conferences.getUrl("rc3-2021", List.of(), ""));
        assertFalse(streams.onAcceptUrl("https://example.com/v/event-guid"));
    }

    @Test
    void acceptsConferenceCollectionAliasesAndRecentTrailingSlash() {
        final MediaCCCConferencesListLinkHandlerFactory conferences =
                MediaCCCConferencesListLinkHandlerFactory.getInstance();
        final MediaCCCRecentListLinkHandlerFactory recent = MediaCCCRecentListLinkHandlerFactory.getInstance();

        assertTrue(conferences.onAcceptUrl("https://media.ccc.de/b/conferences"));
        assertTrue(conferences.onAcceptUrl("https://media.ccc.de/public/conferences"));
        assertTrue(conferences.onAcceptUrl("https://api.media.ccc.de/public/conferences"));
        assertFalse(conferences.onAcceptUrl("http://media.ccc.de/public/conferences"));
        assertTrue(recent.onAcceptUrl("media.ccc.de/recent/"));
    }

    @Test
    void encodesSearchTermsAsUtf8QueryParameters() throws ParsingException {
        assertEquals("https://media.ccc.de/public/events/search?q=space+%26+caf%C3%A9",
                MediaCCCSearchQueryHandlerFactory.getInstance()
                        .getUrl("space & café", List.of(), ""));
    }
}
