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
import org.jsoup.nodes.Element;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BandcampChannelExtractor extends ChannelExtractor {
    private JsonObject artist;

    public BandcampChannelExtractor(final StreamingService service,
                                    final ListLinkHandler linkHandler) {
        super(service, linkHandler);
    }

    @Nonnull
    @Override
    public List<Image> getAvatars() {
        return BandcampExtractorHelper.getImagesFromImageId(artist.getLong("bio_image_id"), false);
    }

    @Nonnull
    @Override
    public List<Image> getBanners() throws ParsingException {
        final String url = artist.getString("bandcamp_url");
        if (url == null || url.isBlank()) return List.of();
        try {
            final String html = getDownloader().get(https(url)).responseBody();
            final List<Image> banners = new ArrayList<>();
            for (final Element image : Jsoup.parse(html).select("#customHeader img[src]")) {
                banners.addAll(BandcampExtractorHelper.getImagesFromImageUrl(image.attr("src")));
            }
            return List.copyOf(banners);
        } catch (final IOException | com.vyonex.internal.fallback.exceptions.ReCaptchaException e) {
            throw new ParsingException("Could not load the artist banner", e);
        }
    }

    @Override
    public String getFeedUrl() { return null; }

    @Override
    public long getSubscriberCount() { return -1; }

    @Override
    public String getDescription() { return artist.getString("bio", ""); }

    @Override
    public String getParentChannelName() { return null; }

    @Override
    public String getParentChannelUrl() { return null; }

    @Nonnull
    @Override
    public List<Image> getParentChannelAvatars() { return List.of(); }

    @Override
    public boolean isVerified() { return false; }

    @Nonnull
    @Override
    public List<ListLinkHandler> getTabs() throws ParsingException {
        final JsonArray discography = artist.getArray("discography");
        final ReadyTabBuilder builder = new ReadyTabBuilder(discography);
        boolean tracks = false;
        boolean albums = false;
        final List<ListLinkHandler> tabs = new ArrayList<>(2);

        for (final Object value : discography) {
            if (!(value instanceof JsonObject item)) continue;
            final String type = item.getString("item_type");
            if (!tracks && "track".equals(type)) {
                tracks = true;
                tabs.add(tab(ChannelTabs.TRACKS, builder));
            }
            if (!albums && "album".equals(type)) {
                albums = true;
                tabs.add(tab(ChannelTabs.ALBUMS, builder));
            }
            if (tracks && albums) break;
        }
        return List.copyOf(tabs);
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        artist = BandcampExtractorHelper.getArtistDetails(getId());
    }

    @Nonnull
    @Override
    public String getName() throws ParsingException {
        final String name = artist.getString("name");
        if (name == null || name.isBlank()) throw new ParsingException("Artist has no name");
        return name;
    }

    private ListLinkHandler tab(final String type, final ReadyTabBuilder builder)
            throws ParsingException {
        return new ReadyChannelTabListLinkHandler(getUrl()
                + BandcampChannelTabLinkHandlerFactory.getUrlSuffix(type), getId(), type, builder);
    }

    private static String https(final String value) {
        return value.startsWith("http://") ? "https://" + value.substring(7) : value;
    }

    private static final class ReadyTabBuilder
            implements ReadyChannelTabListLinkHandler.ChannelTabExtractorBuilder {
        private final JsonArray discography;

        private ReadyTabBuilder(final JsonArray discography) {
            this.discography = discography;
        }

        @Nonnull
        @Override
        public ChannelTabExtractor build(@Nonnull final StreamingService service,
                                         @Nonnull final ListLinkHandler linkHandler) {
            return BandcampChannelTabExtractor.fromDiscography(service, linkHandler, discography);
        }
    }
}
