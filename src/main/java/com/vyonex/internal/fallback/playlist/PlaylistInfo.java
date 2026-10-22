// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.playlist;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.ListExtractor;
import com.vyonex.internal.fallback.ListInfo;
import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.stream.Description;
import com.vyonex.internal.fallback.stream.StreamInfoItem;
import com.vyonex.internal.fallback.utils.ExtractorHelper;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class PlaylistInfo extends ListInfo<StreamInfoItem> {
    public enum PlaylistType {
        NORMAL,
        MIX_STREAM,
        MIX_MUSIC,
        @Deprecated MIX_CHANNEL,
        MIX_GENRE
    }

    private String uploaderUrl = "";
    private String uploaderName = "";
    private String subChannelUrl;
    private String subChannelName;
    private Description description;
    @Nonnull private List<Image> banners = List.of();
    @Nonnull private List<Image> subChannelAvatars = List.of();
    @Nonnull private List<Image> thumbnails = List.of();
    @Nonnull private List<Image> uploaderAvatars = List.of();
    private long streamCount;
    private PlaylistType playlistType;

    private PlaylistInfo(final int serviceId,
                         final ListLinkHandler linkHandler,
                         final String name) throws ParsingException {
        super(serviceId, linkHandler, name);
    }

    public static PlaylistInfo getInfo(final String url) throws IOException, ExtractionException {
        return getInfo(VyonEx.getServiceByUrl(url), url);
    }

    public static PlaylistInfo getInfo(final StreamingService service, final String url)
            throws IOException, ExtractionException {
        final PlaylistExtractor extractor = service.getPlaylistExtractor(url);
        extractor.fetchPage();
        return getInfo(extractor);
    }

    public static ListExtractor.InfoItemsPage<StreamInfoItem> getMoreItems(
            final StreamingService service, final String url, final Page page)
            throws IOException, ExtractionException {
        return service.getPlaylistExtractor(url).getPage(page);
    }

    public static PlaylistInfo getInfo(final PlaylistExtractor extractor)
            throws ExtractionException {
        final PlaylistInfo info = new PlaylistInfo(
                extractor.getServiceId(), extractor.getLinkHandler(), extractor.getName());
        final List<Throwable> uploaderErrors = new ArrayList<>();

        copy(extractor::getOriginalUrl, info::setOriginalUrl, info::addError);
        copy(extractor::getStreamCount, info::setStreamCount, info::addError);
        copy(extractor::getDescription, info::setDescription, info::addError);
        copy(extractor::getThumbnails, info::setThumbnails, info::addError);
        copy(extractor::getUploaderUrl, info::setUploaderUrl, uploaderErrors::add);
        copy(extractor::getUploaderName, info::setUploaderName, uploaderErrors::add);
        copy(extractor::getUploaderAvatars, info::setUploaderAvatars, uploaderErrors::add);
        copy(extractor::getSubChannelUrl, info::setSubChannelUrl, uploaderErrors::add);
        copy(extractor::getSubChannelName, info::setSubChannelName, uploaderErrors::add);
        copy(extractor::getSubChannelAvatars, info::setSubChannelAvatars, uploaderErrors::add);
        copy(extractor::getBanners, info::setBanners, info::addError);
        copy(extractor::getPlaylistType, info::setPlaylistType, info::addError);

        if (!uploaderErrors.isEmpty()
                && (!info.getErrors().isEmpty() || uploaderErrors.size() < 3)) {
            info.addAllErrors(uploaderErrors);
        }

        final ListExtractor.InfoItemsPage<StreamInfoItem> firstPage =
                ExtractorHelper.getItemsPageOrLogError(info, extractor);
        info.setRelatedItems(firstPage.getItems());
        info.setNextPage(firstPage.getNextPage());
        return info;
    }

    private static <T> void copy(final Field<T> read,
                                 final Consumer<T> write,
                                 final Consumer<Throwable> onError) {
        try {
            write.accept(read.read());
        } catch (final Exception error) {
            onError.accept(error);
        }
    }

    @FunctionalInterface
    private interface Field<T> {
        T read() throws Exception;
    }

    @Nonnull public List<Image> getThumbnails() { return thumbnails; }
    public void setThumbnails(@Nonnull final List<Image> thumbnails) {
        this.thumbnails = thumbnails;
    }

    @Nonnull public List<Image> getBanners() { return banners; }
    public void setBanners(@Nonnull final List<Image> banners) { this.banners = banners; }

    public String getUploaderUrl() { return uploaderUrl; }
    public void setUploaderUrl(final String uploaderUrl) { this.uploaderUrl = uploaderUrl; }

    public String getUploaderName() { return uploaderName; }
    public void setUploaderName(final String uploaderName) { this.uploaderName = uploaderName; }

    @Nonnull public List<Image> getUploaderAvatars() { return uploaderAvatars; }
    public void setUploaderAvatars(@Nonnull final List<Image> uploaderAvatars) {
        this.uploaderAvatars = uploaderAvatars;
    }

    public String getSubChannelUrl() { return subChannelUrl; }
    public void setSubChannelUrl(final String subChannelUrl) {
        this.subChannelUrl = subChannelUrl;
    }

    public String getSubChannelName() { return subChannelName; }
    public void setSubChannelName(final String subChannelName) {
        this.subChannelName = subChannelName;
    }

    @Nonnull public List<Image> getSubChannelAvatars() { return subChannelAvatars; }
    public void setSubChannelAvatars(@Nonnull final List<Image> subChannelAvatars) {
        this.subChannelAvatars = subChannelAvatars;
    }

    public long getStreamCount() { return streamCount; }
    public void setStreamCount(final long streamCount) { this.streamCount = streamCount; }

    public Description getDescription() { return description; }
    public void setDescription(final Description description) { this.description = description; }

    public PlaylistType getPlaylistType() { return playlistType; }
    public void setPlaylistType(final PlaylistType playlistType) {
        this.playlistType = playlistType;
    }
}
