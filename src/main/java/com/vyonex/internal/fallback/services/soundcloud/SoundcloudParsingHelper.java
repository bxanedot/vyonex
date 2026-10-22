// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.MultiInfoItemsCollector;
import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.channel.ChannelInfoItemsCollector;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.exceptions.ReCaptchaException;
import com.vyonex.internal.fallback.localization.DateWrapper;
import com.vyonex.internal.fallback.services.soundcloud.extractors.SoundcloudChannelInfoItemExtractor;
import com.vyonex.internal.fallback.services.soundcloud.extractors.SoundcloudLikesInfoItemExtractor;
import com.vyonex.internal.fallback.services.soundcloud.extractors.SoundcloudPlaylistInfoItemExtractor;
import com.vyonex.internal.fallback.services.soundcloud.extractors.SoundcloudStreamInfoItemExtractor;
import com.vyonex.internal.fallback.stream.StreamInfoItemsCollector;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonParser;
import com.vyonex.internal.json.JsonParserException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SoundcloudParsingHelper {
    public static final String SOUNDCLOUD_API_V2_URL = "https://api-v2.soundcloud.com/";
    private static final String SITE = "https://soundcloud.com";
    private static final Pattern CLIENT_ID = Pattern.compile(
            "client_id\\s*[:=]\\s*[\\\"']([^\\\"']+)[\\\"']");
    private static final Pattern SHORT_URL = Pattern.compile(
            "https?://on\\.soundcloud\\.com/[A-Za-z0-9]+/?", Pattern.CASE_INSENSITIVE);
    private static volatile String cachedClientId;

    private SoundcloudParsingHelper() { }

    public static String clientId() throws ExtractionException, IOException {
        if (cachedClientId != null) return cachedClientId;
        synchronized (SoundcloudParsingHelper.class) {
            if (cachedClientId != null) return cachedClientId;
            final Downloader downloader = VyonEx.getDownloader();
            final Response homepage = downloader.get(SITE);
            if (homepage == null || homepage.responseBody().isBlank()) {
                throw new ExtractionException("SoundCloud homepage returned no HTML");
            }
            final List<String> scripts = new ArrayList<>();
            for (final Element script : Jsoup.parse(homepage.responseBody()).select("script[src]")) {
                final String source = script.absUrl("src");
                if (source.contains("sndcdn.com/assets/") && source.endsWith(".js")) {
                    scripts.add(source);
                }
            }
            final Map<String, List<String>> headers = Map.of("Range", List.of("bytes=0-50000"));
            for (int i = scripts.size() - 1; i >= 0; i--) {
                try {
                    final Response response = downloader.get(scripts.get(i), headers);
                    if (response == null) continue;
                    final Matcher match = CLIENT_ID.matcher(response.responseBody());
                    if (match.find()) return cachedClientId = match.group(1);
                } catch (final IOException | ReCaptchaException ignored) {
                    // A later application bundle may still contain the public client key.
                }
            }
            throw new ExtractionException("Could not find SoundCloud client ID in its scripts");
        }
    }

    public static DateWrapper parseDate(final String value) throws ParsingException {
        try {
            return DateWrapper.fromInstant(value);
        } catch (final DateTimeParseException e) {
            try {
                return new DateWrapper(OffsetDateTime.parse(value,
                        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss Z")));
            } catch (final DateTimeParseException fallback) {
                fallback.addSuppressed(e);
                throw new ParsingException("Invalid SoundCloud date: " + value, fallback);
            }
        }
    }

    public static JsonObject resolveFor(final Downloader downloader, final String url)
            throws IOException, ExtractionException {
        return requestObject(downloader, SOUNDCLOUD_API_V2_URL + "resolve?url="
                + encode(url) + "&client_id=" + clientId());
    }

    public static String resolveUrlWithEmbedPlayer(final String apiUrl)
            throws IOException, ReCaptchaException {
        final Response response = VyonEx.getDownloader().get(
                "https://w.soundcloud.com/player/?url=" + encode(apiUrl));
        if (response == null) throw new IOException("SoundCloud embed returned no response");
        final Element canonical = Jsoup.parse(response.responseBody())
                .selectFirst("link[rel=canonical]");
        if (canonical == null || canonical.absUrl("href").isBlank()) {
            throw new IOException("SoundCloud embed has no canonical URL");
        }
        return canonical.absUrl("href");
    }

    public static String resolveIdWithWidgetApi(final String originalUrl)
            throws IOException, ParsingException {
        String url = originalUrl;
        if (SHORT_URL.matcher(url).matches()) {
            try {
                final Response redirect = VyonEx.getDownloader().head(url);
                if (redirect == null || redirect.latestUrl() == null) {
                    throw new ParsingException("SoundCloud short link did not redirect");
                }
                url = redirect.latestUrl();
            } catch (final ReCaptchaException e) {
                throw new ParsingException("Unable to follow SoundCloud short link", e);
            }
        }
        final String normalized = normalizeUserUrl(url);
        final JsonObject response;
        try {
            final String widgetUrl = "https://api-widget.soundcloud.com/resolve?url="
                    + encode(normalized) + "&format=json&client_id=" + clientId();
            response = requestObject(VyonEx.getDownloader(), widgetUrl);
        } catch (final ExtractionException e) {
            throw new ParsingException("Unable to resolve SoundCloud URL", e);
        }
        final Object id = response.get("id");
        if (id == null) throw new ParsingException("SoundCloud resolution has no ID");
        return id.toString();
    }

    public static String getUsersFromApiMinItems(final int minimum,
                                                 final ChannelInfoItemsCollector collector,
                                                 final String apiUrl)
            throws IOException, ReCaptchaException, ParsingException {
        String next = getUsersFromApi(collector, apiUrl);
        final Set<String> visited = new HashSet<>();
        visited.add(apiUrl);
        while (!next.isEmpty() && collector.getItems().size() < minimum) {
            if (!visited.add(next)) return "";
            next = getUsersFromApi(collector, next);
        }
        return next;
    }

    public static String getUsersFromApi(final ChannelInfoItemsCollector collector,
                                         final String apiUrl)
            throws IOException, ReCaptchaException, ParsingException {
        final JsonObject response = requestObject(VyonEx.getDownloader(), apiUrl);
        for (final Object value : response.getArray("collection")) {
            if (value instanceof JsonObject) {
                collector.commit(new SoundcloudChannelInfoItemExtractor((JsonObject) value));
            }
        }
        return nextPage(response);
    }

    public static String getStreamsFromApiMinItems(final int minimum,
                                                   final StreamInfoItemsCollector collector,
                                                   final String apiUrl)
            throws IOException, ReCaptchaException, ParsingException {
        String next = getStreamsFromApi(collector, apiUrl);
        final Set<String> visited = new HashSet<>();
        visited.add(apiUrl);
        while (!next.isEmpty() && collector.getItems().size() < minimum) {
            if (!visited.add(next)) return "";
            next = getStreamsFromApi(collector, next);
        }
        return next;
    }

    public static String getStreamsFromApi(final StreamInfoItemsCollector collector,
                                           final String apiUrl)
            throws IOException, ReCaptchaException, ParsingException {
        return getStreamsFromApi(collector, apiUrl, false);
    }

    public static String getStreamsFromApi(final StreamInfoItemsCollector collector,
                                           final String apiUrl, final boolean charts)
            throws IOException, ReCaptchaException, ParsingException {
        final JsonObject response = requestObject(VyonEx.getDownloader(), apiUrl);
        for (final Object value : response.getArray("collection")) {
            if (!(value instanceof JsonObject)) continue;
            final JsonObject item = (JsonObject) value;
            collector.commit(new SoundcloudStreamInfoItemExtractor(
                    charts ? item.getObject("track") : item));
        }
        return nextPage(response);
    }

    public static String getInfoItemsFromApi(final MultiInfoItemsCollector collector,
                                             final String apiUrl)
            throws IOException, ReCaptchaException, ParsingException {
        final JsonObject response = requestObject(VyonEx.getDownloader(), apiUrl);
        for (final Object value : response.getArray("collection")) {
            if (!(value instanceof JsonObject)) continue;
            final JsonObject item = (JsonObject) value;
            switch (item.getString("kind", "")) {
                case "user" -> collector.commit(new SoundcloudChannelInfoItemExtractor(item));
                case "track" -> collector.commit(new SoundcloudStreamInfoItemExtractor(item));
                case "playlist" -> collector.commit(new SoundcloudPlaylistInfoItemExtractor(item));
                case "like" -> {
                    final JsonObject playlist = item.getObject("playlist", null);
                    collector.commit(playlist == null
                            ? new SoundcloudLikesInfoItemExtractor(item)
                            : new SoundcloudPlaylistInfoItemExtractor(playlist));
                }
                default -> { }
            }
        }
        return nextPage(response);
    }

    public static String getUploaderUrl(final JsonObject object) {
        return https(object.getObject("user").getString("permalink_url", ""));
    }

    public static String getAvatarUrl(final JsonObject object) {
        return https(object.getObject("user").getString("avatar_url", ""));
    }

    public static String getUploaderName(final JsonObject object) {
        return object.getObject("user").getString("username", "");
    }

    public static String toHttpsUrl(final String value) { return https(value); }

    public static List<Image> getAllImagesFromTrackObject(final JsonObject track)
            throws ParsingException {
        String url = track.getString("artwork_url");
        if (url == null) url = track.getObject("user").getString("avatar_url");
        if (url == null || url.isBlank()) throw new ParsingException("Track has no artwork");
        return image(url);
    }

    public static List<Image> getAllImagesFromArtworkOrAvatarUrl(final String url) {
        return image(url);
    }

    public static List<Image> getAllImagesFromVisualUrl(final String url) {
        return image(url);
    }

    private static List<Image> image(final String url) {
        // shortcut: image dimensions are unknown; add variants after validating current service URLs.
        return url == null || url.isBlank() ? List.of() : List.of(new Image(https(url),
                Image.HEIGHT_UNKNOWN, Image.WIDTH_UNKNOWN, Image.ResolutionLevel.UNKNOWN));
    }

    private static JsonObject requestObject(final Downloader downloader, final String url)
            throws IOException, ReCaptchaException, ParsingException {
        final Response response = downloader.get(url);
        if (response == null || response.responseBody().isBlank()) {
            throw new ParsingException("SoundCloud returned an empty response");
        }
        if (response.responseCode() >= 400) {
            throw new IOException("SoundCloud request failed with HTTP " + response.responseCode());
        }
        try {
            return JsonParser.object().from(response.responseBody());
        } catch (final JsonParserException e) {
            throw new ParsingException("Invalid SoundCloud API response", e);
        }
    }

    private static String nextPage(final JsonObject response) {
        final String next = response.getString("next_href");
        if (next == null || next.isBlank()) return "";
        if (next.contains("client_id=")) return next;
        try {
            return next + (next.contains("?") ? "&" : "?") + "client_id=" + clientId();
        } catch (final Exception e) {
            return "";
        }
    }

    private static String normalizeUserUrl(final String value) throws ParsingException {
        if (value == null || value.isBlank()) throw new ParsingException("SoundCloud URL is empty");
        try {
            final URI input = URI.create(value);
            final String host = input.getHost();
            if (host == null || !(host.equalsIgnoreCase("soundcloud.com")
                    || host.equalsIgnoreCase("www.soundcloud.com")
                    || host.equalsIgnoreCase("m.soundcloud.com"))) {
                throw new ParsingException("URL is not a SoundCloud page");
            }
            String path = input.getRawPath() == null ? "" : input.getRawPath();
            while (path.endsWith("/") && !path.isEmpty()) path = path.substring(0, path.length() - 1);
            return "https://soundcloud.com" + path;
        } catch (final IllegalArgumentException e) {
            throw new ParsingException("Invalid SoundCloud page URL", e);
        }
    }

    private static String encode(final String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String https(final String value) {
        if (value == null || value.isBlank()) return "";
        if (value.startsWith("//")) return "https:" + value;
        return value.replaceFirst("^http://", "https://");
    }
}
