// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube;

import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Request;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ContentNotAvailableException;
import com.vyonex.internal.fallback.exceptions.ReCaptchaException;
import com.vyonex.internal.fallback.feed.FeedExtractor;
import com.vyonex.internal.fallback.feed.FeedInfo;
import com.vyonex.internal.fallback.services.youtube.extractors.YoutubeFeedExtractor;
import com.vyonex.internal.fallback.stream.StreamInfoItem;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class YoutubeFeedExtractorTest {
    private static final String CHANNEL_URL = "https://www.youtube.com/channel/UC123";
    private static final String FEED_URL = "https://www.youtube.com/feeds/videos.xml?channel_id=UC123";
    private static final String FEED = "<feed><author><name>Channel</name>"
            + "<uri>https://www.youtube.com/channel/UC123</uri></author>"
            + "<entry><title>Video</title><link rel=\"alternate\" "
            + "href=\"https://www.youtube.com/watch?v=abcdefghijk\"/>"
            + "<author><name>Uploader</name><uri>https://www.youtube.com/channel/UC456</uri>"
            + "</author><published>2024-01-02T03:04:05+00:00</published>"
            + "<media:group><media:thumbnail url=\"https://i.ytimg.com/vi/abcdefghijk/hqdefault.jpg\" "
            + "width=\"480\" height=\"360\"/><media:statistics views=\"1234\"/>"
            + "</media:group></entry></feed>";

    @Test
    void mapsChannelFeedOffline() throws Exception {
        final StubDownloader downloader = new StubDownloader(200, FEED);
        final Downloader previous = VyonEx.getDownloader();
        VyonEx.init(downloader);
        try {
            final YoutubeFeedExtractor extractor = extractor();

            extractor.onFetchPage(downloader);

            assertEquals(FEED_URL, downloader.request.url());
            assertEquals("UC123", extractor.getId());
            assertEquals(CHANNEL_URL, extractor.getUrl());
            assertEquals("Channel", extractor.getName());
            final FeedInfo feedInfo = FeedInfo.getInfo(extractor);
            assertEquals("UC123", feedInfo.getId());
            assertEquals(1, feedInfo.getRelatedItems().size());
            final List<StreamInfoItem> items = extractor.getInitialPage().getItems();
            assertEquals(1, items.size());
            final StreamInfoItem item = items.get(0);
            assertEquals("Video", item.getName());
            assertEquals("https://www.youtube.com/watch?v=abcdefghijk", item.getUrl());
            assertEquals("Uploader", item.getUploaderName());
            assertEquals("https://www.youtube.com/channel/UC456", item.getUploaderUrl());
            assertEquals(1234, item.getViewCount());
            assertEquals(320, item.getThumbnails().get(0).getHeight());
            assertEquals(180, item.getThumbnails().get(0).getWidth());
        } finally {
            VyonEx.init(previous);
        }
    }

    @Test
    void mapsMissingFeedToUnavailable() {
        final StubDownloader downloader = new StubDownloader(404, "");
        final Downloader previous = VyonEx.getDownloader();
        VyonEx.init(downloader);
        try {
            assertThrows(ContentNotAvailableException.class,
                    () -> extractor().onFetchPage(downloader));
        } finally {
            VyonEx.init(previous);
        }
    }

    private static YoutubeFeedExtractor extractor() throws Exception {
        final FeedExtractor extractor = new YoutubeService(1).getFeedExtractor(CHANNEL_URL);
        return (YoutubeFeedExtractor) extractor;
    }

    private static final class StubDownloader extends Downloader {
        private final int status;
        private final String body;
        private Request request;

        private StubDownloader(final int status, final String body) {
            this.status = status;
            this.body = body;
        }

        @Override public Response execute(final Request request)
                throws IOException, ReCaptchaException {
            this.request = request;
            return new Response(status, status == 200 ? "OK" : "Not Found", Map.of(), body,
                    request.url());
        }
    }
}
