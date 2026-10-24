// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.comments.CommentsExtractor;
import com.vyonex.internal.fallback.comments.CommentsInfoItem;
import com.vyonex.internal.fallback.comments.CommentsInfoItemsCollector;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.localization.Localization;
import com.vyonex.internal.fallback.localization.TimeAgoParser;
import com.vyonex.internal.fallback.utils.JsonUtils;
import com.vyonex.internal.fallback.utils.Utils;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonWriter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getJsonPostResponse;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getTextFromObject;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.prepareDesktopJsonBuilder;

/** Fetches YouTube comment pages and maps them into VyonEx's comment API. */
public class YoutubeCommentsExtractor extends CommentsExtractor {
    private static final String VIEW_MODEL = "commentViewModel";
    private static final String RENDERER = "commentRenderer";

    private boolean commentsDisabled;
    @Nullable private JsonObject firstCommentsResponse;

    public YoutubeCommentsExtractor(final StreamingService service,
                                    final ListLinkHandler uiHandler) {
        super(service, uiHandler);
    }

    @Nonnull
    @Override
    public InfoItemsPage<CommentsInfoItem> getInitialPage() throws IOException, ExtractionException {
        return commentsDisabled ? emptyPage() : mapPage(requireCommentsResponse());
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        final Localization localization = getExtractorLocalization();
        final JsonObject videoResponse = postNext("videoId", getId(), localization);
        final String token = initialToken(videoResponse);
        commentsDisabled = token == null;
        if (commentsDisabled) return;

        firstCommentsResponse = postNext("continuation", token, localization);
    }

    @Nonnull
    private JsonObject postNext(final String key, final String value,
                                final Localization localization)
            throws IOException, ExtractionException {
        final byte[] body = JsonWriter.string(
                prepareDesktopJsonBuilder(localization, getExtractorContentCountry())
                        .value(key, value).done()).getBytes(StandardCharsets.UTF_8);
        return getJsonPostResponse("next", body, localization);
    }

    @Nullable
    private String initialToken(final JsonObject response) {
        final JsonArray sections;
        try {
            sections = JsonUtils.getArray(response,
                    "contents.twoColumnWatchNextResults.results.results.contents");
        } catch (final ParsingException ignored) {
            return null;
        }

        for (final JsonObject section : sections.streamAsJsonObjects().toList()) {
            try {
                if (!"comments-section".equals(
                        JsonUtils.getString(section, "itemSectionRenderer.targetId"))) continue;
                final JsonArray contents = JsonUtils.getArray(section, "itemSectionRenderer.contents");
                if (contents.isEmpty()) return null;
                final String token = JsonUtils.getString(contents.getObject(0),
                        "continuationItemRenderer.continuationEndpoint"
                                + ".continuationCommand.token");
                return token.isBlank() ? null : token;
            } catch (final ParsingException ignored) {
                return null;
            }
        }
        return null;
    }

    @Nonnull
    private InfoItemsPage<CommentsInfoItem> mapPage(final JsonObject response)
            throws ExtractionException {
        final CommentsInfoItemsCollector items = new CommentsInfoItemsCollector(getServiceId());
        final JsonArray entries = commentEntries(response);
        final JsonArray mutations = response.getObject("frameworkUpdates")
                .getObject("entityBatchUpdate").getArray("mutations");
        final String videoUrl = getUrl();
        final TimeAgoParser dates = getTimeAgoParser();

        final int end = hasTrailingContinuation(entries) ? entries.size() - 1 : entries.size();
        for (int index = 0; index < end; index++) {
            if (entries.get(index) instanceof JsonObject) {
                mapComment(entries.getObject(index), mutations, items, videoUrl, dates);
            }
        }
        return new InfoItemsPage<>(items, continuation(response));
    }

    @Nonnull
    private JsonArray commentEntries(final JsonObject response) {
        final JsonArray endpoints = response.getArray("onResponseReceivedEndpoints");
        for (int index = endpoints.size() - 1; index >= 0; index--) {
            final JsonObject endpoint = endpoints.getObject(index);
            final JsonObject command = endpoint.getObject("reloadContinuationItemsCommand",
                    endpoint.getObject("appendContinuationItemsAction"));
            if (command.has("continuationItems")) return command.getArray("continuationItems");
        }
        return new JsonArray();
    }

    private boolean hasTrailingContinuation(final JsonArray entries) {
        return !entries.isEmpty()
                && entries.getObject(entries.size() - 1).has("continuationItemRenderer");
    }

    @Nullable
    private Page continuation(final JsonObject response) throws ParsingException {
        final JsonArray entries = commentEntries(response);
        for (int index = entries.size() - 1; index >= 0; index--) {
            final JsonObject item = entries.getObject(index).getObject("continuationItemRenderer");
            if (item.isEmpty()) continue;
            final String path = item.has("button")
                    ? "button.buttonRenderer.command.continuationCommand.token"
                    : "continuationEndpoint.continuationCommand.token";
            try {
                final String token = JsonUtils.getString(item, path);
                return token.isBlank() ? null : new Page(getUrl(), token);
            } catch (final ParsingException ignored) {
                return null;
            }
        }
        return null;
    }

    private void mapComment(@Nonnull final JsonObject entry, @Nonnull final JsonArray mutations,
                            @Nonnull final CommentsInfoItemsCollector items,
                            @Nonnull final String videoUrl, @Nonnull final TimeAgoParser dates)
            throws ParsingException {
        final JsonObject thread = entry.getObject("commentThreadRenderer");
        if (!thread.isEmpty()) {
            final JsonObject replies = thread.getObject("replies")
                    .getObject("commentRepliesRenderer");
            if (thread.has(VIEW_MODEL)) {
                final JsonObject model = thread.getObject(VIEW_MODEL).getObject(VIEW_MODEL);
                items.commit(new YoutubeCommentsEUVMInfoItemExtractor(model, replies,
                        entityPayload(mutations, model.getString("commentKey"),
                                "commentEntityPayload"),
                        entityPayload(mutations, model.getString("toolbarStateKey"),
                                "engagementToolbarStateEntityPayload"), videoUrl, dates));
            } else if (thread.has("comment")) {
                items.commit(new YoutubeCommentsInfoItemExtractor(
                        thread.getObject("comment").getObject(RENDERER), replies,
                        videoUrl, dates));
            }
        } else if (entry.has(VIEW_MODEL)) {
            final JsonObject model = entry.getObject(VIEW_MODEL);
            items.commit(new YoutubeCommentsEUVMInfoItemExtractor(model, null,
                    entityPayload(mutations, model.getString("commentKey"),
                            "commentEntityPayload"),
                    entityPayload(mutations, model.getString("toolbarStateKey"),
                            "engagementToolbarStateEntityPayload"), videoUrl, dates));
        } else if (entry.has(RENDERER)) {
            items.commit(new YoutubeCommentsInfoItemExtractor(entry.getObject(RENDERER), null,
                    videoUrl, dates));
        }
    }

    @Nonnull
    private JsonObject entityPayload(final JsonArray mutations, @Nullable final String key,
                                     final String payloadName) throws ParsingException {
        for (final JsonObject mutation : mutations.streamAsJsonObjects().toList()) {
            if (key != null && key.equals(mutation.getString("entityKey"))) {
                return mutation.getObject("payload").getObject(payloadName);
            }
        }
        throw new ParsingException("Comment entity data is missing");
    }

    @Override
    public InfoItemsPage<CommentsInfoItem> getPage(final Page page)
            throws IOException, ExtractionException {
        if (commentsDisabled) return emptyPage();
        if (page == null || page.getId() == null || page.getId().isBlank()) {
            throw new IllegalArgumentException("Page continuation is missing");
        }
        return mapPage(postNext("continuation", page.getId(), getExtractorLocalization()));
    }

    @Nonnull
    private JsonObject requireCommentsResponse() throws ParsingException {
        if (firstCommentsResponse == null) {
            throw new ParsingException("Comments have not been fetched");
        }
        return firstCommentsResponse;
    }

    @Nonnull
    private InfoItemsPage<CommentsInfoItem> emptyPage() {
        return new InfoItemsPage<>(Collections.emptyList(), null, Collections.emptyList());
    }

    @Override public boolean isCommentsDisabled() { return commentsDisabled; }

    @Override
    public int getCommentsCount() throws ExtractionException {
        assertPageFetched();
        if (commentsDisabled) return -1;

        final JsonArray entries = commentEntries(requireCommentsResponse());
        for (final Object value : entries) {
            if (!(value instanceof JsonObject)) continue;
            final JsonObject header = ((JsonObject) value).getObject("commentsHeaderRenderer");
            if (header.isEmpty()) continue;
            final String text = getTextFromObject(header.getObject("countText"));
            try {
                return Integer.parseInt(Utils.removeNonDigitCharacters(text));
            } catch (final RuntimeException error) {
                throw new ExtractionException("Unable to read the comments count", error);
            }
        }
        throw new ExtractionException("Comments count is missing");
    }
}
