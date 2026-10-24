// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.InfoItem;
import com.vyonex.internal.fallback.MultiInfoItemsCollector;
import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.channel.tabs.ChannelTabExtractor;
import com.vyonex.internal.fallback.channel.tabs.ChannelTabs;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.localization.TimeAgoParser;
import com.vyonex.internal.fallback.services.youtube.YoutubeChannelHelper;
import com.vyonex.internal.fallback.services.youtube.YoutubeContinuationTokenParser;
import com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubeChannelTabLinkHandlerFactory;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonWriter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static com.vyonex.internal.fallback.services.youtube.YoutubeChannelHelper.getChannelResponse;
import static com.vyonex.internal.fallback.services.youtube.YoutubeChannelHelper.resolveChannelId;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.DISABLE_PRETTY_PRINT_PARAMETER;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.YOUTUBEI_V1_URL;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getJsonPostResponse;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.prepareDesktopJsonBuilder;

/** Maps channel tab responses into VyonEx items and preserves channel data across pages. */
public class YoutubeChannelTabExtractor extends ChannelTabExtractor {
    private static final String LOCKUP_ITEM = "lockupViewModel";
    @Nullable protected YoutubeChannelHelper.ChannelHeader channelHeader;
    @Nullable private JsonObject response;
    @Nullable private String channelId;

    public YoutubeChannelTabExtractor(final StreamingService service,
                                      final ListLinkHandler linkHandler) {
        super(service, linkHandler);
    }

    private String tabParameters() throws ParsingException {
        return switch (getName()) {
            case ChannelTabs.VIDEOS -> "EgZ2aWRlb3PyBgQKAjoA";
            case ChannelTabs.SHORTS -> "EgZzaG9ydHPyBgUKA5oBAA%3D%3D";
            case ChannelTabs.LIVESTREAMS -> "EgdzdHJlYW1z8gYECgJ6AA%3D%3D";
            case ChannelTabs.ALBUMS -> "EghyZWxlYXNlc_IGBQoDsgEA";
            case ChannelTabs.PLAYLISTS -> "EglwbGF5bGlzdHPyBgQKAkIA";
            default -> throw new ParsingException("Unsupported channel tab: " + getName());
        };
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        final YoutubeChannelHelper.ChannelResponseData data = getChannelResponse(
                resolveChannelId(super.getId()), tabParameters(), getExtractorLocalization(),
                getExtractorContentCountry());
        response = data.jsonResponse;
        channelHeader = YoutubeChannelHelper.getChannelHeader(response);
        channelId = data.channelId;
    }

    @Nonnull
    @Override
    public String getUrl() throws ParsingException {
        try {
            return YoutubeChannelTabLinkHandlerFactory.getInstance().getUrl(
                    "channel/" + getId(), List.of(getName()), "");
        } catch (final ParsingException ignored) {
            return super.getUrl();
        }
    }

    @Nonnull
    @Override
    public String getId() throws ParsingException {
        return YoutubeChannelHelper.getChannelId(channelHeader, requireResponse(), channelId);
    }

    protected String getChannelName() throws ParsingException {
        return YoutubeChannelHelper.getChannelName(channelHeader,
                YoutubeChannelHelper.getChannelAgeGateRenderer(requireResponse()), requireResponse());
    }

    @Nonnull
    @Override
    public InfoItemsPage<InfoItem> getInitialPage() throws IOException, ExtractionException {
        final MultiInfoItemsCollector items = new MultiInfoItemsCollector(getServiceId());
        final JsonArray initialItems = tabItems(getTabData());
        final VerifiedStatus verified = channelHeader == null ? VerifiedStatus.UNKNOWN
                : YoutubeChannelHelper.isChannelVerified(channelHeader)
                ? VerifiedStatus.VERIFIED : VerifiedStatus.UNVERIFIED;
        final String name = getChannelName();
        final String url = getUrl();
        final List<String> channelData = List.of(name, url, verified.name());
        final JsonObject continuation = collectItems(initialItems, items, verified, name, url);
        return new InfoItemsPage<>(items, nextPage(continuation, channelData));
    }

    @Nonnull
    private JsonArray tabItems(final Optional<JsonObject> tab) {
        if (tab.isEmpty()) return new JsonArray();
        final JsonObject content = tab.get().getObject("content");
        final JsonArray sections = content.getObject("sectionListRenderer").getArray("contents");
        final JsonArray rows = sections.getObject(0).getObject("itemSectionRenderer")
                .getArray("contents");
        final JsonArray grid = rows.getObject(0).getObject("gridRenderer").getArray("items");
        if (!grid.isEmpty()) return grid;
        final JsonArray richGrid = content.getObject("richGridRenderer").getArray("contents");
        return richGrid.isEmpty() ? sections : richGrid;
    }

    @Override
    public InfoItemsPage<InfoItem> getPage(final Page page)
            throws IOException, ExtractionException {
        if (page == null || page.getBody() == null) {
            throw new IllegalArgumentException("Channel continuation body is missing");
        }
        final JsonObject result = getJsonPostResponse("browse", page.getBody(),
                getExtractorLocalization());
        final JsonArray actions = result.getArray("onResponseReceivedActions");
        JsonArray entries = new JsonArray();
        for (final JsonObject action : actions.streamAsJsonObjects().toList()) {
            final JsonArray appended = action.getObject("appendContinuationItemsAction")
                    .getArray("continuationItems");
            if (!appended.isEmpty()) {
                entries = appended;
                break;
            }
        }

        final MultiInfoItemsCollector items = new MultiInfoItemsCollector(getServiceId());
        final ChannelData channelData = readChannelData(page.getIds());
        final JsonObject continuation = collectItems(entries, items, channelData.verified,
                channelData.name, channelData.url);
        final List<String> channelIds = page.getIds() == null ? List.of() : page.getIds();
        return new InfoItemsPage<>(items, nextPage(continuation, channelIds));
    }

    Optional<JsonObject> getTabData() {
        if (response == null) return Optional.empty();
        final String suffix;
        try {
            suffix = YoutubeChannelTabLinkHandlerFactory.getUrlSuffix(getName());
        } catch (final RuntimeException ignored) {
            return Optional.empty();
        }
        return response.getObject("contents").getObject("twoColumnBrowseResultsRenderer")
                .getArray("tabs").streamAsJsonObjects()
                .map(tab -> tab.getObject("tabRenderer"))
                .filter(tab -> tab.getObject("endpoint").getObject("commandMetadata")
                        .getObject("webCommandMetadata").getString("url", "").endsWith(suffix))
                .findFirst();
    }

    @Nonnull
    private JsonObject requireResponse() throws ParsingException {
        if (response == null) throw new ParsingException("Channel tab was not fetched");
        return response;
    }

    @Nullable
    private JsonObject collectItems(final JsonArray entries,
                                    final MultiInfoItemsCollector collector,
                                    final VerifiedStatus verified,
                                    @Nullable final String channelName,
                                    @Nullable final String channelUrl) {
        for (final JsonObject entry : entries.streamAsJsonObjects().toList()) {
            final JsonObject continuation = collectItem(entry, collector, verified,
                    channelName, channelUrl);
            if (continuation != null) return continuation;
        }
        return null;
    }

    @Nullable
    private JsonObject collectItem(final JsonObject entry,
                                   final MultiInfoItemsCollector collector,
                                   final VerifiedStatus verified,
                                   @Nullable final String channelName,
                                   @Nullable final String channelUrl) {
        final TimeAgoParser dates = getTimeAgoParser();
        if (entry.has("continuationItemRenderer")) return entry.getObject("continuationItemRenderer");
        if (entry.has("richItemRenderer")) {
            return collectItem(entry.getObject("richItemRenderer").getObject("content"), collector,
                    verified, channelName, channelUrl);
        }
        if (entry.has("gridVideoRenderer")) {
            commitVideo(collector, entry.getObject("gridVideoRenderer"), dates, verified,
                    channelName, channelUrl);
        } else if (entry.has("videoRenderer")) {
            commitVideo(collector, entry.getObject("videoRenderer"), dates, verified,
                    channelName, channelUrl);
        } else if (entry.has("gridPlaylistRenderer")) {
            commitPlaylist(collector, entry.getObject("gridPlaylistRenderer"), verified,
                    channelName, channelUrl);
        } else if (entry.has("playlistRenderer")) {
            commitPlaylist(collector, entry.getObject("playlistRenderer"), verified,
                    channelName, channelUrl);
        } else if (entry.has("gridShowRenderer")) {
            collector.commit(new ChannelShowItem(entry.getObject("gridShowRenderer"), verified,
                    channelName, channelUrl));
        } else if (entry.has("reelItemRenderer")) {
            commitReel(collector, entry.getObject("reelItemRenderer"), verified,
                    channelName, channelUrl);
        } else if (entry.has("shortsLockupViewModel")) {
            commitShorts(collector, entry.getObject("shortsLockupViewModel"), verified,
                    channelName, channelUrl);
        } else if (entry.has(LOCKUP_ITEM)) {
            commitLockup(collector, entry.getObject(LOCKUP_ITEM), dates, verified,
                    channelName, channelUrl);
        } else {
            for (final String childKey : List.of("shelfRenderer", "itemSectionRenderer",
                    "horizontalListRenderer", "expandedShelfContentsRenderer")) {
                if (!entry.has(childKey)) continue;
                final JsonObject container = entry.getObject(childKey);
                final JsonArray children = switch (childKey) {
                    case "shelfRenderer" -> new JsonArray(List.of(container.getObject("content")));
                    case "itemSectionRenderer" -> container.getArray("contents");
                    default -> container.getArray("items");
                };
                final JsonObject continuation = collectItems(children, collector, verified,
                        channelName, channelUrl);
                if (continuation != null) return continuation;
            }
        }
        return null;
    }

    private void commitVideo(final MultiInfoItemsCollector collector, final JsonObject video,
                             final TimeAgoParser dates, final VerifiedStatus verified,
                             @Nullable final String channelName, @Nullable final String channelUrl) {
        collector.commit(new YoutubeStreamInfoItemExtractor(video, dates) {
            @Override public String getUploaderName() throws ParsingException {
                return has(channelName) ? channelName : super.getUploaderName();
            }
            @Override public String getUploaderUrl() throws ParsingException {
                return has(channelUrl) ? channelUrl : super.getUploaderUrl();
            }
            @Override public boolean isUploaderVerified() throws ParsingException {
                return verified == VerifiedStatus.UNKNOWN ? super.isUploaderVerified()
                        : verified == VerifiedStatus.VERIFIED;
            }
        });
    }

    private void commitPlaylist(final MultiInfoItemsCollector collector, final JsonObject playlist,
                                final VerifiedStatus verified, @Nullable final String channelName,
                                @Nullable final String channelUrl) {
        collector.commit(new YoutubePlaylistInfoItemExtractor(playlist) {
            @Override public String getUploaderName() throws ParsingException {
                return has(channelName) ? channelName : super.getUploaderName();
            }
            @Override public String getUploaderUrl() throws ParsingException {
                return has(channelUrl) ? channelUrl : super.getUploaderUrl();
            }
            @Override public boolean isUploaderVerified() throws ParsingException {
                return verified == VerifiedStatus.UNKNOWN ? super.isUploaderVerified()
                        : verified == VerifiedStatus.VERIFIED;
            }
        });
    }

    private static void commitReel(final MultiInfoItemsCollector collector, final JsonObject reel,
                                   final VerifiedStatus verified, @Nullable final String name,
                                   @Nullable final String url) {
        collector.commit(new YoutubeReelInfoItemExtractor(reel) {
            @Override public String getUploaderName() throws ParsingException {
                return has(name) ? name : super.getUploaderName();
            }
            @Override public String getUploaderUrl() throws ParsingException {
                return has(url) ? url : super.getUploaderUrl();
            }
            @Override public boolean isUploaderVerified() {
                return verified == VerifiedStatus.VERIFIED;
            }
        });
    }

    private static void commitShorts(final MultiInfoItemsCollector collector,
                                     final JsonObject shorts, final VerifiedStatus verified,
                                     @Nullable final String name, @Nullable final String url) {
        collector.commit(new YoutubeShortsLockupInfoItemExtractor(shorts) {
            @Override public String getUploaderName() throws ParsingException {
                return has(name) ? name : super.getUploaderName();
            }
            @Override public String getUploaderUrl() throws ParsingException {
                return has(url) ? url : super.getUploaderUrl();
            }
            @Override public boolean isUploaderVerified() {
                return verified == VerifiedStatus.VERIFIED;
            }
        });
    }

    private void commitLockup(final MultiInfoItemsCollector collector, final JsonObject lockup,
                              final TimeAgoParser dates, final VerifiedStatus verified,
                              @Nullable final String name, @Nullable final String url) {
        switch (lockup.getString("contentType", "")) {
            case "LOCKUP_CONTENT_TYPE_PLAYLIST", "LOCKUP_CONTENT_TYPE_PODCAST" ->
                    collector.commit(new YoutubeMixOrPlaylistLockupInfoItemExtractor(lockup) {
                        @Override public String getUploaderName() throws ParsingException {
                            return has(name) ? name : super.getUploaderName();
                        }
                        @Override public String getUploaderUrl() throws ParsingException {
                            return has(url) ? url : super.getUploaderUrl();
                        }
                        @Override public boolean isUploaderVerified() throws ParsingException {
                            return verified == VerifiedStatus.UNKNOWN ? super.isUploaderVerified()
                                    : verified == VerifiedStatus.VERIFIED;
                        }
                    });
            case "LOCKUP_CONTENT_TYPE_VIDEO" -> collector.commit(
                    new YoutubeStreamInfoItemLockupExtractor(lockup, dates) {
                        @Override protected boolean isChannelOrCoursePlaylistLockupItem() {
                            return true;
                        }
                        @Override public List<Image> getUploaderAvatars() { return List.of(); }
                        @Override public String getUploaderName() throws ParsingException {
                            return has(name) ? name : super.getUploaderName();
                        }
                        @Override public String getUploaderUrl() throws ParsingException {
                            return has(url) ? url : super.getUploaderUrl();
                        }
                        @Override public boolean isUploaderVerified() {
                            return verified == VerifiedStatus.VERIFIED;
                        }
                    });
            default -> { }
        }
    }

    private static boolean has(@Nullable final String value) {
        return value != null && !value.isBlank();
    }

    @Nullable
    private Page nextPage(@Nullable final JsonObject renderer, final List<String> channelData)
            throws IOException, ExtractionException {
        if (renderer == null) return null;
        final String token = YoutubeContinuationTokenParser.INSTANCE.parse(JsonWriter.string(renderer));
        if (!has(token)) return null;
        final byte[] body = JsonWriter.string(prepareDesktopJsonBuilder(getExtractorLocalization(),
                        getExtractorContentCountry()).value("continuation", token).done())
                .getBytes(StandardCharsets.UTF_8);
        return new Page(YOUTUBEI_V1_URL + "browse?" + DISABLE_PRETTY_PRINT_PARAMETER,
                null, channelData, null, body);
    }

    private static ChannelData readChannelData(@Nullable final List<String> values) {
        if (values == null || values.size() < 3) {
            return new ChannelData(null, null, VerifiedStatus.UNKNOWN);
        }
        VerifiedStatus status;
        try {
            status = VerifiedStatus.valueOf(values.get(2));
        } catch (final RuntimeException ignored) {
            status = VerifiedStatus.UNKNOWN;
        }
        return new ChannelData(values.get(0), values.get(1), status);
    }

    private static final class ChannelData {
        @Nullable private final String name;
        @Nullable private final String url;
        private final VerifiedStatus verified;
        private ChannelData(@Nullable final String name, @Nullable final String url,
                            final VerifiedStatus verified) {
            this.name = name;
            this.url = url;
            this.verified = verified;
        }
    }

    public static final class VideosTabExtractor extends YoutubeChannelTabExtractor {
        private final JsonObject tabRenderer;
        private final String channelId;
        private final String channelName;
        private final String channelUrl;

        VideosTabExtractor(final StreamingService service, final ListLinkHandler linkHandler,
                           final JsonObject tabRenderer,
                           @Nullable final YoutubeChannelHelper.ChannelHeader channelHeader,
                           final String channelName, final String channelId,
                           final String channelUrl) {
            super(service, linkHandler);
            this.tabRenderer = tabRenderer;
            this.channelHeader = channelHeader;
            this.channelName = channelName;
            this.channelId = channelId;
            this.channelUrl = channelUrl;
        }

        @Override public void onFetchPage(@Nonnull final Downloader downloader) { }
        @Nonnull @Override public String getId() { return channelId; }
        @Nonnull @Override public String getUrl() { return channelUrl; }
        @Override protected String getChannelName() { return channelName; }
        @Override Optional<JsonObject> getTabData() { return Optional.of(tabRenderer); }
    }

    private enum VerifiedStatus { VERIFIED, UNVERIFIED, UNKNOWN }

    private static final class ChannelShowItem extends YoutubeBaseShowInfoItemExtractor {
        private final VerifiedStatus verified;
        @Nullable private final String channelName;
        @Nullable private final String channelUrl;

        private ChannelShowItem(final JsonObject renderer, final VerifiedStatus verified,
                                @Nullable final String channelName, @Nullable final String channelUrl) {
            super(renderer);
            this.verified = verified;
            this.channelName = channelName;
            this.channelUrl = channelUrl;
        }

        @Override public String getUploaderName() { return channelName; }
        @Override public String getUploaderUrl() { return channelUrl; }
        @Override public boolean isUploaderVerified() throws ParsingException {
            if (verified == VerifiedStatus.UNKNOWN) {
                throw new ParsingException("Channel verification status is unavailable");
            }
            return verified == VerifiedStatus.VERIFIED;
        }
    }
}
