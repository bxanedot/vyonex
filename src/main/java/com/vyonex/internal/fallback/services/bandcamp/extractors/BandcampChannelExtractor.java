// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.channel.ChannelExtractor;
import com.vyonex.internal.fallback.channel.tabs.ChannelTabExtractor;
import com.vyonex.internal.fallback.channel.tabs.ChannelTabs;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.linkhandler.ReadyChannelTabListLinkHandler;
import com.vyonex.internal.fallback.services.bandcamp.linkHandler.BandcampChannelTabLinkHandlerFactory;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;
import org.jsoup.Jsoup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BandcampChannelExtractor extends ChannelExtractor {
    @Nullable private JsonObject artist;
    @Nullable private Downloader downloader;

    public BandcampChannelExtractor(final StreamingService service,
                                    final ListLinkHandler linkHandler) {
        super(service, linkHandler);
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader requestDownloader)
            throws IOException, ExtractionException {
        artist = BandcampExtractorHelper.getArtistDetails(requestDownloader, getId());
        downloader = requestDownloader;
    }

    @Nonnull
    @Override
    public String getName() throws ParsingException {
        final String name = details().getString("name");
        if (name == null || name.isBlank()) throw new ParsingException("Artist has no name");
        return name;
    }

    @Override
    public String getDescription() { return artist == null ? "" : artist.getString("bio", ""); }

    @Nonnull
    @Override
    public List<Image> getAvatars() {
        return BandcampExtractorHelper.getImagesFromImageId(
                artist == null ? 0 : artist.getLong("bio_image_id"), false);
    }

    @Nonnull
    @Override
    public List<Image> getBanners() throws ParsingException {
        final String url = artist == null ? null : artist.getString("bandcamp_url");
        if (url == null || url.isBlank() || downloader == null) return List.of();
        try {
            final String html = downloader.get(https(url)).responseBody();
            return Jsoup.parse(html).select("#customHeader img[src]").stream()
                    .flatMap(image -> BandcampExtractorHelper
                            .getImagesFromImageUrl(image.attr("src")).stream())
                    .toList();
        } catch (final IOException | com.vyonex.internal.fallback.exceptions.ReCaptchaException e) {
            throw new ParsingException("Could not load the artist banner", e);
        }
    }

    @Nonnull
    @Override
    public List<ListLinkHandler> getTabs() throws ParsingException {
        if (artist == null) return List.of();
        final JsonArray discography = artist.getArray("discography");
        if (discography == null) return List.of();

        final List<String> types = new ArrayList<>(2);
        for (final Object value : discography) {
            if (!(value instanceof JsonObject item)) continue;
            final String itemType = item.getString("item_type");
            if (itemType == null) continue;
            final String type = switch (itemType) {
                case "track" -> ChannelTabs.TRACKS;
                case "album" -> ChannelTabs.ALBUMS;
                default -> null;
            };
            if (type != null && !types.contains(type)) types.add(type);
        }

        final ReadyTabBuilder builder = new ReadyTabBuilder(discography);
        final List<ListLinkHandler> tabs = new ArrayList<>(types.size());
        for (final String type : types) tabs.add(tab(type, builder));
        return List.copyOf(tabs);
    }

    @Override public String getFeedUrl() { return null; }
    @Override public long getSubscriberCount() { return -1; }
    @Override public String getParentChannelName() { return null; }
    @Override public String getParentChannelUrl() { return null; }
    @Nonnull @Override public List<Image> getParentChannelAvatars() { return List.of(); }
    @Override public boolean isVerified() { return false; }

    private JsonObject details() throws ParsingException {
        if (artist == null) throw new ParsingException("Artist details have not been fetched");
        return artist;
    }

    private ListLinkHandler tab(final String type, final ReadyTabBuilder builder)
            throws ParsingException {
        return new ReadyChannelTabListLinkHandler(getUrl()
                + BandcampChannelTabLinkHandlerFactory.getUrlSuffix(type), getId(), type, builder);
    }

    private static String https(final String url) {
        return url.startsWith("http://") ? "https://" + url.substring(7) : url;
    }

    private static final class ReadyTabBuilder
            implements ReadyChannelTabListLinkHandler.ChannelTabExtractorBuilder {
        private final JsonArray discography;

        private ReadyTabBuilder(final JsonArray discography) { this.discography = discography; }

        @Nonnull
        @Override
        public ChannelTabExtractor build(@Nonnull final StreamingService service,
                                         @Nonnull final ListLinkHandler linkHandler) {
            return BandcampChannelTabExtractor.fromDiscography(service, linkHandler, discography);
        }
    }
}
