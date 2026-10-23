// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.fallback.services.youtube.YoutubeService;
import com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubeStreamLinkHandlerFactory;
import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Request;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ReCaptchaException;
import com.vyonex.internal.fallback.linkhandler.LinkHandler;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudService;
import com.vyonex.internal.fallback.services.soundcloud.extractors.SoundcloudStreamExtractor;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class YoutubeStreamTimestampTest {
    @Test
    void parsesTimeComponentsWhenTrailingUnitsAreOmitted() throws Exception {
        final Downloader previous = VyonEx.getDownloader();
        try {
            VyonEx.init(new NoNetworkDownloader());
            assertAll(
                    () -> assertEquals(120, timestamp("2m")),
                    () -> assertEquals(3_603, timestamp("1h3s")),
                    () -> assertEquals(120, soundcloudTimestamp("2m")),
                    () -> assertEquals(3_723, soundcloudTimestamp("1h2m3s")));
        } finally {
            VyonEx.init(previous);
        }
    }

    private static long timestamp(final String value) throws Exception {
        final var link = YoutubeStreamLinkHandlerFactory.getInstance().fromUrl(
                "https://www.youtube.com/watch?v=abcdefghijk&t=" + value);
        return new YoutubeStreamExtractor(new YoutubeService(1), link).getTimeStamp();
    }

    private static long soundcloudTimestamp(final String value) throws Exception {
        final String url = "https://api-v2.soundcloud.com/tracks/123#t=" + value;
        final var link = new LinkHandler(url, url, "123");
        return new SoundcloudStreamExtractor(new SoundcloudService(2), link).getTimeStamp();
    }

    private static final class NoNetworkDownloader extends Downloader {
        @Override
        public Response execute(final Request request) throws IOException, ReCaptchaException {
            throw new AssertionError("Timestamp parsing must not make network requests");
        }
    }
}
