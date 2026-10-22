// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.AccountTerminatedException;
import com.vyonex.internal.fallback.exceptions.ContentNotAvailableException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.localization.Localization;
import com.vyonex.internal.fallback.stream.AudioTrackType;
import com.vyonex.internal.json.JsonParser;
import com.vyonex.internal.json.JsonParserException;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Random;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Test
    void readsFirstCollaboratorAndHandlesMissingDialogData() throws Exception {
        final var navigation = JsonParser.object().from("{\"showDialogCommand\":{"
                + "\"panelLoadingStrategy\":{\"inlineContent\":{\"dialogViewModel\":{"
                + "\"customContent\":{\"listViewModel\":{\"listItems\":[{"
                + "\"listItemViewModel\":{\"title\":{\"simpleText\":\"Channel\"}}}]}}}}}}}");
        final var collaborator = YoutubeParsingHelper.getFirstCollaborator(navigation);
        assertEquals("Channel", collaborator.getObject("title").getString("simpleText"));
        assertNull(YoutubeParsingHelper.getFirstCollaborator(JsonParser.object().from("{}")));
    }

    @Test
    void readsAudioTrackTypeFromEncodedTags() throws IOException {
        assertEquals(AudioTrackType.ORIGINAL,
                YoutubeParsingHelper.extractAudioTrackType(audioTags("original")));
        assertEquals(AudioTrackType.DUBBED,
                YoutubeParsingHelper.extractAudioTrackType(audioTags("dubbed-auto")));
        assertEquals(AudioTrackType.DESCRIPTIVE,
                YoutubeParsingHelper.extractAudioTrackType(audioTags("descriptive")));
        assertEquals(AudioTrackType.SECONDARY,
                YoutubeParsingHelper.extractAudioTrackType(audioTags("secondary")));
        assertNull(YoutubeParsingHelper.extractAudioTrackType("!"));
        assertNull(YoutubeParsingHelper.extractAudioTrackType(
                Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[]{1})));
    }

    @Test
    void resolvesCommonNavigationEndpointKinds() throws JsonParserException {
        assertEquals("https://video.example/watch?v=abcdefghijk",
                navigation("{\"urlEndpoint\":{\"url\":\"https://video.example/watch?v=abcdefghijk\"}}"));
        assertEquals("https://video.example/watch?v=abcdefghijk",
                navigation("{\"urlEndpoint\":{\"url\":\"https://www.youtube.com/redirect?q=https%3A%2F%2Fvideo.example%2Fwatch%3Fv%3Dabcdefghijk\"}}"));
        assertEquals("https://www.youtube.com/watch?v=abcdefghijk&list=PL123&t=42",
                navigation("{\"watchEndpoint\":{\"videoId\":\"abcdefghijk\","
                        + "\"playlistId\":\"PL123\",\"startTimeSeconds\":42}}"));
        assertEquals("https://www.youtube.com/channel/UC123",
                navigation("{\"browseEndpoint\":{\"browseId\":\"UC123\"}}"));
        assertEquals("https://www.youtube.com/playlist?list=PL123",
                navigation("{\"browseEndpoint\":{\"browseId\":\"VLPL123\"}}"));
        assertEquals("https://www.youtube.com/@artist",
                navigation("{\"browseEndpoint\":{\"canonicalBaseUrl\":\"/@artist\"}}"));
        assertEquals("https://www.youtube.com/playlist?list=PL123",
                navigation("{\"watchPlaylistEndpoint\":{\"playlistId\":\"PL123\"}}"));
        assertEquals("https://www.youtube.com/@artist",
                navigation("{\"commandMetadata\":{\"webCommandMetadata\":{\"url\":\"/@artist\"}}}"));
    }

    @Test
    void escapesTextAndAllowsOnlySafeLinksInHtmlMode() throws JsonParserException {
        final var plain = JsonParser.object().from("{\"simpleText\":\"<b>plain</b>\"}");
        assertEquals("<b>plain</b>", YoutubeParsingHelper.getTextFromObject(plain));
        assertEquals("&lt;b&gt;plain&lt;/b&gt;",
                YoutubeParsingHelper.getTextFromObject(plain, true));

        final var linked = JsonParser.object().from("{\"runs\":["
                + "{\"text\":\"<tag>\",\"bold\":true,\"navigationEndpoint\":{"
                + "\"urlEndpoint\":{\"url\":\"https://example.org/?a=1&b=2\"}}},"
                + "{\"text\":\"tail\",\"italics\":true}]}");
        final String html = YoutubeParsingHelper.getTextFromObject(linked, true);
        final var body = Jsoup.parseBodyFragment(html).body();
        assertEquals("<tag>tail", body.text());
        assertEquals("https://example.org/?a=1&b=2", body.selectFirst("a").attr("href"));
        assertEquals(1, body.select("b").size());

        final var unsafeLink = JsonParser.object().from("{\"runs\":[{\"text\":\"go\","
                + "\"navigationEndpoint\":{\"urlEndpoint\":{\"url\":\"javascript:alert(1)\"}}}]}");
        assertEquals("go", Jsoup.parseBodyFragment(
                YoutubeParsingHelper.getTextFromObject(unsafeLink, true)).body().text());
    }

    @Test
    void buildsClientHeadersAndReadsNamedCookies() {
        assertEquals(Map.of("Origin", List.of("https://music.youtube.com"),
                        "Referer", List.of("https://music.youtube.com")),
                YoutubeParsingHelper.getOriginReferrerHeaders("https://music.youtube.com"));
        assertEquals(Map.of("X-YouTube-Client-Name", List.of("WEB"),
                        "X-YouTube-Client-Version", List.of("2.test")),
                YoutubeParsingHelper.getClientHeaders("WEB", "2.test"));
        assertTrue(YoutubeParsingHelper.getAndroidUserAgent(new Localization("fr", "CA"))
                .endsWith("; CA) gzip"));
        assertTrue(YoutubeParsingHelper.getIosUserAgent(null).contains("CPU iOS"));
        assertTrue(YoutubeParsingHelper.getVisionOsUserAgent(null).contains("CPU visionOS"));

        final Response response = new Response(200, "OK",
                Map.of("Set-Cookie", List.of("XSOCS=wrong; path=/", "SOCS=right; path=/",
                        "SOCS=ignored; path=/")), "", "https://www.youtube.com/");
        assertEquals("ignored", YoutubeParsingHelper.extractCookieValue("SOCS", response));
        assertEquals("", YoutubeParsingHelper.extractCookieValue("MISSING", response));

        YoutubeParsingHelper.setConsentAccepted(false);
        assertEquals("SOCS=CAE=", YoutubeParsingHelper.generateConsentCookie());
        assertEquals(Map.of("Cookie", List.of("SOCS=CAE=")),
                YoutubeParsingHelper.getCookieHeader());
        YoutubeParsingHelper.setConsentAccepted(true);
        assertEquals("SOCS=CAISAiAD", YoutubeParsingHelper.generateConsentCookie());
        YoutubeParsingHelper.setConsentAccepted(false);
    }

    @Test
    void classifiesServiceAlertsByAvailabilityAndTerminationReason()
            throws JsonParserException, ParsingException {
        YoutubeParsingHelper.defaultAlertsCheck(JsonParser.object().from("{}"));
        YoutubeParsingHelper.defaultAlertsCheck(JsonParser.object().from(
                "{\"alerts\":[{\"alertRenderer\":{\"type\":\"WARNING\","
                        + "\"text\":{\"simpleText\":\"Warning\"}}}]}"));

        final var unavailable = JsonParser.object().from(
                "{\"alerts\":[{\"alertRenderer\":{\"type\":\"ERROR\","
                        + "\"text\":{\"simpleText\":\"Unavailable\"}}}]}");
        assertThrows(ContentNotAvailableException.class,
                () -> YoutubeParsingHelper.defaultAlertsCheck(unavailable));

        final var violation = JsonParser.object().from(
                "{\"alerts\":[{\"alertRenderer\":{\"type\":\"ERROR\","
                        + "\"text\":{\"simpleText\":\"This account has been terminated for "
                        + "violating policy\"}}}]}");
        final var violationError = assertThrows(AccountTerminatedException.class,
                () -> YoutubeParsingHelper.defaultAlertsCheck(violation));
        assertEquals(AccountTerminatedException.Reason.VIOLATION, violationError.getReason());

        final var unknownReason = JsonParser.object().from(
                "{\"alerts\":[{\"alertRenderer\":{\"type\":\"ERROR\","
                        + "\"text\":{\"simpleText\":\"This channel was removed\"}}}]}");
        final var unknownError = assertThrows(AccountTerminatedException.class,
                () -> YoutubeParsingHelper.defaultAlertsCheck(unknownReason));
        assertEquals(AccountTerminatedException.Reason.UNKNOWN, unknownError.getReason());
    }

    private static String navigation(final String json) throws JsonParserException {
        return YoutubeParsingHelper.getUrlFromNavigationEndpoint(JsonParser.object().from(json));
    }

    private static String audioTags(final String value) throws IOException {
        final byte[] keyBytes = "acont".getBytes(StandardCharsets.UTF_8);
        final byte[] valueBytes = value.getBytes(StandardCharsets.UTF_8);
        final ByteArrayOutputStream pair = new ByteArrayOutputStream();
        pair.write(0x0a);
        pair.write(keyBytes.length);
        pair.write(keyBytes);
        pair.write(0x12);
        pair.write(valueBytes.length);
        pair.write(valueBytes);

        final ByteArrayOutputStream tags = new ByteArrayOutputStream();
        tags.write(0x0a);
        tags.write(pair.size());
        tags.write(pair.toByteArray());
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tags.toByteArray());
    }
}
