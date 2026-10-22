// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.linkHandler;

import com.vyonex.internal.fallback.exceptions.FoundAdException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.LinkHandlerFactory;
import com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper;

import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class YoutubeStreamLinkHandlerFactory extends LinkHandlerFactory {
    private static final YoutubeStreamLinkHandlerFactory INSTANCE =
            new YoutubeStreamLinkHandlerFactory();
    private static final Pattern VIDEO_ID = Pattern.compile("^([A-Za-z0-9_-]{11})");
    private static final List<String> ID_PATHS = List.of(
            "/embed/", "/live/", "/shorts/", "/watch/", "/v/", "/w/");

    private YoutubeStreamLinkHandlerFactory() { }

    public static YoutubeStreamLinkHandlerFactory getInstance() { return INSTANCE; }

    @Override public String getUrl(final String id) {
        return "https://www.youtube.com/watch?v=" + id;
    }

    @Override public String getId(final String value) throws ParsingException {
        if (value == null || value.isBlank()) throw new ParsingException("Video URL is empty");
        try {
            final URI uri = new URI(value);
            final String scheme = uri.getScheme();
            if ("vnd.youtube".equalsIgnoreCase(scheme)
                    || "vnd.youtube.launch".equalsIgnoreCase(scheme)) {
                final String target = uri.getRawSchemeSpecificPart();
                if (target.startsWith("//")) {
                    final String candidate = target.substring(2);
                    final String direct = extractId(candidate);
                    if (direct != null) return direct;
                    final URL embedded = YoutubeRouteSupport.parse("https:" + target);
                    return parseWebUrl(embedded, value);
                }
                return requireId(target);
            }
        } catch (final java.net.URISyntaxException e) {
            throw new ParsingException("Video URL is invalid", e);
        }
        return parseWebUrl(YoutubeRouteSupport.parse(value), value);
    }

    private static String parseWebUrl(final URL url, final String original)
            throws ParsingException {
        if (url == null) throw new ParsingException("Video URL is invalid");
        final String host = url.getHost();
        if ("googleads.g.doubleclick.net".equalsIgnoreCase(host)) {
            throw new FoundAdException("The URL points to an advertisement");
        }
        if (!YoutubeRouteSupport.isYoutubeOrMirror(url)) {
            throw new ParsingException("URL is not a supported video page: " + original);
        }
        if (YoutubePlaylistLinkHandlerFactory.getInstance().acceptUrl(original)) {
            throw new ParsingException("URL identifies a playlist rather than a video");
        }

        final String path = url.getPath();
        if ("attribution_link".equals(stripSlash(path))) {
            final String destination = YoutubeRouteSupport.queryValue(url, "u");
            if (destination != null) {
                final URL nested = YoutubeRouteSupport.parse("https://www.youtube.com" + destination);
                final String nestedId = YoutubeRouteSupport.queryValue(nested, "v");
                if (nestedId != null) return requireId(nestedId);
            }
            throw new ParsingException("Attribution URL has no video ID");
        }

        final boolean youtubeDomain = YoutubeParsingHelper.isYoutubeURL(url);
        final boolean shortLink = "youtu.be".equalsIgnoreCase(host)
                || "y2u.be".equalsIgnoreCase(host);
        final boolean embedOnly = host.toLowerCase(java.util.Locale.ROOT).endsWith("youtube-nocookie.com");
        if (embedOnly) return requirePathId(path, "/embed/");
        if (shortLink) {
            final String queryId = YoutubeRouteSupport.queryValue(url, "v");
            return requireId(queryId == null ? stripSlash(path) : queryId);
        }
        for (final String prefix : ID_PATHS) {
            if (path.startsWith(prefix)) return requireId(path.substring(prefix.length()));
        }
        final String queryId = YoutubeRouteSupport.queryValue(url, "v");
        if (queryId != null) return requireId(queryId);
        if (!youtubeDomain) return requireId(stripSlash(path));
        throw new ParsingException("Video URL has no video ID");
    }

    private static String requirePathId(final String path, final String prefix)
            throws ParsingException {
        if (!path.startsWith(prefix)) throw new ParsingException("Embed URL has no video ID");
        return requireId(path.substring(prefix.length()));
    }

    private static String stripSlash(final String path) {
        return path.startsWith("/") ? path.substring(1) : path;
    }

    private static String extractId(final String value) {
        if (value == null) return null;
        final Matcher matcher = VIDEO_ID.matcher(value);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String requireId(final String value) throws ParsingException {
        final String id = extractId(value);
        if (id == null) throw new ParsingException("String does not begin with an 11-character video ID");
        return id;
    }

    @Override public boolean onAcceptUrl(final String url) throws FoundAdException {
        try {
            getId(url);
            return true;
        } catch (final FoundAdException e) {
            throw e;
        } catch (final ParsingException e) {
            return false;
        }
    }
}
