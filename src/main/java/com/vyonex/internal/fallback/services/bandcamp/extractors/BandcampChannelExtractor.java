// Created by Fynn Godau 2019, licensed GNU GPL version 3 or later

// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors;

import static com.vyonex.internal.fallback.Image.HEIGHT_UNKNOWN;
import static com.vyonex.internal.fallback.Image.WIDTH_UNKNOWN;
import static com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampExtractorHelper.getArtistDetails;
import static com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampExtractorHelper.getImagesFromImageId;
import static com.vyonex.internal.fallback.utils.Utils.replaceHttpWithHttps;

import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;

import org.jsoup.Jsoup;
import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.Image.ResolutionLevel;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.channel.ChannelExtractor;
import com.vyonex.internal.fallback.channel.tabs.ChannelTabExtractor;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.exceptions.ReCaptchaException;
import com.vyonex.internal.fallback.channel.tabs.ChannelTabs;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.linkhandler.ReadyChannelTabListLinkHandler;
import com.vyonex.internal.fallback.services.bandcamp.linkHandler.BandcampChannelTabLinkHandlerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

public class BandcampChannelExtractor extends ChannelExtractor {

    private JsonObject channelInfo;

    public BandcampChannelExtractor(final StreamingService service,
                                    final ListLinkHandler linkHandler) {
        super(service, linkHandler);
    }

    @Nonnull
    @Override
    public List<Image> getAvatars() {
        return getImagesFromImageId(channelInfo.getLong("bio_image_id"), false);
    }

    @Nonnull
    @Override
    public List<Image> getBanners() throws ParsingException {
        /*
         * Mobile API does not return the header or not the correct header.
         * Therefore, we need to query the website
         */
        try {
            final String html = getDownloader()
                    .get(replaceHttpWithHttps(channelInfo.getString("bandcamp_url")))
                    .responseBody();

            return Stream.of(Jsoup.parse(html).getElementById("customHeader"))
                    .filter(Objects::nonNull)
                    .flatMap(element -> element.getElementsByTag("img").stream())
                    .map(element -> element.attr("src"))
                    .filter(url -> !url.isEmpty())
                    .map(url -> new Image(
                            replaceHttpWithHttps(url), HEIGHT_UNKNOWN, WIDTH_UNKNOWN,
                            ResolutionLevel.UNKNOWN))
                    .collect(Collectors.toUnmodifiableList());

        } catch (final IOException | ReCaptchaException e) {
            throw new ParsingException("Could not download artist web site", e);
        }
    }

    /**
     * Bandcamp discontinued their RSS feeds because it hadn't been used enough.
     */
    @Override
    public String getFeedUrl() {
        return null;
    }

    @Override
    public long getSubscriberCount() {
        return -1;
    }

    @Override
    public String getDescription() {
        return channelInfo.getString("bio");
    }

    @Override
    public String getParentChannelName() {
        return null;
    }

    @Override
    public String getParentChannelUrl() {
        return null;
    }

    @Nonnull
    @Override
    public List<Image> getParentChannelAvatars() {
        return List.of();
    }

    @Override
    public boolean isVerified() throws ParsingException {
        return false;
    }

    @Nonnull
    @Override
    public List<ListLinkHandler> getTabs() throws ParsingException {
        final JsonArray discography = channelInfo.getArray("discography");
        final TabExtractorBuilder builder = new TabExtractorBuilder(discography);

        final List<ListLinkHandler> tabs = new ArrayList<>();

        boolean foundTrackItem = false;
        boolean foundAlbumItem = false;

        for (final Object discographyItem : discography) {
            if (foundTrackItem && foundAlbumItem) {
                break;
            }

            if (!(discographyItem instanceof JsonObject)) {
                continue;
            }

            final JsonObject discographyJsonItem = (JsonObject) discographyItem;
            final String itemType = discographyJsonItem.getString("item_type");

            if (!foundTrackItem && "track".equals(itemType)) {
                foundTrackItem = true;
                tabs.add(new ReadyChannelTabListLinkHandler(getUrl()
                        + BandcampChannelTabLinkHandlerFactory.getUrlSuffix(ChannelTabs.TRACKS),
                        getId(),
                        ChannelTabs.TRACKS,
                        builder));
            }

            if (!foundAlbumItem && "album".equals(itemType)) {
                foundAlbumItem = true;
                tabs.add(new ReadyChannelTabListLinkHandler(getUrl()
                        + BandcampChannelTabLinkHandlerFactory.getUrlSuffix(ChannelTabs.ALBUMS),
                        getId(),
                        ChannelTabs.ALBUMS,
                        builder));
            }
        }

        return Collections.unmodifiableList(tabs);
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        channelInfo = getArtistDetails(getId());
    }

    @Nonnull
    @Override
    public String getName() {
        return channelInfo.getString("name");
    }

    private static final class TabExtractorBuilder
            implements ReadyChannelTabListLinkHandler.ChannelTabExtractorBuilder {
        private final JsonArray discography;

        TabExtractorBuilder(final JsonArray discography) {
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
