// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.exceptions.ReCaptchaException;
import com.vyonex.internal.fallback.localization.DateWrapper;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonParser;
import com.vyonex.internal.json.JsonParserException;
import com.vyonex.internal.json.JsonWriter;
import org.jsoup.nodes.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public final class BandcampExtractorHelper {
    public static final String BASE_URL = "https://bandcamp.com";
    public static final String BASE_API_URL = BASE_URL + "/api";
    private static final String IMAGE_BASE_URL = "https://f4.bcbits.com/img/";
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);

    private BandcampExtractorHelper() { }

    public static String getStreamUrlFromIds(final long bandId, final long itemId,
                                              final String itemType) throws ParsingException {
        if (itemType == null || itemType.isEmpty()) {
            throw new ParsingException("Bandcamp item type is missing");
        }
        final String endpoint = BASE_API_URL + "/mobile/22/tralbum_details?band_id=" + bandId
                + "&tralbum_id=" + itemId + "&tralbum_type=" + itemType.charAt(0);
        try {
            return https(JsonParser.object().from(downloader().get(endpoint)
                    .responseBody()).getString("bandcamp_url"));
        } catch (final IOException | ReCaptchaException | JsonParserException e) {
            throw new ParsingException("Could not resolve Bandcamp item URL", e);
        }
    }

    public static JsonObject getArtistDetails(final String id) throws ParsingException {
        final byte[] body = JsonWriter.string().object().value("band_id", id).end().done()
                .getBytes(StandardCharsets.UTF_8);
        try {
            return JsonParser.object().from(downloader().postWithContentTypeJson(
                    BASE_API_URL + "/mobile/22/band_details", java.util.Map.of(), body)
                    .responseBody());
        } catch (final IOException | ReCaptchaException | JsonParserException e) {
            throw new ParsingException("Could not load Bandcamp artist details", e);
        }
    }

    public static String getImageUrl(final long id, final boolean album) {
        return IMAGE_BASE_URL + (album ? "a" : "") + id + "_10.jpg";
    }

    public static boolean isArtistDomain(final String url) throws ParsingException {
        final URI uri = parseUri(url);
        final String host = uri.getHost().toLowerCase(Locale.ROOT);
        if (host.equals("bandcamp.com")) return false;
        if (host.endsWith(".bandcamp.com")) return true;
        if (VyonEx.getDownloader() == null) return false;

        try {
            final String html = downloader().get(https(uri.toString())).responseBody();
            return org.jsoup.Jsoup.parse(html).select(
                    ".cart-wrapper a[href='https://bandcamp.com/cart']").size() > 0;
        } catch (final IOException | ReCaptchaException e) {
            throw new ParsingException("Could not verify custom Bandcamp artist domain", e);
        }
    }

    public static boolean isRadioUrl(final String url) {
        try {
            final URI uri = new URI(url);
            return "bandcamp.com".equalsIgnoreCase(uri.getHost())
                    && ("http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme()))
                    && (uri.getPath().isEmpty() || "/".equals(uri.getPath()))
                    && uri.getRawQuery() != null
                    && uri.getRawQuery().matches("show=[0-9]+");
        } catch (final URISyntaxException | NullPointerException e) {
            return false;
        }
    }

    public static DateWrapper parseDate(final String textDate) throws ParsingException {
        try {
            return new DateWrapper(ZonedDateTime.parse(textDate, DATE_FORMAT).toInstant());
        } catch (final DateTimeException | NullPointerException e) {
            throw new ParsingException("Could not parse Bandcamp date: " + textDate, e);
        }
    }

    @Nonnull
    public static List<Image> getImagesFromSearchResult(@Nonnull final Element searchResult) {
        final Element image = searchResult.selectFirst(".art img[src]");
        return getImagesFromImageUrl(image == null ? "" : image.attr("src"));
    }

    @Nonnull
    public static List<Image> getImagesFromImageUrl(@Nullable final String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return List.of();
        try {
            final URI uri = URI.create(imageUrl.startsWith("//") ? "https:" + imageUrl : imageUrl);
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    && !"http".equalsIgnoreCase(uri.getScheme())) return List.of();
            return List.of(new Image(https(uri.toString()), Image.HEIGHT_UNKNOWN,
                    Image.WIDTH_UNKNOWN, Image.ResolutionLevel.UNKNOWN));
        } catch (final IllegalArgumentException e) {
            return List.of();
        }
    }

    @Nonnull
    public static List<Image> getImagesFromImageId(final long id, final boolean album) {
        return id == 0 ? List.of() : getImagesFromImageUrl(getImageUrl(id, album));
    }

    private static URI parseUri(final String value) throws ParsingException {
        try {
            final URI uri = new URI(value);
            if (uri.getHost() == null || uri.getUserInfo() != null
                    || !("http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme()))) {
                throw new URISyntaxException(value, "Expected an HTTP(S) URL with a host");
            }
            return uri;
        } catch (final URISyntaxException | NullPointerException e) {
            throw new ParsingException("Invalid Bandcamp URL: " + value, e);
        }
    }

    private static String https(final String url) {
        return url != null && url.startsWith("http://") ? "https://" + url.substring(7) : url;
    }

    private static Downloader downloader() throws ParsingException {
        final Downloader downloader = VyonEx.getDownloader();
        if (downloader == null) throw new ParsingException("VyonEx has not been initialized");
        return downloader;
    }
}
