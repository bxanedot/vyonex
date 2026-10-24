// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import static com.vyonex.internal.fallback.services.youtube.YoutubeChannelHelper.getChannelResponse;
import static com.vyonex.internal.fallback.services.youtube.YoutubeChannelHelper.resolveChannelId;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getTextFromObject;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.channel.ChannelExtractor;
import com.vyonex.internal.fallback.channel.tabs.ChannelTabs;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.linkhandler.ReadyChannelTabListLinkHandler;
import com.vyonex.internal.fallback.services.youtube.YoutubeChannelHelper;
import com.vyonex.internal.fallback.services.youtube.YoutubeChannelHelper.ChannelHeader;
import com.vyonex.internal.fallback.services.youtube.YoutubeChannelHelper.ChannelHeader.HeaderType;
import com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper;
import com.vyonex.internal.fallback.services.youtube.extractors.YoutubeChannelTabExtractor.VideosTabExtractor;
import com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubeChannelLinkHandlerFactory;
import com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubeChannelTabLinkHandlerFactory;
import com.vyonex.internal.fallback.utils.Utils;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class YoutubeChannelExtractor extends ChannelExtractor {
    private static final String IMAGE = "image";
    private static final String CONTENTS = "contents";
    private static final String CONTENT_PREVIEW_IMAGE_VIEW_MODEL = "contentPreviewImageViewModel";
    private static final String PAGE_HEADER_VIEW_MODEL = "pageHeaderViewModel";
    private static final String TAB_RENDERER = "tabRenderer";
    private static final String CONTENT = "content";
    private static final String METADATA = "metadata";
    private static final String AVATAR = "avatar";
    private static final String THUMBNAILS = "thumbnails";
    private static final String SOURCES = "sources";
    private static final String BANNER = "banner";

    private JsonObject response;
    @Nullable private ChannelHeader header;
    private String redirectedChannelId;
    @Nullable private JsonObject ageGate;

    public YoutubeChannelExtractor(final StreamingService service,
                                   final ListLinkHandler linkHandler) {
        super(service, linkHandler);
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        final String requestedId = resolveChannelId(super.getId());
        final YoutubeChannelHelper.ChannelResponseData data = getChannelResponse(requestedId,
                "EgZ2aWRlb3PyBgQKAjoA", getExtractorLocalization(), getExtractorContentCountry());
        response = data.jsonResponse;
        redirectedChannelId = data.channelId;
        header = YoutubeChannelHelper.getChannelHeader(response);
        ageGate = YoutubeChannelHelper.getChannelAgeGateRenderer(response);
    }

    @Nonnull
    @Override
    public String getUrl() throws ParsingException {
        try {
            return YoutubeChannelLinkHandlerFactory.getInstance().getUrl("channel/" + getId());
        } catch (final ParsingException e) {
            return super.getUrl();
        }
    }

    @Nonnull
    @Override
    public String getId() throws ParsingException {
        assertPageFetched();
        return YoutubeChannelHelper.getChannelId(header, response, redirectedChannelId);
    }

    @Nonnull
    @Override
    public String getName() throws ParsingException {
        assertPageFetched();
        return YoutubeChannelHelper.getChannelName(header, ageGate, response);
    }

    @Nonnull
    @Override
    public List<Image> getAvatars() throws ParsingException {
        assertPageFetched();
        if (ageGate != null) {
            return YoutubeParsingHelper.getImagesFromThumbnailsArray(
                    ageGate.getObject(AVATAR).getArray(THUMBNAILS));
        }
        if (header == null) throw new ParsingException("Could not get channel avatars");

        final JsonArray sources;
        switch (header.headerType) {
            case PAGE:
                final JsonObject pageImage = header.json.getObject(CONTENT)
                        .getObject(PAGE_HEADER_VIEW_MODEL).getObject(IMAGE);
                if (pageImage.has(CONTENT_PREVIEW_IMAGE_VIEW_MODEL)) {
                    sources = pageImage.getObject(CONTENT_PREVIEW_IMAGE_VIEW_MODEL)
                            .getObject(IMAGE).getArray(SOURCES);
                } else if (pageImage.has("decoratedAvatarViewModel")) {
                    sources = pageImage.getObject("decoratedAvatarViewModel").getObject(AVATAR)
                            .getObject("avatarViewModel").getObject(IMAGE).getArray(SOURCES);
                } else {
                    sources = new JsonArray();
                }
                break;
            case INTERACTIVE_TABBED:
                sources = header.json.getObject("boxArt").getArray(THUMBNAILS);
                break;
            case C4_TABBED:
            case CAROUSEL:
            default:
                sources = header.json.getObject(AVATAR).getArray(THUMBNAILS);
                break;
        }
        return YoutubeParsingHelper.getImagesFromThumbnailsArray(sources);
    }

    @Nonnull
    @Override
    public List<Image> getBanners() {
        assertPageFetched();
        if (ageGate != null || header == null) return List.of();
        final JsonArray images;
        if (header.headerType == HeaderType.PAGE) {
            final JsonObject pageHeader = header.json.getObject(CONTENT)
                    .getObject(PAGE_HEADER_VIEW_MODEL);
            if (!pageHeader.has(BANNER)) return List.of();
            images = pageHeader.getObject(BANNER).getObject("imageBannerViewModel")
                    .getObject(IMAGE).getArray(SOURCES);
        } else {
            images = header.json.getObject(BANNER).getArray(THUMBNAILS);
        }
        return YoutubeParsingHelper.getImagesFromThumbnailsArray(images);
    }

    @Override
    public String getFeedUrl() throws ParsingException {
        try {
            return YoutubeParsingHelper.getFeedUrlFrom(getId());
        } catch (final Exception e) {
            throw new ParsingException("Could not get feed URL", e);
        }
    }

    @Override
    public long getSubscriberCount() throws ParsingException {
        assertPageFetched();
        if (ageGate != null || header == null
                || header.headerType == HeaderType.INTERACTIVE_TABBED) {
            return UNKNOWN_SUBSCRIBER_COUNT;
        }
        if (header.headerType == HeaderType.PAGE) return pageHeaderSubscriberCount(header.json);

        JsonObject count = header.json.getObject("subscriberCountText");
        if (count.isEmpty()) count = header.json.getObject("subtitle");
        if (count.isEmpty()) return UNKNOWN_SUBSCRIBER_COUNT;
        try {
            return Utils.mixedNumberWordToLong(getTextFromObject(count));
        } catch (final NumberFormatException e) {
            throw new ParsingException("Could not parse subscriber count", e);
        }
    }

    private static long pageHeaderSubscriberCount(final JsonObject channelHeader)
            throws ParsingException {
        final JsonObject metadata = channelHeader.getObject(CONTENT)
                .getObject(PAGE_HEADER_VIEW_MODEL).getObject(METADATA);
        if (!metadata.has("contentMetadataViewModel")) return UNKNOWN_SUBSCRIBER_COUNT;
        final JsonArray rows = metadata.getObject("contentMetadataViewModel")
                .getArray("metadataRows");
        if (rows.isEmpty()) return UNKNOWN_SUBSCRIBER_COUNT;
        final JsonArray parts = rows.getObject(rows.size() - 1).getArray("metadataParts");
        if (parts.size() < 2) return UNKNOWN_SUBSCRIBER_COUNT;
        try {
            final String text = parts.getObject(0).getObject("text").getString("content");
            return Utils.mixedNumberWordToLong(text);
        } catch (final NumberFormatException e) {
            throw new ParsingException("Could not parse subscriber count", e);
        }
    }

    @Override
    public String getDescription() throws ParsingException {
        assertPageFetched();
        if (ageGate != null) return null;
        try {
            if (header != null && header.headerType == HeaderType.INTERACTIVE_TABBED) {
                return getTextFromObject(header.json.getObject("description"));
            }
            return response.getObject(METADATA).getObject("channelMetadataRenderer")
                    .getString("description");
        } catch (final Exception e) {
            throw new ParsingException("Could not get channel description", e);
        }
    }

    @Override
    public String getParentChannelName() { return ""; }

    @Override
    public String getParentChannelUrl() { return ""; }

    @Nonnull
    @Override
    public List<Image> getParentChannelAvatars() { return List.of(); }

    @Override
    public boolean isVerified() throws ParsingException {
        assertPageFetched();
        if (ageGate != null) return false;
        if (header == null) throw new ParsingException("Could not get channel verification status");
        return YoutubeChannelHelper.isChannelVerified(header);
    }

    @Nonnull
    @Override
    public List<ListLinkHandler> getTabs() throws ParsingException {
        assertPageFetched();
        return ageGate == null ? channelTabs() : ageRestrictedTabs();
    }

    private List<ListLinkHandler> channelTabs() throws ParsingException {
        final JsonArray responseTabs = response.getObject(CONTENTS)
                .getObject("twoColumnBrowseResultsRenderer").getArray("tabs");
        final List<ListLinkHandler> tabs = new ArrayList<>();
        final String name = getName();
        final String url = getUrl();
        final String id = getId();
        for (final JsonObject tab : responseTabs.streamAsJsonObjects().toList()) {
            if (!tab.has(TAB_RENDERER)) continue;
            final JsonObject renderer = tab.getObject(TAB_RENDERER);
            final String tabUrl = renderer.getObject("endpoint").getObject("commandMetadata")
                    .getObject("webCommandMetadata").getString("url");
            if (tabUrl == null) continue;
            final String[] segments = tabUrl.split("/");
            final String suffix = segments[segments.length - 1];
            switch (suffix) {
                case "videos":
                    final ChannelHeader headerCopy = header == null ? null
                            : new ChannelHeader(header.json, header.headerType);
                    tabs.add(0, new ReadyChannelTabListLinkHandler(tabUrl, redirectedChannelId,
                            ChannelTabs.VIDEOS,
                            (service, linkHandler) -> new VideosTabExtractor(service, linkHandler,
                                    renderer, headerCopy, name, id, url)));
                    break;
                case "shorts":
                    addTab(tabs, ChannelTabs.SHORTS);
                    break;
                case "streams":
                    addTab(tabs, ChannelTabs.LIVESTREAMS);
                    break;
                case "releases":
                    addTab(tabs, ChannelTabs.ALBUMS);
                    break;
                case "playlists":
                    addTab(tabs, ChannelTabs.PLAYLISTS);
                    break;
                default:
                    break;
            }
        }
        return List.copyOf(tabs);
    }

    private void addTab(final List<ListLinkHandler> tabs, final String tabName) {
        try {
            tabs.add(YoutubeChannelTabLinkHandlerFactory.getInstance().fromQuery(
                    redirectedChannelId, List.of(tabName), ""));
        } catch (final ParsingException ignored) {
            // Unsupported routes are omitted from this channel's tab list.
        }
    }

    private List<ListLinkHandler> ageRestrictedTabs() throws ParsingException {
        final String channelUrl = getUrl();
        final List<ListLinkHandler> tabs = new ArrayList<>();
        for (final String tabName : List.of(ChannelTabs.VIDEOS, ChannelTabs.SHORTS,
                ChannelTabs.LIVESTREAMS)) {
            tabs.add(new ReadyChannelTabListLinkHandler(channelUrl + "/" + tabName,
                    redirectedChannelId, tabName, YoutubeChannelTabPlaylistExtractor::new));
        }
        return List.copyOf(tabs);
    }

    @Nonnull
    @Override
    public List<String> getTags() throws ParsingException {
        assertPageFetched();
        if (ageGate != null) return List.of();
        final List<String> tags = new ArrayList<>();
        for (final Object tag : response.getObject("microformat")
                .getObject("microformatDataRenderer").getArray("tags")) {
            if (tag instanceof String) tags.add((String) tag);
        }
        return List.copyOf(tags);
    }
}
