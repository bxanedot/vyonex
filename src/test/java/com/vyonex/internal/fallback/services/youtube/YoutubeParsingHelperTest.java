// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.json.JsonParser;
import com.vyonex.internal.json.JsonParserException;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YoutubeParsingHelperTest {
    @Test
    void readsTextAndBuildsThumbnailImages() throws JsonParserException, ParsingException {
        final var object = JsonParser.object().from(
                "{\"plain\":\"title\",\"rich\":{\"simpleText\":\"caption\"},"
                        + "\"thumbnail\":{\"thumbnails\":["
                        + "{\"url\":\"//img.example/low.jpg\",\"height\":144,\"width\":256},"
                        + "{\"url\":\"http://img.example/high.jpg\",\"height\":720,\"width\":1280},"
                        + "{\"height\":100}]}}");
        assertEquals("title", YoutubeParsingHelper.getTextAtKey(object, "plain"));
        assertEquals("caption", YoutubeParsingHelper.getTextAtKey(object, "rich"));

        final var images = YoutubeParsingHelper.getThumbnailsFromInfoItem(object);
        assertEquals(2, images.size());
        assertEquals("https://img.example/low.jpg", images.get(0).getUrl());
        assertEquals(256, images.get(0).getWidth());
        assertEquals(Image.ResolutionLevel.LOW, images.get(0).getEstimatedResolutionLevel());
        assertEquals("https://img.example/high.jpg", images.get(1).getUrl());
        assertEquals(Image.ResolutionLevel.HIGH, images.get(1).getEstimatedResolutionLevel());
        assertThrows(UnsupportedOperationException.class,
                () -> images.add(new Image("https://img.example/x", 1, 1,
                        Image.ResolutionLevel.LOW)));
    }

    @Test
    void recognizesVerifiedBadgesAndIconAttachments() throws JsonParserException {
        final var badges = JsonParser.array().from(
                "[null,{}, {\"metadataBadgeRenderer\":{\"style\":\"BADGE_STYLE_TYPE_VERIFIED_ARTIST\"}}]");
        assertTrue(YoutubeParsingHelper.isVerified(badges));
        assertFalse(YoutubeParsingHelper.isVerified(JsonParser.array().from(
                "[{\"metadataBadgeRenderer\":{\"style\":\"BADGE_STYLE_TYPE_LIVE_NOW\"}}]")));

        final var attachments = JsonParser.array().from(
                "[{}, {\"element\":{\"type\":{\"imageType\":{\"image\":{\"sources\":["
                        + "{\"clientResource\":{\"imageName\":\"AUDIO_BADGE\"}}]}}}}}]");
        assertTrue(YoutubeParsingHelper.hasArtistOrVerifiedIconBadgeAttachment(attachments));
        assertFalse(YoutubeParsingHelper.hasArtistOrVerifiedIconBadgeAttachment(
                JsonParser.array().from("[{}]")));
    }

    @Test
    void generatesClientTokensFromTheConfiguredAlphabet() {
        YoutubeParsingHelper.setNumberGenerator(new Random(7));
        try {
            assertTrue(YoutubeParsingHelper.generateContentPlaybackNonce()
                    .matches("[A-Za-z0-9_-]{16}"));
            assertTrue(YoutubeParsingHelper.generateTParameter().matches("[A-Za-z0-9_-]{12}"));
        } finally {
            YoutubeParsingHelper.setNumberGenerator(new Random());
        }
    }

    @Test
    void identifiesStreamingClientFromTheExactQueryValue() {
        assertTrue(YoutubeParsingHelper.isWebStreamingUrl(
                "https://video.example/play?x=1&c=WEB&foo=2"));
        assertTrue(YoutubeParsingHelper.isWebEmbeddedPlayerStreamingUrl(
                "https://video.example/play?c=WEB_EMBEDDED_PLAYER"));
        assertTrue(YoutubeParsingHelper.isAndroidStreamingUrl(
                "https://video.example/play?c=ANDROID"));
        assertTrue(YoutubeParsingHelper.isIosStreamingUrl(
                "https://video.example/play?c=IOS"));
        assertTrue(YoutubeParsingHelper.isVisionOsStreamingUrl(
                "https://video.example/play?c=VISIONOS"));
        assertFalse(YoutubeParsingHelper.isWebStreamingUrl(
                "https://video.example/play?c=WEB_EMBEDDED_PLAYER"));
        assertFalse(YoutubeParsingHelper.isWebStreamingUrl(
                "https://video.example/play?other=c%3DWEB"));
    }
}
