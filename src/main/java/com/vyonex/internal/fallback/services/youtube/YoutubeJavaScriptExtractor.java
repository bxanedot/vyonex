// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube;

import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.localization.Localization;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import javax.annotation.Nonnull;
import java.net.URI;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class YoutubeJavaScriptExtractor {
    private static final Pattern PLAYER_HASH = Pattern.compile("player\\\\/([a-z0-9]{8})\\\\/");
    private static final Pattern JSON_PLAYER_URL = Pattern.compile(
            "\\\"jsUrl\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern VIDEO_ID = Pattern.compile("[A-Za-z0-9_-]{11}");
    private static final String IFRAME_API_URL = "https://www.youtube.com/iframe_api";
    private static final String PLAYER_URL =
            "https://www.youtube.com/s/player/%s/player_ias.vflset/en_GB/base.js";

    private YoutubeJavaScriptExtractor() { }

    @Nonnull
    static String extractJavaScriptPlayerCode(@Nonnull final String videoId)
            throws ParsingException {
        Objects.requireNonNull(videoId, "videoId");
        if (!videoId.isEmpty() && !VIDEO_ID.matcher(videoId).matches()) {
            throw new ParsingException("Invalid video ID");
        }

        try {
            return downloadJavaScriptCode(cleanJavaScriptUrl(
                    extractJavaScriptUrlWithIframeResource()));
        } catch (final Exception iframeFailure) {
            try {
                return downloadJavaScriptCode(cleanJavaScriptUrl(
                        extractJavaScriptUrlWithEmbedWatchPage(videoId)));
            } catch (final Exception embedFailure) {
                embedFailure.addSuppressed(iframeFailure);
                if (embedFailure instanceof ParsingException) {
                    throw (ParsingException) embedFailure;
                }
                throw new ParsingException("Could not retrieve the player JavaScript", embedFailure);
            }
        }
    }

    @Nonnull
    static String extractJavaScriptUrlWithIframeResource() throws ParsingException {
        final String content = getPage(IFRAME_API_URL, "Could not fetch IFrame resource");
        final Matcher matcher = PLAYER_HASH.matcher(content);
        if (!matcher.find()) {
            throw new ParsingException("IFrame resource did not contain a player version");
        }
        return String.format(PLAYER_URL, matcher.group(1));
    }

    @Nonnull
    static String extractJavaScriptUrlWithEmbedWatchPage(@Nonnull final String videoId)
            throws ParsingException {
        final String html = getPage("https://www.youtube.com/embed/" + videoId,
                "Could not fetch embedded watch page");
        final var document = Jsoup.parse(html);
        for (final Element script : document.select("script[name=player/base][src]")) {
            final String source = script.attr("src");
            if (source.contains("base.js")) {
                return source;
            }
        }
        return extractPlayerUrlFromEmbeddedPage(html);
    }

    @Nonnull
    static String extractPlayerUrlFromEmbeddedPage(@Nonnull final String html)
            throws ParsingException {
        final Matcher matcher = JSON_PLAYER_URL.matcher(html);
        if (matcher.find()) {
            return matcher.group(1).replace("\\/", "/");
        }
        throw new ParsingException("Embedded page did not contain a player script URL");
    }

    @Nonnull
    static String cleanJavaScriptUrl(@Nonnull final String playerUrl) throws ParsingException {
        try {
            final URI resolved = URI.create("https://www.youtube.com/").resolve(playerUrl);
            if (!"https".equalsIgnoreCase(resolved.getScheme())
                    || !("youtube.com".equalsIgnoreCase(resolved.getHost())
                    || "www.youtube.com".equalsIgnoreCase(resolved.getHost()))
                    || resolved.getPath() == null
                    || !resolved.getPath().endsWith("/base.js")) {
                throw new IllegalArgumentException("Unexpected player script URL");
            }
            return resolved.toString();
        } catch (final IllegalArgumentException e) {
            throw new ParsingException("The player script URL is invalid", e);
        }
    }

    @Nonnull
    private static String getPage(@Nonnull final String url,
                                  @Nonnull final String errorMessage)
            throws ParsingException {
        try {
            return VyonEx.getDownloader().get(url, Localization.DEFAULT).responseBody();
        } catch (final Exception e) {
            throw new ParsingException(errorMessage, e);
        }
    }

    @Nonnull
    private static String downloadJavaScriptCode(@Nonnull final String playerUrl)
            throws ParsingException {
        return getPage(playerUrl, "Could not download the player JavaScript");
    }
}
